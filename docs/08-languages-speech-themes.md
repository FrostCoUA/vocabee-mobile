# 08 — Мови, напрямок перекладу, мовлення і теми словників

Документ описує мовну модель Vocabee: профільні мови, мову окремого словника, напрямок перекладу та його зв'язок зі STT/TTS, бекендне визначення мови (`lang-detect`) і візуальні теми словників (кольори + іконки).

Позначення: **[ЗАРАЗ]** — поточна поведінка коду; **[НОВЕ]** — затверджена зміна (D6/D7/D8); **Припущення (уточнити)** — там, де рішення немає.

---

## 0. Глосарій сутностей

| Поняття | Тип у коді | Що означає |
|---|---|---|
| `userLanguage` («Я розмовляю») | `LanguageOption` | Рідна мова користувача; ціль перекладу за замовчуванням. |
| `learningLanguage` («Я вивчаю») | `LanguageOption` | Мова, яку вчать; джерело перекладу за замовчуванням. |
| `LanguageOption` | `data class` | `VocabularyModels.kt:42` — `code` (ISO-639-1), `name`, `shortName`, `speechTag` (BCP-47, напр. `uk-UA`). |
| `topic.sourceLanguage` / `topic.targetLanguage` | `LanguageOption` на словник | Пара мов конкретного словника (зафіксована на момент створення). |
| `speechTag` | `String` | BCP-47-тег для STT/TTS (`uk-UA`, `en-US`, ...). |
| `coverIndex` | `Int` | Індекс теми-кольору словника. |

Підтримувані мови (джерело істини — бекенд `supported-languages.ts:18`): `uk`, `en`, `de`, `es`, `fr`, `pl`, `it`, `pt`, `tr`, `he`, `ar`, `lt`, `cs` (13 мов; у кожної є `speechTag` і `flag`). Клієнтський локальний список також містить legacy `ru`, але gateway не приймає `ru` як офіційну мову.

---

## 1. Профільні мови (дефолт для нових словників)

### 1.1 Що це
Пара мов у профілі (Налаштування → «Я розмовляю» / «Я вивчаю») — це **дефолт для нових словників** (D6). Це НЕ глобальний перекладач для всіх словників: кожен словник зберігає власну пару.

### 1.2 Інваріант: дві мови не можуть збігатися
**[ЗАРАЗ]** Авто-корекція виконана в обох селекторах `VocabeeStore.kt`:

- `selectSpeakingLanguage` (`VocabeeStore.kt:378`) — якщо нова «розмовляю» = поточній «вивчаю», то «вивчаю» автоматично перемикається на перший інший підтримуваний код (`state.supportedLanguages.first { it.code != language.code }`).
- `selectLearningLanguage` (`VocabeeStore.kt:393`) — симетрично коригує «розмовляю».

Додатково UI не дає вибрати дубль: `LanguageSheet` отримує `excludeCode` (`App.kt:846`, `851`) — мова-«пара» виключена зі списку вибору.

При завантаженні з Preferences інваріант теж тримається: якщо збережена «вивчаю» дорівнює «розмовляю», береться перша інша (`VocabeeStore.kt:256-258`).

### 1.3 Що зберігається в Preferences
**[ЗАРАЗ]** У `PreferencesManager` персистяться лише **коди** мов (не повний `LanguageOption`):

| Ключ | Тип | Джерело |
|---|---|---|
| `userLanguageCode` | `String?` | `PreferencesManager` (інтерфейс), запис у `persistLanguageChoice()` `VocabeeStore.kt:415` |
| `learningLanguageCode` | `String?` | те саме, `VocabeeStore.kt:416` |

`LanguageOption` відновлюється з `supportedLanguages` за збереженим кодом при старті (`VocabeeStore.kt:252-258`). Зміна профільної мови одразу персиститься (`persistLanguageChoice()` викликається в обох селекторах: `VocabeeStore.kt:389`, `405`) і бампає локальну ревізію (`touchLocalRevision()`), щоб синк підхопив зміну.

---

## 2. [НОВЕ за D6] Мова конкретного словника

### 2.1 Поточний стан
**[ЗАРАЗ]** При створенні словника мова **жорстко** береться з профілю, оверрайду немає:

```
createTopicUseCase(
    title = cleanedTitle,
    sourceLanguage = state.learningLanguage,   // VocabeeStore.kt:282
    targetLanguage = state.userLanguage,        // VocabeeStore.kt:283
    coverIndex = coverIndex,
)
```

