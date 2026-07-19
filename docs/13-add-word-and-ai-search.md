# 13 — Додавання слова + AI-пошук / переклад

**[ЗАРАЗ]** Сигнатурний флов Vocabee: морф-оверлей «Додати слово» → дебаунсений текстовий/голосовий ввід → серверний `/search`-пайплайн (детект мови → кеш lexicon → провайдери перекладу → AI-збагачення) → список результатів із кнопкою `+`/`✓`.

Суміжні документи (не дублюються тут): онбординг — `02`, дані/кеш — `03`, економіка/монетки — `04`, sync+мерж — `06`, мови/мовлення/теми (деталі STT) — `08`, крайові випадки — `10`, motion-бриф (морф-анімація) — `12`.

Легенда: **[ЗАРАЗ]** — як у коді сьогодні; **[НОВЕ]** — затверджена зміна (D1–D11); **[МАЙБУТНЄ]** — відкладено.

---

## 1. Вхідна точка: морф-оверлей із пігулки

> **[ЗАРАЗ, після редизайну]** Живий шлях додавання — **док унизу словника** (`InlineAddWordBar` + `InlineTranslationPanel` в `App.kt`), див. §3.1 і §4.1. Композабл `AddWordOverlay` більше нізвідки не викликається (лишились у вжитку тільки його `AddWordLoadingState` / `AddWordErrorState` / `AddWordResultsList` / `VoiceWaveform`); §1–§2 і таблиця §4 описують саме його і тримаються як опис legacy-поверхні до окремого прибирання.

Оверлей `AddWordOverlay` морфиться з пігулки «+ слово» у словнику до повноекранної поверхні.

| Аспект | Поведінка | Код |
|---|---|---|
| Старт морфа | Пігулка передає свій rect `AddWordOrigin(left, top, width, height)` у dp; `graphicsLayer` інтерполює scale+translation від origin до повного екрана | `AddWordOverlay.kt:75-80`, `:250-268` |
| Анімація входу | `morph` 0→1 за 420 мс (форма/колір), далі `content` 0→1 за 280 мс (контент); фон — чорний 20% × `morph.value` | `AddWordOverlay.kt:164-171`, `:244` |
| Колір | `lerpColor(accent → White, morph.value)` — акцент словника перетікає в білу поверхню | `AddWordOverlay.kt:249`, `:923-931` |
| Кут | `(32f * (1f - morph)).dp` — від заокругленої пігулки до прямого кута екрана | `AddWordOverlay.kt:248` |
| Закриття | `close()` грає `content` 1→0 (150 мс) + `morph` 1→0 (380 мс), тоді `onClose()`; гард `closing` від подвійного запуску | `AddWordOverlay.kt:173-181` |
| Хедер | «Додати у «{title}»» + read-only індикатор пари мов (прапор → прапор) + кнопка закриття. Пара мов **незмінна** тут (бейкається при створенні словника — D6) | `AddWordOverlay.kt:344-410` |

> Деталі тривалостей/кривих морфа — `12-motion-and-interaction-brief.md`. Тут лише факт, що це сигнатурний перехід.

---

## 2. Текстовий ввід із дебаунсом ~1 с

Поле вводу — `AddWordSearchField` (плейсхолдер «Введи слово англійською…»), без авто-фокуса (щоб не піднімати клавіатуру одразу — `AddWordOverlay.kt:419-421`).

**Дебаунс-пайплайн** `AddWordOverlay.kt:191-199`:

```
LaunchedEffect(cleanedQuery) {
    if (cleanedQuery.isEmpty()) { searchState = AddWordSearchState(); return }
    searchState = searchState.copy(isLoading = true, errorMessage = null)  // спінер ОДРАЗУ
    delay(1000)                                                            // ~1 с пауза
    searchState = searchRemote(cleanedQuery)                              // запит на /search
}
```

| Властивість | Поведінка |
|---|---|
| Тригер | Кожна зміна `cleanedQuery` (`query.trim()`) — і з клавіатури, і з голосу | `AddWordOverlay.kt:147`, `:191` |
| Скасування | `LaunchedEffect` рестартує на кожен символ → попередній `delay(1000)` скасовується. Запит летить лише через 1 с після паузи | `AddWordOverlay.kt:197` |
| Спінер | `isLoading=true` виставляється до `delay`, тож спінер «пришпилений» весь час набору | `AddWordOverlay.kt:196` |
| Очистка поля | Кнопка `×` (видима при непорожньому полі) → `query=""` + `resetSpeech()` | `AddWordOverlay.kt:288-292`, `:468-484` |
| Порожній запит | Скидає `searchState` до дефолту → показ MicStage | `AddWordOverlay.kt:192-194`, `:301` |

---

## 3. Голосовий ввід (коротко)

Мікрофон через `SpeechInputController` (платформна реалізація). Тут — лише інтеграція з оверлеєм; деталі STT (платформні API, дозволи, мовні теги) — `08-languages-speech-themes.md` (D8: напрямок STT зберігається по словнику).

