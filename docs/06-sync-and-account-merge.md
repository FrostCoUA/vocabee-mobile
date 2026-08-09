# 06 — Синхронізація та мерж акаунтів

Документ описує, як Vocabee синхронізує словники/слова між клієнтом (KMP/Compose) і
gateway (NestJS + Postgres), а також як планується мерж локальних анонімних даних із
серверним акаунтом при вході через Google.

Позначки: **[ЗАРАЗ]** — поточний код, **[НОВЕ]** — затверджена зміна (D1/D9),
**[МАЙБУТНЄ]** — концепція, реалізація TBD. Посилання на код у форматі `file:рядок`.

Ключові файли:

| Шар | Файл |
|---|---|
| Мапер клієнт↔DTO | `VocabularySyncMapper.kt` |
| DTO клієнта | `SyncDtos.kt` |
| Контракт API | `VocabeeApi.kt`, `KtorVocabeeApi.kt` |
| Оркестрація на UI | `App.kt` (`startGoogleSignIn`, `syncVocabularyNow`, `runStartupSync`, `SyncConflictSheet`) |
| Стан/прапорці | `VocabeeStore.kt` |
| Сервер | `topics.service.ts`, `topics.controller.ts`, `dto/sync.dto.ts` |

---

## 1. [ЗАРАЗ] Механізм синхронізації

### 1.1 Sync-статуси на клієнті

`SyncStatus` (`VocabularyModels.kt:49-54`) — локальний прапорець «бруду» на кожному
словнику й слові:

| Статус | Значення | Куди йде при push |
|---|---|---|
| `PendingCreate` | Створено локально, ще не на сервері | `deleted=false`, INSERT на сервері |
| `PendingUpdate` | Змінено локально після синку | `deleted=false`, UPDATE на сервері |
| `Synced` | Звірено з сервером | Все одно потрапляє в payload (повний снапшот) |
| `PendingDelete` | Позначено на видалення | `deleted=true` (soft-delete на сервері) |

Нові слова/словники створюються як `PendingCreate` (`FakeVocabularyRepository.kt:47,89`),
будь-яка правка наявного `Synced`-рядка переводить його в `PendingUpdate`
(`FakeVocabularyRepository.kt:94-98,119-123,147-150`).

### 1.2 PUSH — `applySync`

`syncVocabularyNow()` збирає **повний** снапшот явно захопленого користувача
(`store.exportSyncSnapshot(userKey, includeDeleted = true)`), мапить його в
`ApplySyncRequest` і шле `POST /v1/topics/sync/apply` усередині
`VocabularySyncCoordinator`. Запит не покладається на динамічний `currentUserKey` під
час застосування відповіді: він також несе `expectedUserId` із того самого захопленого
auth lease. Gateway порівнює його з JWT principal **до першого запису** і повертає
`403`, якщо payload акаунта A був відправлений або повторений уже з токеном акаунта B.
Поле optional для старих binary, але поточний mobile надсилає його для кожного PUSH.

Мапінг (`VocabularySyncMapper.kt`):
- словник → `ClientTopicSync` з `deleted = syncStatus == PendingDelete`;
- слово → `ClientTopicWordSync` з `deleted = (слово PendingDelete) АБО (його словник PendingDelete)`
  (`VocabularySyncMapper.kt:55-56`) — каскад видалення словника на всі його слова;
- `color = "cover-<coverIndex>"`, `icon` зашитий як `"book"` (`VocabularySyncMapper.kt:37-39`).

Сервер у `applyClientSync` (`topics.service.ts:318-333`):
1. застосовує кожен `topic` через `applyClientTopic` (`:335-395`);
2. застосовує кожне `word` через `applyClientWord` (`:397-458`);
3. якщо `replaceServerState=true` — викликає `softDeleteMissingClientRows` (`:460-511`);
4. перед відповіддю версійно звіряє linked saved words із read-only Dictionary snapshot (D15);
5. повертає **повний** снапшот через `this.sync(userId, null)` (`:332`).

### 1.3 PULL — `syncTopics(since)`

