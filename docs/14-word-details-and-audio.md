# 14 — Деталі слова/фрази та озвучення

Документ описує **багату модель деталей слова** (senses, синоніми/антоніми, форми, частини мови, приклади, IPA), як ці деталі відображаються в розгортуваному рядку словника, **звідки вони беруться** (серверне збагачення `lexicon` + `lexicon_examples`), як працює **озвучення (TTS)** і як за D15 офлайн-снапшот перекладу/IPA у `topic_words` версійно оновлюється через vocabulary sync.

Позначення: **[ЗАРАЗ]** — поточна поведінка коду; **[НОВЕ]** — затверджена зміна; **Припущення (уточнити)** — там, де рішення немає.

Не дублює: озвучення-механіку (асинхронна ініціалізація, source-мова, обмеження) детально розкрито в **doc 08 §4** — тут лише підсумок + крос-лінки. Зв'язок напрямку STT/TTS — **doc 08 §3, §4.2**. Кеш/Room-конвертери — **doc 03**.

---

## 0. Глосарій сутностей

| Сутність | Тип | Де визначено |
|---|---|---|
| `WordDetails` | `data class` (mobile) | `VocabularyModels.kt:30` — контейнер усього збагачення слова. |
| `WordSense` | `data class` (mobile) | `VocabularyModels.kt:8` — одне значення слова. |
| `WordForm` | `data class` (mobile) | `VocabularyModels.kt:18` — одна словоформа. |
| `TranslationOption` | `data class` (mobile) | `VocabularyModels.kt:101` — варіант перекладу в Add-Word; несе `details`. |
| `WordEntry.details` | `WordDetails?` | `VocabularyModels.kt:67` — збагачення збереженого слова (nullable). |
| `lexicon_senses` | таблиця (gateway) | Значення слова; V2 додає stable nullable `sense_key`, унікальний у межах lexeme. |
| `lexicon_relations` | таблиця (gateway) | `lexicon.ts:155`, `0005:45` — синоніми/антоніми (`kind`). |
| `lexicon_word_forms` | таблиця (gateway) | `lexicon.ts:186`, `0005:68` — словоформи. |
| `lexicon_examples` | таблиця (gateway) | `lexicon.ts:98` — приклади (можуть линкуватись до sense). |
| `lexicon_words` | таблиця (gateway) | `lexicon.ts:38` — головне слово: `ipa`, `audio_url`, `part_of_speech[]`. |
| `topic_words` | таблиця (gateway) | `topics.ts:50` — слова словника користувача (снапшот). |

---

## 1. Модель деталей слова

### 1.1 `WordDetails` — контейнер збагачення

**[ЗАРАЗ]** `VocabularyModels.kt:30-40`:

```kotlin
data class WordDetails(
    val translationId: String? = null,
    val lexiconSchemaVersion: Int? = null,
    val lexiconRevision: String? = null,
    val senseKeys: List<String> = emptyList(),
    val senseIndex: Int? = null,              // legacy positional attribution
    val senses: List<WordSense> = emptyList(),
    val synonyms: List<String> = emptyList(),   // word-level
    val antonyms: List<String> = emptyList(),   // word-level
    val forms: List<WordForm> = emptyList(),
    val partOfSpeech: List<String> = emptyList(),
    val lexicalUnitKind: LexicalUnitKind = Word,
    val registerTags: List<LexicalRegisterTag> = emptyList(),
    val expansion: String? = null,
    val translatedExpansion: String? = null,
    val meaning: String? = null,
    val literalTranslation: String? = null,
    val usageExample: String? = null,
    val usageExampleTranslation: String? = null,
    val contextGlossary: ContextGlossary? = null,
) {
    // Control fields deliberately do not make a card visually expandable.
    val isEmpty get() = senses.isEmpty() && synonyms.isEmpty() &&
        antonyms.isEmpty() && forms.isEmpty() && partOfSpeech.isEmpty() &&
        lexicalUnitKind == Word && registerTags.isEmpty() &&
        expansion.isNullOrBlank() && translatedExpansion.isNullOrBlank() &&
        meaning.isNullOrBlank() && literalTranslation.isNullOrBlank() &&
        usageExample.isNullOrBlank() && usageExampleTranslation.isNullOrBlank() &&
        contextGlossary == null

    val hasLexiconSnapshot get() = !translationId.isNullOrBlank() ||
        lexiconSchemaVersion != null || !lexiconRevision.isNullOrBlank()
    val shouldPersist get() = !isEmpty || hasLexiconSnapshot
}
```

