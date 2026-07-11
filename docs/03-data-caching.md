# 03 — Модель локальних даних і кешування

Документ описує, як Vocabee зберігає дані на пристрої: Room-база (словники та слова), `Preferences` (налаштування, баланс, курсори синку) і `AuthTokenStore` (токени). Для кожної поведінки явно позначено **[ЗАРАЗ]** (поточний код) і **[НОВЕ]** (затверджена зміна).

Джерела:
- `VocabeeDatabase.kt`, `VocabularyDao.kt`, `VocabeeTypeConverters.kt`
- `entity/TopicEntity.kt`, `entity/WordEntity.kt`
- `RoomVocabularyRepository.kt`
- `AndroidPreferencesManager.kt`, `PreferencesManager.kt`
- `AuthTokenStore.kt`, `UserSessionManager.kt`
- `VocabeeStore.kt`, `VocabularyModels.kt`

---

## 1. Room-схема

База: `VocabeeDatabase`, `version = 4`, `exportSchema = true`, дві таблиці, `@TypeConverters(VocabeeTypeConverters::class)` (`VocabeeDatabase.kt:9`).

### 1.1. Таблиця `vocabulary_topics` (TopicEntity)

Сутність: `TopicEntity.kt:9`. Первинний ключ — `id` (рядковий UUID).
Індекси: `Index(["user_key"])`, `Index(["user_key", "updated_at_epoch_millis"])` (`TopicEntity.kt:11`).

| Колонка (SQL) | Поле Kotlin | Тип | Null? | Призначення / нотатки |
|---|---|---|---|---|
| `id` | `id` | `String` (PK) | ні | UUID словника. Генерується клієнтом у `createTopic` через `UUID.randomUUID()` (`RoomVocabularyRepository.kt:73`). [ЗАРАЗ] |
| `user_key` | `userKey` | `String` | ні | Партиціювання по користувачу. `'local-user'` для аноніма, інакше `currentUserId`. Усі запити DAO фільтрують по `user_key`. Див. §2. |
| `title` | `title` | `String` | ні | Назва словника. |
| `source_language_code` | `sourceLanguageCode` | `String` | ні | ISO-код мови-джерела. Фіксується на момент створення (D6). |
| `target_language_code` | `targetLanguageCode` | `String` | ні | ISO-код мови-цілі. Фіксується на момент створення (D6). |
| `cover_index` | `coverIndex` | `Int` | ні | Індекс обкладинки/кольору словника. [НОВЕ за D7] цей же індекс розшириться до набору ІКОНОК тем (серіал, книга, подорожі тощо); поки що — лише колір. |
| `created_at_epoch_millis` | `createdAtEpochMillis` | `Long` | ні | Час створення (epoch ms). Сортування списку словників — `ORDER BY created_at_epoch_millis ASC` (`VocabularyDao.kt:19`). |
| `updated_at_epoch_millis` | `updatedAtEpochMillis` | `Long` | ні | Час останньої зміни. Оновлюється при додаванні/видаленні слова, зміні knowledge тощо. Базис для людиночитаного лейбла «Сьогодні / Вчора / N днів тому» (`updatedLabelFor`, `RoomVocabularyRepository.kt:405`). |
| `sync_status` | `syncStatus` | `SyncStatus` (enum) | ні | Стан синку рядка. Зберігається як рядок через конвертер (§1.3). Значення: `PendingCreate`, `PendingUpdate`, `PendingDelete`, `Synced`. |

