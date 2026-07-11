# 07. Видалення слів і словників

Цей документ описує всі шляхи видалення слів і словників у Vocabee: локальну
поведінку (Room), синхронізацію з сервером (NestJS gateway, Postgres soft-delete),
питання повернення монеток та затверджену зміну з Undo (рішення **D3**).

Кожна поведінка позначена:
- **[ЗАРАЗ]** — як працює в поточному коді.
- **[НОВЕ]** — затверджена зміна / новий функціонал.
- **[Припущення (уточнити)]** — не зафіксовано рішенням, потребує підтвердження.

Ключові файли:

| Шар | Файл |
|-----|------|
| Стор / події | `VocabeeStore.kt` (`removeTopic` :292, `removeWord` :331) |
| Локальне сховище | `RoomVocabularyRepository.kt` (`removeTopic` :88, `removeWordByTranslation` :168) |
| DAO | `VocabularyDao.kt` (`deleteTopic` :158, `markTopicDeleted` :173, `deleteWordByTranslation` :216, `markWordDeletedByTranslation` :233) |
| UI (словник) | `App.kt` (`DeleteDictionaryConfirmationSheet` :999, dispatch RemoveTopic :811–818) |
| UI (слово) | `App.kt` (`onRemoveWord` :714–717) |
| Сервер | `topics.service.ts` (`softDelete` :146, `deleteWord` :221, sync :305–314, applyClientTopic/Word :335, :397) |
| Схема | `db/schema/topics.ts` (`deletedAt` :35, :73; FK CASCADE :21, :56) |

---

## 1. [ЗАРАЗ] Видалення СЛОВА

### Локальна поведінка — hard vs soft (залежить від користувача)

Точка входу: `VocabeeStore.removeWord(topicId, translation)` (`VocabeeStore.kt:331`),
далі `RoomVocabularyRepository.removeWordByTranslation` (`RoomVocabularyRepository.kt:168`).
Слово ідентифікується **за текстом перекладу** (case-insensitive), бо UI видалення
(`App.kt:714`) тримає лише `translation`, а не `wordId`.

Видалення розгалужується за `userKey`:

| Користувач (`userKey`) | Метод DAO | Тип видалення |
|---|---|---|
| Анонім (`DEFAULT_LOCAL_USER_KEY`) | `deleteWordByTranslation` (`VocabularyDao.kt:216`) | **HARD-delete** — `DELETE FROM vocabulary_words …` |
| Авторизований | `markWordDeletedByTranslation` (`VocabularyDao.kt:233`) | **SOFT-delete** — `sync_status = PendingDelete` |

> Коментар у DAO (`VocabularyDao.kt:201–207`) каже: «Hard-delete by translation …
> **Local-only delete for now**; once topic-word sync is wired, switch to a soft
> delete with `sync_status = PendingDelete` so the server picks it up.» Цей коментар
> **застарів** для авторизованого шляху — soft-delete вже реалізовано
> (`markWordDeletedByTranslation`). Коментар лишається релевантним лише для
> анонімного hard-delete (анонім ніколи не синхронізується, тож hard-delete безпечний).

### Що шлеться на сервер (soft-delete)

Лише для авторизованого користувача. SOFT-видалене слово (`sync_status = PendingDelete`)
експортується через `exportCurrentSyncSnapshot(includeDeleted = true)` (`App.kt:393`)
і шлеться в `applySync`. На сервері `applyClientWord` (`topics.service.ts:397`) при
`word.deleted == true` ставить `deletedAt = now()` (`topics.service.ts:412–419`) — за
умови, що рядок уже існує на сервері. Анонімне слово на сервер **не йде взагалі**
(анонім не синхронізується; `syncVocabularyNow` робить early-return, якщо акаунт не
`Authenticated` — `App.kt:390`).

### Оновлення `words_updated_at`

