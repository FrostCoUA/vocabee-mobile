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
| `client-admin-web` | Реалізований у source: users/premium/sessions/admin accounts/audit через `client-gateway`, server-side фільтр словників за парою мов, без прямого DB-доступу | **[НОВЕ]** independent container, Compose wiring і deployed-browser verification |
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
- **Admin JWT** — окремий short-lived RS256 bearer: issuer
  `vocabee-client-gateway`, audience `vocabee-admin`, `typ='admin'`, pinned `kid`,
  opaque `sub`, fixed role і role-bounded scopes. Він не є mobile JWT, не містить
  email і не має refresh-flow. `AdminAccessGuard` перевіряє credential, потім
  `AdminScopesGuard` — точний scope операції.
- **Dictionary consumer key** — opaque `X-API-Key` для app-neutral search. Він має
  лише allowlisted consumer scopes і не може авторизувати `/admin`; Admin JWT, навпаки,
  не може авторизувати ordinary Dictionary search.

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
| POST | `/v1/admin/auth/google` | public Google exchange | 200 | Google ID token → short-lived RS256 admin bearer, лише для env/DB allowlist |
| GET | `/v1/admin/auth/jwks.json` | public | 200 | RS256 public JWK; private key не виходить із `client-gateway` |
| GET | `/v1/admin/me` | Admin JWT | 200 | `{issuer,subject,role,scopes}` без email |
| GET | `/v1/admin/dashboard` | `client:dashboard:read` | 200 | Агрегати users/status/premium/topics/words/reward events |
| GET | `/v1/admin/users` | `client:users:read` | 200 | Cursor-list; `q?`, `status?`, `premium?`, `limit?`, `cursor?` |
| GET | `/v1/admin/users/:userId` | `client:users:read` | 200 | Allowlisted user summary |
| GET | `/v1/admin/users/:userId/topics` | `client:users:read` | 200 | Словники власника; optional validated `sourceLang?`/`targetLang?` застосовуються в DB до cursor pagination |
| GET | `/v1/admin/users/:userId/topics/:topicId/words` | `client:users:read` | 200 | Слова лише user-owned словника |
| GET | `/v1/admin/users/:userId/rewarded-ad-events` | `client:users:read` | 200 | Append-only +10 history, без `requestId` у DTO |
| GET | `/v1/admin/users/:userId/sessions` | `client:users:read` | 200 | Session id/timestamps та UA/IP, якщо є; без token hash |
| GET | `/v1/admin/admin-accounts` | `admin:manage` | 200 | Immutable env context + окремо cursor-page DB admins |
| GET | `/v1/admin/audit-events` | `client:audit:read` | 200 | Append-only журнал адмін-мутацій |

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

`client-admin-web` **[ЗАРАЗ]** реалізований у source; API і Swagger також реалізовані.
Independent container/Compose deployment і browser E2E ще **[НОВЕ]**. Mobile app ці
routes не викликає.

### 1.3.2 Dictionary administration + API consumers (`dictionary-gateway /v1/admin`)

`vocabee-gateway/src/dictionary-admin/*`, `src/dictionary-access/*`. **[ЗАРАЗ]**
Dictionary admin приймає лише окремий RS256 `admin-access-token`; consumer
`X-API-Key` не є адмінським credential. `super_admin`/`dictionary_admin` отримують
рівно ці scopes:

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
| POST | `/v1/admin/lexicon/import-v1` | `dictionary:lexicon:write` | Multipart `file` з canonical `formatVersion: "v1"` JSONL; локально провалідовані `sourceUnit`/`targetUnit` для кожного перекладу, без AI/кредитів, дублікати пропускаються, а зміни lexical metadata оновлюють існуючий exact translation |
| POST | `/v1/admin/lexicon/quality-feedback` | `dictionary:lexicon:write` | `{targetType: "translation"\|"example", targetId, comment?}`; адмінський dislike не видаляє рядок, додає 100 балів якості один раз для цього адміністратора й повертає поточний бал |
| POST | `/v1/admin/lexicon/translations/:translationId/delete` | `dictionary:lexicon:write` | `{reason}` 3..500; soft-delete + pending pair repair + audit в одній transaction; repeated state →409 |
| POST | `/v1/admin/lexicon/translations/:translationId/restore` | `dictionary:lexicon:write` | `{reason}` 3..500; відновлює останній рядок і скасовує pending repair slot, якщо його ще не спожито; audit atomically |
| GET | `/v1/admin/providers` | `dictionary:providers:read` | Лише configured/active/model/masked hint; provider-secret write route немає |
| GET | `/v1/admin/api-usage` | `dictionary:usage:read` | Consumer/key/outcome/status/duration; `consumerId?`, `keyId?`, `outcome?`, `from?`, `to?`; без query/URL/IP/UA |
| GET | `/v1/admin/audit-events` | `dictionary:audit:read` | Append-only журнал; `actor?`, `action?`, `target?`, inclusive `from?`, exclusive `to?` |
| GET/POST | `/v1/admin/api-consumers` | `dictionary:consumers:read` / `dictionary:consumers:write` | List/create external consumer; server фіксує plan/scopes |
| GET | `/v1/admin/api-consumers/:consumerId` | `dictionary:consumers:read` | Safe consumer summary |
| POST | `/v1/admin/api-consumers/:consumerId/enable` / `disable` | `dictionary:consumers:write` | External lifecycle + `{reason}`; protected system consumer immutable |
| POST | `/v1/admin/api-consumers/:consumerId/keys` | `dictionary:keys:write` | Створити ключ; `{key,rawKey}`, raw видимий один раз |
| GET | `/v1/admin/api-consumers/:consumerId/keys` | `dictionary:consumers:read` | Лише safe summaries без raw/digest/pepper |
| POST | `/v1/admin/api-keys/:keyId/rotate` | `dictionary:keys:write` | Active successor + one-time raw; predecessor `retiring` на 900 с |
| POST | `/v1/admin/api-keys/:keyId/revoke` | `dictionary:keys:write` | Незворотний revoke; останній usable system key захищений 409 |

