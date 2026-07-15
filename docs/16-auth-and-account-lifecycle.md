# 16. Автентифікація та життєвий цикл акаунта

Як Vocabee видає та звіряє токени, як ідентифікує суб'єкта запиту (анонім / зареєстрований / premium), і як локальний анонімний користувач стає серверним акаунтом. Економіка тут не описується — див. `04-coins-economy.md`; мерж локального з серверним при вході — `06-sync-and-account-merge.md`; онбординг-розвилка входу — `02-onboarding-and-launch.md`.

Маркування поведінки: **[ЗАРАЗ]** — як у коді сьогодні; **[НОВЕ]** — затверджена зміна; **[МАЙБУТНЄ]** — відкладено.

Джерела істини (код):
- Gateway: `vocabee-gateway/src/auth/*`, `vocabee-gateway/src/db/schema/users.ts`.
- Mobile: `…/data/api/AuthResponse.kt`, `AuthTokenStore.kt`, `KtorVocabeeApi.kt`, `…/presentation/platform/GoogleAuthController.kt`, `…/domain/manager/UserSessionManager.kt`, `…/presentation/App.kt`.

---

## 16.1 Модель суб'єкта: два стани, без анонімного рядка

Vocabee має два стани користувача (загальний контекст): **anonymous** (без акаунта) і **authenticated** (Google / email).

**[ЗАРАЗ]** На сервері анонім — це «стан без авторизації на дроті», а **не** рядок у БД і **не** JWT.

- `users.ts:24` — колонка `is_anonymous boolean not null default false` існує у схемі, але жоден кодовий шлях не створює рядок з `isAnonymous: true`: і `register` (`auth.service.ts:66`), і `loginWithGoogle` (`auth.service.ts:118`) явно ставлять `isAnonymous: false`. Тобто колонка зараз фактично спляча.
- `jwt-payload.type.ts:1-11` — `JwtSubjectKind = 'user'` (єдиний вид), коментар прямо каже: «anonymous callers send no token at all». У payload немає прапора анонімності.
- `user-tier.ts:25-29` — `tierFromUserRow(null) → 'anonymous'`; коментар: «we deliberately do not store anything for anonymous users».

> ⚠️ Розбіжність із формулюванням завдання. Постановка згадує «анонімний JWT (is_anonymous)». У реальному коді анонімного JWT не існує: анонім = відсутність `Authorization` заголовка. Прапор `is_anonymous` у токені/таблиці не використовується для розрізнення анонімів — він зарезервований під майбутні «гостьові серверні» акаунти. Якщо потрібен справжній анонімний серверний рядок — це **[МАЙБУТНЄ]** (уточнити рішення).

**Узгодження з D2.** Це повністю збігається з D2: анонім живе лише локально (пробний режим 2 словники / 50 слів), без серверного представлення; економіка вмикається лише після входу.

### Визначення tier зі рядка користувача

`user-tier.ts:25-29`:

| Вхід | Tier |
|---|---|
| `user == null` (немає JWT) | `anonymous` |
| `user.isPremium == true` | `premium` |
| інакше (є рядок) | `registered` |

`UserTier = 'anonymous' | 'registered' | 'premium'`. Зараз tier впливає лише на ліміт варіантів пошуку `TIER_MAX_RESULTS` (`user-tier.ts:14-18`), і всі три значення = `50` — premium **поки що ≈ registered**.

> **Відкрите питання (premium).** `is_premium` (`users.ts:25`) і гілка `premium` існують, але жодного монетизаційного важеля немає (коментар `user-tier.ts:6-12`: «no monetisation lever until premium ships»). Що саме дає premium — **[МАЙБУТНЄ]**, уточнити.

---

## 16.2 Способи входу (gateway)