Тобто **source = learning** («вивчаю»), **target = user** («розмовляю»). У `CreateDictionarySheet` мови показані **read-only** через `LanguageInfoStrip` (`PrototypeBottomSheets.kt:206-209`, `284-307`) — це інформаційний рядок «Мова: 🇬🇧 English → 🇺🇦 Українська», без можливості змінити.

Бекенд уже зберігає пару на словник: топіки мають `source_lang`/`target_lang` на запис (підтверджено постановкою D6), тож модель готова до пер-словникової пари.

### 2.2 Затверджена поведінка
**[НОВЕ за D6]**
1. **Дефолт із профілю.** Нові словники за замовчуванням беруть пару з профілю (як зараз).
2. **Оверрайд при створенні.** У `CreateDictionarySheet` додаємо опційний вибір пари мов для ЦЬОГО словника — `LanguageInfoStrip` (нині read-only) стає інтерактивним (тап → `LanguageSheet` для source і для target, з тим самим інваріантом «не збігаються»). Якщо користувач не чіпає — лишається профільний дефолт.
3. **Існуючі словники незмінні.** Зміна профільної пари впливає ЛИШЕ на МАЙБУТНІ словники. Пара словника фіксується на момент створення і не «їде» за профілем.

### 2.3 Вплив на `createTopic`
**[НОВЕ]** Сигнатуру/виклик `createTopic` (`VocabeeStore.kt:270`) треба розширити опційними `sourceLanguage`/`targetLanguage`-оверрайдами: якщо передані — використовуються вони; інакше fallback на `state.learningLanguage` / `state.userLanguage`. Семантика напрямку зберігається: **source = мова, яку вивчають; target = рідна мова**.

> **Припущення (уточнити):** UX оверрайду в шторці — чи показуємо обидва селектори завжди, чи ховаємо за «Змінити мову». Рекомендація: згорнутий стан = профільний дефолт + кнопка «Змінити», щоб не ускладнювати типовий шлях.

---

## 3. Напрямок перекладу і STT

### 3.1 Як напрямок задає мову розпізнавача
Розпізнавач (`AndroidSpeechInputController.startListening`, `AndroidSpeechInputController.kt:27`) приймає `languageTag` (primary) + `alternativeLanguageTags`. Логіка побудови інтенту:

1. Список кандидатів = `[languageTag] + alternativeLanguageTags`, тримлений, без порожніх і дублів (`AndroidSpeechInputController.kt:129-132`).
2. `primaryLanguage` = перший кандидат (`:133`).
3. В інтент пишуться **обидва** екстра-теги на той самий primary:
   - `EXTRA_LANGUAGE = primaryLanguage` (`:137`)
   - `EXTRA_LANGUAGE_PREFERENCE = primaryLanguage` (`:138`)
   - `EXTRA_LANGUAGE_MODEL = LANGUAGE_MODEL_FREE_FORM` (`:136`)

### 3.2 Звідки береться primary і alternative
**[ЗАРАЗ]** На екрані словника (`DictionaryDetail`) напрямок задає `speechDirectionReversed`:

```
val speechInputLanguage  = if (speechDirectionReversed) topic.targetLanguage else topic.sourceLanguage   // App.kt:2061
val speechOutputLanguage = if (speechDirectionReversed) topic.sourceLanguage else topic.targetLanguage   // App.kt:2062
```

і передається в STT так (`App.kt:2150-2152`):

```
speechInputController.startListening(
    languageTag = speechInputLanguage.speechTag,                       // primary = напрямок розпізнавання
    alternativeLanguageTags = listOf(speechOutputLanguage.speechTag),  // друга мова словника як запасна
    ...
)
```

Тобто **primary = мова, якою користувач зараз диктує** (за замовчуванням `topic.sourceLanguage`, тобто «вивчаю»), **alternative = інша мова словника**. Перемикач у хедері (`onToggleSpeechDirection`, `App.kt:2658`, `2713`) інвертує `speechDirectionReversed` → primary і alternative міняються місцями. У шторці «Додати слово» (`AddWordOverlay.kt:210-212`) primary = `topic.targetLanguage`, alternative = `topic.sourceLanguage` (там диктують переклад).

### 3.3 Android 14+ (UPSIDE_DOWN_CAKE): language detection / switch
**[ЗАРАЗ]** Якщо `SDK_INT >= 34` **і** кандидатів > 1 (`AndroidSpeechInputController.kt:139`), додатково вмикаються:

| Екстра | Значення | Ефект |
|---|---|---|
| `EXTRA_ENABLE_LANGUAGE_DETECTION` | `true` | Розпізнавач сам визначає мову вводу. |
| `EXTRA_ENABLE_LANGUAGE_SWITCH` | `LANGUAGE_SWITCH_BALANCED` | Дозволяє перемикання мови «збалансовано». |
| `EXTRA_LANGUAGE_DETECTION_ALLOWED_LANGUAGES` | `recognitionLanguages` | Детекція ТІЛЬКИ серед дозволених (primary + alternative). |
| `EXTRA_LANGUAGE_SWITCH_ALLOWED_LANGUAGES` | `recognitionLanguages` | Перемикання ТІЛЬКИ серед дозволених. |

Тобто на Android 14+ користувач може продиктувати або source-, або target-мовою словника, і движок підхопить правильну — у межах пари. На Android < 14 цей блок не виконується (див. §7).

### 3.4 [НОВЕ за D8] Напрямок зберігається по словнику
**[ЗАРАЗ]** `speechDirectionReversed` — **ефемерний per-screen стан**:

```
var speechDirectionReversed by remember(topic.id, topic.sourceLanguage.code, topic.targetLanguage.code) {
    mutableStateOf(false)   // App.kt:2045-2047
}
```

`remember(topic.id, ...)` означає: при кожному відкритті словника напрямок скидається на `false`, ніде не персиститься.

**[НОВЕ за D8]** Перемикач напрямку STT має **зберігатися по словнику** (як поле словника / в локальному сторі по `topic.id`), щоб збережений напрямок відновлювався при наступному відкритті словника й переживав перезапуск.

> **Припущення (уточнити):** де зберігати — окреме локальне поле `sttDirectionReversed` на топіку (локально/в синку) vs суто клієнтський per-topic preference. Рекомендація: локальне поле на словнику, бо це властивість словника, а не глобальна.

### 3.5 На що ще впливає напрямок (і де є розбіжність)
| Підсистема | [ЗАРАЗ] яку мову бере | Розбіжність / нотатка |
|---|---|---|
| **STT (розпізнавання)** | primary = напрямок (`topic.sourceLanguage`/`targetLanguage` залежно від `speechDirectionReversed`) | Коректно зав'язано на словник і напрямок. |
| **Search (пошук перекладу)** | **ПРОФІЛЬНІ** `state.userLanguage` / `state.learningLanguage` (`App.kt:699-700`) | **Розбіжність:** пошук НЕ враховує мови словника й напрямок STT. Якщо словник має оверрайдну пару (D6) або користувач перемкнув напрямок, search усе одно йде на профільну пару `(speakLang, learnLang)`. **[НОВЕ — узгодити з D6/D8]:** після D6 search має брати пару зі словника (а не профілю); напрямок STT може впливати на те, яку мову вважати «вводом». Поки не реалізовано. |
| **TTS (озвучення)** | завжди `topic.sourceLanguage.speechTag` (`App.kt:2233`) | Озвучення слова завжди source-мовою словника, **незалежно** від `speechDirectionReversed`. Див. §4. |

---

## 4. TTS (озвучення)

### 4.1 Асинхронна ініціалізація, перший `speak` може мовчати
**[ЗАРАЗ]** `AndroidSpeechOutputController` (`AndroidSpeechOutputController.kt:18`) ініціалізує `TextToSpeech` асинхронно. Поки движок не готовий:

- `speak()` запам'ятовує запит у `pendingUtterance` і **повертається без звуку** (`AndroidSpeechOutputController.kt:53-57`).
- Коли приходить `onInit == SUCCESS`, програється збережений запит (`:24-30`).

**Обмеження:** зберігається лише ОДИН pending-запит (останній перезаписує попередній). Якщо движок ініціалізується невдало (`status != SUCCESS`) — пишеться `Log.w`, pending не програється, звуку не буде (`:31-33`). Тобто **перший тап на «озвучити» одразу після відкриття може бути беззвучним** — звук піде з наступного тапу або коли движок «прогріється».

### 4.2 Озвучення завжди source-мовою (обмеження)
**[ЗАРАЗ]** Виклик `onSpeak(group.sourceWord, topic.sourceLanguage.speechTag)` (`App.kt:2233`) озвучує **слово-джерело** мовою `topic.sourceLanguage`. У `doSpeak` (`AndroidSpeechOutputController.kt:61-70`): `Locale.forLanguageTag(languageTag)` → `setLanguage`. Якщо результат `LANG_MISSING_DATA`/`LANG_NOT_SUPPORTED` — лог і вихід без звуку (`:64-67`).