> [НОВЕ за D8] **НАПРЯМОК STT** (пріоритетна мова розпізнавача всередині словника) має зберігатися ПО СЛОВНИКУ — зараз його в схемі немає (тримається лише в пам'яті екрана й скидається при відкритті). Потрібна нова колонка (напр. `stt_direction` / `stt_priority_lang`) + міграція бази. **Припущення (уточнити):** точна назва/тип колонки.

> [НОВЕ за D7] Якщо тему-іконку зберігати окремо від кольору, знадобиться додаткова колонка (напр. `theme_icon`) + міграція. Зараз єдиний візуальний слот — `cover_index`. **Припущення (уточнити):** одна колонка під «колір+іконка» чи дві.

### 1.2. Таблиця `vocabulary_words` (WordEntity)

Сутність: `WordEntity.kt:10`. Первинний ключ — `id`.
Зовнішній ключ: `topic_id → vocabulary_topics.id`, `onDelete = CASCADE` (`WordEntity.kt:12`).
Індекси: `Index(["topic_id"])`, `Index(["user_key"])`, `Index(["user_key","topic_id"])`, `Index(["user_key","added_at_epoch_millis"])` (`WordEntity.kt:20`).

| Колонка (SQL) | Поле Kotlin | Тип | Null? | Призначення / нотатки |
|---|---|---|---|---|
| `id` | `id` | `String` (PK) | ні | UUID слова (`UUID.randomUUID()`, `RoomVocabularyRepository.kt:135`). |
| `user_key` | `userKey` | `String` | ні | Партиціювання по користувачу (дублюється на слові для прямих запитів без джойна по словнику). Див. §2. |
| `topic_id` | `topicId` | `String` (FK) | ні | Належність до словника. CASCADE-видалення разом зі словником. |
| `source` | `source` | `String` | ні | Слово-джерело. |
| `translation` | `translation` | `String` | ні | Переклад. Деякі операції (видалення, дедуп) ключуються по `LOWER(translation)`, бо UI не тримає id слова (`VocabularyDao.kt:208`, `:233`). |
| `ipa` | `ipa` | `String?` | так | Транскрипція IPA. |
| `details_json` | `detailsJson` | `String?` | так | Серіалізований `WordDetails` (значення/синоніми/антоніми/форми) одним JSON-стовпцем, щоб не плодити дочірні таблиці для read-only збагачення, яке сервер може віддати знову (`WordEntity.kt:41`). Кодек: лояльний `Json` (`ignoreUnknownKeys`, `isLenient`), помилка декоду → `null` (`RoomVocabularyRepository.kt:378`). |
| `knowledge_percent` | `knowledgePercent` | `Int` (0..100) | ні (default 0) | Прогрес знання слова. Завжди `coerceIn(0,100)`. Дельта застосовується в `adjustWordKnowledgePercent` (`RoomVocabularyRepository.kt:235`). |
| `added_at_epoch_millis` | `addedAtEpochMillis` | `Long` | ні | Час додавання. Сортування слів — `ORDER BY added_at_epoch_millis DESC` (`VocabularyDao.kt:61`). |
| `updated_at_epoch_millis` | `updatedAtEpochMillis` | `Long` | ні | Час останньої зміни слова. |
| `sync_status` | `syncStatus` | `SyncStatus` (enum) | ні | Стан синку рядка (як у словника). |

### 1.3. SyncStatus + TypeConverter

`VocabeeTypeConverters` (`VocabeeTypeConverters.kt:6`):
- `syncStatusToStorage`: `SyncStatus.name` → `String` (зберігаємо ім'я enum).
- `syncStatusFromStorage`: шукає enum за `name`; **якщо не знайдено — повертає `SyncStatus.PendingUpdate`** (`VocabeeTypeConverters.kt:14`). Див. неочевидний момент §6.6.

| SyncStatus | Семантика | Як використовується |
|---|---|---|
| `PendingCreate` | Створено локально, ще не на сервері | Стартовий статус нового словника/слова (`RoomVocabularyRepository.kt:81`, `:147`). |
| `PendingUpdate` | Існує на сервері, локально змінено | Ставиться при правці слова/словника, якщо рядок уже не `PendingCreate`. Також дефолт невідомого значення з БД. |
| `PendingDelete` | М'яке видалення (soft-delete), чекає синку | Для авторизованого користувача замість фізичного видалення (`markTopicDeleted`/`markWordDeletedByTranslation`). Запити списків виключають `sync_status != 'PendingDelete'`. |
| `Synced` | Узгоджено з сервером | Ставиться в `markSynced` після успішної синхронізації. |

---

## 2. Per-user партиціювання через `user_key`

Усі рядки Room тегуються `user_key`. Один фізичний файл бази тримає дані всіх локальних користувачів цього пристрою; ізоляція — на рівні колонки.

- **`DEFAULT_LOCAL_USER_KEY = "local-user"`** (`VocabularyModels.kt:5`) — ключ АНОНІМНОГО користувача. Будь-які дані, створені до входу через Google, осідають під цим ключем.
- **`currentUserKey`** обчислюється в `PreferencesUserSessionManager` (`UserSessionManager.kt:17`):
  ```kotlin
  override val currentUserKey: String
      get() = preferencesManager.currentUserId ?: DEFAULT_LOCAL_USER_KEY
  ```
  Тобто: є `currentUserId` (увійшов) → ключ = id юзера; немає (анонім або вийшов) → ключ = `'local-user'`.
- `StaticUserSessionManager` (`UserSessionManager.kt:10`) — тестовий варіант, завжди `DEFAULT_LOCAL_USER_KEY`.

**Кожен запит DAO явно приймає `userKey`** і фільтрує `WHERE user_key = :userKey` (читання, лічильники, дедуп, видалення, mark-synced). Тому два акаунти на одному пристрої не бачать дані одне одного, навіть якщо живуть в одній таблиці.

| Стан | `currentUserId` | `currentUserKey` (ключ Room) |
|---|---|---|
| Анонім (чистий старт) | `null` | `'local-user'` |
| Авторизований | id юзера з сервера | id юзера |
| Вийшов (`signOutKeepLastUserState`) | `null` | `'local-user'` ← див. §6.2 |

---

## 3. Що зберігається де

Три сховища з різними ролями.

### 3.1. Room (`VocabeeDatabase`) — синхронізований контент

Лише словники й слова (таблиці `vocabulary_topics`, `vocabulary_words`). Це «істина контенту», яку ганяємо в обидва боки із сервером. Коментар `PreferencesManager.kt:5`: «Room зарезервовано під синхронізований словниковий вміст».

### 3.2. Preferences (`AndroidPreferencesManager`, файл `vocabee_prefs`) — налаштування + курсори

Синхронний `SharedPreferences`, окремий файл, читається раз на старті (`AndroidPreferencesManager.kt:10`).

| Ключ (SharedPreferences) | Поле | Тип / дефолт | Призначення |
|---|---|---|---|
| `has_completed_onboarding` | `hasCompletedOnboarding` | `Boolean` / `false` | Чи завершено перший запуск. `true` → наступний старт Splash→Main. **Device-global** (див. §6.1). |
| `user_language_code` | `userLanguageCode` | `String?` / `null` | Мова, якою користувач говорить (відома). Дефолт пари для нових словників (D6). |
| `learning_language_code` | `learningLanguageCode` | `String?` / `null` | Мова, яку вивчає. Дефолт пари для нових словників (D6). |
| `dark_theme_enabled` | `darkThemeEnabled` | `Boolean` / `false` | Тема UI. |
| `bee_balance` | `beeBalance` | `Int` / `50` (`DEFAULT_BEE_BALANCE`) | Локальний баланс монеток. `set` робить `coerceAtLeast(0)`. Див. §6.7 і §7. |
| `current_user_id` | `currentUserId` | `String?` / `null` | Id поточного авторизованого юзера. Джерело `currentUserKey`. `null` = анонім/вийшов. |
| `last_authenticated_user_id` | `lastAuthenticatedUserId` | `String?` / `null` | Id останнього юзера, що входив (НЕ обнуляється при вийти-зберегти-стан). Див. §6.2. |
| `last_sync_at` | `lastSyncAt` | `String?` / `null` | Серверний час останнього успішного синку (курсор). Ставиться в `markCurrentVocabularySynced`. |
| `local_revision_epoch_millis` | `localRevisionEpochMillis` | `Long` / `0` | Локальний «dirty»-лічильник ревізій. Інкрементиться при локальних змінах (`touchLocalRevision`), скидається в `0` після синку. Див. §6.3. |
| `access_token` | `accessToken` | `String?` / `null` | Bearer-токен (фізично теж у prefs, але доступ — лише через `AuthTokenStore`). |
| `refresh_token` | `refreshToken` | `String?` / `null` | Refresh-токен (аналогічно). |

Інтерфейс — `PreferencesManager` (`PreferencesManager.kt:21`); тестовий двійник — `InMemoryPreferencesManager` (поводиться як чистий інстал, `hasCompletedOnboarding = false`).

### 3.3. AuthTokenStore — токени сесії

`AuthTokenStore` (`AuthTokenStore.kt:13`) — єдине джерело істини для bearer-токена, але **фізично пише в ті самі prefs** (`accessToken`/`refreshToken`). Тримає реактивний `StateFlow<String?>`, щоб мережевий шар читав поточний токен. `current()` звіряється з prefs (якщо хтось змінив поза стором). `set(AuthTokensResponse)` кладе обидва токени; `clear()` стирає обидва й обнуляє state.

> Розмежування ролей: токени фізично живуть у `vocabee_prefs`, але код працює з ними через `AuthTokenStore`, а не напряму через `PreferencesManager` — щоб мати один реактивний потік токена для HTTP-шару.

---

## 4. Кешування: анонім vs авторизований

| Сутність | Анонім (`'local-user'`) | Авторизований (`currentUserId`) |
|---|---|---|
| Словники/слова (Room) | Кешуються локально. **Hard-delete** при видаленні (`userKey == DEFAULT_LOCAL_USER_KEY` → `deleteTopic`/`deleteWordByTranslation`, `RoomVocabularyRepository.kt:95`, `:177`). НЕ синкаються. | Кешуються локально + **синкаються** із сервером. Видалення — **soft-delete** (`PendingDelete`), синк підхоплює. |
| `beeBalance` | [ЗАРАЗ] поле існує, але економіки немає: `addBees`/`spendBees` роблять early-return при `!isAuthenticated` (D2). Анонім «бачить» дефолт 50, але монетки не списуються/не нараховуються. | Синхронізується з сервера: перезаписується в `applyAuthenticatedAccount` і при кожній серверній відповіді (D1). |
| Мови (`userLanguageCode`/`learningLanguageCode`) | Локально в prefs, обираються в онбордингу й зберігаються (D5). | Тягнуться з сервера при вході (`applyAuthenticatedAccount`), пишуться в prefs. |
| Тема (`darkThemeEnabled`) | Локально в prefs. | Перезаписується з сервера в `applyAuthenticatedAccount`. |
| `hasCompletedOnboarding` | Device-global, спільний (§6.1). | Device-global, спільний (§6.1). |
| `lastSyncAt` / `localRevisionEpochMillis` | Технічно ростуть (`localRevisionEpochMillis` інкрементиться й для аноніма), але **синку немає**, тож фактично не використовуються (§6.3). | Активні курсори синку: revision росте на змінах, обнуляється після `markSynced`; `lastSyncAt` = серверний час. |
| Токени (`AuthTokenStore`) | Відсутні (`null`). | Зберігаються; чистяться на logout. |

Що скидається коли:
- **Вхід (анонім→авторизований):** локальні дані аноніма переносяться під ключ юзера (`moveUserVocabulary`), баланс/мови/тема перезаписуються з сервера (§5).
- **`replaceSyncSnapshot`:** повне затирання — видаляє всі рядки `userKey` і вставляє серверні як `Synced` (`RoomVocabularyRepository.kt:293`). Використовується для варіантів «затерти серверне локальним» / «відкинути локальне» (D9).
- **`markSynced`:** чистить `PendingDelete`-рядки (`purgePendingDeleted*`) і переводить решту в `Synced` (`RoomVocabularyRepository.kt:342`).
- **Logout (`signOutKeepLastUserState`):** обнуляє лише `currentUserId`; Room-дані юзера лишаються в базі під його ключем, але стають недосяжними (§6.2).

---

## 5. Перехід анонім → авторизований

Послідовність при вході через Google, коли є локальні анонімні дані.

1. **Перенесення словників/слів — `moveUserVocabulary` (UPDATE in-place):**
   `moveAnonymousVocabularyToCurrentUser` (`VocabeeStore.kt:203`) викликає `repository.moveUserVocabulary(DEFAULT_LOCAL_USER_KEY, currentUserKey)`. Це **не копіювання**, а `UPDATE ... SET user_key = :toUserKey WHERE user_key = :fromUserKey` для обох таблиць у транзакції (`moveTopicsToUser`/`moveWordsToUser`, `VocabularyDao.kt:314`, `RoomVocabularyRepository.kt:351`). Рядки лишаються тими самими (id незмінні), змінюється лише `user_key`. Після — `touchLocalRevision()` (позначити dirty) і перечитування топіків.
   > No-op, якщо `fromUserKey == toUserKey` (`RoomVocabularyRepository.kt:355`).

2. **`beeBalance` — перезапис із сервера:**
   У `applyAuthenticatedAccount` (`VocabeeStore.kt:419`) баланс БЕРЕТЬСЯ З ПОДІЇ (тобто з сервера) і затирає локальне значення: `preferencesManager.beeBalance = event.beeBalance.coerceAtLeast(0)` (`VocabeeStore.kt:430`), і так само в `state`. Анонімний «дефолт 50» НЕ переноситься — авторитет за сервером.

3. **Мови:** з події (`event.speakLang`/`event.learnLang`) із фолбеком на поточні, пишуться в prefs (`VocabeeStore.kt:420`–`428`).

4. **Тема:** `darkThemeEnabled = event.darkThemeEnabled` — перезапис із сервера (`VocabeeStore.kt:429`).

5. **Ідентичність:** `currentUserId = event.userId` і `lastAuthenticatedUserId = event.userId` (`VocabeeStore.kt:431`). З цього моменту `currentUserKey` = id юзера, і всі Room-запити йдуть під ним.

Альтернативні гілки D9 (мерж-конфлікт) на рівні кешу:
- «Затерти серверне локальним» / «Відкинути локальне» → `replaceSyncSnapshot` під ключем юзера.
- «Відкинути анонімне» → `discardAnonymousVocabulary` = `replaceSyncSnapshot(DEFAULT_LOCAL_USER_KEY, empty)` (`VocabeeStore.kt:209`).
- «Погодитись на списання → мерж» → `moveUserVocabulary` + серверне списання монеток за зайві словники (D1/D9).

---

## 6. Неочевидні моменти

### 6.1. `hasCompletedOnboarding` — device-global
[ЗАРАЗ] Прапор онбордингу зберігається БЕЗ прив'язки до `user_key`/`userId` — один булеан на пристрій (`AndroidPreferencesManager.kt:27`). Наслідок: якщо анонім пройшов онбординг і вийшов/перемкнув акаунт, онбординг більше не показується нікому на цьому пристрої. Для D5-розвилки («новий акаунт → показати вибір мов») рішення про показ вибору мов треба приймати за наявністю серверних даних, а не за цим прапором.

### 6.2. `signOutKeepLastUserState`: дані «зависають»
[ЗАРАЗ] `signOutKeepLastUserState` (`VocabeeStore.kt:449`) обнуляє `currentUserId`, але НЕ чіпає `lastAuthenticatedUserId` і НЕ видаляє Room-рядки юзера. Оскільки `currentUserKey = currentUserId ?: DEFAULT_LOCAL_USER_KEY`, після виходу всі запити падають на `'local-user'`. Тобто:
- Словники колишнього юзера лишаються в базі під його id, але стають недосяжними (UI бачить анонімні дані).
- При повторному вході тим самим id (`currentUserId` знову встановлюється) дані «повертаються».
- `lastAuthenticatedUserId` зберігає, хто це був (для UX «увійти знову як …»), але сам по собі ключем доступу до Room НЕ є.

### 6.3. `localRevisionEpochMillis` росте і для аноніма
[ЗАРАЗ] `touchLocalRevision` інкрементить лічильник при будь-якій локальній зміні (`VocabeeStore.kt:457`), у т.ч. для аноніма. Але анонім НЕ синкається, тож для нього лічильник просто росте й ніколи не обнуляється (обнуляється лише `markCurrentVocabularySynced`, `VocabeeStore.kt:199`). Практично для аноніма це мертве значення; стає змістовним лише після входу.

### 6.4. Видалення: anonymous=hard, authenticated=soft
[ЗАРАЗ] Гілка видалення обирається по `userKey == DEFAULT_LOCAL_USER_KEY` (`RoomVocabularyRepository.kt:95`, `:177`): анонім — фізичний `DELETE`; авторизований — `sync_status = PendingDelete` (щоб сервер підхопив). Списки скрізь фільтрують `sync_status != 'PendingDelete'`.
> [НОВЕ за D3] Потрібен **Undo** (снекбар ~5–10с) і для слів, і для словників; монетки за видалення платного словника НЕ повертаються. Локально — pending-delete до синку (що вже відповідає поточному soft-delete для авторизованого); для аноніма «Undo» доведеться будувати поверх pending-стану або відкладеного фактичного видалення, бо зараз воно одразу hard.

### 6.5. Видалення слова ключується по `translation`, не по `id`
[ЗАРАЗ] `removeWordByTranslation`/`deleteWordByTranslation`/`markWordDeletedByTranslation` оперують `LOWER(translation)`, бо overlay додавання слова тримає лише `option.value` (текст перекладу), а не id (`VocabularyDao.kt:201` коментар). Дедуп — по парі `LOWER(source)+LOWER(translation)` (`duplicateWordCount`, `VocabularyDao.kt:112`): один source може мати кілька різних перекладів, і всі вони дозволені.

### 6.6. Конвертер SyncStatus дефолтить невідоме у `PendingUpdate`
[ЗАРАЗ] `syncStatusFromStorage` при невідомому рядку повертає `SyncStatus.PendingUpdate` (`VocabeeTypeConverters.kt:14`). Наслідок: якщо в БД лежить статус, який поточна версія enum не знає (стара/майбутня міграція, пошкоджене значення), рядок безпечно вважається «локально зміненим» і потрапить у наступний синк, а не випаде й не зламає читання. Це консервативний дефолт — гірше було б мовчки вважати його `Synced`.

### 6.7. `beeBalance` НЕ синкається через vocabulary-sync
[ЗАРАЗ] Баланс монеток НЕ є частиною `VocabularySyncSnapshot` і не проходить через `markSynced`/`replaceSyncSnapshot`. Він оновлюється з сервера лише через `applyAuthenticatedAccount` (вхід / `/auth/me`) і, за наявною логікою економіки, через відповіді типу `/search`. Тобто vocabulary-sync і wallet-баланс — два незалежні канали; перетягування словників (`moveUserVocabulary`) баланс не чіпає.

### 6.8. `id` генерується клієнтом
[ЗАРАЗ] І словники, і слова отримують клієнтський `UUID` як PK ще до синку (`RoomVocabularyRepository.kt:73`, `:135`). Сервер має приймати ці id (або мапити), щоб soft-delete/update по id залишались узгодженими після синку.

---

## 7. [НОВЕ за D1] Як зміниться кеш балансу: сервер авторитетний

[ЗАРАЗ] Локальне списання й нарахування монеток роблять `spendBees`/`addBees` у `VocabeeStore`, одразу пишучи `preferencesManager.beeBalance` і `state` (`VocabeeStore.kt:357`–`375`). Це призводить до **подвійного списання** при створенні словника: і на клієнті (`createTopic`), і на сервері (`topics.service.ts`).

[НОВЕ за D1] Цільова модель кешу балансу — **оптимістично, потім звірка**:

1. **Сервер — єдине джерело істини** для балансу, усіх списань (створення словника понад безкоштовний ліміт; пошук перекладу) і лімітів.
2. **Клієнт списує оптимістично** (миттєвий UX): локальний `beeBalance` зменшується одразу для плавності.
3. **Звірка при кожній відповіді/синхронізації:** будь-яка серверна відповідь (`applySync`, `/search`, `/auth/me`) повертає авторитетний баланс, яким клієнт **перезаписує** локальний кеш — так само, як зараз робить `applyAuthenticatedAccount` (`VocabeeStore.kt:430`). Оптимістичне значення відкочується/коригується під серверне.
4. **`applySync` валідує квоти й списує:** відхиляє/обрізає понадлімітне (зайві словники над безкоштовним/наявним лімітом), тож клієнтське оптимістичне створення може бути «відкочене» серверною відповіддю.
5. **Прибрати клієнтське списання за створення словника** (`VocabeeStore.createTopic`), лишивши на клієнті лише оптимістичне відображення + звірку — це усуває подвійне списання.
6. **Нарахування за рекламу** стає верифікованим та ідемпотентним на сервері (SSV / разовий nonce) замість «голого» `POST /wallet/rewarded-ad`. Локальний кеш балансу оновлюється з підтвердженої серверної відповіді, а не оптимістично «+10» наосліп.

Наслідки для сховищ:
- `beeBalance` у prefs лишається **кешем для миттєвого рендера**, але джерело істини — сервер; будь-яке розходження вирішується на користь серверного значення при наступній відповіді.
- Для аноніма все без змін: монеток немає (D2), баланс — лише декоративний дефолт.