`POST /v1/topics/sync` із тілом `{ since }` (`KtorVocabeeApi.kt:128-143`,
контролер `topics.controller.ts:57-73`). Сервер `sync()` (`topics.service.ts:243-316`):
- читає всі топіки, де `updatedAt > since` **АБО** `wordsUpdatedAt > since`
  (`:248-256`) — другий OR ловить випадок, коли змінилося лише слово, а рядок топіка ні;
- читає слова цих топіків із `updatedAt > since` (`:259-269`);
- штампує `lastSyncedAt = now()` і `wordsSyncedAt = wordsUpdatedAt` (`:274-289`);
- повертає `SyncResponse` із розділенням на активні (`deletedAt IS NULL`) та видалені
  (`deletedAt IS NOT NULL` → `deletedTopicIds`/`deletedWordIds`) (`:305-315`).

### 1.4 Per-user курсор, dirty-ревізія та lease синку

- **`serverTime`** — ISO-час відповіді сервера. Після успішного застосування клієнт
  зберігає його як `preferencesManager.lastSyncAt(userKey)`. Це per-user курсор
  наступного `syncTopics(since)`; акаунти на одному пристрої не ділять його.
- **`localRevisionEpochMillis(userKey)`** — per-user монотонний лічильник локальних
  правок. `> 0` або наявність `Pending*`-рядків у Room означає, що спочатку потрібен
  PUSH; додаткова перевірка Room захищає апгрейд зі старих device-global prefs.
- Усі мережеві vocabulary sync виконуються через один `VocabularySyncCoordinator`
  (`Mutex`). Перед запитом він фіксує `userKey` і локальну ревізію, а перед повною
  заміною Room перевіряє їх ще раз. Відповідь попереднього акаунта або відповідь,
  під час якої користувач локально щось змінив, відкидається без очищення dirty-стану;
  паралельні відповіді не можуть застосуватися у зворотному порядку.
- Мережевий шар окремо фіксує generation bearer-сесії. Після login/logout retry не
  може перечитати токен нового акаунта, старий refresh не може його перезаписати, а
  `expectedUserId` на `applySync` є другим серверним бар'єром до будь-якої мутації.
- `markVocabularySynced(userKey, ...)` пересуває курсор, обнуляє ревізію й маркує
  Room-рядки synced лише для явно переданого користувача після успішного lease.

### 1.5 deletedTopicIds / deletedWordIds

Сервер не присилає видалені рядки в `topics`/`words`, лише їхні id у
`deletedTopicIds`/`deletedWordIds` (`topics.service.ts:308-313`). Клієнт мапить їх у
`VocabularySyncSnapshot.deletedTopicIds/deletedWordIds`
(`VocabularySyncMapper.kt:96-97`), щоб локально прибрати ці рядки.

### 1.6 LWW-таймстемпи (бекенд)

Конфлікти розв'язуються за принципом **Last-Write-Wins** на рівні рядка. Поля
(виходять у `TopicSyncResponse`, `SyncDtos.kt:71-100`):

| Поле | Сенс |
|---|---|
| `updated_at` (топік) | Остання зміна рядка топіка |
| `words_updated_at` | Остання зміна будь-якого слова топіка; bump через `bumpTopicWordsTimestamp` (`topics.service.ts:236-241`) |
| `last_synced_at` | Коли сервер востаннє віддав цей топік у sync (`:276-282`) |
| `words_synced_at` | До якого `words_updated_at` слова вважаються синканими (`:280`) |
| `updated_at` (слово) | Остання зміна слова; `last_synced_at` (слово) штампується при PULL (`:287`) |

> **Важливо [ЗАРАЗ]:** у поточному PUSH-шляху сервер бере `updatedAt` **із клієнта**
> (`topics.service.ts:338,391,406,453`). Тобто LWW-арбітр — таймстемп, який присилає
> недовірений клієнт (див. розділ 2).

### 1.7 Приватний контекстний glossary

`WordDetails.contextGlossary` як і раніше синкається всередині `topic_words.metadata`,
щоб конкретне слово мало offline-снапшот на кожному пристрої. Додатково `applySync`
валідує ці снапшоти й ідемпотентно проєктує токени у серверні
`user_context_glossary_entries/examples`, прив'язані до `user_id`. Ключ entry:
`user + напрямок мов + normalized word/lemma + normalized concrete translation`;
речення й offset — окремий приклад. Завдяки цьому локальний anonymous glossary потрапляє
до акаунта при першому push, а повторний sync не дублює пару чи те саме входження.
Видалення topic/word не каскадить у glossary: він зберігає накопичену історію контекстів.