Усі ендпоінти під `@Controller('auth')` → префікс `/auth` (на дроті mobile б'є у `/v1/auth/...`, див. `KtorVocabeeApi.kt:56`).

### Email + пароль — register / login

**[ЗАРАЗ]** `auth.controller.ts:23-34`, `auth.service.ts:44-84`.

- `POST /auth/register` (`RegisterDto`): `email` (`@IsEmail`), `password` (`@MinLength(8) @MaxLength(72)`), опційні `displayName` (≤120), `speakLang`/`learnLang` (з `SUPPORTED_LANGUAGE_CODES`). Сервіс перевіряє унікальність email (lower-case), хешує bcrypt cost=12, створює рядок `isAnonymous:false`, одразу видає пару токенів. Дублікат email → `409 ConflictException`.
- `POST /auth/login` (`LoginDto`): `email` + `password` (`@MinLength(1)`). Якщо немає рядка або немає `passwordHash` (наприклад, акаунт лише через Google) або bcrypt не збігся → `401 UnauthorizedException('Invalid credentials')` (однакове повідомлення в обох випадках — не розкриває існування email).

### Google ID-token

**[ЗАРАЗ]** `auth.controller.ts:36-41`, `auth.service.ts:86-128`.

`POST /auth/google` (`GoogleAuthDto`): `idToken` (обов'язковий) + опційні `speakLang`/`learnLang`.

Перевірка токена (`verifyGoogleIdToken`, `auth.service.ts:210-250`): тягне `https://oauth2.googleapis.com/tokeninfo?id_token=…`, вимагає `aud == auth.googleClientId`, `iss ∈ {accounts.google.com, https://accounts.google.com}`, і якщо email присутній — `email_verified === true`. Якщо Google не сконфігуровано на gateway → `503 ServiceUnavailableException`.

Логіка прив'язки акаунта (важливо — **колізія email = реюз існуючого рядка**):

1. Шукаємо `oauth_accounts` за (`provider='google'`, `providerAccountId=sub`). Якщо знайдено — одразу видаємо токени для `linkedAccount.userId` (`auth.service.ts:93-104`).
2. Інакше шукаємо `users` за email (lower-case). **Якщо рядок із таким email уже існує — переюзуємо його** (`existingUsers[0] ?? create(...)`, `auth.service.ts:107-119`), не створюючи дубль.
3. До знайденого/створеного користувача додаємо запис у `oauth_accounts` (`auth.service.ts:121-125`) і видаємо токени.

> Наслідок: акаунт, заведений через email+пароль, при першому Google-вході з тим самим email **зливається в один рядок** (Google лінкується до існуючого user), без другого рядка та без втрати даних. Справжній «account merge» різних акаунтів — **[МАЙБУТНЄ]**, див. D9 і `06-sync-and-account-merge.md`.

---

## 16.3 Токени: видача та ротація

**[ЗАРАЗ]** `issueTokens` (`auth.service.ts:166-194`):

- **Access JWT**: підписаний `auth.accessSecret`, TTL `auth.accessTtl` (напр. `15m`). Payload = `{ sub, kind:'user', jti? }`.
- **Refresh token**: окремий JWT, підписаний `auth.refreshSecret`, TTL `auth.refreshTtl` (напр. `30d`), з вкладеним `jti = randomBytes(48)`. У БД зберігається **лише SHA-256 хеш** (`AuthService.hashToken`, `auth.service.ts:206-208`) у `refresh_tokens` (`users.ts:39-59`) разом із `expiresAt`. Сирий refresh у БД не лежить.
- Відповідь `AuthTokens = { accessToken, refreshToken, expiresIn }`, де `expiresIn` — секунди життя **access**-токена.

### Ротація refresh

`POST /auth/refresh` (`RefreshDto.refreshToken`, `@MinLength(1)`) → `auth.service.ts:130-156`:

1. Криптоверифікація підпису refresh (`verifyRefreshToken`); невалідний → `401`.
2. Пошук рядка в `refresh_tokens` за `tokenHash`, де `revokedAt IS NULL` і `expiresAt > now`. Якщо немає рядка або `row.userId != decoded.sub` → `401 Invalid refresh token`.
3. **Revoke пред'явленого** (`set revokedAt = now`) і **видача нової пари** (`issueTokens`). Тобто rotation: один refresh = одне використання.

> Reuse-detection відсутній: повторне пред'явлення вже відкликаного refresh просто отримає `401` (рядок уже `revoked`), без каскадного відкликання сімейства токенів. Підсилення (виявлення крадіжки) — **[МАЙБУТНЄ]**.

### Вихід (logout / revoke)

`POST /auth/logout` (`@HttpCode(204)`, `RefreshDto`) → `auth.service.ts:158-164`: ставить `revokedAt = now` для рядка з відповідним `tokenHash`. Ідемпотентно (no-op якщо рядка немає). Дані користувача **не чіпає** — лише відкликає refresh.

---

## 16.4 Перевірка доступу: стратегія та guard'и (gateway)

### JWT-стратегія

**[ЗАРАЗ]** `jwt-access.strategy.ts` (`'jwt-access'`): `ExtractJwt.fromAuthHeaderAsBearerToken()`, `ignoreExpiration:false`, secret `auth.accessSecret`. У `validate(payload)` тягне `usersService.findById(payload.sub)`; якщо рядка нема → `401 'Subject not found'`; інакше кладе в `req.user` об'єкт `AuthenticatedUser = { id, kind, isAnonymous, speakLang, learnLang }` (`authenticated-user.ts`). Тобто `isAnonymous` в `req.user` береться зі **свіжого рядка БД**, а не з токена.

### Guard'и

| Guard | Поведінка | Де застосований |
|---|---|---|
| `JwtAccessGuard` (`jwt-access.guard.ts`) | Класичний: немає/невалідний/прострочений токен → **401**. | `auth.controller.ts:58` (`GET /auth/me`); `users.controller.ts:13`; `wallet.controller.ts:13`; `topics.controller.ts:26`. |
| `OptionalJwtAccessGuard` (`optional-jwt.guard.ts`) | Прострочений/відсутній/невалідний токен → `req.user = null`, віддає **200** (не 401). Працює і для аноніма, і для автентифікованого. | `client-search/client-search.controller.ts` (`/search`). |
| `RegisteredUserGuard` (`registered-user.guard.ts`) | Після `JwtAccessGuard`: `!user` → 403 `Authentication required`; `user.isAnonymous` → 403 `A registered account is required`. | **[ЗАРАЗ] не застосований ніде** — клас визначений, але жоден контролер його не вішає (grep по `src` дає лише саме визначення). Зарезервований під майбутні маршрути, що вимагають справжнього зареєстрованого. Оскільки серверних анонімних рядків зараз нема, його `isAnonymous`-гілка de-facto мертва, поки D2/[МАЙБУТНЄ] не введе гостьові акаунти. |

### OptionalJwt і прострочений токен

**[ЗАРАЗ]** Ключова деталь `optional-jwt.guard.ts:14-34`: `canActivate` ловить будь-яку помилку Passport (включно з простроченим access) у `try/catch`, ставить `request.user = null` і повертає `true`; `handleRequest` при `err || !user` повертає `null` замість кидати. Декоратор `@OptionalUser()` (`optional-user.decorator.ts`) повертає `req.user ?? null`. Підсумок: на `/search` **прострочений токен деградує до аноніма (200)**, а не валить запит 401 — анонім і «протух» бачать однаковий результат (`tierFromUserRow(null)='anonymous'`).

> Контраст: на захищених `JwtAccessGuard`-маршрутах прострочений access → 401, і клієнт має зробити refresh (див. 16.6 startup-sync).

### Таблиця: ендпоінт → потрібен суб'єкт → нотатки

| Ендпоінт | Guard | Потрібен суб'єкт | Нотатки |
|---|---|---|---|
| `POST /auth/register` | — | будь-хто (публічний) | 409 при дублі email; видає пару токенів. |
| `POST /auth/login` | — | будь-хто (публічний) | 401 `Invalid credentials` (єдине повідомлення). |
| `POST /auth/google` | — | будь-хто (публічний) | Реюз рядка за email; лінк `oauth_accounts`. 503 якщо Google не налаштовано. |
| `POST /auth/refresh` | — (валідує сам сервіс) | валідний refresh | Rotation: revoke старого + нова пара; 401 на невалід/прострочений/revoked. |
| `POST /auth/logout` | — | будь-який refresh | 204; revoke за хешем; ідемпотентно. |
| `GET /auth/me` | `JwtAccessGuard` | зареєстрований (будь-який рядок) | 401 без/прострочений токен. |
| `PATCH /me`, `GET /me` (users) | `JwtAccessGuard` | зареєстрований | Профіль/налаштування. |
| `… /wallet/*` | `JwtAccessGuard` | зареєстрований | Економіка лише для авторизованих (узгоджено з D1/D2). |
| `… /topics/*`, `/topics/sync*` | `JwtAccessGuard` | зареєстрований | Sync словників (D9, doc 06). |
| `GET /search` (lexicon) | `OptionalJwtAccessGuard` | анонім **або** зареєстрований | Прострочений/відсутній токен → анонім, 200. Tier → `TIER_MAX_RESULTS` (зараз усі 50). |

> **[МАЙБУТНЄ]** маршрути, що мають бути недоступні анонімам як справжнім гостям, отримають `RegisteredUserGuard` поверх `JwtAccessGuard`.

---

## 16.5 Mobile: зберігання токенів і виклики

### AuthTokenStore

**[ЗАРАЗ]** `AuthTokenStore.kt`:

- Єдине джерело правди для bearer-токена. `token: StateFlow<String?>` — реактивний потік **access**-токена (`asStateFlow`), ініціалізований з `preferencesManager.accessToken`.
- `current()` — синхронно звіряє зі сховищем (re-read з prefs, оновлює `StateFlow` якщо розійшлося) і повертає актуальний access. Саме `current()` викликається перед кожним bearer-запитом у `KtorVocabeeApi` (`tokenStore.current()?.let { bearerAuth(it) }`).
- `refreshToken()` — повертає **refresh** з prefs (refresh у `StateFlow` не тримається — він не потрібен реактивно).
- `set(AuthTokensResponse)` — пише `refreshToken` у prefs і виставляє новий `accessToken`. `clear()` — гасить обидва токени і `StateFlow=null`.
- Refresh зберігається в `PreferencesManager` (`PreferencesManager.kt:34-36`: `accessToken`, `refreshToken`; також `currentUserId`, `lastAuthenticatedUserId`, `lastSyncAt`, `localRevisionEpochMillis`).

### Виклики API (KtorVocabeeApi)

**[ЗАРАЗ]**: `loginWithGoogle` → `POST /v1/auth/google`, у відповідь `tokenStore.set(tokens)` (`KtorVocabeeApi.kt:50-76`). `refreshSession` → `POST /v1/auth/refresh`, теж `tokenStore.set(tokens)` (`:110-126`). `currentUser` → `GET /v1/auth/me` з bearer (`:78-91`). Захищені виклики (`/me`, `/topics/sync*`, `/wallet/*`) додають bearer через `tokenStore.current()`.

> **Розрив: mobile не кличе `/auth/logout`.** Серед методів `VocabeeApi`/`KtorVocabeeApi` немає виклику `logout` — у клієнта немає `revoke` на дроті. Sign-out локально лише чистить prefs (16.7). Тобто **refresh-токен на сервері залишається валідним до природного протермінування** після виходу. Узгодити: чи додавати клієнтський виклик `/auth/logout` при sign-out — **[НОВЕ]/уточнити**.

### Google на клієнті

**[ЗАРАЗ]** `GoogleAuthController.kt` (commonMain): інтерфейс `requestIdToken(): GoogleAuthResult` з варіантами `Success(idToken) / NotConfigured / Cancelled / Failure(message)`; дефолт `NoGoogleAuthController` повертає `NotConfigured`. Платформова реалізація (Android) інжектиться зовні; цей файл лише контракт. Отриманий `idToken` віддається на `/auth/google` (16.2).

### UserSessionManager

**[ЗАРАЗ]** `UserSessionManager.kt`: інтерфейс із одним `currentUserKey: String`. `StaticUserSessionManager` → завжди `DEFAULT_LOCAL_USER_KEY`. `PreferencesUserSessionManager` → `preferencesManager.currentUserId ?: DEFAULT_LOCAL_USER_KEY`. Тобто **локальний неймспейс даних**: поки немає `currentUserId`, дані лежать під ключем «локальний/анонімний» користувач; після входу `currentUserId` стає серверним id, і дані переносяться під нього (16.6).

---

## 16.6 Життєвий цикл: анонім → зареєстрований

**[ЗАРАЗ]** Послідовність у `App.kt` `startGoogleSignIn()` (`:493-554`) + `signInWithGoogle()` (`:212-260`):

1. До входу фіксуємо локальний анонімний стан: `hasLocalAnonymousVocabulary()`, `localAnonymousWordCount()`, `LocalProfileSyncState` (мови, нотифікації, тема) — `App.kt:498-505`.
2. `signInWithGoogle`: `requestIdToken()` → `/auth/google` → `currentUser()` → `store.onEvent(ApplyAuthenticatedAccount(...))` (id, displayName/email, мови, налаштування, `beeBalance`).
3. Після успіху тягнемо `api.syncTopics(null)` (серверний знімок) і вирішуємо розвилку:
   - **Є локальні слова І сервер має дані** → `pendingSyncConflict` + `sheet = SyncConflict` (снекбар «Потрібно обрати стан синхронізації»). Це справжній мерж — повна логіка/варіанти у **`06-sync-and-account-merge.md`** (D9).
   - **Є локальні, сервер порожній** → `moveAnonymousVocabularyToCurrentUser()` + `pushProfileSettings(localProfileBeforeSignIn)` + `syncVocabularyNow(replaceServerState=false)` → «Акаунт синхронізовано».
   - **Локальних нема** → `applyServerSnapshot(serverSnapshot)` → «Акаунт синхронізовано».
4. Помилки оформлюються через `profileAuthNotice` + снекбар.

Ключове: для аноніма **сервер рядок не заводиться заздалегідь** (16.1) — рядок з'являється рівно в момент `loginWithGoogle`/`register`. Локальні дані аноніма переносяться під новий `currentUserId` (16.5), без втрати (узгоджено з D2/D3/D9). Економіка (баланс, ліміти) до цього моменту локальна-пробна, після входу стає серверно-авторитетною (D1, doc 04).

---

## 16.7 Startup-sync (холодний старт)

**[ЗАРАЗ]** `App.kt` `runStartupSync()` (`:451-491`), викликається з `LaunchedEffect(Unit)` (`:556-558`):

1. Якщо немає ні `accessToken`, ні `refreshToken` у prefs → ранній вихід (нема що синхронити, лишаємось локально/анонімно).
2. Якщо є `refreshToken` → `backend.refreshSession(refreshToken)` (ротація: новий access+refresh у store, 16.3/16.5).
3. `backend.currentUser()` → `ApplyAuthenticatedAccount(...)` (підтягуємо профіль і `beeBalance` з сервера).
4. Якщо є незалиті локальні зміни (`localRevisionEpochMillis > 0`) → `syncVocabularyNow()` (заливаємо локальне). Інакше — інкрементальний `syncTopics(since=lastSyncAt)`; за наявності змін застосовуємо знімок (`since==null` → повний).
5. Будь-яка помилка ковтається (`catch (_) {}`): лишаємось офлайн/локально, явний вхід покаже помилки.

> Тобто на кожному старті прострочений access відновлюється через refresh **до** першого захищеного запиту; завдяки 16.4 `/search` працює навіть якщо refresh не вдався (деградація до аноніма).

---

## 16.8 Вихід (sign-out) на клієнті

**[ЗАРАЗ]** Дві точки в `App.kt` роблять однакове:

- Кнопка профілю `onLogoutClick` (`:760-764`): `preferencesManager.accessToken = null`; `preferencesManager.refreshToken = null`; `store.signOutKeepLastUserState()`.
- Гілка sync-конфлікту «інший email» `clearAuthenticatedSessionForAnotherEmail()` (`:442-449`): додатково чистить `currentUserId`, скидає `pendingSyncConflict`/`sheet`, теж `signOutKeepLastUserState()`.

`signOutKeepLastUserState()` — **дані лишаються** (узгоджено з D3: вихід не видаляє словники; локальні дані зберігаються під попереднім ключем користувача). Очищаються лише токени та активна сесія.

> ⚠️ Як зазначено в 16.5, клієнтський вихід **не кличе `/auth/logout`** → серверний refresh-рядок не відкликається. Gateway вміє revoke (16.3), але mobile цей виклик не робить. Це безпековий розрив для сценарію «вийшов на чужому пристрої». Рішення (додати виклик `/auth/logout` з `refreshToken` перед його очищенням) — **[НОВЕ]/уточнити**.

---

## 16.9 Зведення відкритих питань

| # | Питання | Стан |
|---|---|---|
| O1 | Що дає `premium` (зараз ≡ registered, усі ліміти 50) | **[МАЙБУТНЄ]**, уточнити |
| O2 | Справжній анонімний серверний рядок / гостьовий акаунт (колонка `is_anonymous` зараз спляча) | **[МАЙБУТНЄ]** |
| O3 | `RegisteredUserGuard` визначений, але не застосований — на яких маршрутах вмикати | **[МАЙБУТНЄ]** |
| O4 | Mobile sign-out не кличе `/auth/logout` (refresh не revoke) | **[НОВЕ]/уточнити** |
| O5 | Reuse-detection для refresh (каскадне відкликання при крадіжці) | **[МАЙБУТНЄ]** |
| O6 | Account merge різних акаунтів (не лише email-колізія) | **[МАЙБУТНЄ]**, див. D9 / doc 06 |

---

### Перехресні посилання
- Економіка та сервер-авторитетність (D1): `04-coins-economy.md`.
- Пробний режим аноніма (D2): `00-overview-and-decisions.md`, `02-onboarding-and-launch.md`.
- Sync, конфлікт і мерж при вході (D9): `06-sync-and-account-merge.md`.
- Видалення / збереження даних при виході (D3): `07-deletion.md`.
- Онбординг-розвилка входу (D5): `02-onboarding-and-launch.md`.
