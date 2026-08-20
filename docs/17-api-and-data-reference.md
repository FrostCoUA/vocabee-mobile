# 17 · Довідник API та даних

Повний технічний довідник: ендпоінти `client-gateway` / `dictionary-gateway`, доменні моделі клієнта, схема даних (Room + Postgres) і sync-контракт. Стан кожної поведінки позначено **[ЗАРАЗ]** (як у коді) / **[НОВЕ]** (затверджена зміна) / **[МАЙБУТНЄ]**.

Не дублює: економіку — див. `04-coins-economy.md`; promo — `05-promo-api-and-banners.md`; sync+мерж — `06-sync-and-account-merge.md`; видалення — `07-deletion.md`; мови/мовлення/теми — `08-languages-speech-themes.md`; тренування — `11-practice-training.md`.

**[ЗАРАЗ]** Бекенд стартує як два NestJS-процеси: `client-gateway` (`:3000`) і
`dictionary-gateway` (`:3001`). Спільний bootstrap задає обом глобальний префікс
`v1` (`vocabee-gateway/src/common/bootstrap-gateway.ts`). Нижче поточні шляхи
наведено повністю (`/v1/...`); цільові v2-контракти позначено окремо.

---

## 1. Ендпоінти gateway

### 1.0 Межі сервісів і rollout-контракт

| Компонент | **[ЗАРАЗ]** реалізовано | **[НОВЕ] / [МАЙБУТНЄ]** |
|---|---|---|
| `client-gateway` | mobile API, users/auth, wallet, rewarded ads, premium, topics, saved words, sync, compatibility search facade і захищений client-admin API | lookup receipts, feedback outbox, D11 save-time economy |
| `dictionary-gateway` | app-neutral directional search, lexicon, translation generation/cache, DB-backed consumer keys/quotas, consumer/key lifecycle, dashboard/lexicon/provider-status/usage/audit admin API, soft-delete/restore останньої ревізії, reviewed JSONL import із дедуплікацією, quality feedback із пороговою AI-регенерацією | повна immutable історія всіх ревізій, provider-secret write API, optional MCP |
| `client-admin-web` | Реалізований у source: users/premium/sessions/admin accounts/audit через `client-gateway`, server-side фільтр словників за парою мов, без прямого DB-доступу | **[НОВЕ]** розділ «Економіка» з versioned spend/reward/market policy (D13); independent container, Compose wiring і deployed-browser verification |
| `dictionary-admin-web` | dashboard, active/deleted/all translations, language/provenance dropdown-фільтри, повна lexical detail, soft-delete/restore з причиною, provider status, consumers+keys/usage/audit, reviewed JSONL import із послідовною чергою файлів через `dictionary-gateway` (черга сортується за номером батчу, окремі файли можна видалити до аплоаду; поточний файл і результат/помилка показуються над чергою), quality score і admin dislike з коментарем | **[МАЙБУТНЄ]** provider-secret/full-revision-history/feedback export/MCP UI |

**[ЗАРАЗ]** Мобільний застосунок викликає **лише `client-gateway`**.
`ClientSearchController` делегує пошук через `DictionaryClientService` у
`dictionary-gateway` по внутрішньому HTTP з `X-API-Key`; ключ ніколи не потрапляє у
мобільний клієнт. Код підтримує protected database-backed ключ consumer
`client-gateway` на плані `system-unlimited`, але live cutover ще не виконаний:
поточний Compose передає обом gateway один literal `DICTIONARY_APP_API_KEY` і тому
працює через legacy fallback. Dictionary API app-neutral: без user id, app JWT, email,
premium або балансу.

### 1.0.1 Адреса gateway за build-конфігурацією

**[ЗАРАЗ]** Мобільні build-конфігурації розділяють середовища:

| Платформа / конфігурація | `client-gateway` |
|---|---|
| Android `debug` | `https://dev-api.vocabee.online` |
| Android `release` | `https://api.vocabee.online` |
| iOS `Debug` | `https://dev-api.vocabee.online` |
| iOS `Release` | `https://api.vocabee.online` |

Android може перевизначити адреси через `vocabee.api.devBaseUrl` /
`vocabee.api.prodBaseUrl` у gitignored `local.properties` або через
`VOCABEE_DEV_API_BASE_URL` / `VOCABEE_PROD_API_BASE_URL`. iOS бере адресу з
`VocabeeApiBaseUrl` у `Info.plist`, яку Xcode підставляє з build setting.
Окремі локальні LAN-адреси не є default і мають задаватися лише явним override.

**[ЗАРАЗ]** Зовнішні consumers/ключі лежать у БД, raw key повертається лише один раз,
ротація має default overlap 15 хв, а `external-standard` quota = 60/min + 1000/UTC-day
на consumer (зміна ключа quota не скидає). DB-backed lifecycle є **[ЗАРАЗ]**
можливістю коду; фактичний Compose deployment усе ще legacy-key і не є доказом
cutover.

```http
GET  client-gateway /v1/search                         # compatibility facade
GET  dictionary-gateway /v1/search                    # X-API-Key, app-neutral
POST client-gateway /v2/translation-lookups           # [НОВЕ] free lookup receipt
POST client-gateway /v2/topics/{topicId}/words/from-result # [НОВЕ]
POST client-gateway /v1/translation-feedback          # [ЗАРАЗ] user quality report
POST dictionary-gateway /v1/quality-feedback          # [ЗАРАЗ] internal client→dictionary hop
POST dictionary-gateway /v1/admin/lexicon/quality-feedback # [ЗАРАЗ] admin +100 report
```

Legacy v1 pricing лишається **без змін лише на час rollout**. D11 (`searchCost=0`,
charge за saved result) не можна enforce до релізу сумісного v2-клієнта.
**[ЗАРАЗ]** Обидва gateway мають власні Swagger UI `/docs`, OpenAPI JSON
`/openapi.json` і `/v1/health`; client Swagger описує окремі mobile
`access-token` та administrator `admin-access-token` bearer schemes, dictionary
Swagger — `X-API-Key` для search і окремий `admin-access-token` для dictionary-admin
операцій. Для Optional JWT операцій `/search` і `/support` client
OpenAPI описує дві `security`-альтернативи: `{}` (анонім) або
`{ "access-token": [] }` (bearer), а не обов'язкову авторизацію.

Swagger UI обох gateway **[ЗАРАЗ]** плавно показує вміст tag, endpoint і блок
`Try it out`/`Execute` за реальною висотою; при `prefers-reduced-motion: reduce`
контент з'являється без анімації. Collapse лишається миттєвим, бо Swagger синхронно
демонтує закритий блок.

Swagger UI обох gateway **[ЗАРАЗ]** брендований під «Vocabee Redesign»
(`swagger-ui-theme.ts`): чорнильний topbar зі знаком і словомаркою «Vocabee API»,
шрифти Manrope/JetBrains Mono (Google Fonts; CSP gateway дозволяє
`fonts.googleapis.com` у style-src і `fonts.gstatic.com` у font-src), сірі пілюлі
версії, «боксові» tag-секції і скруглені method-бейджі зі стандартними кольорами
методів; favicon — знак із трьох сот (data URI). Вбудована темна тема Swagger UI
вимкнена (тогл прихований, клас `dark-mode` знімається скриптом) — дизайн
світлий; заголовок вкладки = назва API (`customSiteTitle`).

Контракт «суб'єкт» нижче:
- **public** — unguarded endpoint без серверного auth-суб'єкта; `Authorization` не інтерпретується (health, auth credential exchange, languages).
- **Optional JWT** — `/search` і `/support`: без властивості `Authorization` guard ставить `user=null`; якщо заголовок передано, credential мусить бути валідним і належати `active` user, інакше 401 (empty/malformed/expired/inactive не деградують до аноніма).
- **JWT** — обов'язковий `Authorization: Bearer <accessToken>`, гард `JwtAccessGuard`; access і refresh runtime-валідують object payload, UUID `sub` і `kind='user'` до DB lookup, а strategy потім звіряє `users.account_status='active'`; `@CurrentUser()` дає `{ id }`.
- **Admin JWT** — окремий RS256 bearer: issuer
  `vocabee-client-gateway`, audience `vocabee-admin`, `typ='admin'`, pinned `kid`,
  opaque `sub`, fixed role і role-bounded scopes. Він не є mobile JWT, не містить
  email і не має refresh-flow. `AdminAccessGuard` перевіряє credential, потім
  `AdminScopesGuard` — точний scope операції.
  > **[НОВЕ] Довгоживуча адмін-сесія.** Оскільки refresh-flow немає, TTL токена і
  > є тривалістю сесії. `ADMIN_JWT_TTL` за замовчуванням **`365d`** (стеля, яку
  > приймає браузерний клієнт: `expiresIn ≤ 31 536 000`), а сама сесія
  > зберігається у `localStorage` під ключем `vocabee.admin.session.v1.<apiOrigin>`,
  > щоб перезавантаження сторінки не вимагало повторного входу через Google.
  > **Свідомий компроміс власника**, а не недогляд: адмін-bearer із правами на
  > запис лежить у сховищі origin і читається будь-яким XSS на цьому домені,
  > протягом до року. Ослаблення прийнято для персональної адмінки з єдиним
  > адміністратором. Скидається при `signOut` і при першій же 401-відповіді.
  > Якщо адмінка колись стане багатокористувацькою — потрібен серверний
  > відкликуваний refresh-flow з HttpOnly-кукою замість цього.
- **Dictionary consumer key** — opaque `X-API-Key` з allowlisted consumer scopes.
  Звичайні search keys не авторизують `/admin`; єдина виняткова машинна route
  `POST /v1/admin/lexicon/import-v2` вимагає одночасно scope
  `dictionary:lexicon:write` і fixed identity захищеного `translation-uploader`.
  Admin JWT, навпаки, не авторизує ordinary Dictionary search або цю upload route.

Внутрішні auth exceptions (`Invalid credentials`, `Account is not active`,
`Invalid refresh token`, bare `Unauthorized`) не є wire-контрактом. Глобальний
`ApiExceptionFilter` повертає для звичайного 401 стабільне тіло
`{ "statusCode": 401, "errorType": "unauthorized", "message": "Потрібна повторна авторизація.", "requestId": "…" }`; malformed claims не відлунюються клієнту.

### 1.1 Health

| Метод | Шлях | Суб'єкт | Призначення | Відповідь |
|---|---|---|---|---|
| GET | `/v1/health` | public | Liveness-пінг | `{ status: 'ok', ts: ISO8601 }` |

`vocabee-gateway/src/common/health.controller.ts:7`.

### 1.2 Auth (`/v1/auth`)

`vocabee-gateway/src/auth/auth.controller.ts`.

| Метод | Шлях | Суб'єкт | Код | Призначення | Запит (DTO) | Відповідь |
|---|---|---|---|---|---|---|
| POST | `/v1/auth/register` | public | 201 | Створити акаунт email+пароль | `RegisterDto` | `AuthTokens` |
| POST | `/v1/auth/login` | public | 200 | Обмін email+пароль на токени | `LoginDto` | `AuthTokens` |
| POST | `/v1/auth/google` | public | 200 | Обмін Google ID-token на токени Vocabee | `GoogleAuthDto` | `AuthTokens` |
| POST | `/v1/auth/refresh` | public | 200 | Ротація refresh-токена | `RefreshDto` | `AuthTokens` |
| POST | `/v1/auth/logout` | public | 204 | Відкликати refresh-токен | `RefreshDto` | — |
| GET | `/v1/auth/me` | JWT | 200 | Поточний активний авторизований суб'єкт | — | `UserResponseDto` |

`AuthTokens` (форма): `{ accessToken, refreshToken, expiresIn }` — дзеркало клієнтського `AuthTokensResponse` (`AuthResponse.kt:6`).