| Аспект | Поведінка | Код |
|---|---|---|
| Старт | `HoldToTalkButton` (`detectTapGestures.onPress` → `onStart`, `tryAwaitRelease` → `onStop`) | `AddWordOverlay.kt:553-601` |
| Мови розпізнавача | `languageTag = topic.targetLanguage.speechTag` (пріоритет — мова, яку вчать), `alternativeLanguageTags = [sourceLanguage.speechTag]` — **D8** | `AddWordOverlay.kt:210-213` |
| Частковий результат | `onPartialResult → partialText` (живий прев'ю) | `AddWordOverlay.kt:214` |
| Фінальний результат | `onResult`: trim → `heardText`, якщо не порожній → `query = text` (запускає той самий дебаунс-пайплайн з §2) | `AddWordOverlay.kt:215-220` |
| Помилка | `onError → speechError`, скидає `partialText`, `isListening=false` | `AddWordOverlay.kt:221-225` |
| Стоп із грейсом | `stopListeningWithGrace()`: `delay(700)` перед `stopListening()` — щоб не обрізати хвіст фрази | `AddWordOverlay.kt:230-233` |
| Очистка на dispose | `DisposableEffect → stopListening()` при виході з оверлея | `AddWordOverlay.kt:183-185` |

Голос і текст конвергують в одне поле `query` → далі флов однаковий.

### 3.1 [ЗАРАЗ] Голос у доку словника (жива поверхня)

`DictionaryDetailScreen` тримає той самий `SpeechInputController`, але UI — док (`InlineAddWordBar`):

| Аспект | Поведінка | Код |
|---|---|---|
| Старт | утримання кнопки міка 250 мс (`VoicePressStartDelayMillis`), відпускання → `stopListeningWithGrace()` з `delay(700)` | `App.kt` `InlineAddWordBar` |
| Стан кнопки | idle — акцент словника; listening — `Orange` + хвиля з 24 барів у полі; панель відкрита/фокус — `Close` на `Ink` (скасувати пошук) | `App.kt` `InlineAddWordBar` |
| Мови розпізнавача | `speechInputLanguage.speechTag` + альтернатива `speechOutputLanguage.speechTag`; напрямок перемикається в хедері деталі — **D8** | `App.kt` `DictionaryDetailScreen` |
| Помилка | `onError → speechError` (локалізований рядок з `R.string.voice_error_*`), плюс снекбар «Голосове введення перервано: {причина}» | `App.kt` `startListening` |

---

## 4. Стани оверлея: Idle / Recording / Loading / Error / Results

Перемикач у `AddWordOverlay.kt:300-332` (`when`-ланцюг, порядок важливий):

| Стан | Умова входу | Що показано | Код |
|---|---|---|---|
| **Idle (Mic)** | `cleanedQuery.isBlank()` і не слухаємо | `MicStage`: «Продиктуй слово / або почни вводити…» + кнопка мікрофона | `AddWordOverlay.kt:301`, `:495-551` |
| **Recording** | `isListening == true` | `MicStage` зі станом «Слухаю…», помаранчева хвиля `VoiceWaveform` (28 барів), «торкнись, щоб зупинити» | `AddWordOverlay.kt:301`, `:521-526`, `:611-653` |
| **Loading** | `searchState.isLoading` | `AddWordLoadingState`: спінер + «Шукаю переклад…» | `AddWordOverlay.kt:308`, `:655-675` |
| **Error** | `searchState.errorMessage != null` | `AddWordErrorState`: «Не вдалось отримати переклад» + повідомлення залежно від build: у release — «Something went wrong», у debug — сирий текст API/мережі | `App.kt`, `AddWordOverlay.kt:309-312`, `:677-707` |
| **Results** | інакше | `AddWordResultsList` (порожній → «Нічого не знайдено для «{q}»»; інакше — список) | `AddWordResultsList`, `:709-789` |

> `AddWordMode { Idle, Recording, Results }` (`:82`) — декларований enum; фактично станами керують `isListening` / `searchState.isLoading` / `searchState.errorMessage` напряму. `AddWordSearchState` (`:126-133`) — носій loading/results/error + `tier`/`maxResults`.

Recording-слот хвилі має фіксовану висоту (80 dp + 30 dp spacer), порожній в Idle — щоб мікрофон не «стрибав» між станами (`:519-527`).

### 4.1 [ЗАРАЗ] Стани панелі доку (`InlineTranslationPanel`)

Панель відкривається, щойно є запит, завантаження, помилка пошуку або помилка голосу
(`showTranslationPanel`). Порядок `when`-ланцюга важливий:

| Стан | Умова входу | Що показано |
|---|---|---|
| **Помилка голосу** | `speechError != null && query.isBlank()` | `VoiceRecognitionErrorState` — див. нижче |
| **Loading** | `searchState.isLoading` | `AddWordLoadingState`: спінер `Purple` на треку `Tint` + «Шукаю переклад…» |
| **Error** | `searchState.errorMessage != null` | `AddWordErrorState`: ✕ `OrangeText` + «Не вдалось отримати переклад»; у release — «Something went wrong», у debug — сирий текст API/мережі; монетка **не** списується (гейт відпрацював до запиту) |
| **Results / порожньо** | інакше | `AddWordResultsList`; порожній результат — «Нічого не знайдено для «{q}»» |

**[ЗАРАЗ] Повноекранна помилка голосу** (борд 4, «перероблено»): раніше панель показувала
сирий рядок розпізнавача під заголовком «Варіанти перекладу» — на англомовному пристрої це
протікало як `Could not recognize the word`. Тепер:

- хедер панелі пише «Голосове введення» / **«Спробуй ще раз»** (замість «Варіанти перекладу»);
- центр: мік 32dp у колі 76dp (`OrangeText` 12%), «Не вдалося розпізнати» 20sp/800,
  підказка «Скажи слово трохи повільніше й ближче до мікрофона — або введи його вручну.»;
- кнопка «Спробувати ще раз» (`Tint` + `PurpleText`, іконка міка, 50dp r16) викликає
  `startListening()` — перезапускає запис;
- кнопка доку в цей момент у стані cancel (✕) — закриває пошук;
- сирий текст помилки лишається тільки у снекбарі «Голосове введення перервано: {причина}».

Для помилки пошуку `searchRemotely` обирає copy залежно від build. Release не розкриває
деталі інфраструктури та показує сталу `TranslationSearchFailureMessage` («Something went
wrong»). Debug передає в панель `result.message` без обгортання — наприклад, реальне
повідомлення gateway про timeout. На Android режим визначає `BuildConfig.DEBUG`; на iOS
debug-конфігурація використовує `https://dev-api.vocabee.online`, release — production URL.
Гейт-повідомлення (`translationGateMessage`, «потрібні монетки…») ставиться окремо й
лишається без змін.

---

## 5. Список результатів

Кожен рядок — `AddWordResultRow` (`AddWordOverlay.kt:798-921`), джерело — `TranslationOption` (мапиться з `SearchVariant` у `RemoteLexiconSearchUseCase.kt:82-115`).

| Елемент | Джерело | Код |
|---|---|---|
| **Вихідне слово** (learning-side, канонічне) | `option.learningWord` — **не сире введення** (тож «circumstanc» → «circumstance») | `AddWordOverlay.kt:835-848`, коментар `:836-837` |
| **IPA** | `option.ipa` (праворуч від слова, якщо є) | `AddWordOverlay.kt:849-858` |
| **Переклад** (known-side) | `option.value` (== `knownWord`) | `AddWordOverlay.kt:866-877` |
| **Тип/регістр** | Під перекладом компактно: «Фраза», «Вислів», «Абревіатура» + незалежні «Сленг», «Інтернет» тощо; повні поля видно після розгортання | `SearchVariant → WordDetails`, `AddWordResultRow` |
| **AI-атрибуція (per-row)** | фіолетова іконка `Sparkle` біля кожного слова | `AddWordOverlay.kt:859-864` |
| **AI-атрибуція (footer)** | «Переклади та приклади згенеровано AI» (per-tier капи знято — див. §8) | `AddWordOverlay.kt:765-787`, `:791-796` |
| **Розгортання деталей** | клік по рядку (`canExpand = hasDetails ∨ overflow джерела/перекладу`) розкриває `WordDetailsBlock` (senses/синоніми/антоніми/форми/приклади) | `AddWordOverlay.kt:807-822`, `:914-919` |
| **Шеврон** | `ChevronDown`, поворот 180° при `expanded`; акцент-колір якщо `canExpand`, інакше сірий | `AddWordOverlay.kt:879-894` |
| **Кнопка `+`/`✓`** | 44×44 (фіксована геометрія — рядок не стрибає); акцент `+` коли немає, фіолетова `✓` коли в словнику | `AddWordOverlay.kt:898-912` |

`TranslationOption.note` (Primary/Additional/Alternative/AlreadyAdded) виводиться з `variant.source` (`dictionary`/`translator`/`ai`) у `RemoteLexiconSearchUseCase.kt:84-91`.

---

## 6. Виявлення дубліката (`+` ↔ `✓`)

Дубль детектиться **двома шляхами**, обидва зведені в один `Set` нормалізованих перекладів — toggle миттєвий:

```
isAdded = option.alreadyAdded                                   // сервер позначив на момент пошуку
       || existingTranslations.contains(option.value.lower())  // або щойно додано в цій сесії
```
`AddWordOverlay.kt:744-755`.

| Шлях | Деталі | Код |
|---|---|---|
| Серверний `alreadyAdded` | `searchRemotely` віддає `existing = topic.words.map { translation }` в use-case; `SearchVariant.toOption` ставить `alreadyAdded` + note `AlreadyAdded` | `App.kt:173`, `RemoteLexiconSearchUseCase.kt:82-95` |
| Живий локальний `Set` | `existingTranslations = topic.words.map { translation.trim().lowercase() }.toSet()`, перераховується на кожен recompose `topic.words` | `AddWordOverlay.kt:154-156` |
| Toggle | `isAdded` → клік `onRemove` (фіолетова `✓`); інакше `onAdd` (акцент `+`). Хост перерендерює оверлей зі свіжим `topic` після стор-апдейту | `AddWordOverlay.kt:898-903`, `:319-330` |
| Нормалізація | `.trim().lowercase()` з обох боків при порівнянні | `AddWordOverlay.kt:155`, `:755` |

### 6.1 [ЗАРАЗ] Фоновий словничок контекстного речення

Після успішного локального `AddWord` UI не чекає додаткової мережі: слово одразу є в Room,
а `MainApp.enrichContextGlossaryInBackground` окремою корутиною бере точний
`word.contextSentence()` і викликає один `POST /v1/search/context-glossary` для всієї пари
мов словника. Gateway детерміновано ділить речення на максимум 64 словесні токени й одним
structured-output запитом отримує контекстний переклад **кожного входження**. Це окреме
безкоштовне збагачення: воно не викликає wallet і не списує монетку за кожне слово.

Успішна відповідь валідується на мобільному проти точного тексту й UTF-16 діапазонів,
мапиться в `WordDetails.contextGlossary`, пишеться тим самим JSON-блобом у Room, бампить
локальну ревізію та синхронізується в `topic_words.metadata`. Якщо API/AI недоступний або
повернув неповний набір, збереження слова не відкочується: контекст у тренуванні лишається
звичайним текстом без клікабельних перекладів. Деталі контракту — doc 17 §1.5.

**[ЗАРАЗ] Додавання просто з контексту.** У розкритій картці слова блок «Контекстний приклад»
рендериться тим самим `ContextGlossarySentence`, що й тренування, але з дією
`ContextGlossaryTokenAction.AddToDictionary`: тап по нецільовому слові відкриває попап із
контекстним перекладом і кнопкою `+`, яка одразу кладе слово в **цей** словник за 1 монетку
(`onAddContextWord` → `spendTranslationBee` → `AddWord`). Слово, яке вже є у словнику,
показується жовтою заливкою, у попапі має `✓` і не клікається — повторного списання немає.
Гейти ті самі, що для закладок тренування: гість → «Потрібен акаунт», нема монеток →
«Закладки зачекають». Стани токенів і геометрія попапа — doc 11 §1.

Для авторизованого користувача client gateway одночасно проєктує кожен token у приватний
серверний glossary. Його ключ — не лише текст слова: унікальність складається з
`user + sourceLang + targetLang + normalized word/lemma + normalized concrete translation`,
тому `bank → банк` і `bank → берег` не перетирають одне одного. Exact речення, surface та
позиція входження зберігаються окремими прикладами цієї пари. Для anonymous/offline save
снапшот лишається в metadata, а після входу `applySync` ідемпотентно переносить його в
server glossary. Це накопичувальна персональна база: видалення слова зі словника не
видаляє вже зібрані пари/приклади.

Під час тренування цільовий token не розкривається. Нецільовий token можна покласти в
сесійну закладку; при подальшому save у вибраний сумісний словник його `lemma/surface`,
контекстний переклад і той самий `contextGlossary` стають новим `WordEntry`. Це явна дія
користувача й коштує 1 монетку на нове слово; недостатній баланс не очищає закладки.

У деталях уже збереженого слова той самий exact sentence рендериться цілісно, без
окремого списку розібраних токенів. Popup залежного слова має дію `+`, яка без проміжного
вибору словника додає пару `lemma/surface + concrete translation` до поточного словника;
наявна пара позначена `✓` і повторно не додається. Економічний та auth-гейт такий самий,
як для збереження закладки. Ключ клієнтського стану також містить конкретний переклад,
тому омонімічні `bank → банк` і `bank → берег` не склеюються.

**[ЗАРАЗ, legacy]** Видалення з рядка `+`/`✓` — без повернення монеток + Undo-снекбар (історична цінова деталь D3, superseded by D11). **[НОВЕ]** Після v2 delete повертає точний word-charge, якщо від нього минуло не більше 3600 секунд; Undo/soft-delete лишаються. Деталі — `07-deletion.md`.

---

## 7. [ЗАРАЗ, legacy v1] Ціна 1 монетка за пошук — де enforced

**[ЗАРАЗ, legacy v1]** Константа: `TRANSLATION_SEARCH_BEE_COST = 1` (gateway `wallet.constants.ts:5`) == `TranslationSearchBeeCost` (моб); superseded by D11 у v2.

### [ЗАРАЗ] — подвійне списання (клієнт + сервер)

| Сторона | Що робить | Код |
|---|---|---|
| **Сервер** (авторитетний) | `ClientSearchController.search` для **залогінених** викликає `walletService.spendBees(user.id, 1)` ДО делегації, повертає `meta.beeBalance` | `client-search/client-search.controller.ts` |
| **Клієнт** (локальний) | `App.kt` має `onSpendSearchBee = { store.spendTranslationBee() }`; `spendTranslationBee` для auth викликає `spendBees(1)` локально | `App.kt:686`, `VocabeeStore.kt:241-244`, `:362-370` |
| Звірка | `searchRemotely` бере серверний `result.beeBalance` → `onBeeBalanceChanged` → `VocabeeEvent.SetBeeBalance` → клієнт **приймає серверне значення** як істину | `App.kt:175-177`, `:701-703`, `VocabeeStore.kt:372-376` |

> Ризик: клієнт може списати локально **і** сервер списує своє — без звірки це −2 за один пошук. Зараз серверний `beeBalance` затирає локальний баланс, тож кінцеве значення = серверне, але порядок подій крихкий.

### [ІСТОРИЧНИЙ ПЛАН D1] Серверне списання за пошук — superseded by D11

- На час rollout legacy v1 списання рахує **сервер** (`spendBees` у контролері), клієнт — **оптимістичний показ + звірка** з `meta.beeBalance`.
- Прибрати локальне `spendBees` як джерело істини; `spendTranslationBee`/`onSpendSearchBee` стають оптимістичним preview, що завжди підтверджується серверним балансом.
- Це не цільова ціна: правило «платний пошук» **superseded by D11**. Економіка лишається тільки для **authenticated** (D2: анонім без монеток).

### [НОВЕ за D11] Безкоштовний lookup, оплата конкретного збереженого результату

`POST client-gateway /v2/translation-lookups` створює безкоштовний lookup receipt.
Кожен показаний варіант має opaque `resultId`, який сервер прив'язує до точних
`translationId`/`revisionId` із `dictionary-gateway`; мобільний клієнт не може
підмінити ці dictionary ids.

`POST client-gateway /v2/topics/{topicId}/words/from-result` приймає `resultId`,
`savedWordId` і `Idempotency-Key`. Для авторизованого користувача одна транзакція:

1. перевіряє, що result належить його lookup і ще придатний до збереження;
2. створює snapshot saved word;
3. списує окремий immutable word-charge за активною політикою (початково −1);
4. повертає підтверджений баланс.

Два варіанти перекладу — це два `resultId`, два saved word і два незалежні charge.
Пошук має `searchCost=0`; D11 не вмикається, доки не випущено сумісний v2-клієнт.
`GET client-gateway /v1/search` і його legacy pricing лишаються без змін лише на час rollout.

### Гостьовий режим (анонім, D2)

- Сервер для аноніма **не списує** (`if (user)` у `client-search.controller.ts`), `beeBalance=null`.
- Клієнтський `spendTranslationBee` для не-auth повертає `canSearchTranslation()` без списання (`VocabeeStore.kt:242`).
- Замість монеток — ліміт 50 слів (див. §9).

---

## 8. [ЗАРАЗ, legacy v1] Гейти: `canSearchTranslation` / `canAddWordToDictionary`

`VocabeeStore.kt:229-239`:

| Гейт | authenticated | anonymous | Код |
|---|---|---|---|
| `canSearchTranslation()` | `beeBalance >= 1` | `!anonymousWordLimitReached()` (ліміт 50 слів) | `:229-235` |
| `canAddWordToDictionary()` | `true` (завжди — економіка на пошуку, не на додаванні) | `!anonymousWordLimitReached()` | `:237-239` |

Застосування в UI/сторі:

| Точка | Поведінка | Код |
|---|---|---|
| Перед пошуком | `canUseTranslationSearch = store.canSearchTranslation()`; якщо blocked → шит `AuthRequired(WordLimit)` (анонім вичерпав 50) або `NeedBees(TranslationSearch)` (auth без монеток) | `App.kt:680`, `:687-693` |
| Повідомлення гейта | auth: «Потрібна 1 монетка для пошуку перекладу.»; анонім: «Гостьовий ліміт 50 слів вичерпано.» | `App.kt:681-685` |
| `onAddWord` | якщо `canAddWordToDictionary()` → `AddWord` + `syncVocabularyNow()`; інакше шит `AuthRequired(WordLimit)` | `App.kt:706-713` |
| У сторі (захист) | `addWord` повторно перевіряє `canAddWordToDictionary()` перед `addWordUseCase` | `VocabeeStore.kt:313` |
| Поріг ≤3 | `CriticalBeeThreshold` рендерить критичні бейджі/банер «Лишилось N монеток» | `App.kt:774-778`, `:1631` |

> **[НОВЕ D11/v2]:** `canSearchTranslation()` більше не перевіряє баланс для authenticated: lookup безкоштовний. NeedBees перевіряється перед `words/from-result`, але фінальне рішення, сума та charge — лише за `client-gateway`. Анонімний ліміт 50 saved words лишається окремим правилом D2.

---

## 9. Гостьовий ліміт 50 слів (анонім, D2)

| Аспект | Поведінка | Код |
|---|---|---|
| Поріг | `anonymousWordLimitReached()` — анонім обмежений 50 словами сумарно | `VocabeeStore.kt:229-239` (споживачі) |
| Блок пошуку | анонім за лімітом → `canSearchTranslation()=false` → шит `AuthRequired(WordLimit)` | `App.kt:688-690` |
| Блок додавання | той самий шит при `onAddWord` | `App.kt:710-711` |
| Монетки/реклама/промо | **відсутні** для аноніма (D2); економіка вмикається після Google-входу | — |

Деталі анонімного режиму та порогів — `04-coins-economy.md`, `10-edge-cases-and-open-items.md`.

---

## 10. [ЗАРАЗ] Серверний `/search`-пайплайн

`GET /v1/search?q=&speak=&learn=` (`KtorVocabeeApi.kt:28-48` →
`ClientSearchController.search` → `DictionaryClientService` → внутрішній HTTP
`DictionarySearchController.search` → `LexiconService.search`).

**[ЗАРАЗ, split foundation]** Мобільний клієнт і надалі звертається лише до `client-gateway`.
Його `GET /v1/search` є compatibility facade, а app-neutral пошук живе у
`GET dictionary-gateway /v1/search` під `X-API-Key`; `dictionary-gateway` не отримує
app JWT, user id, premium status або баланс. **[НОВЕ, v2]** `client-gateway` створює
lookup/result receipts поверх dictionary response.

DTO запиту `SearchQueryDto` (`search.dto.ts`): `q` (1–200, trim), `speak`/`learn` (ISO-639-1 з `SUPPORTED_LANGUAGE_CODES`). Токен — опціональний bearer; без нього tier `anonymous` (`OptionalJwtAccessGuard`).

### Кроки пайплайну (`lexicon.service.ts:132-284`)

| # | Крок | Деталі | Код |
|---|---|---|---|
| 0 | **Нормалізація** | `trim` → `normalize()` = NFKC + trim + lowercase; `isPhrase = слів > 1` | `lexicon.service.ts:137-138`, `:860-862` |
| 1 | **Детект мови (speak/learn)** | `LanguageDetector.detectBetween`: (a) скрипт-евристика Кирилиця vs Латиниця, (b) `franc-min` якщо ≥4 символи, (c) fallback → `learnLang`. `otherLang` = протилежна detected | `lang-detect.ts:30-55`, `lexicon.service.ts:140-147` |
| 2 | **Префікс-кеш lexicon** | `findPrefixMatches(detectedLang, normalized, otherLang, maxResults)` — `LIKE 'q%'`, exact-first, тоді primary, тоді коротші слова. Дає живі підказки «cir → circle/circus/circumstance» | `lexicon.repository.ts:308-358`, `lexicon.service.ts:151-161` |
| 3 | **Freshness / top-up** | Якщо є exact-cached і провайдер `isAvailable`: перевірити, чи є рядок із поточного tier (`acceptedTierNames`). Нема → `needsTopUp` (авто-рефетч при DeepL Free→Pro, зміні AI-моделі) | `lexicon.service.ts:171-191` |
| 4 | **Word-validator (квота-гейт)** | Для слова — Hunspell, для фрази — phrase-validator; додатково пропускаються короткі/uppercase/dotted кандидати на абревіатуру (`btw`, `LOL`, `NATO`, `U.S.`), а остаточну валідність вирішує structured AI | `lexicon.service.ts`, word-validator §12 |
| 5 | **Провайдер перекладу + lexical metadata** | OpenAI класифікує обидві мовні сторони як `word/phrase/expression/abbreviation`, окремо дає register tags, розшифровки, значення, дослівний переклад і приклади; напрямок орієнтується так, щоб mobile завжди отримав metadata learning-side одиниці | `openai-translation.provider.ts`, `translation.provider.ts` |
| 6 | **Echo-гард** | Кандидати = провайдер-результати, де `normalize(text) != normalizedQuery`. Усі збіглися з вводом → `echo` (НЕ персистимо). `null` → `no_provider_data` | `lexicon.service.ts:209-242` |
| 7 | **Upsert lexicon + directional cache** | `persistAllVariants`: upsert source і target words, зв'язати `target_word_id`, зберегти тільки `detectedLang → otherLang`. Lexical metadata лежить у `translations.metadata`, тому exact-cache повертає ті самі тип/розшифровку/значення без нового AI-виклику. Старий exact-cache без `sourceUnit/targetUnit` один раз ліниво оновлюється наступним пошуком; уже його відповідь містить metadata, далі знову працює кеш | `lexicon.service.ts`, `lexicon.repository.ts` |
| 8 | **Збагачення (IPA/audio/examples/senses)** | `enrichLearningEntry` (тільки не-фраза): dictionary-ланцюг (OpenAI → FreeDictionary) дає IPA, audio, senses+приклади, синоніми/антоніми, форми; усе ідемпотентно персиститься; heal-on-read для старого IPA | `lexicon.service.ts`, `lexicon-core.module.ts` |
| 9 | **Quality repair перед композицією** | Активні translation rows із `qualityScore >= 100` форсують AI-виклик, передають comments та `excludedTranslations`; позначені examples ремонтуються окремим dictionary-викликом із `rejectedExamples`; після успішного ремонту score обнуляється, безуспішний кеш лишається. | `lexicon.service.ts`, `quality-feedback.service.ts` |
| 10 | **Композиція відповіді** | Провайдер-хіти спершу, тоді не-дубль префікс-підказки, до `maxResults`; під час успішного quality repair старі low-quality variants фільтруються; дедуп по `normalize(knownWord)` | `lexicon.service.ts:245-264` |

### `providerReason` (meta — чому викликали/не викликали провайдер)

| Значення | Зміст | Код |
|---|---|---|
| `exact_cached` | вже є свіжий кеш — не дзвонили | `lexicon.service.ts:197-198` |
| `not_a_word` | не пройшов spell-чек — пропуск заради квоти | `:199-201`, `:241` |
| `echo` | провайдер віддав ввід дослівно — трактуємо як «нема перекладу», не зберігаємо | `:233-234` |
| `no_provider_data` | реальне слово, але ланцюг нічого не дав | `:235-239` |
| `translated` | провайдер дав реальний переклад → персист | `:214-216` |

### Відповідь `SearchResponseDto` → клієнт

`SearchResponse` (моб, `SearchResponse.kt`) дзеркалить `SearchResponseDto`. Крім звичайних dictionary-полів, кожен `SearchVariant` має `lexicalUnitKind`, `registerTags`, `expansion`, `translatedExpansion`, `meaning`, `literalTranslation`, `usageExample`, `usageExampleTranslation`. `RemoteLexiconSearchUseCase.toOption` мапить їх у JSON-снапшот `WordDetails`.

---

## 11. Провайдери та ланцюги (DI: `TRANSLATION_PROVIDER` / `DICTIONARY_PROVIDER`)

### Провайдер перекладу (`CompositeTranslationProvider`)

Активний DI-ланцюг зараз містить **OpenAI translation**. Модель конфігурується через
`OPENAI_MODEL`, default — `gpt-5.6-sol`; structured JSON повертає до 8 варіантів.
Кожен результат тегається `openai-<model>`, тому історичні рядки з попередньою моделлю
лишаються видимими як власний provider tier, а наступний пошук може долити поточний tier.

| Провайдер | tier-name | variants/call | Доступність | Код |
|---|---|---|---|---|
| Wiktionary (мультиваріант, per-sense таблиці) | `wiktionary` | 10 | `!!baseUrl`; знімає наголоси `́`, дедуп | `wiktionary-translation.provider.ts:45-104` |
| DeepL (1 переклад/запит) | `deepl-free`/`deepl-pro` (детект по суфіксу `:fx`) | 1 | `!!apiKey`; `EN-GB`/`PT-PT` overrides | `deepl.provider.ts:31-77` |
| MyMemory (TM-фолбек, без ключа) | `mymemory` | 1 | завжди | `mymemory.provider.ts:25-55` |
| OpenAI translation (structured JSON) | `openai-<model>` (default `openai-gpt-5.6-sol`) | 8 | `!!apiKey`; `reasoning_effort=none`, temp 0.2; repair запитує тільки `desiredVariants` і передає exclusions | `openai-translation.provider.ts` |
| Mock (тести/офлайн) | `mock` | 1 | завжди | `mock-translation.provider.ts:10-44` |

`CompositeTranslationProvider`: `acceptedTierNames` = union дітей, `variantsPerCall` = max, `isAvailable` = є хоч один usable (`composite-translation.provider.ts:19-58`).

### Ланцюг словника / збагачення (`CompositeDictionaryProvider`)

Порядок: **OpenAI dictionary → FreeDictionary** (перший із даними виграє; кожен ставить свій `origin`).

| Провайдер | origin | Деталі | Код |
|---|---|---|---|
| FreeDictionary (Wiktionary-backed, `?translations=true`) | `freedictionaryapi.com` | IPA (`extractPhonemicIpa`), PoS, до 8 senses, синоніми/антоніми, форми; **form-of-only → `null`** (щоб AI добив) | `free-dictionary.provider.ts:68-242` |
| OpenAI dictionary | `openai-<model>` (default `openai-gpt-5.6-sol`) | structured JSON: ≤5 senses (≥1 приклад кожен), ≤5 синонімів, ≤4 антонімів, ≤6 форм; підтримує `en/uk/ru/pl/de/es` | `openai-dictionary.provider.ts` |

`CompositeDictionaryProvider.supports/lookup` — `composite-dictionary.provider.ts:20-49`. Сервісний `cacheLooksRich`/`cachedSensesAreFormOfOnly` змушує рефетч, якщо кеш «form-of-only» або без IPA (`lexicon.service.ts:607-619`, `:870-885`).

FreeDictionary лишається безкоштовним fallback для dictionary-збагачення; окремого
Wiktionary translation-провайдера в активному DI-ланцюгу зараз немає.

### Видалення й відновлення останньої ревізії

`dictionary-admin-web` показує активні, видалені або всі переклади. Адміністратор зі
scope `dictionary:lexicon:write` може soft-delete рядок лише після явного
підтвердження та введення причини. `deleted_at` прибирає варіант із mobile search, але
залишає його як останню відновлювану ревізію разом з audit-подією.

Delete атомарно збільшує `translation_pair_repairs.missing_variants`. Наступний запит
цієї пари бере готові активні варіанти з БД і генерує тільки відсутню кількість,
виключаючи тексти активних і видалених рядків. Restore очищає `deleted_at` і, якщо
repair ще не був спожитий, скасовує один pending slot. Повної immutable історії всіх
версій поки немає — відновлюється саме останній soft-deleted рядок.

---

## 12. Word-validator (квота-гейт перед провайдерами)

`WordValidator` (Hunspell через `nspell`, словники `dictionary-en` / `dictionary-uk`) — `word-validator.ts`.

| Метод | Правило | Код |
|---|---|---|
| `isPlausibleWord(word, lang)` | мін. 2 символи; лише `\p{L}'-` (цифри/`$`/`?` → reject, щоб не отруїти кеш echo); є словник → Hunspell (`correct(raw)∨correct(lower)`), нема → евристика | `word-validator.ts:71-93` |
| `isPlausiblePhrase(phrase, lang)` | 2–12 токенів, кожен чистий лексичний; ≥1 «суттєвий» токен (lex-довжина ≥2) має пройти spell-чек | `word-validator.ts:102-121` |
| Евристика (без словника) | `length ≥ 3` + ≥1 голосна (латиниця/кирилиця) | `word-validator.ts:124-129` |

Мета — не палити DeepL/MyMemory/AI-квоту на одруківках, частковому наборі чи рандомі. Невдача → `providerReason='not_a_word'`, повертаються лише префікс-підказки кешу.

---

## 13. Тіри та `maxResults` — поточний контракт і майбутня політика

Backend-код і Swagger тепер узгоджені; відкритим лишається майбутнє продуктове
рішення про різницю між tier та застарілий KDoc мобільного API:

| Джерело | Заявлене | Код |
|---|---|---|
| `TIER_MAX_RESULTS` (істина в коді) | `anonymous=50, registered=50, premium=50` — капи **знято** для всіх | `user-tier.ts:14-18` |
| Swagger-опис client facade | Усі tier зараз отримують до 50 варіантів | `client-search.controller.ts` |
| Док-стрінг `VocabeeApi.search` (моб) | «Without a token the gateway responds with up to 3 variants» | `VocabeeApi.kt:8-10` |
| Моб footer-caption | per-tier капи знято; «до N варіантів»/«увійди для більше» **прибрано**, лишилась лише AI-атрибуція | `AddWordOverlay.kt:791-796` |

**Стан:** фактично всі tier-и бачать `min(50, variantsPerCall провайдера)` — без
штучного гейтингу (`user-tier.ts:5-13`: «нема монетизаційного важеля, поки не вийде
premium»). Backend Swagger уже показує 50; KDoc `VocabeeApi.search` про 3 варіанти
лишається **застарілим**.

**Припущення (уточнити):** чи лишається `50/50/50` довгостроково, чи `premium`
згодом отримає реальний lever (тоді `maxResults` стане tier-залежним і
`footerCaptionFor` поверне copy «увійди для більше»)?
`tierFromUserRow`/`maxResultsForTier` (`user-tier.ts:25-33`) вже готові під
tier-залежність — слот зарезервовано. **Рекомендація:** зараз синхронізувати лише
`VocabeeApi.search` KDoc з реальним `TIER_MAX_RESULTS`; майбутню зміну капів робити
одночасно в policy, Swagger і mobile copy.

---

## 14. Підсумок флову (E2E)

### [ЗАРАЗ, legacy v1]

```
Пігулка «+»  ─morph→  AddWordOverlay
   │
   ├─ текст (дебаунс 1с) ─┐
   ├─ голос (STT, D8) ────┤→ query → LaunchedEffect(cleanedQuery)
   │                       │     isLoading=true → delay(1000) → searchRemote
   │
   │   [гейт] canSearchTranslation()  (auth: bee≥1 / анонім: <50 слів)
   │   [ЗАРАЗ] клієнт spendTranslationBee  +  [D1 НОВЕ] сервер spendBees(1) (авторитетно)
   ▼
GET /v1/search ─► LexiconService:
   нормалізація → детект мови → префікс-кеш → freshness/top-up
   → word-validator → OpenAI translate(requested direction, missing variants only)
   → echo-гард → upsert source/target lexicon + directional translations
   → enrich (OpenAI→FreeDictionary: IPA/audio/senses/syn/ant/forms)
   → compose (maxResults) → SearchResponseDto{results, tier, maxResults, meta.beeBalance}
   ▼
RemoteLexiconSearchUseCase.toOption → List<TranslationOption>
   ▼
AddWordResultsList (слово+IPA+переклад+Sparkle, розгортання деталей, +/✓)
   │   дубль: alreadyAdded(сервер) ∨ existingTranslations(локально)
   ▼
onAdd → canAddWordToDictionary() → AddWord (learningWord, value, ipa, details) → sync
   │   серверний meta.beeBalance → SetBeeBalance (звірка балансу, D1)
   ▼
AddedCountBar «N слів додано · Готово» → close() (morph назад)
```

### [НОВЕ, D11/v2]

```text
query → POST client-gateway /v2/translation-lookups (searchCost=0)
      → client-gateway → GET dictionary-gateway /v1/search (X-API-Key)
      ← resultId + snapshot/provenance ids
      → користувач натискає «+» на конкретному resultId
      → POST /v2/topics/{topicId}/words/from-result + Idempotency-Key
      → атомарно: saved word + immutable word-charge(wordAdditionCost=1)
      ← підтверджений баланс
```
