# 17 · Довідник API та даних

Повний технічний довідник: ендпоінти gateway, доменні моделі клієнта, схема даних (Room + Postgres) і sync-контракт. Стан кожної поведінки позначено **[ЗАРАЗ]** (як у коді) / **[НОВЕ]** (затверджена зміна) / **[МАЙБУТНЄ]**.

Не дублює: економіку — див. `04-coins-economy.md`; promo — `05-promo-api-and-banners.md`; sync+мерж — `06-sync-and-account-merge.md`; видалення — `07-deletion.md`; мови/мовлення/теми — `08-languages-speech-themes.md`; тренування — `11-practice-training.md`.

Глобальний префікс усіх роутів — `v1` (`vocabee-gateway/src/main.ts:13`). Нижче шляхи наведено повністю (`/v1/...`).

---

## 1. Ендпоінти gateway

Контракт «суб'єкт» нижче:
- **public** — без токена; `OptionalJwtAccessGuard` дозволяє і анонімний, і авторизований виклик.
- **JWT** — обов'язковий `Authorization: Bearer <accessToken>`, гард `JwtAccessGuard`; `@CurrentUser()` дає `{ id }`.

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
| GET | `/v1/auth/me` | JWT | 200 | Поточний авторизований суб'єкт | — | `UserResponseDto` |

`AuthTokens` (форма): `{ accessToken, refreshToken, expiresIn }` — дзеркало клієнтського `AuthTokensResponse` (`AuthResponse.kt:6`).

**DTO:**
- `RegisterDto` (`register.dto.ts`): `email` (IsEmail), `password` (8..72), `displayName?` (≤120), `speakLang?`/`learnLang?` (IsIn кодів мов).
- `LoginDto` (`login.dto.ts`): `email`, `password` (MinLength 1).
- `GoogleAuthDto` (`google-auth.dto.ts`): `idToken` (NotEmpty), `speakLang?`, `learnLang?`.
- `RefreshDto` (`refresh.dto.ts`): `refreshToken` (MinLength 1).

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
| `createdAt` / `updatedAt` | string | ISO8601 |

### 1.4 Wallet (`/v1/wallet`)

`vocabee-gateway/src/wallet/wallet.controller.ts` — під `JwtAccessGuard`.

| Метод | Шлях | Суб'єкт | Код | Призначення | Відповідь |
|---|---|---|---|---|---|
| GET | `/v1/wallet` | JWT | 200 | Поточний баланс монеток (виклик `addBees(id, 0)`) | `UserResponseDto` |
| POST | `/v1/wallet/rewarded-ad` | JWT | 200 | Нарахувати монетки за переглянуту рекламу (`+REWARDED_AD_BEE_AMOUNT`) | `UserResponseDto` |

Константа `REWARDED_AD_BEE_AMOUNT = 10` (`wallet.constants.ts`). Тіло запиту в обох випадках відсутнє.

> **[ЗАРАЗ]** `rewarded-ad` НЕ верифікований і НЕ ідемпотентний — кожен POST безумовно додає +10. **[НОВЕ]** за **D1** має стати верифікованим (звірка з рекламною мережею) та ідемпотентним (де-дуп за токеном винагороди). Деталі — `04-coins-economy.md`.

### 1.5 Search / Lexicon (`/v1/search`)

`vocabee-gateway/src/lexicon/lexicon.controller.ts`.

| Метод | Шлях | Суб'єкт | Призначення |
|---|---|---|---|
| GET | `/v1/search?q=&speak=&learn=` | public (`OptionalJwtAccessGuard`) | Пошук слова/фрази в парі мов |

**Запит `SearchQueryDto`** (`search.dto.ts`): `q` (1..200, trim), `speak` (IsIn кодів — відома мова), `learn` (IsIn кодів — мова, що вивчається).