- **Read-only на мобільному**: сервер — єдине джерело істини для всього всередині (`VocabularyModels.kt:23-28`). Клієнт не редагує lexical details і не маскує серверні повтори локальним дедупом; sync повністю замінює snapshot. Єдиний client-owned підоб'єкт — персональний `contextGlossary`.
- **Контекстний снапшот**: `ContextGlossary(sentence, sourceLang, targetLang, tokens[])`; кожен `ContextGlossaryToken` має exact `surface`, normalized-форму, UTF-16 `start/endExclusive`, контекстний `translation` і опційний `lemma`. Він добудовується gateway після локального save, а не руками користувача, і дає офлайн-попапи в тренуванні.
- `isEmpty` керує лише видимим розгортанням і навмисно ігнорує три opaque control
  fields. `shouldPersist` додатково враховує `hasLexiconSnapshot`, тому навіть варіант
  без видимих senses/examples не губить identity/schema/revision, потрібні наступному
  серверному refresh (`RemoteLexiconSearchUseCase` робить `.takeIf { it.shouldPersist }`).
- **Два рівні синонімів/антонімів:** word-level (`WordDetails.synonyms/antonyms`) і sense-level (`WordSense.synonyms/antonyms`). У бекенді обидва зберігаються в `lexicon_relations` з nullable `sense_id` (`0005:49`); у UI зараз рендеряться лише word-level (§3.3).

### 1.2 Структурний тип і регістр — різні виміри

**[ЗАРАЗ]** `LexicalUnitKind` має `Word / Phrase / Expression / Abbreviation`:

- `Phrase` — композиційна фраза, сенс якої читається зі складників;
- `Expression` — сталий вислів/ідіома з окремим значенням і, за можливості, дослівним перекладом;
- `Abbreviation` — абревіатура або акронім із `expansion` мовою вивчення та `translatedExpansion` відомою мовою.

`LexicalRegisterTag` незалежно додає `Slang / Informal / Formal / Technical / Offensive / Humorous / Internet`. Тому `LOL` коректно моделюється як **Abbreviation + Slang + Internet**, а не як взаємовиключний «тип сленг». Для нетипових одиниць AI також повертає `meaning`, `literalTranslation` і пару `usageExample`/`usageExampleTranslation`. Старі JSON-снапшоти без цих полів десеріалізуються з дефолтом `Word` + порожні метадані; список і тренування локально позначають багатослівні значення як `Фраза`, а короткі uppercase-значення як `Абревіатура`, не запускаючи повторний платний пошук.

### 1.3 `WordSense` — одне значення

**[ЗАРАЗ]** `VocabularyModels.kt:8-15`:

| Поле | Тип | Значення | Серверне джерело |
|---|---|---|---|
| `senseKey` | `String?` | Stable V2 identity значення; null для legacy backend row без персистованого key або старого локального snapshot | `lexicon_senses.sense_key` |
| `definition` | `String` | Текст означення (обов'язкове) | `lexicon_senses.definition` (`lexicon.ts:132`) |
| `partOfSpeech` | `String?` | Частина мови саме цього значення | `lexicon_senses.part_of_speech` (`lexicon.ts:133`) |
| `tags` | `List<String>` | Грам./стильові мітки значення | `lexicon_senses.tags` (`lexicon.ts:134`) |
| `examples` | `List<String>` | Приклади речень для значення | `lexicon_examples` з `sense_id` (`lexicon.ts:104`) |
| `synonyms` | `List<String>` | Синоніми значення | `lexicon_relations kind='synonym'` зі `sense_id` |
| `antonyms` | `List<String>` | Антоніми значення | `lexicon_relations kind='antonym'` зі `sense_id` |

### 1.4 `WordForm` — словоформа (1/2/3 форми дієслів)

**[ЗАРАЗ]** `VocabularyModels.kt:18-21`:

```kotlin
data class WordForm(val text: String, val tags: List<String> = emptyList())
```

- `text` — сама форма (`played`, `plays`, `playing` для `play`).
- `tags` — граматика форми (`["past","participle"]`). Семантично `tags` тут означають **інше**, ніж у synonyms/antonyms, тому в бекенді словоформи лежать в окремій таблиці `lexicon_word_forms`, а не в `lexicon_relations` (`0005:17-18`, `lexicon.ts:181-185`).
- «1/2/3 форми дієслів» (інфінітив / past simple / past participle) виражаються набором `WordForm` з відповідними `tags`. Окремого enum форм нема — це просто список із грам-мітками.

### 1.5 `partOfSpeech: List<String>` — частини мови всього слова

**[ЗАРАЗ]** Word-level список усіх частин мови (слово може бути і дієсловом, і іменником). Серверне джерело — `lexicon_words.part_of_speech` (text-array, `lexicon.ts:47`). Відрізняється від `WordSense.partOfSpeech` (per-sense, один рядок).

---

## 2. Звідки беруться деталі (серверне збагачення)

### 2.1 Серверні таблиці lexicon

**[ЗАРАЗ]** Збагачення зливається з кількох партиціонованих (по `word_lang`) таблиць навколо `lexicon_words`:

| Таблиця | Що дає | Ключові поля |
|---|---|---|
| `lexicon_words` | головне слово | `ipa`, `audio_url`, `part_of_speech[]`, `lemma`, `normalized` (`lexicon.ts:38-65`) |
| `lexicon_senses` | значення | `sense_key?` (stable V2 identity), `definition`, `part_of_speech`, `tags[]`, `position` (лише ordering), `source` |
| `translation_senses` | зв'язки V2 | many-to-many `translation_id → (sense_word_lang, sense_id)`; один переклад може відповідати кільком значенням |
| `lexicon_relations` | синоніми + антоніми (одна полі-таблиця) | `kind` ∈ `synonym\|antonym\|related`, `related_text`, `sense_id?` (`lexicon.ts:155`, `RELATION_KINDS` `:265`) |
| `lexicon_word_forms` | словоформи | `form_text`, `tags[]` (граматика) (`lexicon.ts:186`) |
| `lexicon_examples` | приклади | `text`, `translation_text?`, `sense_id?` (NULL = на все слово) (`lexicon.ts:98`) |

- **Походження даних:** Free Dictionary API (`GET .../entries/{lang}/{word}`) — джерело форми схеми (`0005:3-6`); також provider'и `dictionary`/`translator`/`ai` і curated seed `seed` (`ENTRY_SOURCES` `lexicon.ts:24`). `source='ai'` — LLM-збагачення (напр. `openai-dictionary.provider.ts`); `source='seed'` — reviewed/imported seed data з `origin` на кшталт `vocabee-translate/...`.
- **Curated seed є авторитетним:** V2 `seed-import` переносить reviewed IPA, якщо Kaikki має однозначну вимову, stable sense keys, examples і повну translation↔senses атрибуцію. Відсутній IPA допустимий і не запускає AI. Звичайний пошук не дописує до такого слова IPA/приклади/значення через dictionary або sense AI; прогалину виправляють у V2 batch і повторно імпортують. V1 імпорт збережений як legacy-сумісність.
- **Runtime також V2:** dictionary senses отримують той самий детермінований key-контракт; OpenAI sense attribution повертає `senseKeys[]` і може прив'язати один переклад до кількох значень. Для старих provider-cache rows exact search робить одноразовий lazy upgrade з `sense_id` до `translation_senses`.
- **Партиціонування:** усі lexicon-таблиці партиціоновані `LIST (word_lang)` — по партиції на мову (`uk,en,de,es,fr,pl,it`, `0005:90`). PK включає `word_lang`, бо Postgres вимагає ключ партиції в PK (`lexicon.ts:36`).
- **Без FK до партиціонованого батька:** `lexicon_examples` і relations тримають `(word_lang, word_id)` як «м'який» лінк — цілісність на рівні застосунку, бо FK до партиціонованих таблиць обмежені (`lexicon.ts:93-97`).
- **`sense_id` опційний** усюди (examples/relations): NULL → застосовується до слова в цілому; не-NULL → до конкретного значення (`lexicon.ts:104`, `0005:49`).
- **Дедуплікація:** після `0021_lexicon_relation_sense_scope.sql` word-level relation унікальна по `(word, kind, lower(text)) WHERE sense_id IS NULL`, а sense-level — по `(word, sense_id, kind, lower(text)) WHERE sense_id IS NOT NULL`. Той самий синонім може належати двом різним значенням, але повтор у межах одного значення лишається ідемпотентним. Форми окремо унікальні по `(word, lower(form_text))`.

### 2.2 Як це доходить до мобільного

**[ЗАРАЗ]** Деталі **не тягнуться окремим запитом** на мобільному — вони приходять у **відповіді пошуку** в момент, коли користувач шукає слово в Add-Word. Маппінг wire → доменна модель: `RemoteLexiconSearchUseCase.kt:82-115` (`SearchVariant.toOption`):

```kotlin
details = WordDetails(
    translationId = translationId,
    lexiconSchemaVersion = lexiconSchemaVersion,
    lexiconRevision = lexiconRevision,
    senseKeys = senseKeys,
    senseIndex = senseIndex,
    senses = senses.map { WordSense(it.definition, it.partOfSpeech, it.tags,
        it.examples.map { ex -> ex.text }, it.synonyms, it.antonyms) },
    synonyms = synonyms,
    antonyms = antonyms,
    forms = forms.map { WordForm(it.text, it.tags) },
    partOfSpeech = partOfSpeech,
    lexicalUnitKind = lexicalUnitKind,
    registerTags = registerTags,
    expansion = expansion,
    translatedExpansion = translatedExpansion,
    meaning = meaning,
    literalTranslation = literalTranslation,
    usageExample = usageExample,
    usageExampleTranslation = usageExampleTranslation,
).takeIf { it.shouldPersist } // null лише без visible details і control snapshot
```

- **Момент персисту:** коли користувач тапає «+», `RoomVocabularyRepository` кодує збагачення через свій `WordDetailsJsonCodec` і записує в Room **одним JSON-блобом** (`details_json`). `VocabeeTypeConverters` тут не бере участі — він конвертує `SyncStatus`. Це миттєвий офлайн-снапшот відповіді пошуку; для auth-користувача наступний sync зв'язує його з Dictionary translation/lexeme, додає `lexiconRevision`/`lexiconSchemaVersion` і надалі замінює свіжою канонічною проєкцією (D15, §5).
- **Частковий пошук не створює обрізаний снапшот:** prefix-підказка (`circumstanc → circumstance`) уже містить персистовані IPA/PoS/senses/examples/forms/relations і `senseKeys` так само, як exact cache-hit. Сервер читає деталі лише для 15 варіантів, які повертає, і не запускає dictionary/AI або repair side effects під час autocomplete. Якщо старий sense ще не має персистованого key, DTO чесно несе `senseKey=null`, а не обчислений, але відсутній у БД ID.
- **`WordEntry.details` nullable**: `null`, якщо слово додане до появи цього поля або
  відповідь не має ані видимого збагачення, ані opaque snapshot identity. Результат без
  senses/examples, але з `translationId`/revision, зберігає control-only `WordDetails`.

### 2.3 AI-маркування (звідки «AI»)

**[ЗАРАЗ]** Джерело варіанта мапиться в `TranslationOptionNote` (`RemoteLexiconSearchUseCase.kt:84-91`):

| `source` (wire) | Note | Сенс |
|---|---|---|
| `dictionary` | `Primary` | зі словникового провайдера |
| `translator` | `Primary` | з перекладача (DeepL/MyMemory/…) |
| `ai` | `Additional` | LLM-збагачення |
| інше | `Alternative` | запасне |
| вже додано | `AlreadyAdded(source)` | дублікат у словнику |

- **AI-атрибуція в UI:** футер списку результатів Add-Word завжди показує рядок **«Переклади та приклади згенеровано AI»** (`AddWordOverlay.kt:791-795`) — незалежно від тиру (`footerCaptionFor` ігнорує per-tier капи, які знято на сервері). Це і є «AI-тег» збагачення в потоці додавання.
- В **самих деталях збереженого слова** (розгорнутий рядок словника, §3) окремого per-sense AI-бейджа **зараз нема** — `WordDetailsBlock` не рендерить джерело. Атрибуція живе лише на етапі пошуку.

> **Припущення (уточнити):** чи треба окремий AI-бейдж на рівні розгорнутих деталей збереженого слова (напр. коли `source='ai'`). Зараз — поза скоупом коду; атрибуція тільки у футері Add-Word.

---

## 3. Як деталі відображаються (розгортуваний рядок)

### 3.1 Розгортання рядка

**[ЗАРАЗ]** `WordRow` (`App.kt:2900`) і груповий аналог (`App.kt:~2870`):

- `hasDetails = word.details != null && !word.details.isEmpty` (`App.kt:2909`).
- Рядок розгортуваний, якщо `canExpand = hasDetails || sourceTextOverflows || translationTextOverflows` (`App.kt:2912`) — тобто або є деталі, або обрізаний довгий текст source/translation.
- Тап по рядку (`clickable enabled = canExpand`, `App.kt:2932-2936`) тогглить `expanded`. У розгорнутому стані `source`/`translation` показуються повністю (`maxLines = Int.MAX_VALUE`, `:2953`, `:2976`).
- Шеврон `ChevronDown` обертається на 180° при `expanded && canExpand` і фарбується `accent`, якщо розгортуваний, інакше `Muted3` (`App.kt:3001-3007`).
- При `expanded && word.details != null` рендериться `WordDetailsBlock` (`App.kt:3011-3016`).

### 3.2 `WordDetailsBlock` — порядок секцій

**[ЗАРАЗ]** `App.kt:3040-3078`. Картка з градієнтом + рамкою; секції в порядку:

1. **Тип + регістр** — чипи «Фраза» / «Вислів» / «Абревіатура» та «Сленг» / «Неформальне» тощо; нижче — наявні «Що означає», «Розшифровка», «Розшифровка перекладу», «Дослівно», приклад і його переклад.
2. **Контекстний приклад** — exact `contextGlossary.sentence` показується одним цілісним реченням із клікабельними span-ами, як у тренуванні. Тап по залежному слову відкриває popup із його конкретним контекстним перекладом; `+` одразу додає пару до поточного словника за 1 монетку, а вже наявна пара показує `✓` і не дублюється. Усі входження слова, яке вивчається, виділені, але не мають підказки чи дії. Окремого технічного списку `surface — translation` немає.
3. **Senses** — backend V2 зберігає всі зв'язки перекладу через `translation_senses` і повертає `senses[].senseKey` + `variant.senseKeys[]`; перший зв'язок також проєктується в legacy `senseIndex`. Клієнт конкретного варіанта показує всі й лише linked senses; legacy/unattributed рядок відступає до `details.senses.take(3)`. Групова картка кількох збережених перекладів свідомо показує до 3 спільних значень.
4. **Синоніми** — для V2-атрибутованого варіанта показується union синонімів лише його linked senses; без атрибуції або у груповій картці — word-level `synonyms.take(12)`. **Ліміт 12.**
5. **Антоніми** — симетрично: union `antonyms` linked senses для конкретного перекладу, word-level fallback для legacy/group. **Ліміт 12, помаранчевий акцент.**
6. **Форми** — `WordChipsRow("Форми", forms.map{it.text}.distinct().take(10), accent=Muted)`. **Ліміт 10, дедуп по тексту.**

> `WordSenseBlock` рендерить номер, partOfSpeech, definition та examples. Sense-level synonyms/antonyms конкретного атрибутованого перекладу показуються нижче окремими `WordChipsRow`; word-level списки лишаються fallback для legacy та групових карток. Форми завжди word-level.

> **[ЗАРАЗ→виправлено фазою 0]** До фази 0 сервер завжди клав у **кожен**
> `VariantDto` **однаковий word-level блоб** `senses`/`synonyms`/`antonyms`/
> `examples` — усі варіанти того самого слова діставали ідентичний список
> значень незалежно від того, з яким сенсом конкретно повʼязаний саме цей
> переклад. Коректне відображення трималось лише на клієнтському
> `WordDetails.displayContent()`/`attributedSenseIndexes()`
> (`App.kt:4580-4619`), який вже після факту вирізав linked senses з повного
> блоба (і досі так робить — див. нижче).
>
> **[НОВЕ] (фаза 0)** `projectEnrichmentForVariant`
> (`vocabee-gateway/src/lexicon/lexicon.service.ts:1946`) тепер сам скоупить
> `senses`/`synonyms`/`antonyms`/`examples` під конкретний варіант ще на
> сервері, з тим самим фолбеком на word-level пул при порожній sense-scoped
> видачі (деталі контракту — `17-api-and-data-reference.md` §1.5). Клієнтський
> `displayContent()` лишається без змін і без ризику: для атрибутованого
> варіанта він тепер працює над уже звуженими даними (по суті no-op, бо
> `senses` вже містить лише лінковані значення), для legacy/group-карток —
> як і раніше.

### 3.3 `WordSenseBlock` — одне значення

**[ЗАРАЗ]** `App.kt:3080-3127`:

- Кружок-бейдж із порядковим номером значення `(index+1)` в `accent` (`App.kt:3088-3099`).
- `partOfSpeech` (якщо не порожній) поряд із номером, `Muted2`, 12sp (`App.kt:3100-3108`).
- `definition` — основний текст, `Ink`, 14sp (`App.kt:3110-3116`).
- `examples.take(2)` — приклади в лапках «…», `Muted`, 13sp (`App.kt:3117-3125`). **Ліміт 2 приклади на значення.**

### 3.4 `WordChipsRow` — чипи синонімів/антонімів/форм

**[ЗАРАЗ]** `App.kt:3129-3159`: заголовок-лейбл (`Muted2`, 11sp, caps-spacing) + `FlowRow` чипів. Кожен чип — `Surface` із `accent.copy(alpha=0.10f)`, текст `accent`, 12sp, заокруглення 10dp. Колір чипів = переданий `accent` (синоніми/форми) або `Orange` (антоніми).

### 3.5 IPA в рядку

**[ЗАРАЗ]** IPA показується **у згорнутому й розгорнутому стані** поряд із source-словом, якщо `word.ipa` не порожній (`App.kt:2959-2968`): `Muted2`, 13sp, SemiBold. Джерело — `WordEntry.ipa` (снапшот, §5), не з `details`.

---

## 4. Озвучення (TTS) — підсумок

> Повний розбір механіки — **doc 08 §4** (асинхронна ініціалізація, source-мова, обмеження). Тут лише ключове + крос-лінки, щоб не дублювати.

### 4.1 Контракт

**[ЗАРАЗ]** `SpeechOutputController` (`SpeechOutputController.kt:8-21`):

```kotlin
interface SpeechOutputController {
    val isSupported: Boolean
    fun speak(text: String, languageTag: String)  // повертається одразу; аудіо асинхронне
    fun shutdown()
}
```

- `NoSpeechOutputController` — no-op фолбек для прев'ю/тестів (`isSupported=false`, `SpeechOutputController.kt:23-27`).
- Android-реалізація `AndroidSpeechOutputController` поверх `android.speech.tts.TextToSpeech` (`AndroidSpeechOutputController.kt:18`).

### 4.2 Асинхронна ініціалізація (перший тап може мовчати)

**[ЗАРАЗ]** Движок ініціалізується асинхронно. Поки `initialized=false`, `speak()` лише запам'ятовує запит у `pendingUtterance` і **повертається без звуку** (`AndroidSpeechOutputController.kt:51-59`); запит програється, коли движок повідомить `SUCCESS` (`:23-34`).

**Обмеження:**
- Зберігається **лише один** pending-запит — останній перезаписує попередній (`:55`).
- Якщо ініціалізація провалилась (`status != SUCCESS`) — `Log.w`, pending не програється, звуку нема (`:31-33`).
- Наслідок: **перший тап «озвучити» одразу після відкриття словника може бути беззвучним**. Деталі — **doc 08 §4.1**.

### 4.3 Мова озвучення = source (обмеження)

**[ЗАРАЗ]** З рядка слова озвучення викликається як `onSpeak(group.sourceWord, topic.sourceLanguage.speechTag)` (`App.kt:2233`) — тобто завжди **source-слово** мовою `topic.sourceLanguage`.

- У `doSpeak`: `Locale.forLanguageTag(languageTag)` → `engine.setLanguage(...)`; при `LANG_MISSING_DATA`/`LANG_NOT_SUPPORTED` — лог і вихід без звуку (`AndroidSpeechOutputController.kt:61-70`).
- Повторний `speak` робить `engine.stop()` + `QUEUE_FLUSH` — нова репліка скасовує попередню (анти-каскад при дабл-тапі, `:68-69`).
- **Озвучується ТІЛЬКИ source-слово і ТІЛЬКИ source-мовою.** Переклад (target) озвучити не можна.

### 4.4 Зв'язок з напрямком (крос-лінк doc 08)

**[ЗАРАЗ]** Перемикач напрямку (`speechDirectionReversed`) впливає на **STT** (мову розпізнавача: primary = напрямок, alternative = друга мова словника — `App.kt:2150-2152`), але **на TTS не впливає** — озвучення завжди `topic.sourceLanguage.speechTag` незалежно від напрямку (**doc 08 §3.5, §4.2**, `App.kt:2233`, `:2146`).

- **[НОВЕ за D8]** Напрямок STT зберігається **по словнику** (виставляє пріоритетну мову розпізнавача). Деталі та поточна розбіжність (напрямок зараз `remember(topic.id)` і не персиститься) — **doc 08 §3.4**. TTS це **не** зачіпає (озвучення лишається source).

> **Припущення (уточнити):** чи давати озвучення target-слова target-мовою (логічно під D8). Зараз поза скоупом коду — **doc 08 §4.2**.

---

## 5. Версійний snapshot перекладу/IPA у `topic_words` (D15)

### 5.1 `topic_words` — офлайн-копія, а не live join

**[ЗАРАЗ]** Слово словника користувача зберігає **власну копію** тексту/перекладу/IPA, а не лайв-посилання на `lexicon_words` (`topics.ts:50-81`):

| Поле | Тип | Роль |
|---|---|---|
| `wordText` | `text NOT NULL` | слово-джерело (снапшот) |
| `translationText` | `text NOT NULL` | переклад (снапшот) |
| `ipa` | `text` | IPA-снапшот (`topics.ts:59`) |
| `sourceWordLang` / `sourceWordId` | `varchar(8)?` / `uuid?` | **м'яке** посилання на `lexicon_words` (nullable, без FK) |
| `source` / `origin` | `varchar(16)` / `text` | провайдер, що дав запис |
| `metadata` | `jsonb` | додаткове збагачення (за потреби) |

### 5.2 Чому це все ще snapshot

- UI ніколи не робить live join із Dictionary API: `wordText`/`translationText`/`ipa`/`details` локально доступні офлайн і рендеряться одразу.
- `sourceWordId` лишається **м'яким** server-side лінком (nullable, без cross-DB FK). Додатковий opaque translation ref, schema version і SHA-256 revision лежать у `metadata.lexiconSnapshot`; мобільний кешує їх у backward-compatible `details_json`.
- Перед кожною auth sync-відповіддю `client-gateway` батчем просить у `dictionary-gateway` read-only канонічну проєкцію. Вона обмежена парою мов словника: для EN→UK не потрапляють EN→ES приклади або EN→DE значення. Якщо revision змінилась, сервер робить **повну заміну**, а не JSON merge, і bump-ить timestamps слова/словника — звичайний delta-sync привозить оновлення.
- Stale client payload не може відкотити новішу lexical revision. Для linked слова backend приймає від mobile лише user-owned прогрес/delete і валідний `contextGlossary`; canonical texts/IPA/details відбудовує Dictionary projector.
- `contextGlossary` і навчальний прогрес мають окремий lifecycle. Glossary зберігається під час звичайного refresh; якщо він був похідним саме від старого canonical sentence, яке замінили, snapshot інвалідується для повторного збагачення.

### 5.3 Наслідки та межі

| Сценарій | Поведінка |
|---|---|
| Провайдер/куратор уточнив IPA, definition, example або form | Наступний auth sync порівнює content revision і повністю замінює локальний snapshot. |
| У БД є однаковий source-example для кількох target languages | Projector читає лише `translation_lang = targetLang OR NULL`, віддає target-specific рядок першим і дедуплікує normalized source text на сервері. |
| Інша мовна пара імпортувала окремий sense того самого headword | У details входить лише union senses, linked до активних translations поточного `learningWord + targetLang`; cross-direction sense не рендериться. |
| Слово додане до появи `details`/lexicon ref | Sync робить legacy resolve лише за однозначною парою мов + normalized source/translation; неоднозначний/manual запис не вгадується й не втрачає старий payload. |
| Змінилася schema `WordDetails` | Server revision містить schema version; нова версія app робить full pull. Еволюція має бути additive або мати паралельну versioned projection — старий binary не може зрозуміти breaking rename. |
| Анонім без JWT | Vocabulary sync відсутній (D2), тому snapshot лишається локальним до входу/мержу в акаунт. |

---

## 6. Крос-лінки

- **doc 03** — Room-кеш і зберігання `WordDetails` у `details_json`; фактичний JSON codec належить `RoomVocabularyRepository`, а `VocabeeTypeConverters` обслуговує `SyncStatus`.
- **doc 08 §3** — напрямок перекладу і STT (primary/alternative мови розпізнавача), D8.
- **doc 08 §4** — повна механіка TTS (асинхронна ініціалізація, source-мова, обмеження).
- **doc 06** — sync/мерж: як `topic_words`-снапшоти зливаються при вході.
- **doc 01** — екран словника й рядок слова в загальному контексті UI.