**DTO:**
- `RegisterDto` (`register.dto.ts`): `email` (IsEmail), `password` (8..72), `displayName?` (≤120), `speakLang?`/`learnLang?` (IsIn кодів мов).
- `LoginDto` (`login.dto.ts`): `email`, `password` (MinLength 1).
- `GoogleAuthDto` (`google-auth.dto.ts`): `idToken` (NotEmpty), `speakLang?`, `learnLang?`.
- `RefreshDto` (`refresh.dto.ts`): `refreshToken` (MinLength 1).

**[ЗАРАЗ] Account-status enforcement:** `login` завжди робить рівно один cost-12
`bcrypt.compare` (реальний hash або фіксований валідний dummy для unknown/no-hash)
і викликає active-check лише після успішної password verification.
Linked/existing-email Google user знаходиться case-insensitively через
`lower(users.email) = normalizedEmail` та перевіряється до OAuth-link/token issue;
`refresh` валідує claims і user до revoke/rotation; access strategy валідує claims
до DB та перевіряє статус на кожному bearer-запиті. Центральний active-gate для
відсутнього, `banned` і `deactivated` акаунта має однаковий внутрішній exception
`Account is not active`; невідомий email/no-hash/хибний password до цього gate має
внутрішній `Invalid credentials`. Wire-body обох нормалізує глобальний фільтр, як
описано вище. Нові register/Google users отримують схемний дефолт `active`.

> **[ЗАРАЗ] Розбіжності з контекстом завдання:**
> - **Немає `/auth/anonymous`.** Анонімність = відсутність JWT (міграція `0002_premium_flag.sql`: «anonymous = no JWT»; рядок у `users` для аноніма не створюється). Це збігається з рішенням **D2**.
> - Мобільний клієнт сьогодні викликає лише `/auth/google`, `/auth/me`, `/auth/refresh` (`KtorVocabeeApi.kt:56,80,112`). `register`/`login`/`logout` існують на бекенді, але клієнтом не використовуються — вхід через Google (**D5**).

### 1.3 Users (`/v1/me`)

`vocabee-gateway/src/users/users.controller.ts` — увесь контролер під `JwtAccessGuard`.

| Метод | Шлях | Суб'єкт | Призначення | Запит | Відповідь |
|---|---|---|---|---|---|
| GET | `/v1/me` | JWT | Профіль поточного користувача | — | `UserResponseDto` |
| PATCH | `/v1/me` | JWT | Оновити профіль | `UpdateProfileDto` | `UserResponseDto` |

`UpdateProfileDto` (`update-profile.dto.ts`) — усі поля опціональні: `displayName?` (≤120), `speakLang?`, `learnLang?` (IsIn кодів), `notificationsEnabled?`, `darkThemeEnabled?`.

> Клієнтський `UpdateProfileRequest` (`SyncDtos.kt:7`) надсилає лише `speakLang/learnLang/notificationsEnabled/darkThemeEnabled` (без `displayName`).

**`UserResponseDto`** (`user-response.dto.ts`) — спільна відповідь для `/me`, `/auth/me`, `/wallet*`:

| Поле | Тип | Нотатка |
|---|---|---|
| `id` | string | UUID |
| `email` | string \| null | null для майбутніх анонімних рядків |
| `displayName` | string \| null | |
| `speakLang` | string | дефолт `uk` |
| `learnLang` | string | дефолт `en` |
| `notificationsEnabled` | boolean | |
| `darkThemeEnabled` | boolean | |
| `isAnonymous` | boolean | дормантне поле (див. `0002`) |
| `isPremium` | boolean | впливає на tier пошуку |
| `beeBalance` | number | баланс монеток (**D1**) |
| `accountStatus` | `active` \| `banned` \| `deactivated` | additive public status; credential paths приймають лише `active` |
| `createdAt` / `updatedAt` | string | ISO8601 |

`passwordHash`, `referralCode`, `statusReason`, `statusChangedAt` і
`statusChangedBy` у `UserResponseDto` не потрапляють.

**Referral (`GET /v1/referral/me`, JWT).** Відповідь:
`{ code, link, rewardBees }`, де `link = https://vocabee.app/i/<code>`, а
`rewardBees = 50`. Це джерело суми для invite UI/share-copy; саме deferred
deep-link зіставлення друга й фактичний credit обом ще [МАЙБУТНЄ].

### 1.3.1 Administrator identity + client administration (`/v1/admin`)

`vocabee-gateway/src/client-admin/*`, `src/admin-auth/*`. **[ЗАРАЗ]** Це окремий
credential domain, не розширення mobile user JWT.

| Метод | Шлях | Суб'єкт / scope | Код | Призначення |
|---|---|---|---|---|
| POST | `/v1/admin/auth/google` | public Google exchange | 200 | Google ID token → RS256 admin bearer (TTL = ADMIN_JWT_TTL, деф. 365d), лише для env/DB allowlist |
| GET | `/v1/admin/auth/jwks.json` | public | 200 | RS256 public JWK; private key не виходить із `client-gateway` |
| GET | `/v1/admin/me` | Admin JWT | 200 | `{issuer,subject,role,scopes}` без email |
| GET | `/v1/admin/dashboard` | `client:dashboard:read` | 200 | Агрегати users/status/premium/topics/words/reward events; **[НОВЕ D14]** headline wallet balances/spend |
| GET | `/v1/admin/users` | `client:users:read` | 200 | Cursor-list; `q?`, `status?`, `premium?`, `limit?`, `cursor?` |
| GET | `/v1/admin/users/:userId` | `client:users:read` | 200 | Allowlisted user summary |
| GET | `/v1/admin/users/:userId/topics` | `client:users:read` | 200 | Словники власника; optional validated `sourceLang?`/`targetLang?` застосовуються в DB до cursor pagination |
| GET | `/v1/admin/users/:userId/topics/:topicId/words` | `client:users:read` | 200 | Слова лише user-owned словника |
| GET | `/v1/admin/users/:userId/rewarded-ad-events` | `client:users:read` | 200 | Append-only +10 history, без `requestId` у DTO |
| GET | `/v1/admin/users/:userId/sessions` | `client:users:read` | 200 | Session id/timestamps та UA/IP, якщо є; без token hash |
| GET | `/v1/admin/admin-accounts` | `admin:manage` | 200 | Immutable env context + окремо cursor-page DB admins |
| GET | `/v1/admin/audit-events` | `client:audit:read` | 200 | Append-only журнал адмін-мутацій |
| GET | `/v1/admin/economy/policies/active` | `client:economy:read` | 200 | Активна versioned economy policy **[НОВЕ D13]** |
| GET | `/v1/admin/economy/policies` | `client:economy:read` | 200 | Cursor-list draft/scheduled/active/archived policy **[НОВЕ D13]** |
| GET | `/v1/admin/economy/policies/:id` | `client:economy:read` | 200 | Sanitized config, validation і publish metadata **[НОВЕ D13]** |
| GET | `/v1/admin/wallet/summary` | `client:wallet:read` | 200 | Outstanding balance, credits, gross/net spend, refunds, admin bonuses **[НОВЕ D14]** |
| GET | `/v1/admin/wallet/ledger` | `client:wallet:read` | 200 | Cursor-list глобального append-only ledger **[НОВЕ D14]** |
| GET | `/v1/admin/users/:userId/wallet/summary` | `client:wallet:read` | 200 | Баланс і lifetime/complete-from flow metrics користувача **[НОВЕ D14]** |
| GET | `/v1/admin/users/:userId/wallet/ledger` | `client:wallet:read` | 200 | Cursor-list ledger користувача **[НОВЕ D14]** |
| GET | `/v1/admin/wallet/bonus-jobs` | `client:wallet:read` | 200 | Bulk bonus job history/status **[НОВЕ D14]** |

Усі cursor-list мають default `limit=50`, max `100`, читають `limit+1` для
`nextCursor`; opaque cursor кодує строгий `(createdAt,id)` boundary. Nested-resource
ownership mismatch і відсутній target дають однаковий safe 404. Read DTO мають явні
projection: password/token hashes, raw credentials, OAuth provider ids, metadata,
приватні moderation reason fields і secret config не серіалізуються.

| Метод | Шлях | Scope | Body | Семантика |
|---|---|---|---|---|
| POST | `/v1/admin/users/:userId/ban` | `client:users:write` | `{reason}` | `active→banned`, revoke всіх live refresh |
| POST | `/v1/admin/users/:userId/unban` | `client:users:write` | `{reason}` | `banned→active`, сесії не відновлюються |
| POST | `/v1/admin/users/:userId/deactivate` | `client:users:write` | `{reason}` | `active→deactivated`, revoke всіх live refresh |
| POST | `/v1/admin/users/:userId/reactivate` | `client:users:write` | `{reason}` | `deactivated→active`, сесії не відновлюються |
| PATCH | `/v1/admin/users/:userId/premium` | `client:premium:write` | `{isPremium,reason}` | Лише stored flag; same value →409 |
| POST | `/v1/admin/users/:userId/topics/:topicId/rename` | `client:users:write` | `{name,reason}` | Active user-owned словник; name після trim 1..120, same name→409, missing/deleted/cross-user→404 |
| POST | `/v1/admin/users/:userId/sessions/:sessionId/revoke` | `client:users:write` | `{reason}` | Лише належна user unrevoked session (навіть expired); missing/cross-user→404, revoked→409 |
| POST | `/v1/admin/users/:userId/sessions/revoke-all` | `client:users:write` | `{reason}` | Лише live; zero live→409 |
| POST | `/v1/admin/admin-accounts` | `admin:manage` | `{email,role,reason}` | Normalized DB admin; duplicate env/DB email→409 |
| POST | `/v1/admin/admin-accounts/:adminId/enable` | `admin:manage` | `{reason}` | DB admin only; wrong/final state→409 |
| POST | `/v1/admin/admin-accounts/:adminId/disable` | `admin:manage` | `{reason}` | DB admin only; env/self-disable protected→409 |
| POST | `/v1/admin/economy/policies` | `client:economy:write` | `{baseVersion?}` | Створити draft із active/вибраної version **[НОВЕ D13]** |
| PATCH | `/v1/admin/economy/policies/:id` | `client:economy:write` | versioned config patch | Змінити лише draft; active/scheduled/archived read-only **[НОВЕ D13]** |
| POST | `/v1/admin/economy/policies/:id/validate` | `client:economy:write` | — | Errors/warnings без publish **[НОВЕ D13]** |
| POST | `/v1/admin/economy/policies/:id/publish` | `client:economy:publish` | `{effectiveAt?,reason}` | Publish now/schedule після stale-base check **[НОВЕ D13]** |
| POST | `/v1/admin/economy/policies/:id/archive` | `client:economy:publish` | `{reason}` | Архівувати draft/scheduled; active не редагується **[НОВЕ D13]** |
| POST | `/v1/admin/users/:userId/wallet/bonus` | `client:wallet:grant` | `{amountBees,reason,message?}` + `Idempotency-Key` | Атомарний positive bonus + ledger + audit **[НОВЕ D14]** |
| POST | `/v1/admin/wallet/bonuses/preview` | `client:wallet:grant` | audience + amount + reason | Resolve immutable recipients і total, без credit **[НОВЕ D14]** |
| POST | `/v1/admin/wallet/bonuses` | `client:wallet:grant` | `{previewToken,...}` + `Idempotency-Key` | Створити idempotent bulk bonus job **[НОВЕ D14]** |
| POST | `/v1/admin/wallet/bonus-jobs/:jobId/retry` | `client:wallet:grant` | — | Retry лише failed recipients **[НОВЕ D14]** |

`reason` після trim обов'язково 3..500. User moderation — reversible status, **не
hard delete**. `ban/deactivate` атомарно revoke refresh rows, а mobile access strategy
звіряє свіжий status на кожному запиті, тому вже виданий access одразу отримує 401.
Перейменування словника оновлює `topics.name` і `updatedAt` в одній transaction з
audit action `topic.renamed`; стандартний delta-sync підхоплює зміну без окремої
mobile API-схеми або міграції БД.