### 1.8 **[ЗАРАЗ]** Server-authoritative lexical refresh (D15)

Перед кожною auth sync-відповіддю `client-gateway` збирає активні `topic_words` і
батчами звертається до захищеного read-only Dictionary snapshot endpoint. Проєкція
визначається парою мов словника, тому повертає лише senses, linked до активних
translations поточного `sourceLang → targetLang`, і приклади з
`translation_lang = targetLang OR NULL`; однаковий normalized source-example
дедуплікується на сервері з пріоритетом target-specific рядка.

Канонічний payload має schema version і SHA-256 `lexiconRevision`, що охоплює
texts, IPA, senses/examples/relations/forms та lexical metadata. Якщо revision не
змінилася — `updated_at` не рухається. Якщо змінилася — `topic_words` отримує повну
заміну, а `topics.words_updated_at` bump-иться, тому звичайний delta-pull одразу
бачить виправлення. `dictionary-gateway` outage не блокує vocabulary sync: сервер
лишає останній валідний offline snapshot і повторить reconciliation пізніше.

`applySync` більше не вважає весь `metadata.details` client-owned. Для linked rows
старий mobile payload не може відкотити canonical revision або стерти невідоме
нове поле; merge-яться лише progress/delete та валідний `contextGlossary`.
Unlinked/manual слово зберігає provisional snapshot. Legacy binding виконується
лише за однозначним збігом мовної пари + normalized word/translation; неоднозначний
рядок не вгадується. `contextGlossary` переживає звичайний refresh, але
інвалідується, якщо був похідним від canonical sentence, яке сервер замінив.
Прямі `POST/PATCH .../words` проходять через ту саму linked/provisional політику,
тому старий клієнт не обходить D15 поза sync. Невалідний `translationId`-hint
відкидається як hint для конкретного рядка, а не зупиняє весь batch.

---

## 2. [ЗАРАЗ] Обхід лімітів та діри в `applySync` — детально

`applyClientSync` (`topics.service.ts:318-333`) задумувався як «дзеркало» локального
стану, тому **не має жодного enforcement**. Конкретні проблеми:

### 2.1 Немає перевірки квот і списання монеток при INSERT

`applyClientTopic` (`topics.service.ts:362-380`) для нового топіка робить голий
`db.insert(topics)` — **без** перевірки `FREE_DICTIONARY_LIMIT` і **без**
`walletService.spendBees(...)`. Для порівняння, звичайний `create()` (`:99-120`) ці
перевірки робить (`:100-106`).

**Експлойт:** анонім офлайн (`AnonymousFreeWordLimit = 50` обходиться вже тим, що ліміт
лише клієнтський) клепає 20 словників, входить у свіжий Google-акаунт і робить PUSH →
усі 20 словників вставляються безкоштовно, в обхід ліміту 2 безкоштовних і ціни
`DICTIONARY_CREATION_BEE_COST = 10` за кожен наступний.

### 2.2 ID не валідуються при INSERT

При INSERT сервер довіряє `id` з клієнта (`topics.service.ts:363`, `:423` для слів) —
це UUID із недовіреного джерела. Перевірка власності `eq(topics.userId, userId)`
застосовується **тільки в гілці UPDATE/existing** (`:343,394,401,410`), а для нового
рядка id просто записується як є. Клієнт може нав'язати конкретний id.

### 2.3 Таймстемпи з недовіреного клієнта

`createdAt`/`updatedAt`/`addedAt` беруться з payload (`topics.service.ts:337-338,405-406`).
Оскільки конфлікти — LWW, клієнт може поставити `updatedAt` у далеке майбутнє і завжди
«вигравати» мерж, затираючи серверні правки з іншого пристрою.

### 2.4 `replaceServerState=true` — деструктивний