Після видалення слова локально топік позначається як змінений
(`RoomVocabularyRepository.kt:193–203`): `sync_status` стає `PendingUpdate` (або
лишається `PendingCreate`, якщо топік ще не синхронізований), оновлюється
`updated_at_epoch_millis`. На сервері `deleteWord` (`topics.service.ts:221`) і
`applyClientWord` (`:418`) викликають `bumpTopicWordsTimestamp` (`:236`), що оновлює
`topics.updatedAt` і `topics.wordsUpdatedAt = now()`. Завдяки цьому наступний `sync`
(`:243`) бачить зміну набору слів навіть якщо сам рядок топіка не мінявся (умова
`gt(topics.wordsUpdatedAt, sinceTs)` — `:254`).

### Порожній словник не видаляється

Видалення останнього слова **не видаляє** словник. У `removeWordByTranslation`
немає жодної перевірки «чи лишились слова» — топік завжди зберігається (порожнім).
Це навмисна поведінка: словник лишається доступним для додавання нових слів.

---

## 2. [ЗАРАЗ] Видалення СЛОВНИКА

### Шторка підтвердження

`DeleteDictionaryConfirmationSheet` (`App.kt:999`):
- Заголовок: **«Видалити словник?»**
- Текст: **«Словник «{назва}» і N {слово/слова/слів} у ньому буде видалено.»**
  (правильна українська плюралізація через `ukrainianPlural`, `App.kt:1005`).
- Червоне попередження: **«Цю дію не можна скасувати.»** (`App.kt:1019`).
- Кнопки: «Скасувати» (нейтральна) і «Видалити» (червона).

Підтвердження (`App.kt:811–818`) робить: `RemoveTopic` → `syncVocabularyNow()` →
закрити шторку → якщо відкрито екран цього словника, зробити `removeLastOrNull()`
(вийти з нього).

### Каскад на слова

Точка входу: `VocabeeStore.removeTopic` (`VocabeeStore.kt:292`) →
`RoomVocabularyRepository.removeTopic` (`RoomVocabularyRepository.kt:88`).

| Користувач | Метод | Тип |
|---|---|---|
| Анонім | `deleteTopic` (`VocabularyDao.kt:158`) | **HARD-delete** рядка топіка |
| Авторизований | `markTopicDeleted` (`VocabularyDao.kt:173`) | **SOFT-delete** — `sync_status = PendingDelete` |

**Локальний каскад на слова (Room):** слова видаляються через **FK CASCADE** на рівні
Room-схеми сутності (`WordEntity` має FK на топік) — при hard-delete топіка слова
зникають автоматично. При soft-delete топіка (авторизований) **слова в Room НЕ
позначаються** окремо як PendingDelete; топік просто стає невидимим
(`topicsForUser` фільтрує `sync_status != 'PendingDelete'` — `VocabularyDao.kt:16–22`),
а разом із ним невидимі й слова (бо `wordsForTopics` повертає слова лише для видимих
топіків).

> Явний `deleteWordsForUser` (`VocabularyDao.kt:306`) використовується **не** при
> видаленні одного словника, а лише в `replaceSyncSnapshot` (`:299`) — повне
> перезатирання локальної бази даними сервера.

**Серверний каскад на слова:** `softDelete` (`topics.service.ts:146`) ставить
`deletedAt = now()` спочатку топіку, потім **усім** його словам
(`topics.service.ts:157–160`, `update(topicWords).set({deletedAt}).where(topicId)`).
Те саме робить `applyClientTopic` при `topic.deleted == true` (`:345–359`). Окрім
soft-delete, у схемі є й «жорсткий» FK CASCADE (`topics.ts:56`,
`onDelete: 'cascade'`) — він спрацював би лише при **фізичному** `DELETE` рядка
топіка (наразі сервер фізично топіки не видаляє, тож цей CASCADE — запасний механізм
для майбутнього GC / видалення акаунта `users → topics`, `topics.ts:21`).

### Синхронізація як `deletedTopicIds`

SOFT-видалений топік (`PendingDelete`) експортується з `includeDeleted = true`
(`App.kt:393`) і шлеться в `applySync`. Сервер ставить `deletedAt` і в наступній
відповіді `sync` повертає його id у масиві **`deletedTopicIds`**
(`topics.service.ts:308–310`), а слова — у `deletedWordIds` (`:311–313`). Клієнт за
цими списками прибирає рядки локально.

---

## 3. [ЗАРАЗ] Монетки НЕ повертаються