Для мутацій уже наявного target одна PostgreSQL transaction повторно валідує actor,
блокує target `FOR UPDATE`, робить зміну/revoke і вставляє рівно один audit event.
`createAdmin` не має наявного target для lock: у тій самій transaction він повторно
валідує actor, перевіряє duplicate env/DB email, вставляє admin + audit, а concurrent
unique race (`23505`) перетворює на 409. Audit failure відкочує все.
`beforeValue/afterValue` проходять strict recursive allowlist (дозволений nested
`profile`), а UPDATE/DELETE журналу відхиляє DB trigger.
Bootstrap env admin list (`BOOTSTRAP_ADMIN_EMAILS`, початково
`frost.co.ua@gmail.com`) immutable; додані через API admins лежать у БД. Admin JWT:
RS256, default 15 хв, issuer `vocabee-client-gateway`, audience `vocabee-admin`,
`typ=admin`, pinned `kid`, role/scopes, без email і без refresh.

**[НОВЕ D13] Economy administration.** У `client-admin-web` додається
навігаційний розділ «Економіка». Нові scopes:
`client:economy:read`, `client:economy:write`,
`client:economy:publish`; початково `client_admin` має read/write для
чернеток, а publish — лише `super_admin`. Конфіг охоплює topic/word costs,
refund window, starting/registration rewards, rewarded ad, referral
inviter/invitee, Promo API та default/pair-override ціни market pack.
Published version immutable; rollback створює нову version; усі charge/reward
зберігають actual amount + `policyVersion`. Повний контракт —
[20-client-admin-economy-config.md](20-client-admin-economy-config.md).

**[НОВЕ D14] Wallet administration.** Overview і
`Економіка → Гаманці` показують суму поточних балансів, gross spent, refunds,
net spent та admin bonuses; user detail — summary + ledger. Нові scopes:
`client:wallet:read` і `client:wallet:grant`; початково client admin має
read, а grant — лише super admin. Bonus завжди позитивний, потребує reason та
idempotency; direct balance edit відсутній. Bulk grant проходить
server-resolved preview перед job execution. Через неповну legacy-історію
current balance точний одразу, а spend має `completeFrom`. Повний контракт —
[21-client-admin-wallet-operations.md](21-client-admin-wallet-operations.md).

`client-admin-web` **[ЗАРАЗ]** реалізований у source; API і Swagger також реалізовані.
Independent container/Compose deployment і browser E2E ще **[НОВЕ]**. Mobile app ці
routes не викликає.

### 1.3.2 Dictionary administration + API consumers (`dictionary-gateway /v1/admin`)

`vocabee-gateway/src/dictionary-admin/*`, `src/dictionary-access/*`. **[ЗАРАЗ]**
Dictionary admin UI та звичайні admin routes приймають окремий RS256
`admin-access-token`; consumer `X-API-Key` не дає доступу до них. Єдина
машинна виняткова route — `POST /v1/admin/lexicon/import-v2`: вона приймає
тільки ключ захищеного `translation-uploader` зі scope
`dictionary:lexicon:write`, а admin bearer її не авторизує.
`super_admin`/`dictionary_admin` отримують рівно ці admin scopes:

`dictionary:lexicon:read`, `dictionary:lexicon:write`, `dictionary:consumers:read`,
`dictionary:consumers:write`, `dictionary:keys:write`, `dictionary:usage:read`,
`dictionary:providers:read`, `dictionary:audit:read`.

| Метод | Шлях | Scope | Семантика |
|---|---|---|---|
| GET | `/v1/admin/dictionary/dashboard` | `dictionary:usage:read` | Consumers, active/retiring/revoked keys, calls і quota failures від UTC midnight |
| GET | `/v1/admin/lexicon/translations` | `dictionary:lexicon:read` | Cursor-list; `status=active\|deleted\|all` (default active), `sourceLang?`, `targetLang?`, `source?`, `origin?`, `providerTier?`, `q?` |
| GET | `/v1/admin/lexicon/translation-filter-options` | `dictionary:lexicon:read` | Sorted distinct non-empty `origins` і `providerTiers`, які реально є в translation rows; без metadata/credentials |
| GET | `/v1/admin/lexicon/translations/:translationId` | `dictionary:lexicon:read` | Останній active або soft-deleted рядок: source/target lexical entry, IPA, senses/examples, synonyms/antonyms/forms, alternatives, provenance і safe metadata; не повна immutable history |
| POST | `/v1/admin/lexicon/import` | `dictionary:lexicon:write` | Legacy reviewed `.jsonl`; перевіряє мови, `needsReview`/review status і структуру рядків, додає source/target lexical entries та enrichment, пропускає дублікати за нормалізованою парою слово+переклад |
| POST | `/v1/admin/lexicon/import-v1` | `dictionary:lexicon:write` | Multipart `file` з `formatVersion: "v1"` JSONL; звичайне word вимагає reviewed `partOfSpeech`, senses і приклад для кожного sense, а IPA є best-effort і може бути відсутнім; multi-sense translations вимагають `senseIndex`. Імпорт сам не викликає AI/провайдера: однозначний reviewed IPA з Kaikki дозаповнює старе значення, але конфлікт двох seed IPA відхиляється; reviewed `senseIndex` одразу встановлює/виправляє translation→sense. `seed-import` після цього immutable у звичайному search; знайдена прогалина створює personless `lexicon_curated_data_missing` для перегенерації batch, без provider fallback. |
| POST | `/v1/admin/lexicon/import-v2` | `X-API-Key` · `dictionary:lexicon:write` | Multipart `file` з `formatVersion: "v2"` JSONL. Кожен sense має stable `senseKey`, кожен example — `senseKey`, а кожен translation — непорожній `senseKeys[]`; positional `senseIndex`, дублікати нормалізованого translation text і `confidence: null` заборонені. Опційний `X-Content-SHA256` звіряє raw bytes до імпорту; відповідь завжди містить `fileSha256` і `fileSizeBytes`. Увесь файл спочатку проходить структурну перевірку, а кожен прийнятий запис імпортується у власній DB-транзакції: stable keys upsert-яться, `translation_senses` авторитетно замінюються в межах запису, а перший link проєктується у legacy `translations.sense_id`. Наявний provider-row атомарно стає curated разом із target linkage/provenance/metadata; точний повтор є no-op і не викликає AI/провайдера. Неочікувана DB-помилка повертає клієнту загальне повідомлення, а оригінал із безпечним контекстом потрапляє у Sentry. |
| POST | `/v1/admin/lexicon/quality-feedback` | `dictionary:lexicon:write` | `{targetType: "translation"\|"example", targetId, comment?}`; адмінський dislike не видаляє рядок, додає 100 балів якості один раз для цього адміністратора й повертає поточний бал |
| POST | `/v1/admin/lexicon/translations/:translationId/delete` | `dictionary:lexicon:write` | `{reason}` 3..500; soft-delete + pending pair repair + audit в одній transaction; repeated state →409 |
| POST | `/v1/admin/lexicon/translations/:translationId/restore` | `dictionary:lexicon:write` | `{reason}` 3..500; відновлює останній рядок і скасовує pending repair slot, якщо його ще не спожито; audit atomically |
| GET | `/v1/admin/providers` | `dictionary:providers:read` | Лише configured/active/model/masked hint; provider-secret write route немає |
| GET | `/v1/admin/api-usage` | `dictionary:usage:read` | Consumer/key/outcome/status/duration; `consumerId?`, `keyId?`, `outcome?`, `from?`, `to?`; без query/URL/IP/UA |
| GET | `/v1/admin/audit-events` | `dictionary:audit:read` | Append-only журнал; `actor?`, `action?`, `target?`, inclusive `from?`, exclusive `to?` |
| GET/POST | `/v1/admin/api-consumers` | `dictionary:consumers:read` / `dictionary:consumers:write` | List/create external consumer; server фіксує plan/scopes |
| GET | `/v1/admin/api-consumers/:consumerId` | `dictionary:consumers:read` | Safe consumer summary |
| POST | `/v1/admin/api-consumers/:consumerId/enable` / `disable` | `dictionary:consumers:write` | External lifecycle + `{reason}`; protected system consumer immutable |
| POST | `/v1/admin/api-consumers/:consumerId/keys` | `dictionary:keys:write` | Створити ключ; `{key,rawKey}`, raw видимий один раз і повертається лише після post-commit read-back та криптографічної перевірки поточним gateway; непідтверджений запис дає safe 503 і Sentry event замість мертвого ключа |
| GET | `/v1/admin/api-consumers/:consumerId/keys` | `dictionary:consumers:read` | Лише safe summaries без raw/digest/pepper |
| POST | `/v1/admin/api-keys/:keyId/rotate` | `dictionary:keys:write` | Active successor + one-time raw після такого самого post-commit read-back; predecessor `retiring` на 900 с |
| POST | `/v1/admin/api-keys/:keyId/revoke` | `dictionary:keys:write` | Незворотний revoke; останній usable system key захищений 409 |

Новий production gate — exact parser без БД:
`npm run db:validate:lexicon-v2 -- <file-or-directory> --report <path>`;
команда має повернути `invalid=0`. V2 generator одразу пише stable keys; міграція
V1→V2 детерміновано переносить single-sense/reviewed mapping, а відсутні
multi-sense links і структурні прогалини обов'язково передає у внутрішню
AI-generation queue leased migration worker-а. Unit не може завершитись, доки
черга не порожня, `needsReview=false`, conversion report не має
`unresolved/invalid`, а exact gateway report не має `invalid`. Міграція не
змінює pair-local `progress.md` або V1 batches: наступний
`vocabee-primary-translate-auto` продовжує з того самого `Next range` і пише
лише нові V2 batches. І V2, і legacy V1 importer приймають source
`entryType="word"` та `entryType="phrase"`: фрази зберігаються у
`lexicon_phrases`, а source/target phrase unit не маскуються під слова. V1 exact
parser/endpoint лишаються доступними для старих пакетів, але нові skills V1 не
генерують.

**[ЗАРАЗ] Resumable V2 upload.** Скіл `vocabee-upload-translations` будує
inventory заново на кожному `plan/status/run`: immutable baseline бере з
`translation-v2-migration/completed` та узгоджених conversion/exact-parser
reports, а нові primary/secondary V2 batches — з живих pair/category checkpoints.
`primary-matrix-progress.md` і secondary `matrix-progress.md` є лише проєкціями,
не джерелом discovery. Raw workspace не відправляється: mutating run
детерміновано формує package лише з `approved/fixed`, виключає `rejected`,
атомарно публікує package manifest, перевіряє canonical `senseKey` та exact
gateway parser, а тоді завантажує всі мовні напрями строго послідовно. Для кожної
чистої відповіді він атомарно пише source+output-checksum-bound receipt у
`service/translation-upload-state/<profile>/receipts/` і оновлює
`progress.md`; наступна сесія з тим самим profile/endpoint повторно сканує
mapping, пропускає попередні receipts і автоматично продовжує з першого нового
pending batch. Тому колишні 100% можуть знову стати pending після генерації
нового immutable range. В адмінці оператор відкриває захищений системний
consumer `translation-uploader`, створює/ротейтить ключ і передає one-time raw
value агенту. Скіл надсилає його тільки як `X-API-Key`; значення читається через
stdin і не потрапляє в argv/state/manifests. HTTP 200 сам по собі не є успіхом: receipt дозволений
лише коли filename/SHA-256/bytes/lines збігаються, `invalid=0`, `failed=0`,
`errors=[]`, а row accounting повний. 401/403, permanent 4xx, змінений завершений
source/package або часткове перекриття пакетів зупиняють upload без втрати попередніх
receipts. Це at-least-once resume з безпечним row replay, а не whole-file
transaction або server-side exactly-once receipt.