Перед завантаженням V1-пакета exact-parser endpoint можна запустити без БД через
`npm run db:validate:lexicon-v1 -- <file-or-directory>`; команда має повернути
`invalid=0`. Поточний V1 importer приймає source `entryType="word"`; source phrases
лишаються явними exclusions у versioned manifest, доки phrase persistence/search не
підключені end-to-end.

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
(`{definition, partOfSpeech?, tags[], examples[], synonyms[], antonyms[]}`),
`synonyms[]`, `antonyms[]`, `forms[]` (`{text, tags[]}`), `senseIndex?` (індекс
sense'а, що його рендерить цей переклад; null — не атрибутовано),
`lexicalUnitKind` (`word|phrase|expression|abbreviation`), `registerTags[]`
(`slang|informal|formal|technical|offensive|humorous|internet`), `expansion?`,
`translatedExpansion?`, `meaning?`, `literalTranslation?`, `usageExample?`,
`usageExampleTranslation?`, `source`, `origin`,
`confidence?`, `isPrimary`, `cached`, `match` (`exact`\|`prefix`). Dictionary response
не містить `tier`/`beeBalance`; їх додає тільки client facade.

`MetaDto`: `totalAvailable`, `triedProvider`, `providerReason` (`exact_cached`\|`not_a_word`\|`echo`\|`no_provider_data`\|`translated`\|null), `dictionarySource?`, `dictionaryOrigin?`, `beeBalance?`.

> Клієнтський `SearchResponse` (`SearchResponse.kt`) — спрощене дзеркало:
> **[ЗАРАЗ]** `SearchVariant` ще не десеріалізує `translationId` або `match`;
> durable `translationId` уже є в обох backend response-контрактах, але mobile ще не
> зберігає його. `SearchMeta` тримає лише `totalAvailable`, `dictionarySource`,
> `dictionaryOrigin`, `beeBalance` (без `triedProvider`/`providerReason`).
> `SearchExample` має поле `translation?` (НЕ плоский рядок).
> Lexical metadata персиститься в `translations.metadata`; у `topic_words.metadata`
> мобільний sync кладе весь `WordDetails`, тож окремої SQL-міграції saved words не треба.

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

**`CreateTopicDto`** (`topic.dto.ts`): `name` (1..120), `color` (≤16, hex `#RGB/#RRGGBB` або palette-ключ), `icon?` (з `TOPIC_ICONS`), `sourceLang`/`targetLang` (IsIn — мова, що вивчається / переклад), `position?` (≥0), `deviceOriginId?` (≤64, дедуп sync).

> **`TOPIC_ICONS` [ЗАРАЗ]** (`topic.dto.ts:18`): `book, plane, film, work, food, feelings, music, sport, tech, travel, study` (11 шт.). **[НОВЕ]** за **D7** набір ширший (серіал/книга/подорожі/їжа/робота/школа/спорт/музика/природа/техніка/шопінг/діти/здоров'я/загальна) — узгодити з `08-languages-speech-themes.md`.

**`UpdateTopicDto`**: усі поля опц. — `name?`, `color?`, `icon?`, `sourceLang?`, `targetLang?`, `position?`.

> **D6**: мова словника — дефолт із профілю при створенні, оверрайд у `CreateTopicDto`; існуючі незмінні. **[ЗАРАЗ]** код дозволяє `sourceLang/targetLang` у `UpdateTopicDto` — суперечить «існуючі незмінні»; **[НОВЕ]** прибрати ці поля з патчу (уточнити в `08`).

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

> **[НОВЕ] Поля знань D10** — у поточному `WordEntry` НЕ існують. Гібрид пріоритет+Leitner потребує: `timesCorrect`, `timesWrong`, `boxLevel`, `lastReviewedAt`, `dueAt`. Поточний бекенд/клієнт мають лише `knowledgePercent`. Канон полів — `11-practice-training.md`; план персистенції — §3 (Room) і §3 (Postgres, майбутня `0016_training_fields.sql` **[НОВЕ, ще не створена]**).

### 2.4 WordDetails / WordSense / WordForm (`VocabularyModels.kt:8-40`)

- **`WordDetails`**: `senseIndex: Int?` (значення пари з бекендової атрибуції), `senses: List<WordSense>`, `synonyms: List<String>`, `antonyms: List<String>`, `forms: List<WordForm>`, `partOfSpeech: List<String>`, lexical metadata та `contextGlossary: ContextGlossary?`; обчислюване `isEmpty`. Read-only на клієнті, серіалізується в Room як один JSON-блоб.
- **`ContextGlossary`**: exact `sentence`, `sourceLang`, `targetLang`, `tokens[]`; token містить `surface`, `normalized`, UTF-16 `start/endExclusive`, `translation`, `lemma?`. Окремої Room/Postgres-міграції не треба, бо снапшот їде всередині наявного details/metadata JSON.
- **`WordSense`**: `definition`, `partOfSpeech?`, `tags[]`, `examples: List<String>`, `synonyms[]`, `antonyms[]`. (Зверни увагу: тут `examples` — плоскі `String`, на відміну від серверного `SenseDto.examples` = `{text, translation?}`.)
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

> **[НОВЕ] Поля тренування D10** для `topic_words` (майбутня `0016_training_fields.sql`, ще не створена): `times_correct` int def 0, `times_wrong` int def 0, `box_level` int def 0 (Leitner), `last_reviewed_at` timestamptz, `due_at` timestamptz.

**`languages`** (`schema/languages.ts`): `code` varchar(8) PK, `name`, `native_name`, `speech_tag`, `flag` — довідник, сидиться з `SUPPORTED_LANGUAGES`.

**Лексикон** (`schema/lexicon.ts`) — джерело перекладів/збагачення; **партиціювання LIST за мовою** (`uk, en, de, es, fr, pl, it, pt, tr, he, ar, lt, cs`), тому PK містить мовний код:
- **`lexicon_words`** — PARTITION BY LIST (`lang`); PK `(lang, id)`; `lemma`, `normalized`, `ipa`, `audio_url`, `part_of_speech text[]`, `source`, `origin`, `metadata` jsonb. Унік. індекс `(lang, normalized)`.
- **`lexicon_phrases`** — PARTITION BY LIST (`lang`); PK `(lang, id)`; `text`, `normalized`, `source`, `origin`, `metadata`. Унік. `(lang, normalized)`.
- **`lexicon_senses`** — PARTITION BY LIST (`word_lang`); PK `(word_lang, id)`; `word_id`, `definition`, `part_of_speech`, `tags text[]`, `position`, `source`, `origin`, `metadata`.
- **`lexicon_relations`** — PARTITION BY LIST (`word_lang`); PK `(word_lang, id)`; `word_id`, `sense_id?`, `kind` (`synonym`\|`antonym`\|`related`), `related_text`, `tags text[]`. Полиморфні зв'язки.
- **`lexicon_word_forms`** — PARTITION BY LIST (`word_lang`); PK `(word_lang, id)`; `word_id`, `form_text`, `tags text[]`. Інфлекції.
- **`lexicon_examples`** — звичайна (не партиціонована) таблиця; `word_lang`+`word_id` (без FK, бо батько партиціонований), `sense_id?`, `text`, `translation_text?`, `translation_lang?`. Індекси за `(word_lang, word_id)` і `(word_lang, word_id, sense_id)`.
- **`translations`** — напрямний міст `source_lang/source_word_id` → `target_lang/target_word_id?` + `target_text`, `confidence`, `source`, `origin`, **`provider_tier` varchar(32)**, `is_primary`, `metadata`, `deleted_at?`. Active partial index виключає tombstones; reverse mirror не створюється.
- **`translation_pair_repairs`** — pending `missing_variants` для конкретної source/target пари після admin soft-delete; пошук атомарно споживає repair і генерує тільки відсутні нові тексти.

**Dictionary API access (`schema/dictionary-api.ts`, migration 0013):**

- **`dictionary_api_plans`** — protected `system-unlimited` (`NULL/NULL`) і
  `external-standard` (`60/1000`) minute/day policies;
- **`dictionary_api_consumers`** — name/kind/status/plan/scopes/protected lifecycle;
  seed рівно один fixed-id protected `client-gateway` system consumer;
- **`dictionary_api_keys`** — public id/display prefix, HMAC digest, pepper version,
  scopes, active/retiring/revoked timestamps і rotation link; **raw key не зберігається**,
  а сама migration не створює жодного key row;
- **`dictionary_api_quota_counters`** — atomic per-consumer minute/day windows;
- **`dictionary_api_usage_events`** — append-only safe attribution без search content;
- **`dictionary_admin_audit_events`** — append-only actor/action/target/reason і
  sanitized before/after; немає FK до client-owned admin identities.

`0013` additive/data-preserving: не змінює `users/topics/topic_words` або lexicon.
Перед rollout потрібні custom-format backup і запис counts `_migrations`, users,
topics, topic_words, translations, lexicon_words/phrases; після двох послідовних
`npm run db:migrate` другий запуск має бути no-op, application/lexicon counts — ті ж,
protected plans/consumer — по одному, system key count — 0 до явного admin create.
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
| `0016_training_fields.sql` **[НОВЕ, ще не створена]** | Майбутня наступна міграція після наявної `0015`; `topic_words`: `times_correct`, `times_wrong`, `box_level` (def 0), `last_reviewed_at`, `due_at` (**D10**). |

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

Клієнт зберігає `serverTime` як новий `since`; `deleted*` → застосовує локально (purge або `PendingDelete`-чистка).

### 4.2 Push + повний снапшот — `POST /v1/topics/sync/apply`

**Запит `ApplySyncRequestDto`** (`sync.dto.ts:159`) / `ApplySyncRequest` (`SyncDtos.kt:34`):

| Поле | Тип | Нотатка |
|---|---|---|
| `topics?` | `ClientTopicSyncDto[]` | локальні словники (вкл. tombstones) |
| `words?` | `ClientTopicWordSyncDto[]` | локальні слова |
| `replaceServerState?` | boolean (def false) | true → серверні рядки, відсутні в payload, soft-видаляються (режим «затерти серверне», **D9**) |

**`ClientTopicSyncDto`**: `id` (UUID), `name` (1..120), `color` (≤16), `icon?` (≤32), `sourceLang`/`targetLang` (IsIn), `position?` (≥0), `createdAt?`, `updatedAt?` (ISO8601), `deleted?` (bool — tombstone).

**`ClientTopicWordSyncDto`**: `id` (UUID), `topicId` (UUID), `wordText` (1..200), `translationText` (1..200), `ipa?` (≤120), `source` (IsIn `ENTRY_SOURCES`), `origin` (≤80), `metadata?` (object), `knowledgePercent?` (0..100), `addedAt?`, `updatedAt?` (ISO8601), `deleted?` (bool).

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

### Зведення розбіжностей код ↔ контекст завдання (для уточнення)

1. Немає `/auth/anonymous` — анонімність реалізована як відсутність JWT (**D2**, міграція 0002). **[ЗАРАЗ]**
2. Маршрут застосування sync — `/v1/topics/sync/apply`, не `/topics/sync`. **[ЗАРАЗ]**
3. `rewarded-ad` не верифікований/не ідемпотентний; економіка ще не сервер-авторитетна в `applySync`/topics — **[НОВЕ]** за D1.
4. Поля знань D10 (`timesCorrect/timesWrong/boxLevel/lastReviewedAt/dueAt`) відсутні і в клієнті, і в Postgres/Room — є лише `knowledgePercent`. **[НОВЕ]** (майбутня `0016_training_fields.sql`, ще не створена, + bump Room до v5).
5. `TOPIC_ICONS` у коді — 11 ключів; набір D7 ширший. **[НОВЕ]**
6. `UpdateTopicDto` дозволяє змінювати `sourceLang/targetLang`, що суперечить D6 «існуючі незмінні». **[НОВЕ]** (уточнити).
7. Promo API (`/v1/promos*`) ще не існує — **[НОВЕ]** за D4 (doc 05).