| Дія | Вартість / повернення |
|---|---|
| Створення 1–2-го словника (в межах `FREE_DICTIONARY_LIMIT = 2`) | 0 монеток |
| Створення 3-го+ словника | −10 (`DICTIONARY_CREATION_BEE_COST`) |
| Видалення будь-якого словника | **0 повернення** |
| Повторне створення словника після видалення | знову −10 |

У коді видалення (`VocabeeStore.removeTopic` :292, `RoomVocabularyRepository.removeTopic`
:88, серверний `softDelete` :146) **немає жодного нарахування монеток** —
повернення коштів не передбачено за дизайном.

Це навмисно (рішення **D3**): запобігає фармінгу за схемою «створив (−10) → видалив
(0) → створив (−10)». Кожне створення понад безкоштовний ліміт коштує заново.

> Зауваження щодо економіки: за рішенням **D1** сервер має стати єдиним авторитетом
> зі списання за створення (зараз монетки списуються і на клієнті у
> `VocabeeStore.createTopic` :277–278, і на сервері у `topics.service.ts:104–106` →
> подвійне списання). Видалення це не зачіпає — повернення немає в жодному з шарів.

---

## 4. [НОВЕ за D3] Undo для слова і для словника

**Затверджена зміна.** І для слова, і для словника додається снекбар
**«Скасувати»** (вікно ~5–10 с) **перед** фактичним застосуванням видалення.
Повернення монеток за цим Undo, як і раніше, **немає** (Undo стосується лише самих
слів/словників, не балансу).

> Це також замінює неправдиве попередження «Цю дію не можна скасувати»
> (`App.kt:1019`) — з Undo дію тепер **можна** скасувати протягом вікна; копірайт
> шторки слід оновити.

### Механіка (рекомендований варіант — відкладене застосування)

1. **Видалення → оптимістичне приховування з UI.** Натискання «Видалити» одразу
   ховає слово/словник з екрана (state) і показує снекбар «Скасувати» з таймером.
   Фактичний запис у Room (soft/hard delete) і `syncVocabularyNow()` **не**
   запускаються негайно.
2. **Скасування протягом вікна** → елемент повертається у видимий стан, нічого не
   записано, синку немає.
3. **Таймер вийшов / снекбар закрито** → застосовується реальний шлях видалення
   (`RemoveWord` / `RemoveTopic` → Room soft/hard delete), потім `syncVocabularyNow()`.

**Альтернатива — відновлення з pending-delete** (для авторизованого, soft-only):
застосувати soft-delete (`PendingDelete`) одразу, але **відкласти** `syncVocabularyNow()`
до закінчення вікна; Undo переводить `sync_status` назад у `Synced`/`PendingUpdate`.
Цей варіант менш безпечний для аноніма (його видалення — hard, відновлення неможливе),
тому базовий варіант — **відкладене застосування** на рівні стора/UI, однаковий для
обох станів користувача.

> Прив'язка до **D3**: «локально — pending-delete до синхронізації; сервер —
> soft-delete». Тобто на сервер ми шлемо видалення лише після завершення вікна Undo —
> до того моменту нічого незворотного не відбувається.

---

## 5. Узгодженість soft/hard

### [ЗАРАЗ] — розбіжність

Видалення **слова** і **словника** використовують **різні стратегії** для одного й
того ж стану користувача — ні, насправді обидва однакові ПО КОРИСТУВАЧУ, але DAO-коментар
і реалізація слова відстають:

| | Анонім | Авторизований |
|---|---|---|
| Слово | hard (`deleteWordByTranslation`) | soft (`markWordDeletedByTranslation`) |
| Словник | hard (`deleteTopic`) | soft (`markTopicDeleted`) |

Фактично логіка вже **симетрична** (анонім → hard, авторизований → soft) для обох.
Проблема в іншому: **застарілий коментар** у `VocabularyDao.kt:201–207`, який стверджує,
що видалення слова «local-only … switch to a soft delete … once topic-word sync is
wired». Word-sync уже підключений (`applyClientWord` :397), отже коментар вводить в оману.

### [НОВЕ] — вирівнювання