**Обмеження (позначити):**
- Озвучується ТІЛЬКИ source-слово і ТІЛЬКИ source-мовою. Переклад (target) озвучити не можна, перемикач напрямку STT на TTS **не впливає**.
- Якщо на пристрої немає голосового пакета для мови — тихий фейл (тільки лог), без UI-помилки.

> **Припущення (уточнити):** чи треба дати озвучення target-слова target-мовою (логічно під D8). Зараз — поза скоупом коду.

---

## 5. Бекенд `lang-detect` (визначення мови вводу)

**[ЗАРАЗ]** `LanguageDetector.detectBetween(input, speakLang, learnLang)` (`lang-detect.ts:30`) обирає, до якої з ДВОХ мов `{speakLang, learnLang}` належить ввід. Стратегія:

1. Порожній ввід → `learnLang` (`:32`).
2. Скрипт-евристика: кирилиця / латиниця / іврит / арабська (`:34-58`) — швидко вирішує однолітерні слова й власні назви. Якщо скрипт однозначний і одна з мов пари йому відповідає → вона.
3. `franc-min` для рядків довжиною ≥ 4 (`:60-66`), обмежений набором `FRANC_TO_ISO1` (`:9-24`: `ukr→uk, eng→en, rus→ru, pol→pl, deu→de, spa→es, fra→fr, ita→it, por→pt, tur→tr, heb→he, arb→ar, lit→lt, ces→cs`); результат приймається лише якщо він збігається зі `speakLang` або `learnLang`.
4. **Fallback → `learnLang`** (`:54`) — «припускаємо, що користувач шукає те, що вчить».

**Обмеження (позначити):**
- Детектор **завжди обирає одну з двох переданих мов** — він не призначений визначати «третю» мову. Якщо ввід насправді іншою мовою (поза парою), результат буде `learnLang` за замовчуванням.
- Списки `isCyrillic`/`isLatin` (`:74-83`) ширші за реально підтримувані 13 мов (напр. `ru`, `nl`, `sv`...). Це нешкідливо в межах детекції, але джерело істини підтримуваних мов — `SUPPORTED_LANGUAGE_CODES` (`supported-languages.ts:34`); `franc` обмежений 14 кодами в `FRANC_TO_ISO1` (включно з `ru`, якого немає в `SUPPORTED_LANGUAGES`).
- `ru` присутній у `FRANC_TO_ISO1` та `isCyrillic`, але **відсутній** у `SUPPORTED_LANGUAGES`. Тобто детектор може «впізнати» російську, але мовою словника вона бути не може. Розбіжність нешкідлива (детектор однаково повертає одну з переданих пари), але варто звірити (уточнити).

---

## 6. Теми словників (D7)

### 6.1 [ЗАРАЗ] Кольори-теми
`PrototypeTopicThemes` (`PrototypeDesignSystem.kt:210-219`) — **8 кольорів** (key + `Color`):

| index | key | HEX |
|---|---|---|
| 0 | indigo | `#4F46E5` |
| 1 | blue | `#5B7BFE` |
| 2 | violet | `#7C5CF6` |
| 3 | grape | `#410FA3` |
| 4 | royal | `#3E63DD` |
| 5 | plum | `#9333EA` |
| 6 | teal | `#0E9FA5` |
| 7 | amber | `#E0820C` |

Вибір — у `CreateDictionarySheet` через `SwatchPalette` (`PrototypeBottomSheets.kt:200-203`, `230-247`), зберігається як `coverIndex` (`VocabeeStore.kt:284`). `prototypeTopicTheme(coverIndex)` (`PrototypeDesignSystem.kt:226`) безпечно мапить індекс по модулю розміру списку. Колір використовується як accent хедера/обкладинки (`App.kt:2032`, `1797`, `3371`).

### 6.2 [НОВЕ за D7] Набір ІКОНОК тем
Додаємо до словника **іконку теми** (поряд із кольором). Іконка — окреме поле словника (напр. `iconKey: String`), яке зберігається аналогічно `coverIndex` і синхронізується.

Запропонований набір (14 іконок):