**[ЗАРАЗ] Manual V2 import UI.** На сторінці «Переклади» є окрема V2-панель:
оператор вставляє one-time `translation-uploader` key, який живе тільки в
React-state поточної вкладки, і може вибрати або перетягнути цілу папку.
Folder drop рекурсивно обходить вкладені каталоги (із folder-picker fallback),
кожен JSONL локально перевіряється по всіх непорожніх рядках, отримує SHA-256,
напрямок і тип (`primary/abbreviation/slang/phrase`). Точні checksum-дублікати
пропускаються, змішані напрями, не-V2 файли та конфлікт одного
`direction+category+range` з різними checksum блокуються до upload. Черга
сортується `direction → category → range`, показується accordion-картками по
мовній парі; у header кожної картки є progress і лічильники success/warning/error,
а батчі мають незалежну пагінацію по 20 рядків. Result/error лишається у рядку
батча, тому окремого нескінченного полотна результатів немає. Запити йдуть
ізольованим API-key transport без admin bearer, `X-Content-SHA256` обов'язковий;
401 від помилкового uploader key не завершує admin-сесію. Є «Пауза після
поточного батчу»; UI-черга живе лише до reload вкладки, тоді як міжсесійне
resume лишається відповідальністю skill receipts. Автопрогін зупиняється на
першому permanent 4xx або навіть HTTP 200 з `clean=false`: проблемний батч
отримує warning із точною причиною, а всі наступні залишаються pending для
безпечного продовження після виправлення.

`dictionary-admin-web` **[ЗАРАЗ]** використовує спільну адмін-дизайн-систему
«Vocabee Redesign» (`@vocabee/admin-ui`) та мобільний знак із трьох сот. Список
перекладів за замовчуванням показує `active` і `deleted` разом (`status=all`), щоб
останню видалену ревізію можна було знайти й відновити. Видимі фільтри — пошук,
два селекти мов (default `en → uk`) і сегмент статусу (Усі/Активні/Видалені);
кнопки «Застосувати» немає — будь-яка зміна параметра застосовується до URL з
дебаунсом 500 мс. `origin`, provider tier і source лишаються API-параметрами, але
не полями цієї адмін-форми. Журнал змін показується стрічкою подій (`AuditFeed`)
з темним diff-блоком «До/Після».

### 1.3.3 Якість перекладів і прикладів **[ЗАРАЗ]**

Якість — це окремий append-only шар поверх лексикона, а не видалення перекладу.
На `translations` і `lexicon_examples` зберігається агрегований `quality_score` із
початковим значенням `0`. Таблиця `lexicon_quality_feedback` зберігає кожен сигнал:
`target_type`, рівно один `translation_id` або `example_id`, `actor_type` (`user` або
`admin`), стабільний `actor_id`, розмір штрафу, коментар і час. Часткові унікальні
індекси не дозволяють одному актору двічі оцінити той самий об’єкт; повтор повертає
`accepted=false` без повторного збільшення бала.

Правила балів: звичайний авторизований користувач додає `+1`, адмін у
dictionary-admin — `+100`, поріг регенерації дорівнює `100`. Коментар нормалізується,
обрізається до 1000 символів і передається AI лише під час ремонту відповідного
translation/example.

Під час пошуку точного слова сервіс читає активні переклади пари. Якщо хоча б один
має `quality_score >= 100`, він викликає translation provider із переліком старих
варіантів у `excludedTranslations` і feedback-коментарями, просить нові варіанти,
зберігає їх, а після успішної відповіді обнуляє score старих рядків. Старий рядок
залишається в БД і доступний адміну; у відповіді пошуку він не показується, якщо AI
повернув новий варіант. Якщо AI недоступний або повернув порожню відповідь, кеш не
втрачається, а бал залишається для наступної спроби.

Для `example` ремонт прив’язаний до learning-side `word_id`, тому перегенеровуються
лише позначені приклади: їхні тексти передаються в `rejectedExamples`, а коментарі —
в `qualityFeedback`. Інші sense/переклади слова не чіпаються. Ремонт дедуплікується
в межах одного пошукового запиту, а повторний dislike ідемпотентний на рівні БД.

Клієнтський маршрут `POST /v1/translation-feedback` захищений mobile JWT і
проксіює запит через внутрішній API-key у dictionary-gateway; мобільний клієнт
ніколи не отримує dictionary API key. Адмінський маршрут використовує окремий
RS256 admin token і scope `dictionary:lexicon:write`. У dictionary-admin списку
перекладів показується `qualityScore/100` і компактна кнопка з іконкою дізлайка
та підписом «-100» (aria-label «Дізлайк -100») із підтвердженням та
необов’язковим коментарем.

`client-admin-web` **[ЗАРАЗ]** використовує ту саму дизайн-систему «Vocabee
Redesign» для Login, shell, метрик, таблиць, карток користувача, адміністраторів і
журналу змін: шрифти Manrope/JetBrains Mono (самохостинг через `@fontsource`, бо
прод-CSP адмінок має `font-src 'self'`), медовий активний пункт рейла з іконками,
аватарки-ініціали, пілюлі статусів із крапкою. Фільтри користувачів і журналу — без
кнопки «Застосувати»: зміни застосовуються до URL з дебаунсом 500 мс; журнал змін —
стрічка подій (`AuditFeed`) з diff-блоком «До/Після». Візуальний рефакторинг не
змінює Google auth, admin scopes, mobile login або API контракти.

Create external consumer завжди дає `external-standard`, `dictionary:search`, 60
admissions/min і 1000/UTC-day. Quota атомарна на consumer, тому rotation не обнуляє
лічильник; 429 має `Retry-After`. `client-gateway` consumer має protected
`system-unlimited`: business counter пропускається, але auth/scope, limit 50, timeout,
technical protections і usage attribution залишаються.

DB зберігає тільки public id/display prefix, HMAC digest, pepper version, scopes і
lifecycle. `DICTIONARY_API_KEY_PEPPER` + version належать лише dictionary runtime і
мають бути стабільні на всіх replicas; поточний verifier приймає одну version, тому
її зміна без coordinated legacy-bridge/reissue одразу інвалідує старі DB keys.
Перший cutover **ще не виконаний**. Поточний Compose використовує одну substitution
`DICTIONARY_APP_API_KEY` для client і dictionary та не інжектить pepper/version/legacy
toggle, тому runbook заблокований до deployment W4 з окремими service inputs. Після W4
`DICTIONARY_APP_API_KEY` на client стане новим DB-backed raw ключем, а dictionary
тимчасово збереже старий literal fallback до
`DICTIONARY_LEGACY_APP_KEY_ENABLED=false`. Тоді порядок: міграція + backup/counts →
dictionary з pepper і legacy=true → створити system key → перезапустити лише client з
ним → перевірити facade → вимкнути legacy на dictionary → повторно перевірити facade й
uniform 401 для старого literal. System consumer не disable/delete; при rotation
спершу deploy/verify successor у 15-хв overlap, лише потім revoke predecessor.

**[МАЙБУТНЄ, після cutover]** Encrypted provider-secret writes, повна immutable
translation history понад одну відновлювану soft-deleted ревізію, quality feedback
export і optional MCP
описані в `vocabee-gateway/docs/superpowers/plans/2026-07-15-post-cutover-product-completion.md`;
поточні provider/translation DTO не означають, що ці фічі вже реалізовані.

### 1.4 Wallet (`/v1/wallet`)

`vocabee-gateway/src/wallet/wallet.controller.ts` — під `JwtAccessGuard`.

| Метод | Шлях | Суб'єкт | Код | Призначення | Відповідь |
|---|---|---|---|---|---|
| GET | `/v1/wallet` | JWT | 200 | Поточний баланс монеток (виклик `addBees(id, 0)`) | `UserResponseDto` |
| POST | `/v1/wallet/rewarded-ad` | JWT | 200 | Нарахувати монетки за переглянуту рекламу (`+REWARDED_AD_BEE_AMOUNT`) | `UserResponseDto` |

Константа `REWARDED_AD_BEE_AMOUNT = 10` (`wallet.constants.ts`). Тіло запиту в обох випадках відсутнє.

> **[ЗАРАЗ]** `rewarded-ad` НЕ верифікований і НЕ ідемпотентний — кожен POST
> безумовно додає +10. Баланс і новий `rewarded_ad_events` row створюються в одній
> транзакції з request-context `requestId`; event має status
> `unverified_legacy`, тож це не SSV і не дедуп-ключ. **[НОВЕ]** за **D1** endpoint
> має стати верифікованим (звірка з рекламною мережею) та ідемпотентним (де-дуп за
> токеном винагороди). Деталі — `04-coins-economy.md`.

### 1.5 [ЗАРАЗ] Client search facade + Dictionary search (`/v1/search`)

Mobile-контракт обслуговує
`vocabee-gateway/src/client-search/client-search.controller.ts`; контролер не
імпортує lexicon service, а викликає
`src/dictionary-client/dictionary-client.service.ts` →
`dictionary-gateway /v1/search` з `X-API-Key` і timeout (дефолт 15 с).

| Метод | Шлях | Суб'єкт | Призначення |
|---|---|---|---|
| GET | `client-gateway /v1/search?q=&speak=&learn=` | Optional JWT: без header / active bearer | Mobile compatibility facade: tier/wallet + делегація; supplied invalid/expired/inactive credential → 401 |
| GET | `dictionary-gateway /v1/search?q=&speak=&learn=&limit=` | `X-API-Key` | App-neutral lexicon search; `limit` 1..50, default 50 |
| POST | `client-gateway /v1/search/context-glossary` | Optional JWT | Безкоштовний mobile facade: один batch для exact прикладу; wallet не викликається |
| POST | `dictionary-gateway /v1/search/context-glossary` | `X-API-Key`, scope `dictionary:search` | Детермінована токенізація + один contextual provider request; один quota admission на речення |
| POST | `dictionary-gateway /v1/snapshots` | `X-API-Key` fixed protected `client-gateway` consumer | До 200 provider-free canonical saved-word projections для vocabulary sync; зовнішній search key отримує 403 |

Dictionary guard спершу шукає DB key за public id і timing-safe звіряє HMAC повного
raw key; під час rollout лише потім може перевірити legacy literal fallback. Missing,
malformed, unknown, wrong environment/secret, expired/retired/revoked key і disabled
consumer мають однаковий стабільний 401. Usage event містить consumer/key/request id,
outcome, HTTP status, duration і timestamp — ніколи `q`, мови, URL, IP/UA, raw/digest.

**Запит `SearchQueryDto`** (`search.dto.ts`): `q` (1..200, trim), `speak` (IsIn кодів — відома мова), `learn` (IsIn кодів — мова, що вивчається). Dictionary DTO додає `limit`; client завжди передає свій tier cap (зараз 50).

**`ContextGlossaryRequestDto`**: `{ sentence, sourceLang, targetLang }`, де sentence
має 1..500 символів, мови входять у `SUPPORTED_LANGUAGE_CODES`. Gateway токенізує exact
trimmed sentence Unicode-регексом у максимум 64 слова (апостроф/дефіс усередині слова
зберігаються) і повертає `{ sentence, sourceLang, targetLang, tokens[] }`. Кожен token:
`surface`, `normalized`, UTF-16 `start/endExclusive`, короткий контекстний `translation`,
`lemma|null`. Неповна відповідь провайдера не персиститься: endpoint відповідає помилкою,
а клієнт лишає вже збережене слово без glossary. Client facade свідомо не викликає
`WalletService`; dictionary quota рахує весь batch як один запит.

Якщо Optional JWT визначив користувача, client facade після успішного batch атомарно
upsert-ить приватний серверний glossary. `user_context_glossary_entries` має унікальний
ключ `(user_id, source_lang, target_lang, normalized_source_word,
normalized_translation)`: конкретний переклад є частиною identity. Речення, surface і
UTF-16 offsets зберігаються в `user_context_glossary_examples` з окремою унікальністю
входження. Для anonymous/offline шляху той самий запис виконує `topics/sync/apply`,
прочитавши `details.contextGlossary` з metadata; повтори ідемпотентні.