`softDeleteMissingClientRows` (`topics.service.ts:460-511`) soft-видаляє **все**, чого
немає в payload. Якщо клієнт надішле неповний снапшот (баг, частковий експорт, гонитва) з
`replaceServerState=true` — серверні дані з інших пристроїв пропадуть. Зараз цей прапорець
вмикається у гілці «Залити локальний стан» (`App.kt:906`).

### 2.5 `applyClientWord` не дедуплікує

`applyClientWord` (`topics.service.ts:397-458`) розрізняє INSERT/UPDATE лише за збігом
`id` (`:407-410`). Якщо два рядки з різними id мають однакові `wordText`/`translationText`
у тому ж топіку — обидва вставляться. Дедуплікації за змістом (на відміну від клієнтського
`addWord`, `FakeVocabularyRepository.kt:74-78`) тут немає.

### 2.6 Обмеження
- Немає ліміту розміру payload — клієнт може надіслати масив будь-якого розміру.
- Немає rate-limit на `/topics/sync/apply`.

---

## 3. [НОВЕ] Серверне enforcement у `applySync` (за рішенням D1)

D1: **сервер — єдине джерело істини**. `applySync` мусить валідувати квоти й списувати
монетки; клієнт лишається оптимістичним і звіряє баланс із відповіддю.

### 3.1 Правила enforcement

| Правило | Поведінка |
|---|---|
| Квота словників | Для кожного нового топіка понад наявні: якщо загальна к-сть `> FREE_DICTIONARY_LIMIT (2)` → списати `DICTIONARY_CREATION_BEE_COST (10)`. Не вистачає монеток → топік **відхиляється** (не вставляється). |
| Списання — атомарне | Підрахунок вартості + `spendBees` в одній транзакції; часткове застосування або відкат. Усуває баг подвійного списання (зараз і `VocabeeStore.createTopic`, і `topics.service.ts:104-106`). |
| Валідація власності id | INSERT-гілка теж перевіряє, що `id` не належить іншому користувачу; чужий/зайнятий id → відхилення рядка. |
| Довірені таймстемпи | `updatedAt`/`createdAt` сервер ставить сам (`now()`); клієнтські — лише підказка, не арбітр LWW. |
| Дедуплікація слів | Перед INSERT перевіряти збіг за змістом у межах топіка, не лише за id. |
| Розмір payload | Жорсткий ліміт к-сті топіків/слів за запит → `413`/`422` при перевищенні. |
| Rate-limit | Throttle на `/topics/sync/apply` per-user. |
| Ідемпотентність | Повтор того самого payload не подвоює списання (напр. ключ запиту/ревізія). |

### 3.2 Новий контракт відповіді (розширення `SyncResponse`)

Окрім поточних полів, відповідь несе результат enforcement:

```jsonc
{
  "topics": [...], "words": [...],
  "deletedTopicIds": [...], "deletedWordIds": [...],
  "serverTime": "…",
  // [НОВЕ]:
  "newBeeBalance": 30,            // авторитетний баланс після списань
  "applied":  { "topicIds": [...], "wordIds": [...] },
  "rejected": [
    { "id": "…", "kind": "topic", "reason": "QUOTA_EXCEEDED", "requiredBees": 10 },
    { "id": "…", "kind": "topic", "reason": "INSUFFICIENT_BEES" }
  ]
}
```

Коди помилок (приклади): `QUOTA_EXCEEDED`, `INSUFFICIENT_BEES`, `ID_CONFLICT`,
`PAYLOAD_TOO_LARGE`, `RATE_LIMITED`. Клієнт після відповіді **звіряє** `newBeeBalance`
зі своїм оптимістичним і відкочує локальні рядки зі списку `rejected`.

---

## 4. [НОВЕ] Мерж при вході (за рішенням D9) — головне

Коли при вході через Google є **і** локальні анонімні дані, **і** дані на сервері —
замість бінарного вибору («взяти серверне / затерти серверне») пробуємо **справжній мерж**:
залити локальні словники/слова в серверний акаунт, **порахувати вартість у монетках** за
зайві словники й чесно показати юзеру, скільки спишеться.

### 4.1 Звідки береться конфлікт [ЗАРАЗ]