1. Оновити/прибрати застарілий коментар у `VocabularyDao.kt:201–207` (word-sync уже є).
2. Чітко зафіксувати інваріант: **анонім = hard-delete** (немає синку, рядки не потрібні),
   **авторизований = soft-delete з `PendingDelete`** — однаково для слів і словників.
3. Узгодити soft-стратегію слова зі словником: при soft-delete **словника** у
   авторизованого розглянути явне позначення його слів як `PendingDelete` локально
   (зараз вони лише «невидимі через невидимий топік»), щоб локальний і серверний стан
   слів збігалися 1:1 і `exportSyncSnapshot(includeDeleted = true)` віддавав ті самі
   видалення, що й сервер.

---

## 6. Неочевидні моменти

| # | Момент | Деталі |
|---|---|---|
| 1 | **Soft-видалені рядки на сервері невидимі, але займають місце** | `listForUser`/`findById`/`sync` фільтрують `isNull(deletedAt)` / `deletedAt === null` (`topics.service.ts:81, 92, 306`). Рядок лишається у таблиці фізично до GC. |
| 2 | **Soft-delete блокує повторне ім'я / id** | Видалений топік усе ще займає свій `id` (PK) у таблиці. Повторне створення з тим самим `id` піде в гілку `existing` у `applyClientTopic` (`:377`) і **воскресить** рядок (`deletedAt: null`, `:392`), а не створить новий. Унікальність назви БД не нав'язує, але id-колізія можлива до фізичного GC. |
| 3 | **`PendingDelete` лишається в Room до `markSynced`** | Локально soft-видалені рядки чистяться лише в `markSynced` через `purgePendingDeletedWords`/`purgePendingDeletedTopics` (`RoomVocabularyRepository.kt:344–345`, `VocabularyDao.kt:280–296`), що викликається з `markCurrentVocabularySynced` (`VocabeeStore.kt:196`) ПІСЛЯ успішного синку. До того вони лежать у БД зі статусом `PendingDelete`. |
| 4 | **Видалення останнього слова не видаляє словник** | Див. розділ 1 — топік завжди лишається, навіть порожній. |
| 5 | **Видалення `PendingCreate` шлеться як delete (0 рядків на сервері)** | Якщо локально створений, але ще не синхронізований топік/слово (`PendingCreate`) видалити, він стане `PendingDelete` і поїде в `applySync` з прапором `deleted`. На сервері такого рядка ще немає → `applyClientTopic`/`applyClientWord` роблять `if (!existing) return` (`topics.service.ts:346, 413`) → **0 рядків зачеплено**, no-op. Локально після `markSynced` рядок видаляється `purge*`. |
| 6 | **Анонімне видалення незворотне** | hard-delete (`deleteTopic` / `deleteWordByTranslation`) фізично прибирає рядок з Room без можливості відновлення — Undo (розділ 4) має бути реалізований як **відкладене застосування** до Room, а не «відновлення з pending», бо для аноніма pending-стану немає. |

---

## 7. Питання приватності / ретеншну

**[Припущення (уточнити)]** — відкрите питання, рішенням не зафіксовано.

Наразі soft-видалені рядки (`topics.deletedAt`, `topic_words.deletedAt`) **ніколи не
видаляються фізично** з Postgres — у `topics.service.ts` немає жодного `DELETE FROM`
чи GC-задачі; усі шляхи лише проставляють `deletedAt = now()`. Це означає:

- Текст слів і перекладів користувача зберігається на сервері необмежено після
  «видалення».
- Для відповідності GDPR (право на стирання, «right to erasure») потрібна окрема
  процедура **hard-delete / GC** soft-видалених рядків після певного retention-вікна,
  а також при видаленні акаунта (`users → topics` FK CASCADE `topics.ts:21` фізично
  прибере топіки лише при фактичному видаленні рядка користувача — це треба перевірити).

**Відкриті питання для уточнення:**
1. Чи має бути фоновий GC, що фізично видаляє рядки з `deletedAt < now() - retention`?
   Який retention (напр. 30/90 днів)?
2. Чи покриває видалення акаунта (Google sign-out / delete account) фізичне стирання
   soft-видалених рядків, чи лише активних?
3. Чи потрібно стирати soft-видалені дані на запит користувача негайно (GDPR DSAR),
   а не чекати GC-вікна?