**Логіка списання та tier** (`client-search.controller.ts`):
- tier визначається з рядка `users` за `is_premium` (`tierFromUserRow`): `anonymous` (без токена) / `registered` / `premium`. **[ЗАРАЗ]** `TIER_MAX_RESULTS` у коді = **50/50/50** (капи знято для всіх), і backend Swagger синхронізовано з цим контрактом. Майбутня різниця tier лишається продуктовим питанням — див. `13-add-word-and-ai-search.md` §13 та `10-edge-cases-and-open-items.md` O1/O3.
- якщо токен присутній — `walletService.spendBees(user.id, TRANSLATION_SEARCH_BEE_COST)`; `TRANSLATION_SEARCH_BEE_COST = 1` (−1 монетка за пошук). Це **legacy v1**, superseded by D11 після v2 rollout. Анонім **не** списується (узгоджено з **D2**).
- `meta.beeBalance` = баланс після списання, або `null` для аноніма.

**Відповідь `SearchResponseDto`** (`search-response.dto.ts`):

| Поле | Тип | Нотатка |
|---|---|---|
| `query` | string | |
| `detectedLang` | string | визначена мова запиту |
| `isPhrase` | boolean | |
| `knownLang` / `learningLang` | string | |
| `tier` | `anonymous`\|`registered`\|`premium` | |
| `maxResults` | number | ліміт варіантів для tier |
| `results` | `VariantDto[]` | |
| `meta` | `MetaDto` | |

`VariantDto`: **`translationId`** (durable id рядка `translations`, незмінно проходить
dictionary → client facade), `knownWord`, `learningWord`, `ipa?`, `audioUrl?`,
`partOfSpeech[]`, `examples[]` (`{text, translation?}`), `senses[]`
(`{senseKey: string|null, definition, partOfSpeech?, tags[], examples[], synonyms[], antonyms[]}`;
`null` дозволено лише для legacy row без персистованого stable key),
`synonyms[]`, `antonyms[]`, `forms[]` (`{text, tags[]}`), `senseKeys[]` (усі stable
значення, які рендерить переклад), `senseIndex?` (**[НОВЕ] фаза 0:** індекс
**у межах масиву `senses` цього ж варіанта**, не глобальний позиційний за словом;
для атрибутованого варіанта завжди `0`; null — не атрибутовано),
`lexicalUnitKind` (`word|phrase|expression|abbreviation`), `registerTags[]`
(`slang|informal|formal|technical|offensive|humorous|internet`), `expansion?`,
`translatedExpansion?`, `meaning?`, `literalTranslation?`, `usageExample?`,
`usageExampleTranslation?`, `source`, `origin`,
`confidence?`, `isPrimary`, `cached`, `match` (`exact`\|`prefix`). Dictionary response
не містить `tier`/`beeBalance`; їх додає тільки client facade.

**[ЗАРАЗ]** `match="prefix"` не означає скорочений DTO. Коли exact-збігу нема,
dictionary-gateway обирає не більш як 15 підказок і read-only підтягує для них уже
персистовані IPA/PoS, senses/examples, synonyms/antonyms, forms та V2 `senseKeys`.
Legacy sense без записаного key лишається `senseKey=null` — gateway не видає
обчислений на льоту ID за стабільний, доки такого рядка немає в БД.
Autocomplete не запускає translator/dictionary, sense-attribution або quality repair.
Це важливо, бо mobile не робить другого detail-запиту: натискання `+` зберігає саме цей
response snapshot у `WordEntry.details`/Room.

**[НОВЕ] (фаза 0) Per-variant sense scoping.** До фази 0 кожен `VariantDto` ніс
однаковий word-level блоб `senses`/`synonyms`/`antonyms`/`examples` незалежно
від того, з яким конкретно сенсом повʼязаний цей переклад — усі варіанти
одного слова отримували ідентичний список. `projectEnrichmentForVariant`
(`lexicon.service.ts:1946`), вплетена в `prefixMatchToVariant` перед return
(`lexicon.service.ts:654`), тепер проєктує це word-level enrichment у
конкретний варіант:
- **Атрибутований варіант** (`senseKeys[]` непорожній, або legacy `senseIndex`
  вказує на існуючий сенс) несе **лише власні** сенси — і їхні `examples`,
  `synonyms`, `antonyms` (union по відібраних сенсах). Якщо в відібраних
  сенсів ці списки порожні — фолбек: `synonyms`/`antonyms` беруться з
  word-level пулу, `examples` — з повного flat-списку.
- `senseIndex` для такого варіанта нормалізується в `0` (індекс у межах
  власного, вже звуженого масиву `senses` — див. вище).
- **Без атрибуції** (немає ні `senseKeys`, ні валідного legacy `senseIndex`) —
  повний word-level блоб без змін, легасі-поведінка збережена.
- Wire-формат `SearchVariant` не змінився — лише вміст перелічених полів.

**[НОВЕ] (фаза 0) Фільтр форм.** `forms[]` більше не містить wiktextract-
службові псевдо-форми таблиці відмінювання (текст `no-table-tags`/`glossary`,
теги `table-tags`/`inflection-template`, рядки без жодної літери/цифри) —
спільний `isJunkWordForm` (`word-form-filter.ts`) фільтрує їх і на записі
(`FreeDictionaryProvider`), і на видачі (`assembleEnrichment`,
`lexicon.service.ts:1816`). Міграція `0022_prune_junk_word_forms.sql`
одноразово вичистила вже накопичений дебрис у `lexicon_word_forms`.

`MetaDto`: `totalAvailable`, `triedProvider`, `providerReason` (`exact_cached`\|`not_a_word`\|`echo`\|`no_provider_data`\|`translated`\|null), `dictionarySource?`, `dictionaryOrigin?`, `beeBalance?`.

> Клієнтський `SearchResponse` (`SearchResponse.kt`) — спрощене дзеркало:
> **[ЗАРАЗ]** `SearchVariant` десеріалізує durable `translationId` і переносить його
> в opaque control-поля `WordDetails`; `match` mobile не моделює. Опційні
> `lexiconSchemaVersion`/`lexiconRevision` сумісно приймаються, хоча первинний search
> може віддати лише id, а server-authoritative revision встановлює sync. `SearchMeta` тримає лише `totalAvailable`, `dictionarySource`,
> `dictionaryOrigin`, `beeBalance` (без `triedProvider`/`providerReason`).
> `SearchExample` має поле `translation?` (НЕ плоский рядок).
> У sync видимі поля йдуть у `topic_words.metadata.details`, а opaque
> `{translationId,lexiconSchemaVersion,lexiconRevision}` — у sibling
> `metadata.lexiconSnapshot`; окремої SQL/Room-міграції saved words не треба.

### 1.6 Topics (`/v1/topics`)

`vocabee-gateway/src/topics/topics.controller.ts` — увесь контролер під `JwtAccessGuard`. Параметри `:id`/`:wordId` валідуються `ParseUUIDPipe`.

| Метод | Шлях | Код | Призначення | Запит | Відповідь |
|---|---|---|---|---|---|
| GET | `/v1/topics` | 200 | Список словників користувача | — | `TopicResponseDto[]` |
| POST | `/v1/topics` | 201 | Створити словник | `CreateTopicDto` | `TopicResponseDto` |
| POST | `/v1/topics/sync` | 200 | Delta-sync since-timestamp | `SyncRequestDto` | `SyncResponseDto` |
| POST | `/v1/topics/sync/apply` | 200 | Залити локальні зміни → повний снапшот | `ApplySyncRequestDto` | `SyncResponseDto` |
| GET | `/v1/topics/:id` | 200 | Словник зі словами | — | `TopicResponseDto` |
| PATCH | `/v1/topics/:id` | 200 | Патч словника | `UpdateTopicDto` | `TopicResponseDto` |
| DELETE | `/v1/topics/:id` | 204 | Soft-delete словника | — | — |
| GET | `/v1/topics/:id/words` | 200 | Слова словника | — | `TopicWordResponseDto[]` |
| POST | `/v1/topics/:id/words` | 201 | Додати слово | `AddTopicWordDto` | `TopicWordResponseDto` |
| PATCH | `/v1/topics/:id/words/:wordId` | 200 | Патч слова | `UpdateTopicWordDto` | `TopicWordResponseDto` |
| DELETE | `/v1/topics/:id/words/:wordId` | 204 | Видалити слово | — | — |

> **[ЗАРАЗ] Розбіжність із контекстом завдання:** маршрут застосування зветься **`/topics/sync/apply`** (не `/topics/sync`). Клієнт викликає саме `/v1/topics/sync/apply` (`KtorVocabeeApi.kt:147`).
>
> **[ЗАРАЗ]** Економіка (списання −10 за словник понад `FREE_DICTIONARY_LIMIT=2`, перевірка квот ANON) у цих ендпоінтах ще НЕ реалізована як сервер-авторитетна; DELETE без повернення — legacy-деталь D3, superseded by D11. **[НОВЕ]** `client-gateway` застосовує версійну політику й immutable charge/refund за D1/D11. Деталі — `04`, `06`, `07`.

**[ЗАРАЗ, D15]** Перед обома sync-відповідями `client-gateway` батчами звіряє
активні saved words із `/v1/snapshots`. Canonical payload містить learning-lexeme ref,
directional translation ref, `schemaVersion`, детерміновану SHA-256 `revision`,
канонічні word/translation text, IPA і повні details. Зміна revision оновлює
`topic_words.updated_at` та `topics.words_updated_at`, тому потрапляє у звичайну delta;
ідентичний payload timestamp не бампить. Dictionary outage лишає останній валідний
snapshot і не блокує vocabulary sync. `SyncResponseDto` завжди додає
`lexiconSchemaVersion`; mobile зберігає applied version per-user і при підвищенні
client-supported schema один раз форсить full pull.

Для linked row lexical metadata належить серверу: stale client не може повернути старі
examples/senses або стерти невідомі йому майбутні поля. Із client payload merge-яться
лише user-owned progress/delete та валідний `contextGlossary`. Unlinked row тимчасово
зберігає offline snapshot і передає `translationId` лише як недовірений hint; Dictionary
перевіряє його UUID, напрямок та lexical identity перед binding. Та сама authority-межа
використовується не лише в `applySync`, а й у прямих `POST/PATCH .../words`: linked
canonical payload не можна відкотити обхідним endpoint, а новий рядок одразу проходить
provider-free reconciliation.

**`CreateTopicDto`** (`topic.dto.ts`): `name` (1..120), `color` (≤16, hex `#RGB/#RRGGBB` або palette-ключ), `icon?` (з `TOPIC_ICONS`), `sourceLang`/`targetLang` (IsIn — мова, що вивчається / переклад), `position?` (≥0), `deviceOriginId?` (≤64, дедуп sync).