**Логіка списання та tier** (`lexicon.controller.ts:41-57`):
- tier визначається з рядка `users` за `is_premium` (`tierFromUserRow`): `anonymous` (без токена) / `registered` / `premium`. **[ЗАРАЗ]** `TIER_MAX_RESULTS` у коді = **50/50/50** (капи знято для всіх). Старий Swagger-опис «3/5/10» застарів — відкрите питання й рекомендація синхронізувати описи див. `13-add-word-and-ai-search.md` §13 та `10-edge-cases-and-open-items.md` O3.
- якщо токен присутній — `walletService.spendBees(user.id, TRANSLATION_SEARCH_BEE_COST)`; `TRANSLATION_SEARCH_BEE_COST = 1` (−1 монетка за пошук). Анонім **не** списується (узгоджено з **D2**).
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

`VariantDto`: `knownWord`, `learningWord`, `ipa?`, `audioUrl?`, `partOfSpeech[]`, `examples[]` (`{text, translation?}`), `senses[]` (`{definition, partOfSpeech?, tags[], examples[], synonyms[], antonyms[]}`), `synonyms[]`, `antonyms[]`, `forms[]` (`{text, tags[]}`), `source`, `origin`, `confidence?`, `isPrimary`, `cached`, `match` (`exact`\|`prefix`).

`MetaDto`: `totalAvailable`, `triedProvider`, `providerReason` (`exact_cached`\|`not_a_word`\|`echo`\|`no_provider_data`\|`translated`\|null), `dictionarySource?`, `dictionaryOrigin?`, `beeBalance?`.

> Клієнтський `SearchResponse` (`SearchResponse.kt`) — спрощене дзеркало: `VariantDto.match` не парситься; `SearchMeta` тримає лише `totalAvailable`, `dictionarySource`, `dictionaryOrigin`, `beeBalance` (без `triedProvider`/`providerReason`). `SearchExample` має поле `translation?` (НЕ плоский рядок).

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
> **[ЗАРАЗ]** Економіка (списання −10 за словник понад `FREE_DICTIONARY_LIMIT=2`, перевірка квот ANON) у цих ендпоінтах ще НЕ реалізована як сервер-авторитетна. **[НОВЕ]** за **D1**: `applySync` має валідувати квоти й списувати монетки; DELETE без повернення (**D3**). Деталі — `04`, `06`, `07`.

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

`SupportedLanguage`: `{ code, name, nativeName, speechTag, flag }`. **[ЗАРАЗ]** 7 мов (`supported-languages.ts`): `uk` 🇺🇦, `en` 🇬🇧, `de` 🇩🇪, `es` 🇪🇸, `fr` 🇫🇷, `pl` 🇵🇱, `it` 🇮🇹. `speechTag` (напр. `uk-UA`) живить STT/TTS (**D8** — напрямок STT зберігається по словнику).

### 1.8 Promo (`/v1/promos`) — **[НОВЕ]** (doc 05)

Ще немає в коді. Контракт за **D4** (config-driven Promo API) — джерело істини `05-promo-api-and-banners.md`:

| Метод | Шлях | Суб'єкт | Призначення |
|---|---|---|---|
| GET | `/v1/promos` | JWT | Список активних промо-кампаній (банер+ботомшит) |
| POST | `/v1/promos/{id}/claim` | JWT | Забрати бонус кампанії (верифіковано+ідемпотентно) |
| GET | `/v1/promos/leaderboard/ad-watchers` | JWT | Тижневий лідерборд переглядачів реклами (топ-10 → +50) |

Кампанії D4: `milestone` 10 реклам → +20; `daily_streak` 5 днів → +50; `registration` → +50; `weekly_leaderboard` топ-10 → +50. База +10 за рекламу лишається, промо-бонус — зверху. Повна форма DTO — у `05`.

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

> **[НОВЕ] Поля знань D10** — у поточному `WordEntry` НЕ існують. Гібрид пріоритет+Leitner потребує: `timesCorrect`, `timesWrong`, `boxLevel`, `lastReviewedAt`, `dueAt`. Поточний бекенд/клієнт мають лише `knowledgePercent`. Канон полів — `11-practice-training.md`; план персистенції — §3 (Room) і §3 (Postgres, міграція 0008 **[НОВЕ]**).