`startGoogleSignIn`: після входу отримує серверний стан через
`VocabularySyncCoordinator`. Якщо для auth-user вже є dirty revision/`Pending*`-рядки,
спершу виконується PUSH; інакше — full pull. Якщо одночасно є локальна анонімна
вокабулярія і серверні дані, відкривається `SyncConflictSheet`, а
`PendingSyncConflict` зберігає captured auth-user revision. Інакше локальне переноситься
й пушиться або серверний snapshot застосовується під тим самим account/revision lease.

### 4.2 Поточна шторка [ЗАРАЗ] — бінарна

`SyncConflictSheet` (`App.kt:1219-1275`) зараз має 3 кнопки, БЕЗ мержу:

| Кнопка | Дія | Код |
|---|---|---|
| Взяти стан з бекенда | Повторно звірити account/revision під coordinator, зробити свіжий `syncTopics(null)`, тоді `applyServerSnapshot` + `discardAnonymousVocabulary`; при локальній зміні попросити підтвердження ще раз | `App.kt`, `VocabularySyncCoordinator.kt` |
| Залити локальний стан | `moveAnonymousVocabularyToCurrentUser` + `syncVocabularyNow(replaceServerState = true)` (затирає серверне!) | `App.kt:901-918` |
| Увійти іншим email | `clearAuthenticatedSessionForAnotherEmail` | `App.kt:919-922` |

Текст шторки прямо каже «Автоматично мержити не можна» (`App.kt:1234`) — саме це й
змінюється за D9.

### 4.3 [НОВЕ] Розширена шторка — 5 варіантів

| # | Варіант | Дія |
|---|---|---|
| 1 | **Погодитись на списання → мерж** | Залити локальні топіки/слова поверх серверних, списати вартість за зайві понадлімітні словники |
| 2 | **Затерти серверні дані локальними** | PUSH із `replaceServerState=true` (поточна «Залити локальний стан») |
| 3 | **Відкинути локальне, лишити серверне** | `applyServerSnapshot` + `discardAnonymousVocabulary` (поточна «Взяти стан з бекенда») |
| 4 | **Нічого не робити → скасувати вхід** | Вийти з акаунта, лишити анонімні дані недоторканими |
| 5 | **(запасний) Увійти іншим email** | `clearAuthenticatedSessionForAnotherEmail` |

Якщо монеток на мерж **не вистачає** → варіант 1 ховаємо, лишаються тільки не-мерж:
2, 3, 4, 5.

### 4.4 Обчислення вартості мержу

```
serverCount      = serverSnapshot.topics.size      // словників уже на акаунті
localToMergeCount = локальні словники, яких немає на сервері (за змістом/id)
afterMerge        = serverCount + localToMergeCount

// скільки нових словників виходить ЗА безкоштовний ліміт:
billable = max(0, afterMerge - max(FREE_DICTIONARY_LIMIT, serverCount))
mergeCost = billable * DICTIONARY_CREATION_BEE_COST   // × 10

canMerge = beeBalance >= mergeCost
```

`max(FREE_DICTIONARY_LIMIT, serverCount)` — щоб уже наявні (хай і понадлімітні) серверні
словники не перераховувалися повторно; платимо лише за **нові**, що додаються мержем.
Приклади:
- сервер 2, локально 3 нових → `billable = (5-2)=3 ⇒ 30 монеток`;
- сервер 5, локально 2 нових → `billable = (7-5)=2 ⇒ 20 монеток`;
- сервер 1, локально 1 новий → `afterMerge=2 ≤ ліміт ⇒ 0 монеток`.

Точну вартість має повертати **сервер** (D1), бо лише він авторитетний; клієнтська
формула — для попереднього показу в шторці.

### 4.5 Дерево рішень при вході

```
Вхід через Google OK
        │
        ▼
syncTopics(null) → serverHasVocabulary?
        │
   ┌────┴───────────────┐
   ні                   так
   │                     │
hadLocalAnon?      hadLocalAnon?
   │                     │
 ┌─┴──┐            ┌──────┴───────┐
 ні  так           ні             так
 │    │            │               │
PULL  PUSH      PULL/PULL      ┌────┴─────────────────────┐  ← КОНФЛІКТ
сервер local→акаунт серверне   │  Порахувати mergeCost     │
                               │  canMerge = bal>=cost      │
                               └────┬───────────────────────┘
                       ┌────────────┴───────────┐
                   canMerge=так              canMerge=ні
                   (варіанти 1–5)            (варіанти 2,3,4,5)
```

