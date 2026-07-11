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

`syncVocabularyNow()` (`App.kt:384-403`) збирає **повний** снапшот користувача
(`store.exportCurrentSyncSnapshot(includeDeleted = true)`), мапить його в
`ApplySyncRequest` (`VocabularySyncMapper.kt:29-62`) і шле `POST /v1/topics/sync/apply`
(`KtorVocabeeApi.kt:145-160`).

Мапінг (`VocabularySyncMapper.kt`):
- словник → `ClientTopicSync` з `deleted = syncStatus == PendingDelete`;
- слово → `ClientTopicWordSync` з `deleted = (слово PendingDelete) АБО (його словник PendingDelete)`
  (`VocabularySyncMapper.kt:55-56`) — каскад видалення словника на всі його слова;
- `color = "cover-<coverIndex>"`, `icon` зашитий як `"book"` (`VocabularySyncMapper.kt:37-39`).

Сервер у `applyClientSync` (`topics.service.ts:318-333`):
1. застосовує кожен `topic` через `applyClientTopic` (`:335-395`);
2. застосовує кожне `word` через `applyClientWord` (`:397-458`);
3. якщо `replaceServerState=true` — викликає `softDeleteMissingClientRows` (`:460-511`);
4. повертає **повний** снапшот через `this.sync(userId, null)` (`:332`).

### 1.3 PULL — `syncTopics(since)`

`POST /v1/topics/sync` із тілом `{ since }` (`KtorVocabeeApi.kt:128-143`,
контролер `topics.controller.ts:57-73`). Сервер `sync()` (`topics.service.ts:243-316`):
- читає всі топіки, де `updatedAt > since` **АБО** `wordsUpdatedAt > since`
  (`:248-256`) — другий OR ловить випадок, коли змінилося лише слово, а рядок топіка ні;
- читає слова цих топіків із `updatedAt > since` (`:259-269`);
- штампує `lastSyncedAt = now()` і `wordsSyncedAt = wordsUpdatedAt` (`:274-289`);
- повертає `SyncResponse` із розділенням на активні (`deletedAt IS NULL`) та видалені
  (`deletedAt IS NOT NULL` → `deletedTopicIds`/`deletedWordIds`) (`:305-315`).

### 1.4 Курсор і «брудний» прапорець

- **`serverTime`** (`SyncDtos.kt:30`) — ISO-час відповіді сервера. Клієнт зберігає його
  як `preferencesManager.lastSyncAt` у `markCurrentVocabularySynced` (`VocabeeStore.kt:196-199`).
  Це курсор для наступного `syncTopics(since)`.
- **`localRevisionEpochMillis`** — монотонний лічильник локальних правок
  (`VocabeeStore.kt:458`, інкремент на кожну зміну). `> 0` означає «є незалиті локальні зміни».
  `runStartupSync` (`App.kt:473-486`) дивиться на нього: якщо `> 0` → робить PUSH
  (`syncVocabularyNow()`), інакше робить дельта-PULL від `lastSyncAt`.
- `markCurrentVocabularySynced` обнуляє `localRevisionEpochMillis = 0L` (`VocabeeStore.kt:198`)
  і викликає `repository.markSynced(...)` (`:197`).

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

`startGoogleSignIn` (`App.kt:493-554`): після входу робить `api.syncTopics(null)`
(`:518`). Якщо `hadLocalAnonymousVocabulary && serverHasVocabulary` (`:521`) — відкриває
`SyncConflictSheet` із `PendingSyncConflict` (`:522-529`). Інакше — або переносить локальне
(`moveAnonymousVocabularyToCurrentUser` + PUSH, `:531-538`), або приймає серверне (`:540-541`).

### 4.2 Поточна шторка [ЗАРАЗ] — бінарна

`SyncConflictSheet` (`App.kt:1219-1275`) зараз має 3 кнопки, БЕЗ мержу:

| Кнопка | Дія | Код |
|---|---|---|
| Взяти стан з бекенда | `applyServerSnapshot` + `discardAnonymousVocabulary` | `App.kt:894-900` |
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
| 4 | **[ЗАРАЗ] PUSH завжди шле повний снапшот** | `exportCurrentSyncSnapshot(includeDeleted = true)` (`App.kt:393`) віддає **всі** топіки/слова, а не лише брудні. Дельти на PUSH немає — за великого словника payload росте лінійно (підсилює потребу в ліміті розміру з розділу 3). |
| 5 | **[ЗАРАЗ] `icon` зашитий як `"book"`** | Мапер завжди шле `icon = "book"` (`VocabularySyncMapper.kt:38`), хоча сервер уже зберігає `icon` per-topic. Це втрачає вибір іконки (пор. D7) при синку. |
| 6 | **[ЗАРАЗ] Усі локальні таймстемпи після PULL = 0** | `toVocabularySyncSnapshot` ставить `createdAtEpochMillis=0`, `updatedAtEpochMillis=0`, `updatedLabel=Today` (`VocabularySyncMapper.kt:75-79,89-90`) — серверні часи на клієнт не маппляться, тож «коли оновлено» після синку недостовірне. |