### 2.4 WordDetails / WordSense / WordForm (`VocabularyModels.kt:8-40`)

- **`WordDetails`**: `senses: List<WordSense>`, `synonyms: List<String>`, `antonyms: List<String>`, `forms: List<WordForm>`, `partOfSpeech: List<String>`; обчислюване `isEmpty`. Read-only на клієнті, серіалізується в Room як один JSON-блоб.
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

### 3.2 Postgres (gateway) — `vocabee-gateway/src/db/schema`

**`users`** (`schema/users.ts`): `id` uuid PK, `email` text, `password_hash` text, `display_name` text, `speak_lang` varchar(8) NN def `uk`, `learn_lang` varchar(8) NN def `en`, `notifications_enabled` bool NN def true, `dark_theme_enabled` bool NN def false, `is_anonymous` bool NN def false, `is_premium` bool NN def false, **`bee_balance` integer NN def 50** (CHECK ≥ 0), `created_at`/`updated_at` timestamptz NN. Унікальний індекс на `lower(email)` (де `email IS NOT NULL`).

Супутні: **`refresh_tokens`** (`token_hash`, `expires_at`, `revoked_at`, FK→users CASCADE) і **`oauth_accounts`** (`provider`, `provider_account_id`, унік. індекс `(provider, provider_account_id)`).

**`topics`** (`schema/topics.ts`): `id` uuid PK, `user_id` uuid FK→users CASCADE, `name` text, `color` varchar(16), `icon` varchar(32) null, `source_lang`/`target_lang` varchar(8), `position` int def 0, `device_origin_id` text, `created_at`/`updated_at` timestamptz, **`deleted_at` timestamptz** (soft-delete, **D3**), **sync**: `last_synced_at` timestamptz, `words_updated_at` timestamptz NN def now(), `words_synced_at` timestamptz. Індекси: `user_id`; `(user_id, updated_at)`; `(user_id, words_updated_at)`.

**`topic_words`** (`schema/topics.ts`): `id` uuid PK, `topic_id` uuid FK→topics CASCADE, `word_text`/`translation_text` text, `ipa` text, `source_word_lang` varchar(8), `source_word_id` uuid, `source` varchar(16), `origin` text, `device_origin_id` text, `metadata` jsonb def `{}`, **`knowledge_percent` integer NN def 0** (CHECK 0..100), `added_at`/`updated_at` timestamptz, **`deleted_at` timestamptz**, `last_synced_at` timestamptz. Індекси: `topic_id`; `(topic_id, updated_at)`; `(topic_id, last_synced_at)`.

> **[НОВЕ] Поля тренування D10** для `topic_words` (міграція 0008 нижче): `times_correct` int def 0, `times_wrong` int def 0, `box_level` int def 0 (Leitner), `last_reviewed_at` timestamptz, `due_at` timestamptz.

**`languages`** (`schema/languages.ts`): `code` varchar(8) PK, `name`, `native_name`, `speech_tag`, `flag` — довідник, сидиться з `SUPPORTED_LANGUAGES`.