Варіант 1 (мерж): локальні топіки/слова заливаються БЕЗ `replaceServerState`, сервер
валідує квоти й списує `mergeCost`, повертає `newBeeBalance` + список `applied/rejected`
(розділ 3.2).

---

## 5. [МАЙБУТНЄ] Мерж акаунтів (TBD)

Окрема функція, не плутати з мержем при вході: перенесення словників **з одного
зареєстрованого акаунта на інший**, коли юзер назбирає монетки.

Концептуально:
- джерело й приймач — два різні Google-акаунти користувача;
- переносяться вибрані словники зі словами;
- за кожен понадлімітний словник у приймачі стягується `DICTIONARY_CREATION_BEE_COST`;
- операція серверна, атомарна, ідемпотентна (як `applySync` за D1);
- UI, ліміти на частоту, поведінка при нестачі монеток — **TBD**.

Реалізація відкладена; зараз достатньо мержу анонім → акаунт (розділ 4).

---

## 6. Неочевидні моменти sync

| # | Момент | Деталі |
|---|---|---|
| 1 | **[ЗАРАЗ] `markSynced` оптимістично чистить `PendingDelete`** | `repository.markSynced` (`FakeVocabularyRepository.kt:185-192`) **сліпо** ставить `Synced` на всі топіки/слова, без поштучного підтвердження від сервера, що delete справді застосовано. Якщо PUSH частково впав/відхилений (розділ 3) — клієнт усе одно вважатиме все синканим. За D1 чистити статус треба лише за списком `applied` з відповіді. |
| 2 | **[ЗАРАЗ] Видалення `PendingCreate`-словника все одно шлеться як delete** | Локально новий словник (`PendingCreate`), якого сервер ще не бачив, при видаленні мапиться в `deleted=true` (`VocabularySyncMapper.kt:42`) і відправляється. На сервері `applyClientTopic` робить early-return, якщо рядка немає (`topics.service.ts:345-346`) — тобто delete по неіснуючому id безпечний, але це зайвий трафік: рядок можна було б просто не слати. |
| 3 | **[ЗАРАЗ] `beeBalance` не йде через vocabulary-sync** | Баланс монеток НЕ передається в `ApplySyncRequest`/`SyncResponse` (`SyncDtos.kt`). Він приходить окремо: через `currentUser()` (`App.kt:460,473`) і `claimRewardedAdBees()` (`App.kt:575`), які кладуть `user.beeBalance` у стор. За D1 авторитетний баланс після списань у `applySync` має повертатися в самій sync-відповіді (`newBeeBalance`, розділ 3.2), щоб не було розсинхрону між списанням за словник і показаним балансом. |
| 4 | **[ЗАРАЗ] PUSH завжди шле повний снапшот** | `exportSyncSnapshot(userKey, includeDeleted = true)` віддає **всі** топіки/слова захопленого користувача, а не лише брудні. Дельти на PUSH немає — за великого словника payload росте лінійно (підсилює потребу в ліміті розміру з розділу 3). |
| 5 | **[ЗАРАЗ] `icon` зашитий як `"book"`** | Мапер завжди шле `icon = "book"` (`VocabularySyncMapper.kt:38`), хоча сервер уже зберігає `icon` per-topic. Це втрачає вибір іконки (пор. D7) при синку. |
| 6 | **[ЗАРАЗ] Усі локальні таймстемпи після PULL = 0** | `toVocabularySyncSnapshot` ставить `createdAtEpochMillis=0`, `updatedAtEpochMillis=0`, `updatedLabel=Today` (`VocabularySyncMapper.kt:75-79,89-90`) — серверні часи на клієнт не маппляться, тож «коли оновлено» після синку недостовірне. |
| 7 | **[ЗАРАЗ D15] App schema upgrade форсить full pull** | Applied lexical schema зберігається per-user. Якщо вона менша за `CLIENT_SUPPORTED_LEXICON_SCHEMA_VERSION`, startup викликає `syncTopics(null)` навіть за порожньої звичайної delta; version фіксується лише після успішного застосування snapshot. |