> **`TOPIC_ICONS` [ЗАРАЗ]** (`topic.dto.ts:18`): `book, plane, film, work, food, feelings, music, sport, tech, travel, study` (11 шт.). **[НОВЕ]** за **D7** набір ширший (серіал/книга/подорожі/їжа/робота/школа/спорт/музика/природа/техніка/шопінг/діти/здоров'я/загальна) — узгодити з `08-languages-speech-themes.md`.

**`UpdateTopicDto`**: усі поля опц. — `name?`, `color?`, `icon?`, `sourceLang?`, `targetLang?`, `position?`.

> **D6 [ЗАРАЗ]**: мова словника — дефолт із профілю при створенні, оверрайд у
> `CreateTopicDto`; для backward-compatible DTO мовні поля ще приймаються, але якщо
> відрізняються від збереженої пари, service повертає `400` і нічого не змінює. Це
> тримає існуючі словники immutable та не дозволяє записати lexical snapshot старого
> напрямку під нову мовну пару.

**`AddTopicWordDto`** (`topic-word.dto.ts`): `wordText` (1..200), `translationText` (1..200), `ipa?` (≤120), `sourceWordLang?`, `sourceWordId?` (UUID), `source` (IsIn `ENTRY_SOURCES`), `origin` (≤80), `deviceOriginId?` (≤64), `metadata?` (object), `knowledgePercent?` (0..100, дефолт 0).

**`UpdateTopicWordDto`**: `wordText?`, `translationText?`, `ipa?`, `metadata?`, `knowledgePercent?`.

**`TopicResponseDto`** (`topic.dto.ts:114`):

| Поле | Тип | Нотатка |
|---|---|---|
| `id` | string | UUID |
| `name` | string | |
| `color` | string | |
| `icon` | string \| null | |
| `sourceLang` / `targetLang` | string | |
| `position` | number | |
| `createdAt` / `updatedAt` | ISO8601 | |
| `lastSyncedAt` | ISO8601 \| null | sync-мітка топіка |
| `wordsUpdatedAt` | ISO8601 | коли востаннє змінювалась колекція слів |
| `wordsSyncedAt` | ISO8601 \| null | коли колекцію слів звіряли з клієнтом |

**`TopicWordResponseDto`** (`topic.dto.ts:151`): `id`, `topicId`, `wordText`, `translationText`, `ipa?`, `knowledgePercent` (0..100), `source`, `origin`, `metadata` (object), `addedAt`, `updatedAt`, `lastSyncedAt?`.

`ENTRY_SOURCES` (`schema/lexicon.ts:24`): `dictionary | translator | ai | user | seed`.

`seed` — curated/imported seed data, зокрема reviewed-батчі з `vocabeeTranslate`. Для таких рядків `origin` має починатися з `vocabee-translate/`, а `metadata.generatedBy = "vocabee-translate"`.

### 1.7 Languages (`/v1/languages`)

`vocabee-gateway/src/languages/languages.controller.ts`.

| Метод | Шлях | Суб'єкт | Призначення | Відповідь |
|---|---|---|---|---|
| GET | `/v1/languages` | public | Підтримувані мови gateway | `SupportedLanguage[]` |

`SupportedLanguage`: `{ code, name, nativeName, speechTag, flag }`. **[ЗАРАЗ]** 13 мов (`supported-languages.ts`): `uk` 🇺🇦, `en` 🇬🇧, `de` 🇩🇪, `es` 🇪🇸, `fr` 🇫🇷, `pl` 🇵🇱, `it` 🇮🇹, `pt` 🇵🇹, `tr` 🇹🇷, `he` 🇮🇱, `ar` 🇸🇦, `lt` 🇱🇹, `cs` 🇨🇿. `speechTag` (напр. `uk-UA`) живить STT/TTS (**D8** — напрямок STT зберігається по словнику).

### 1.8 Promo (`/v1/promos`) — **[НОВЕ]** (doc 05)

Ще немає в коді. Контракт за **D4** (config-driven Promo API) — джерело істини `05-promo-api-and-banners.md`:

| Метод | Шлях | Суб'єкт | Призначення |
|---|---|---|---|
| GET | `/v1/promos` | JWT | Список активних промо-кампаній (банер+ботомшит) |
| POST | `/v1/promos/{id}/claim` | JWT | Забрати бонус кампанії (верифіковано+ідемпотентно) |
| GET | `/v1/promos/leaderboard/ad-watchers` | JWT | Тижневий лідерборд переглядачів реклами (топ-10 → +50) |

Кампанії D4: `milestone` 10 реклам → +20; `daily_streak` 5 днів → +50; `registration` → +50; `weekly_leaderboard` топ-10 → +50. База +10 за рекламу лишається, промо-бонус — зверху. Повна форма DTO — у `05`.

### 1.9 [НОВЕ за D11] Lookup → save-result → charge

| Метод | Сервіс і шлях | Суб'єкт | Призначення |
|---|---|---|---|
| POST | `client-gateway /v2/translation-lookups` | JWT / дозволений anonymous mode | Безкоштовно створити lookup receipt і повернути opaque `resultId` для кожного варіанта |
| POST | `client-gateway /v2/topics/{topicId}/words/from-result` | JWT | За `Idempotency-Key` атомарно зберегти один `resultId` і створити його word-charge |

`client-gateway` зберігає зв'язок `lookupId → resultId → translationId + revisionId`
і не довіряє dictionary ids із клієнтського payload. Початкова policy: `searchCost=0`,
`wordAdditionCost=1`; два збережені result створюють два charge. Charge snapshot містить
версію політики й фактичну суму. Delete/refund посилається на immutable оригінальний
`chargeId`, а не на текст або поточну ціну.

### 1.10 [ЗАРАЗ] Feedback про поганий переклад

| Метод | Сервіс і шлях | Суб'єкт | Призначення |
|---|---|---|---|
| POST | `client-gateway /v1/translation-feedback` | JWT | Прийняти `targetType`, `targetId` і optional comment; додати користувацький `+1` до translation/example через dictionary-gateway |

Користувач може поскаржитися на `translation` або конкретний `example`; один
авторизований user може оцінити кожен target лише один раз. `dictionary-gateway`
зберігає score/event і коментар ідемпотентно. Feedback сам по собі не повертає
монетки; при `100` балів наступний пошук передає проблему AI на точкову регенерацію.

### 1.11 Economy runtime config (`/v1/economy`) — **[НОВЕ D13]**

| Метод | Шлях | Суб'єкт | Призначення |
|---|---|---|---|
| GET | `/v1/economy/config` | Optional JWT | Client-safe active policy: version, costs, refund window і доступні earn options |

Market prices повертає `GET /v1/market/packs`, Promo reward —
`GET /v1/promos`, а referral presentation — `GET /v1/referral/me`; усі вони
формуються з тієї самої active `policyVersion`. Mobile не отримує SSV keys,
anti-fraud signals, provider secrets або повний admin config.

---

## 2. Доменні моделі (клієнт)

Файл `vocabee-mobile/.../feature/vocabulary/domain/model/VocabularyModels.kt`, якщо не вказано інше. DTO транспорту — `data/api/*`.

### 2.1 Account / User — `UserResponse` (`AuthResponse.kt:20`)

| Поле | Тип | Нотатка |
|---|---|---|
| `id` | String | |
| `email` | String? | |
| `displayName` | String? | |
| `speakLang` / `learnLang` | String | |
| `notificationsEnabled` / `darkThemeEnabled` | Boolean | |
| `isAnonymous` / `isPremium` | Boolean | |
| `beeBalance` | Int | дефолт 50 (`INITIAL_BEE_BALANCE`) |
| `createdAt` / `updatedAt` | String | ISO8601 |

> **[ЗАРАЗ] Additive gateway field:** серверний `UserResponseDto` уже повертає
> `accountStatus`, але mobile `UserResponse` його поки не моделює. Android/iOS Ktor
> JSON мають `ignoreUnknownKeys=true`, тому поле безпечно ігнорується та не ламає
> поточний клієнт; gateway все одно enforce статус до відповіді.

`AuthTokensResponse` (`AuthResponse.kt:6`): `accessToken`, `refreshToken`, `expiresIn: Int`.

### 2.2 DictionaryTopic (`VocabularyModels.kt:74`)

| Поле | Тип | Нотатка |
|---|---|---|
| `id` | String | |
| `userKey` | String | дефолт `DEFAULT_LOCAL_USER_KEY = "local-user"` |
| `title` | String | |
| `sourceLanguage` / `targetLanguage` | `LanguageOption` | |
| `updatedLabel` | `TopicUpdatedLabel` | Today/Yesterday/DaysAgo/WeeksAgo |
| `coverIndex` | Int | вибір кольору/іконки обкладинки |
| `createdAtEpochMillis` / `updatedAtEpochMillis` | Long | |
| `syncStatus` | `SyncStatus` | дефолт `PendingCreate` |
| `words` | `List<WordEntry>` | |

### 2.3 WordEntry (`VocabularyModels.kt:56`)

| Поле | Тип | Нотатка |
|---|---|---|
| `id` | String | |
| `source` | String | слово мовою, що вивчається |
| `translation` | String | |
| `ipa` | String? | |
| `details` | `WordDetails?` | rich-збагачення |
| `knowledgePercent` | Int | дефолт 0 (0..100) |
| `addedAtEpochMillis` | Long | |
| `updatedAtEpochMillis` | Long | дефолт = added |
| `syncStatus` | `SyncStatus` | дефолт `PendingCreate` |

> **[НОВЕ] Поля знань D10** — у поточному `WordEntry` НЕ існують. Гібрид пріоритет+Leitner потребує: `timesCorrect`, `timesWrong`, `boxLevel`, `lastReviewedAt`, `dueAt`. Поточний бекенд/клієнт мають лише `knowledgePercent`. Канон полів — `11-practice-training.md`; план персистенції — §3 (Room) і §3 (Postgres, майбутня `0022_training_fields.sql` **[НОВЕ, ще не створена]**).

### 2.4 WordDetails / WordSense / WordForm (`VocabularyModels.kt:8-40`)

- **`WordDetails`**: opaque control `translationId`, `lexiconSchemaVersion`, `lexiconRevision`; `senseKeys: List<String>` (V2 many-to-many атрибуція), legacy `senseIndex: Int?` (перша проєкція), `senses: List<WordSense>`, `senseGroupTranslations: List<String>` (**[НОВЕ] фаза 2:** усі переклади сенс-групи з пошуку, представник першим — best-effort підказка «близьких за значенням», яку канонічний серверний снапшот може замінити; порожній для одинарної групи), `synonyms: List<String>`, `antonyms: List<String>`, `forms: List<WordForm>`, `partOfSpeech: List<String>`, lexical metadata та `contextGlossary: ContextGlossary?`. `isEmpty` ігнорує control-поля, а `shouldPersist` не дає загубити ні control-only snapshot, ні самий лише список сенс-групи. Read-only на клієнті, серіалізується в Room як один JSON-блоб.
- **`ContextGlossary`**: exact `sentence`, `sourceLang`, `targetLang`, `tokens[]`; token містить `surface`, `normalized`, UTF-16 `start/endExclusive`, `translation`, `lemma?`. Окремої Room/Postgres-міграції не треба, бо снапшот їде всередині наявного details/metadata JSON.
- **`WordSense`**: `senseKey?`, `definition`, `partOfSpeech?`, `tags[]`, `examples: List<String>`, `synonyms[]`, `antonyms[]`. Null key дозволений для legacy backend row без персистованого stable key та для старого локального snapshot. (Зверни увагу: тут `examples` — плоскі `String`, на відміну від серверного `SenseDto.examples` = `{text, translation?}`.)
- **`WordForm`**: `text`, `tags: List<String>`.

### 2.5 LanguageOption (`VocabularyModels.kt:42`)

`code`, `name`, `shortName`, `speechTag`. Дзеркало серверного `SupportedLanguage` (без `nativeName`/`flag`; `shortName` — клієнтський).

### 2.6 SyncStatus (`VocabularyModels.kt:49`)

Enum: `PendingCreate`, `PendingUpdate`, `Synced`, `PendingDelete`. Зберігається текстом у Room (`sync_status`). `PendingDelete` = локальний soft-delete (**D3**); рядки з ним приховані в запитах списків і чистяться `purgePendingDeleted*` після успішного sync.

### 2.7 VocabularySyncSnapshot (`VocabularyModels.kt:88`)

`topics: List<DictionaryTopic>`, `deletedTopicIds: List<String>`, `deletedWordIds: List<String>`. Доменний результат застосування серверного снапшоту.

### 2.8 TranslationOption (`VocabularyModels.kt:101`)

| Поле | Тип | Нотатка |
|---|---|---|
| `value` | String | текст перекладу → стає `WordEntry.translation` |
| `note` | `TranslationOptionNote` | Primary/Alternative/Additional/AlreadyAdded(source) |
| `alreadyAdded` | Boolean | |
| `learningWord` | String | канонічне слово мовою вивчення (хедворд); фолбек = `value` |
| `ipa` | String? | для `learningWord` |
| `details` | `WordDetails?` | |

> **[НОВЕ, v2]** Транспортний result також несе opaque `resultId`; durable
> `translationId`/`revisionId` резолвить і зберігає `client-gateway`. Мобільний клієнт
> використовує `resultId` для save/feedback і не формує dictionary ids самостійно.

---

## 3. Схема даних

### 3.1 Room (Android) — `vocabee-mobile/app/src/androidMain/.../data/local`

`VocabeeDatabase` (`VocabeeDatabase.kt`): version **4**, `exportSchema=true`, конвертери `VocabeeTypeConverters`. Сутності: `TopicEntity`, `WordEntity`.

**`vocabulary_topics` → `TopicEntity`** (`entity/TopicEntity.kt`). Індекси: `user_key`; `(user_key, updated_at_epoch_millis)`.

| Колонка | Тип | Нотатка |
|---|---|---|
| `id` | String | PK |
| `user_key` | String | мульти-акаунт ізоляція (`local-user` / id після входу) |
| `title` | String | |
| `source_language_code` | String | |
| `target_language_code` | String | |
| `cover_index` | Int | |
| `created_at_epoch_millis` | Long | |
| `updated_at_epoch_millis` | Long | |
| `sync_status` | SyncStatus | sync-метадані (текст) |

**`vocabulary_words` → `WordEntity`** (`entity/WordEntity.kt`). FK → `vocabulary_topics(id)` ON DELETE CASCADE. Індекси: `topic_id`; `user_key`; `(user_key, topic_id)`; `(user_key, added_at_epoch_millis)`.

| Колонка | Тип | Нотатка |
|---|---|---|
| `id` | String | PK |
| `user_key` | String | |
| `topic_id` | String | FK |
| `source` | String | |
| `translation` | String | |
| `ipa` | String? | |
| `details_json` | String? | серіалізований `WordDetails` (один JSON-стовпець) |
| `knowledge_percent` | Int | дефолт 0 |
| `added_at_epoch_millis` | Long | |
| `updated_at_epoch_millis` | Long | |
| `sync_status` | SyncStatus | sync-метадані (текст) |

> **[НОВЕ]** Для **D10** до `vocabulary_words` додаються `times_correct`, `times_wrong`, `box_level`, `last_reviewed_at_epoch_millis`, `due_at_epoch_millis` → bump версії БД (5) + Room-міграція. `details_json` лишається read-only enrichment. Деталі персистенції — `03-data-caching.md` і `11-practice-training.md`.

**Ключові запити `VocabularyDao`** (`VocabularyDao.kt`): списки фільтрують `sync_status != 'PendingDelete'`; `*IncludingDeleted` — для sync-вивантаження; `markTopicDeleted`/`markWordDeletedByTranslation` → ставлять `PendingDelete` (**D3**); `purgePendingDeleted*` — після успішного sync; `moveTopicsToUser`/`moveWordsToUser` — перенесення `user_key` при вході (**D9** мерж); `migrateLegacy*LanguageDirection` — одноразова міграція напрямку uk↔en. `duplicateWordCount` блокує лише точний дубль пари (source+translation).

### 3.2 [ЗАРАЗ] Спільний Postgres split-foundation — `vocabee-gateway/src/db/schema`

> **[ЗАРАЗ]** `client-gateway` і `dictionary-gateway` є окремими процесами, але в
> першому rollout використовують той самий `DATABASE_URL`, поточну схему й одну
> історію міграцій. Міграції/seed у Compose виконує лише one-shot `db-init`; gateway
> не мігрують схему під час startup. Нових cross-domain imports/joins split не додає.
>
> **[НОВЕ, target split]** Цільовий стан має дві логічні БД/credentials:
> `vocabee_app` належить `client-gateway`, `vocabee_dictionary` —
> `dictionary-gateway`. Між ними немає cross-DB FK або runtime join; app saved word
> тримає snapshot і opaque dictionary identifiers.

**`users`** (`schema/users.ts`): `id` uuid PK, `email` text, `password_hash` text, `display_name` text, `speak_lang` varchar(8) NN def `uk`, `learn_lang` varchar(8) NN def `en`, `notifications_enabled` bool NN def true, `dark_theme_enabled` bool NN def false, `is_anonymous` bool NN def false, `is_premium` bool NN def false, **`bee_balance` integer NN def 50** (CHECK ≥ 0), `referral_code` text, **`account_status user_account_status NN def active`** (`active|banned|deactivated`), приватні moderation-поля `status_reason/status_changed_at/status_changed_by`, `created_at`/`updated_at` timestamptz NN. Індекси: deployed `0001` має unique `lower(email)` partial index, тоді як Drizzle metadata зараз описує plain `email` index (відомий schema/migration drift); також `(account_status, created_at DESC)`.

Супутні: **`refresh_tokens`** (`token_hash`, `expires_at`, `revoked_at`, FK→users CASCADE) і **`oauth_accounts`** (`provider`, `provider_account_id`, унік. індекс `(provider, provider_account_id)`).

**Client administration (`schema/admin.ts`, migration 0012):**

- **`admin_accounts`** — uuid PK, normalized `email` з unique `lower(email)`, fixed
  `role` (`super_admin|client_admin|dictionary_admin`), `is_active`, opaque
  `created_by_subject`, timestamps і `disabled_at`. Env bootstrap admins у цю таблицю
  не копіюються;
- **`admin_audit_events`** — actor subject/type/role, action, target, reason,
  allowlisted `before_value/after_value`, normalized request context
  `request_id/ip_address/user_agent` (request ID і user-agent bounded), `created_at`;
  індекси за time/actor/target, UPDATE/DELETE відхиляє append-only trigger;
- **`rewarded_ad_events`** — FK→users `ON DELETE RESTRICT`, positive `amount`, status
  тільки `unverified_legacy`, request id і timestamp; індекс `(user_id,created_at
  DESC)`, UPDATE/DELETE відхиляє той самий append-only policy. Міграція не синтезує
  events для старих credits.

**`topics`** (`schema/topics.ts`): `id` uuid PK, `user_id` uuid FK→users CASCADE, `name` text, `color` varchar(16), `icon` varchar(32) null, `source_lang`/`target_lang` varchar(8), `position` int def 0, `device_origin_id` text, `created_at`/`updated_at` timestamptz, **`deleted_at` timestamptz** (soft-delete, **D3**), **sync**: `last_synced_at` timestamptz, `words_updated_at` timestamptz NN def now(), `words_synced_at` timestamptz. Індекси: `user_id`; `(user_id, updated_at)`; `(user_id, words_updated_at)`.

**`topic_words`** (`schema/topics.ts`): `id` uuid PK, `topic_id` uuid FK→topics CASCADE, `word_text`/`translation_text` text, `ipa` text, `source_word_lang` varchar(8), `source_word_id` uuid, `source` varchar(16), `origin` text, `device_origin_id` text, `metadata` jsonb def `{}`, **`knowledge_percent` integer NN def 0** (CHECK 0..100), `added_at`/`updated_at` timestamptz, **`deleted_at` timestamptz**, `last_synced_at` timestamptz. Індекси: `topic_id`; `(topic_id, updated_at)`; `(topic_id, last_synced_at)`.

> **[НОВЕ] Поля тренування D10** для `topic_words` (майбутня `0022_training_fields.sql`, ще не створена): `times_correct` int def 0, `times_wrong` int def 0, `box_level` int def 0 (Leitner), `last_reviewed_at` timestamptz, `due_at` timestamptz.

**`languages`** (`schema/languages.ts`): `code` varchar(8) PK, `name`, `native_name`, `speech_tag`, `flag` — довідник, сидиться з `SUPPORTED_LANGUAGES`.

**Лексикон** (`schema/lexicon.ts`) — джерело перекладів/збагачення; **партиціювання LIST за мовою** (`uk, en, de, es, fr, pl, it, pt, tr, he, ar, lt, cs`), тому PK містить мовний код:
- **`lexicon_words`** — PARTITION BY LIST (`lang`); PK `(lang, id)`; `lemma`, `normalized`, `ipa`, `audio_url`, `part_of_speech text[]`, `source`, `origin`, `metadata` jsonb. Унік. індекс `(lang, normalized)`.
- **`lexicon_phrases`** — PARTITION BY LIST (`lang`); PK `(lang, id)`; `text`, `normalized`, `source`, `origin`, `metadata`. Унік. `(lang, normalized)`.
- **`lexicon_senses`** — PARTITION BY LIST (`word_lang`); PK `(word_lang, id)`; `word_id`, nullable stable V2 `sense_key`. Після `0020`: keyed V2 identity partial-unique по `(word_lang, word_id, sense_key) WHERE sense_key IS NOT NULL`; definition-унікальність діє лише для legacy rows із `sense_key IS NULL`, тому однаковий gloss із різними POS/canonical keys більше не конфліктує. `definition`, `part_of_speech`, `tags text[]`, `position`, `source`, `origin`, `metadata`.
- **`translation_senses`** — V2 many-to-many bridge; PK `(translation_id, sense_word_lang, sense_id)`, cascade-delete від translation. `translations.sense_id` збережено як перший/legacy compatibility link.
- **`lexicon_relations`** — PARTITION BY LIST (`word_lang`); PK `(word_lang, id)`; `word_id`, `sense_id?`, `kind` (`synonym`\|`antonym`\|`related`), `related_text`, `tags text[]`. Після `0021` має окрему case-insensitive partial uniqueness для word-level (`sense_id IS NULL`) і sense-level (`sense_id IS NOT NULL`) звʼязків; однаковий synonym/antonym може коректно існувати у двох senses.
- **`lexicon_word_forms`** — PARTITION BY LIST (`word_lang`); PK `(word_lang, id)`; `word_id`, `form_text`, `tags text[]`. Інфлекції.
- **`lexicon_examples`** — звичайна (не партиціонована) таблиця; `word_lang`+`word_id` (без FK, бо батько партиціонований), `sense_id?`, `text`, `translation_text?`, `translation_lang?`. Індекси за `(word_lang, word_id)` і `(word_lang, word_id, sense_id)`.
- **`translations`** — напрямний міст `source_lang/source_word_id` → `target_lang/target_word_id?` + `target_text`, `confidence`, `source`, `origin`, **`provider_tier` varchar(32)**, `is_primary`, `metadata`, `deleted_at?`. Active partial index виключає tombstones; reverse mirror не створюється.
- **`translation_pair_repairs`** — pending `missing_variants` для конкретної source/target пари після admin soft-delete; пошук атомарно споживає repair і генерує тільки відсутні нові тексти.

**Dictionary API access (`schema/dictionary-api.ts`, migrations 0013/0018):**

- **`dictionary_api_plans`** — protected `system-unlimited` (`NULL/NULL`) і
  `external-standard` (`60/1000`) minute/day policies;
- **`dictionary_api_consumers`** — name/kind/status/plan/scopes/protected lifecycle;
  fixed-id protected system consumers: `client-gateway` лише з
  `dictionary:search` і `translation-uploader` лише з
  `dictionary:lexicon:write`;
- **`dictionary_api_keys`** — public id/display prefix, HMAC digest, pepper version,
  scopes, active/retiring/revoked timestamps і rotation link; **raw key не зберігається**,
  а самі migrations не створюють жодного key row;
- **`dictionary_api_quota_counters`** — atomic per-consumer minute/day windows;
- **`dictionary_api_usage_events`** — append-only safe attribution без search content;
- **`dictionary_admin_audit_events`** — append-only actor/action/target/reason і
  sanitized before/after; немає FK до client-owned admin identities.

`0013` additive/data-preserving: не змінює `users/topics/topic_words` або lexicon;
`0018` лише додає захищений `translation-uploader` без raw/digest key material.
Перед rollout потрібні custom-format backup і запис counts `_migrations`, users,
topics, topic_words, translations, lexicon_words/phrases; після двох послідовних
`npm run db:migrate` другий запуск має бути no-op, application/lexicon counts — ті ж,
protected plans — два, protected system consumers — два, system key count — 0 до
явного admin create.
Ідемпотентний саме filename-tracking runner; raw SQL напряму двічі не запускається.

### 3.3 Перелік міграцій (`vocabee-gateway/src/db/migrations`)

| Файл | Призначення |
|---|---|
| `0001_init.sql` | Базова схема: `languages`, `users`, `refresh_tokens`, `oauth_accounts`, партиціоновані `lexicon_words`/`lexicon_phrases` (+початкові партиції на 7 мов), `lexicon_examples`, `translations`, `topics`, `topic_words`. |
| `0002_premium_flag.sql` | `users.is_premium` bool def false. Фіксує: anonymous = no JWT (рядок не створюється). |
| `0003_sync_timestamps.sql` | `topics.last_synced_at`/`words_updated_at`/`words_synced_at`, `topic_words.last_synced_at` + індекси. Семантика per-row last-write-wins. |
| `0004_provider_tier.sql` | `translations.provider_tier` + бекфіл з `origin` + індекс. Tier-aware кеш. |
| `0005_senses_and_relations.sql` | `lexicon_senses`, `lexicon_relations`, `lexicon_word_forms` (початкові партиції на 7 мов) + `lexicon_examples.sense_id`. |
| `0006_topic_word_knowledge.sql` | `topic_words.knowledge_percent` int NN def 0 + CHECK 0..100. |
| `0007_bee_balance.sql` | `users.bee_balance` int NN def 50 + CHECK ≥ 0 (**D1**). |
| `0008_referral_support.sql` | `users.referral_code` + partial unique index; `support_requests` для гостьових/авторизованих звернень. |
| `0009_add_pt_tr_he_ar_languages.sql` | Партиції `lexicon_words`/`lexicon_phrases`/`lexicon_senses`/`lexicon_relations`/`lexicon_word_forms` для `pt`, `tr`, `he`, `ar`. |
| `0010_add_lt_cs_languages.sql` | Партиції `lexicon_words`/`lexicon_phrases`/`lexicon_senses`/`lexicon_relations`/`lexicon_word_forms` для `lt`, `cs`. |
| `0011_translation_sense_link.sql` | `translations.sense_id` — прив'язка перекладу до sense'а слова-джерела (контекстне тренування); легасі-рядки атрибутуються ліниво на exact-hit. |
| `0012_client_admin.sql` | Additive upgrade: `users.account_status` def `active` + private moderation fields/index; `admin_accounts`, append-only `admin_audit_events` і `rewarded_ad_events`; legacy users/topics/words не видаляє й історичні reward events не синтезує. |
| `0013_dictionary_api_consumers.sql` | Additive/data-preserving: плани й consumers Dictionary API, digest-only ключі, consumer-level quota counters, append-only usage/audit та protected seeds `system-unlimited`, `external-standard`, `client-gateway`; не створює raw/system key і не змінює users/topics/words/lexicon. |
| `0014_translation_lifecycle.sql` | Additive/data-preserving: `translations.deleted_at`, active partial index і `translation_pair_repairs`; прибирає лише старі технічні `metadata.cacheRole=reverse_mirror`, не чіпає users/topics/topic_words або прямі переклади. |
| `0015_user_context_glossary.sql` | Приватні user-scoped пари `word+concrete translation+language direction` та exact sentence occurrences; additive backfill із валідних `topic_words.metadata.details.contextGlossary`. |
| `0016_lexicon_quality_feedback.sql` | Приватний user-scoped фідбек якості прикладів і перекладів; `translations.quality_score` як агрегат для регенерації. |
| `0017_lexicon_translation_senses_v2.sql` | Додає `lexicon_senses.sense_key`, many-to-many `translation_senses` та backfill усіх наявних non-null `translations.sense_id`; стару колонку не видаляє. |
| `0018_dictionary_translation_uploader.sql` | Додає fixed-id protected `translation-uploader` на `system-unlimited` лише зі scope `dictionary:lexicon:write`; key row/raw key не створює, оператор генерує one-time key в адмінці. |
| `0019_translation_senses_sense_fk.sql` | Композитний FK `translation_senses (sense_word_lang, sense_id) → lexicon_senses (word_lang, id)` з `ON DELETE CASCADE`; перед додаванням прибирає orphan-рядки. |
| `0020_lexicon_sense_identity.sql` | Прибирає legacy full uniqueness definition для keyed senses; V2 rows ідентифікуються stable `sense_key`, а definition-унікальність лишається тільки для `sense_key IS NULL`. |
| `0021_lexicon_relation_sense_scope.sql` | Розділяє case-insensitive uniqueness relations на word-level і sense-level, щоб однаковий relation text міг належати різним senses. |
| `0022_training_fields.sql` **[НОВЕ, ще не створена]** | Майбутня наступна вільна міграція; `topic_words`: `times_correct`, `times_wrong`, `box_level` (def 0), `last_reviewed_at`, `due_at` (**D10**). |

---

## 4. Sync-контракт

### 4.1 Delta-pull — `POST /v1/topics/sync`

**Запит `SyncRequestDto`** (`sync.dto.ts:24`) / клієнтський `SyncRequest` (`SyncDtos.kt:20`):
- `since?: string` (ISO8601) — повернути все, що змінилося **строго після** цієї мітки; `null`/відсутній → повний пул.

**Відповідь `SyncResponseDto`** (`sync.dto.ts:35`) / `SyncResponse` (`SyncDtos.kt:25`):

| Поле | Тип | Нотатка |
|---|---|---|
| `topics` | `TopicResponseDto[]` | створені/змінені після `since` |
| `words` | `TopicWordResponseDto[]` | створені/змінені після `since` |
| `deletedTopicIds` | `string[]` | soft-deleted топіки (за `deleted_at > since`) |
| `deletedWordIds` | `string[]` | soft-deleted слова |
| `serverTime` | `string` (ISO8601) | мітка курсора для наступного `since` |
| `lexiconSchemaVersion` | integer | Поточна additive schema server-owned lexical snapshot; mobile default = 1 для rolling deploy |

Клієнт зберігає `serverTime` як новий per-user `since`; `deleted*` → застосовує локально
(purge або `PendingDelete`-чистка). Applied lexical schema також per-user і фіксується
лише після успішної повної заміни Room snapshot.

### 4.2 Push + повний снапшот — `POST /v1/topics/sync/apply`

**Запит `ApplySyncRequestDto`** (`sync.dto.ts:159`) / `ApplySyncRequest` (`SyncDtos.kt:34`):

| Поле | Тип | Нотатка |
|---|---|---|
| `expectedUserId?` | UUID | Captured auth user для цього payload; mismatch із JWT → `403` до будь-яких topic/word writes. Optional лише для backward compatibility; поточний mobile завжди надсилає. |
| `topics?` | `ClientTopicSyncDto[]` | локальні словники (вкл. tombstones) |
| `words?` | `ClientTopicWordSyncDto[]` | локальні слова |
| `replaceServerState?` | boolean (def false) | true → серверні рядки, відсутні в payload, soft-видаляються (режим «затерти серверне», **D9**) |

**`ClientTopicSyncDto`**: `id` (UUID), `name` (1..120), `color` (≤16), `icon?` (≤32), `sourceLang`/`targetLang` (IsIn), `position?` (≥0), `createdAt?`, `updatedAt?` (ISO8601), `deleted?` (bool — tombstone).

**`ClientTopicWordSyncDto`**: `id` (UUID), `topicId` (UUID), `wordText` (1..200), `translationText` (1..200), `ipa?` (≤120), `source` (IsIn `ENTRY_SOURCES`), `origin` (≤80), `metadata?` (object), `knowledgePercent?` (0..100), `addedAt?`, `updatedAt?` (ISO8601), `deleted?` (bool). Mobile projection розділяє `metadata.details` (видимий `WordDetails` без control fields) і `metadata.lexiconSnapshot` (`translationId`, `lexiconSchemaVersion`, `lexiconRevision`).

> Клієнтські дефолти (`SyncDtos.kt:55`): `source="translator"`, `origin="vocabee-mobile"`, `metadata={}`, `knowledgePercent=0`, `deleted=false`. Серверні DTO позиційно вимагають `source`/`origin` (без дефолтів) — клієнт завжди надсилає.

**Відповідь** — той самий `SyncResponseDto` (повний звірений снапшот після застосування).

> **[ЗАРАЗ]** `applySync` сьогодні застосовує зміни й повертає снапшот, але **не** валідує квоти й **не** списує монетки. **[НОВЕ] за D1/D9/D11** `client-gateway` має валідувати квоти та не дозволяти обходити charge через sync. Нові збереження з dictionary result проходять через v2 `words/from-result`; legacy-рядки без історичного charge grandfathered і не refund-яться. Повний алгоритм мержу — `06-sync-and-account-merge.md`; економіка — `04-coins-economy.md`.

### 4.3 Семантика часових міток (сервер, `0003`)

- `topics.last_synced_at` — коли рядок топіка востаннє звіряли з клієнтом.
- `topics.words_updated_at` — коли востаннє торкались колекції слів топіка (add/update/soft-delete будь-якого `topic_words`).
- `topics.words_synced_at` — коли колекцію слів востаннє звіряли (= `words_updated_at` на той момент).
- `topic_words.last_synced_at` — коли цей рядок слова востаннє звіряли.
- Усі nullable: `NULL` = «ніколи не синкалось».

---

## 5. Спостережність (Sentry)

**[ЗАРАЗ]** Обидві сторони звітують в особисту Sentry-org `vocabee` (регіон DE):

- **Клієнт (Android)** — проєкт `android`. `io.sentry:sentry-android` ініціалізується вручну у `VocabeeApplication` (auto-init вимкнено в маніфесті). DSN — з `BuildConfig.VOCABEE_SENTRY_DSN` (ланцюжок `local.properties: vocabee.sentry.dsn` → env `VOCABEE_SENTRY_DSN` → дефолт у `app/build.gradle.kts`); порожній DSN повністю вимикає SDK. `environment`: debug → `development`, release → `production`; діагностичні логи SDK (`isDebug`) — лише в debug-збірках. З коробки: крєші/ANR, сесії (Release Health), breadcrumbs; PII не збирається (дефолт `sendDefaultPii=false`).
- **Бекенд** — проєкт `node-nestjs`, спільний для client- і dictionary-gateway (`src/instrument.ts` читає `SENTRY_DSN`; статуси ≥500 → `captureException` в `ApiExceptionFilter`; Sentry Logs пошуку — doc 13). Runtime fallback logs/events не передають raw query/lemma — діагностика привʼязана до `lexicon_word_id`; curated regeneration coordinates походять із reviewed batch, а не user input. У Coolify (Dockerfile build pack) `SENTRY_DSN`/`SENTRY_ENVIRONMENT` задано напряму на кожному застосунку; compose-маппінг `SENTRY_CLIENT_DSN`/`SENTRY_DICTIONARY_DSN` діє лише в локальному docker-compose.

---

### Зведення розбіжностей код ↔ контекст завдання (для уточнення)

1. Немає `/auth/anonymous` — анонімність реалізована як відсутність JWT (**D2**, міграція 0002). **[ЗАРАЗ]**
2. Маршрут застосування sync — `/v1/topics/sync/apply`, не `/topics/sync`. **[ЗАРАЗ]**
3. `rewarded-ad` не верифікований/не ідемпотентний; економіка ще не сервер-авторитетна в `applySync`/topics — **[НОВЕ]** за D1.
4. Поля знань D10 (`timesCorrect/timesWrong/boxLevel/lastReviewedAt/dueAt`) відсутні і в клієнті, і в Postgres/Room — є лише `knowledgePercent`. **[НОВЕ]** (майбутня `0022_training_fields.sql`, ще не створена, + bump Room до v5).
5. `TOPIC_ICONS` у коді — 11 ключів; набір D7 ширший. **[НОВЕ]**
6. `UpdateTopicDto` ще містить `sourceLang/targetLang` для сумісності, але service
   відхиляє фактичну зміну пари з `400`; семантика D6 «існуючі незмінні» виконується.
7. Promo API (`/v1/promos*`) ще не існує — **[НОВЕ]** за D4 (doc 05).