**Лексикон** (`schema/lexicon.ts`) — джерело перекладів/збагачення; **партиціювання LIST за мовою** (`uk, en, de, es, fr, pl, it`), тому PK містить мовний код:
- **`lexicon_words`** — PARTITION BY LIST (`lang`); PK `(lang, id)`; `lemma`, `normalized`, `ipa`, `audio_url`, `part_of_speech text[]`, `source`, `origin`, `metadata` jsonb. Унік. індекс `(lang, normalized)`.
- **`lexicon_phrases`** — PARTITION BY LIST (`lang`); PK `(lang, id)`; `text`, `normalized`, `source`, `origin`, `metadata`. Унік. `(lang, normalized)`.
- **`lexicon_senses`** — PARTITION BY LIST (`word_lang`); PK `(word_lang, id)`; `word_id`, `definition`, `part_of_speech`, `tags text[]`, `position`, `source`, `origin`, `metadata`.
- **`lexicon_relations`** — PARTITION BY LIST (`word_lang`); PK `(word_lang, id)`; `word_id`, `sense_id?`, `kind` (`synonym`\|`antonym`\|`related`), `related_text`, `tags text[]`. Полиморфні зв'язки.
- **`lexicon_word_forms`** — PARTITION BY LIST (`word_lang`); PK `(word_lang, id)`; `word_id`, `form_text`, `tags text[]`. Інфлекції.
- **`lexicon_examples`** — звичайна (не партиціонована) таблиця; `word_lang`+`word_id` (без FK, бо батько партиціонований), `sense_id?`, `text`, `translation_text?`, `translation_lang?`. Індекси за `(word_lang, word_id)` і `(word_lang, word_id, sense_id)`.
- **`translations`** — міст переклад↔переклад; `source_lang/source_word_id` → `target_lang/target_word_id?` + `target_text`, `confidence`, `source`, `origin`, **`provider_tier` varchar(32)** (для рішення про доливання кеша), `is_primary`, `metadata`. Індекси за source/target/provider_tier.

### 3.3 Перелік міграцій (`vocabee-gateway/src/db/migrations`)

| Файл | Призначення |
|---|---|
| `0001_init.sql` | Базова схема: `languages`, `users`, `refresh_tokens`, `oauth_accounts`, партиціоновані `lexicon_words`/`lexicon_phrases` (+партиції на 7 мов), `lexicon_examples`, `translations`, `topics`, `topic_words`. |
| `0002_premium_flag.sql` | `users.is_premium` bool def false. Фіксує: anonymous = no JWT (рядок не створюється). |
| `0003_sync_timestamps.sql` | `topics.last_synced_at`/`words_updated_at`/`words_synced_at`, `topic_words.last_synced_at` + індекси. Семантика per-row last-write-wins. |
| `0004_provider_tier.sql` | `translations.provider_tier` + бекфіл з `origin` + індекс. Tier-aware кеш. |
| `0005_senses_and_relations.sql` | `lexicon_senses`, `lexicon_relations`, `lexicon_word_forms` (партиціоновані на 7 мов) + `lexicon_examples.sense_id`. |
| `0006_topic_word_knowledge.sql` | `topic_words.knowledge_percent` int NN def 0 + CHECK 0..100. |
| `0007_bee_balance.sql` | `users.bee_balance` int NN def 50 + CHECK ≥ 0 (**D1**). |
| **`0008_training_fields.sql` [НОВЕ]** | `topic_words`: `times_correct`, `times_wrong`, `box_level` (def 0), `last_reviewed_at`, `due_at` (**D10**). Ще не написана — узгодити з `11-practice-training.md`. |

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

> **[ЗАРАЗ]** `applySync` сьогодні застосовує зміни й повертає снапшот, але **не** валідує квоти й **не** списує монетки. **[НОВЕ] за D1/D9** він має бути точкою сервер-авторитетної економіки: валідація квот ANON/ліміту словників, списання за словник понад `FREE_DICTIONARY_LIMIT`, підрахунок вартості мержу в монетках і варіанти конфлікту (погодитись / затерти серверне через `replaceServerState=true` / відкинути локальне / скасувати / інший email). Повний алгоритм мержу — `06-sync-and-account-merge.md`; економіка — `04-coins-economy.md`.

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
4. Поля знань D10 (`timesCorrect/timesWrong/boxLevel/lastReviewedAt/dueAt`) відсутні і в клієнті, і в Postgres/Room — є лише `knowledgePercent`. **[НОВЕ]** (міграція 0008 + bump Room до v5).
5. `TOPIC_ICONS` у коді — 11 ключів; набір D7 ширший. **[НОВЕ]**
6. `UpdateTopicDto` дозволяє змінювати `sourceLang/targetLang`, що суперечить D6 «існуючі незмінні». **[НОВЕ]** (уточнити).
7. Promo API (`/v1/promos*`) ще не існує — **[НОВЕ]** за D4 (doc 05).