| Ключ | Іконка (підпис UA) | Сценарій використання |
|---|---|---|
| `series` | Серіал / фільм (екран/clapper) | Лексика з улюблених серіалів і фільмів. |
| `book` | Книга / читання | Слова з книжок, читання, література. |
| `travel` | Подорожі (літачок) | Фрази для поїздок, аеропорт, готель. |
| `food` | Їжа (виделка + ложка) | Меню, ресторан, кулінарія, продукти. |
| `work` | Робота (портфель) | Ділова лексика, офіс, листування. |
| `school` | Школа / навчання (рюкзак/диплом) | Навчальні предмети, академічна лексика. |
| `sport` | Спорт / зал (гантель) | Фітнес, спорт, тренування. |
| `music` | Музика (нота) | Тексти пісень, музична термінологія. |
| `nature` | Природа / тварини (листок/лапка) | Флора, фауна, довкілля. |
| `tech` | Техніка / IT (мікросхема/код) | Технічна та IT-лексика. |
| `shopping` | Шопінг (сумка/кошик) | Покупки, магазини, бренди. |
| `kids` | Діти (м'яка іграшка/повітряна кулька) | Дитяча лексика, для/про дітей. |
| `health` | Здоров'я / медицина (хрест/серце) | Медичні терміни, аптека, лікар. |
| `general` | Загальна (зірка / стільник) | Дефолт; мікс слів без конкретної теми. |

> Примітка: у переліку D7 «загальна» згадана як «зірка/стільник» — це бджолина айдентика застосунку, логічний дефолт.

**Пікер іконок** додається в `CreateDictionarySheet` **поряд із кольорами**: після блоку «Колір теми» (`SwatchPalette`, `PrototypeBottomSheets.kt:199-203`) додаємо секцію «Іконка теми» з сіткою іконок (аналог `SwatchPalette`, але з `PrototypeLineIcon`). `onCreate` розширюється до передачі обраного `iconKey` поряд із `coverIndex`; `createTopic`/`createTopicUseCase` і модель словника отримують відповідне поле.

> **Припущення (уточнити):** конкретні гліфи прив'язати до наявного `PrototypeIcon`-набору; для відсутніх — додати у дизайн-систему. Дефолтна іконка для старих словників без `iconKey` = `general`.

---

## 7. Неочевидні моменти / пастки

1. **Зміна профільної мови «осиротлює» наявні словники — це очікувано (D6).**
   **[ЗАРАЗ + НОВЕ]** Профільні селектори (`selectSpeakingLanguage`/`selectLearningLanguage`) міняють лише `state.userLanguage`/`learningLanguage` і персистять коди; вони **не чіпають** уже створені словники. За D6 це **навмисно**: пара словника фіксується на момент створення. Наслідок: після зміни профілю старі словники лишаються на старій парі, а нові беруть нову. Для користувача це може виглядати як «розсинхрон» — варто комунікувати (напр. показувати пару мов на картці/в хедері словника), щоб «осиротіння» було прозорим.

2. **Search бере профільні мови, а не словникові.**
   **[ЗАРАЗ]** Див. §3.5: пошук перекладу зав'язаний на `state.userLanguage/learningLanguage` (`App.kt:699-700`), а STT/іконка/колір — на словник. Після D6 (оверрайд пари) це стає реальним багом UX: словник з нестандартною парою шукатиме переклади профільною парою. **[НОВЕ]** Узгодити search із парою словника.

3. **Android < 14: alternative-мови ігноруються.**
   **[ЗАРАЗ]** Блок language detection/switch виконується лише при `SDK_INT >= 34` і `recognitionLanguages.size > 1` (`AndroidSpeechInputController.kt:139`). На старіших Android `alternativeLanguageTags` **не передаються** в інтент — розпізнавач слухає ТІЛЬКИ `primaryLanguage` (`EXTRA_LANGUAGE`/`EXTRA_LANGUAGE_PREFERENCE`). Тобто на Android < 14 диктувати треба строго primary-мовою поточного напрямку; друга мова словника не підстрахує.

4. **TTS: перший тап після відкриття може бути беззвучним.**
   Див. §4.1 — асинхронна ініціалізація + один pending-слот. Не баг даних, але UX-нюанс; за потреби — індикатор «озвучення готується».

5. **Озвучення тільки source-слова source-мовою.**
   Перемикач напрямку STT (§3.4) на TTS не впливає (`App.kt:2233` завжди `topic.sourceLanguage`). Target-слово не озвучується.

6. **`speechTag` ≠ `code`.** STT/TTS працюють по BCP-47 (`uk-UA`), детекція/DTO — по ISO-639-1 (`uk`). Мапінг лежить у `LanguageOption.speechTag` (клієнт) та `SupportedLanguage.speechTag` (бекенд, `supported-languages.ts`). При додаванні мови потрібні обидва + (на бекенді) міграція партицій (`supported-languages.ts:9-17`).
