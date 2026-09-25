# 13 — Додавання слова + AI-пошук / переклад

**[ЗАРАЗ, локальна реалізація нової задачі]** Інпут-док унизу словника → дебаунсений текстовий/голосовий ввід → `GET /v1/search` → список результатів **по одному айтему на сенс** із кнопкою `+`/`✓`. За `LEXICON_AI_ONLY=true` (default) бекенд використовує цілісну AI-статтю або її повний кеш; старий складений ланцюг перекладачів, сторонніх словників та валідаторів лишився в коді, але в цьому режимі вимкнений. Локальний код і тести не означають, що режим уже розгорнуто. Ціль і стан інтеграції — [канонічна задача](../../service/tasks/ai-lexicon-and-admin-reset.md), контракт — [doc 17](17-api-and-data-reference.md).

**[НОВЕ, фази 2–4 · D16]** Одиниця пошуку й збереження — **сенс**, а не переклад: варіанти одного значення зливаються в один рядок (§5.1), збереження дедуплікується по сенс-групі (§6.2), скарга на переклад стала явною кнопкою (§5.2).

Суміжні документи (не дублюються тут): онбординг — `02`, дані/кеш — `03`, економіка/монетки — `04`, sync+мерж — `06`, мови/мовлення/теми (деталі STT) — `08`, крайові випадки — `10`, motion-бриф (морф-анімація) — `12`.

Легенда: **[ЗАРАЗ]** — як у коді сьогодні; **[НОВЕ]** — затверджена зміна (D1–D16); **[МАЙБУТНЄ]** — відкладено.

---

## 1. Вхідна точка: морф-оверлей із пігулки

> **[ЗАРАЗ→змінено фазами 2–4] Коду цього оверлея більше немає.** Живий шлях
> додавання — **док унизу словника** (`InlineAddWordBar` + `InlineTranslationPanel` в
> `App.kt`), див. §3.1 і §4.1. Композабл `AddWordOverlay` разом із `AddWordOrigin`,
> `AddWordMode`, `AddWordHeader`, `AddWordSearchField`, `MicStage`,
> `HoldToTalkButton`, `AddedCountBar` і `lerpColor` **видалено** прибиранням мертвого
> коду наприкінці фаз 2–4 — вони не мали жодного споживача з моменту редизайну. У
> вжитку лишились `AddWordLoadingState` / `AddWordErrorState` / `AddWordResultsList` /
> `AddWordResultRow` / `VoiceWaveform` (їх викликає док). **§1–§2, §3 і таблиця §4
> нижче — історичний опис видаленої поверхні**: рядки коду в них уже не резолвляться,
> тримаємо їх лише як пояснення, звідки взявся дизайн доку.

Оверлей `AddWordOverlay` морфився з пігулки «+ слово» у словнику до повноекранної поверхні.

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

> `AddWordMode { Idle, Recording, Results }` — декларований enum, яким **ніхто ніколи не керував** (станами розпоряджались `isListening` / `searchState.isLoading` / `searchState.errorMessage` напряму); **[ЗАРАЗ→змінено фазами 2–4]** видалений разом з оверлеєм. `AddWordSearchState` — носій loading/results/error + `tier`/`maxResults` — лишається живим: його віддає `searchRemotely` і споживає док.

Recording-слот хвилі має фіксовану висоту (80 dp + 30 dp spacer), порожній в Idle — щоб мікрофон не «стрибав» між станами (`:519-527`).

### 4.1 [ЗАРАЗ] Стани панелі доку (`InlineTranslationPanel`)

Панель відкривається, щойно є запит, завантаження, помилка пошуку або помилка голосу
(`showTranslationPanel`). Порядок `when`-ланцюга важливий:

| Стан | Умова входу | Що показано |
|---|---|---|
| **Помилка голосу** | `speechError != null && query.isBlank()` | `VoiceRecognitionErrorState` — див. нижче |
| **Loading** | `searchState.isLoading`, включно з `meta.generation.status=generating` | `AddWordLoadingState`: спінер `Purple` на треку `Tint` + «AI готує значення та приклади…» й «Перший пошук може тривати кілька хвилин»; порожній проміжний `results=[]` не показується як «нічого не знайдено» |
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

**[ЗАРАЗ, локально] Довга AI-генерація.** Звичайний `GET /v1/search?q=…&speak=…&learn=…`
може повернути `meta.generation={id,status:"generating",retryAfterMs}` із тимчасово
порожнім `results`. `RemoteLexiconSearchUseCase` залишає панель у Loading і опитує
**тільки** `GET /v1/search/generations/:id`, передаючи початкові `speak`/`learn`;
цей GET не створює новий пошук і не списує ще одну монетку. `complete` (або відповідь
без generation) віддає готові варіанти; `failed`, недоступна робота чи понад 45 хвилин
очікування дають помилку з повторною спробою. Зміна запиту скасовує корутину й
подальші опитування. Інтервал із сервера обмежений клієнтом до 250 мс…10 с.

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

### 5.1 [НОВЕ, фаза 2] Один айтем на сенс: головний переклад + «близькі за значенням»

Варіанти відповіді зливаються в **одну опцію на сенс**
(`RemoteLexiconSearchUseCase.toSenseGroupedOptions`), тож `бігти` і `гнати` для
значення «рухатися швидко» більше не займають два рядки.

| Правило | Деталі |
|---|---|
| Сенси варіанта | доменні `WordDetails?.senseMergeKeys()` — стабільні `senseKeys`, інакше легасі `legacy:$senseIndex` (лише коли індекс справді вказує на наявний sense). Те саме правило, що й у словнику (`groupBySense`/`overlapsSenseGroup`) — єдина копія живе в `VocabularyModels.kt` |
| Злиття: **якір на головного** | група тримає множину сенсів свого ПЕРШОГО варіанта; кандидат вливається, якщо його ключі **перетинаються** з ключами якоря і слово-джерело (`learningWord`, `trim().lowercase()`) те саме. Рівності вимагати не можна: у V2-даних ~1,6% головних слів мають ширший переклад із зайвим сенсом (`strange` → `дивний[k1]` + `чудернацький[k1,k2]`, так само `chicken`, `determine`) — два рядки з однаковим підписом читались би як баг |
| Без транзитивності | якір НЕ розширюється прийнятими кандидатами: `[k2]` не приклеїться до групи `[k1]` через місток `[k1,k2]` (інакше ланцюжок сенсів злипся б в одну «мега-групу»). Тут відмінність від словника, де `groupBySense` зливає перетин транзитивно — там записи накопичуються між ревізіями лексикону, а тут уся відповідь з однієї |
| Неатрибутовані варіанти | **не зливаються** навіть між собою: без атрибуції невідомо, чи це той самий сенс, тож кожен лишається окремим рядком (стара поведінка) |
| Головний у групі | перший за порядком сервера (сервер уже сортує `isPrimary`/`confidence`); дублі перекладу в межах групи згортаються по `trim().lowercase()`, і з дублів лишається екземпляр із непорожнім `translationId` |
| `TranslationOption.alternatives` | решта групи повноцінними опціями (свій `translationId`, свої `details`) |
| `WordDetails.senseGroupTranslations` | усі переклади групи, представник першим — і в головного, і в кожної альтернативи; порожній для одинарної групи. Пишеться в `detailsJson` (`encodeDefaults=false`, старий JSON читається без міграції) і сам робить `shouldPersist` істинним. `senseKeys` теж зберігаються навіть якщо інших деталей або `translationId` немає, щоб збереження не втратило ідентичність сенсу. **Best-effort підказка, не джерело правди:** канонічний серверний снапшот під час синку може замінити `metadata.details` проєкцією linked-рядка і стерти список. Авторитетне джерело «близьких» у словнику — локальне групування збережених записів (`groupBySense`); підказка лише доповнює його, поки в словнику є один переклад сенсу (рендер — Task 5) |

UI (`AddWordResultRow`):

- під перекладом — **перший рядок сенсу** (`WordDetails.firstSenseLine()`: дефініція АТРИБУТОВАНОГО сенсу, інакше його ж перший приклад; без атрибуції рядка немає) — саме він розрізняє два айтеми одного слова у згорнутому списку; у розгорнутому стані ховається, бо ту саму дефініцію показує `WordDetailsBlock`;
- `canExpand` тепер враховує ще й наявність альтернатив;
- у розгорнутому стані під `WordDetailsBlock` — блок **«Близькі за значенням»** (`SenseAlternativesBlock`): рядок на кожну альтернативу зі своїм `+`/`✓`. Кнопка кладе у словник саме обраний синонім (`onAdd(alternative)`), з тими самими деталями сенсу й списком групи;
- деталі без видимого вмісту (лише список групи) блоком не малюються.

### 5.2 [НОВЕ, фаза 2] Скарга на переклад — явна дія, а не «вирок»

**[ЗАРАЗ→змінено фазами 2–4]** Раніше низ рядка займав текст-вирок «Неякісний
переклад». Тепер це **кнопка-дія** у стилі вторинної: прапорець `PrototypeIcon.Flag`
13dp + підпис «Поскаржитись на переклад», surface `NeutralSurface` з бордером
`Line2`, r12, вирівняна праворуч, `heightIn(min = 40.dp)` (без цього тап-таргет
виходив ~30dp). Показується лише коли `option.translationId` непорожній.

Колбек не змінився: `onDislikeTranslation` → `POST /v1/lexicon/quality-feedback`
(деталі — [17](17-api-and-data-reference.md) §1.10), рядок не видаляється, у відповідь
— снекбар «Дякуємо, переклад позначено для перевірки». Скарга стосується **конкретного
перекладу**, а не всієї сенс-групи: `translationId` береться з тієї опції, на рядку
якої натиснули.

---

## 6. Виявлення дубліката (`+` ↔ `✓`)

**[ЗАРАЗ, локально]** Живе `+`/`✓` звіряє збережені `WordEntry` за джерельним
словом і **stable sense key**, потім `translationId`; лише старі записи без
обох ID відступають до пари текстів. Якщо одна сторона має ID, а старий
збережений рядок тієї самої текстової пари — ні, `✓` також показується:
репозиторій консервативно не дозволяє вставити такий дублікат. Це не ставить ✓ на всіх однаково
написаних перекладах різних значень. Якщо контекстний переклад має іншу
граматичну форму, але той самий sense ID, повний пошук впізнає збережений
сенс. Toggle оновлюється зі свіжим `topic.words` і видаляє **точний UUID**
особистого запису, щоб не стерти інше значення з тією самою парою текстів.

```
isAdded = option.savedWordIn(topic.words) != null
```
`AddWordOverlay.savedWordIn`.

Слово-джерело обов'язкове: збережений `run→серія` не позначає ✓ на
`series→серія`. Якщо обидва боки мають ID, та сама текстова пара з іншим
сенсом залишається окремою й може бути збережена окремо; для неоднозначної
легасі-пари без ID репозиторій консервативно не створює дубль.

Прапорець `TranslationOption.alreadyAdded` у цю умову **не входить**: він
заморожений на момент пошуку і рахується з того самого клієнтського набору, тож
у суміші робив би toggle однобічним (після видалення рядка ✓ лишалась би
назавжди). `alreadyAdded` і `note` досі обчислюються в `toOption`, але
**продакшн-споживача в UI не мають** — підпису «додано раніше» на екрані немає
(рендер `TranslationOptionNote` прибрано історично); поля лишаються заради
сумісності моделі й тестів.

| Шлях | Деталі | Код |
|---|---|---|
| Знімок `alreadyAdded` (без споживача в UI) | `searchRemotely` віддає `saved = topic.words.savedWordKeys()` в use-case; `SearchVariant.toOption` звіряє `savedWordKey(learningWord, knownWord)` і ставить `alreadyAdded` + note `AlreadyAdded`. На екран це наразі не впливає ніяк — ні на `+`/`✓`, ні на підпис | `App.kt` (`searchRemotely`), `RemoteLexiconSearchUseCase.toOption` |
| [НОВЕ, фаза 2] `alreadyAdded` на групі | заморожений знімок для compatibility лишається в use-case; живі `+`/`✓` головного рядка й альтернативи тепер беруть ID-порівняння зі збереженими записами | `RemoteLexiconSearchUseCase.toSenseGroupedOptions`, `AddWordOverlay.savedWordIn` |
| Живі `WordEntry` | `topic.words` — джерело правди для `+`/`✓`, перераховується на recomposition | `AddWordOverlay.kt`, `App.kt` (`DictionaryDetailScreen`) |
| Toggle | `isAdded` → видалити знайдений особистий `word.id` (фіолетова `✓`); інакше додати (акцент `+`) | `AddWordOverlay.kt`, `VocabeeEvent.RemoveWord` |
| Легасі fallback | за відсутності stable ID у **обох** записів `savedWordKey` порівнює нормалізовану пару текстів | `VocabularyModels.kt` (`matchesSavedMeaning`) |

### 6.1 [ЗАРАЗ, локально] Розбір речення лише після підтвердження

`AddWord`, відкриття деталей і поява картки тренування **не запускають** розбір.
Коли користувач торкається власного прикладу збереженого сенсу без готового
`contextGlossary`, застосунок показує діалог «Розібрати речення на слова?» із
«Скасувати» / «Розібрати». Скасування не робить запиту. Підтвердження запускає
один `POST /v1/search/context-glossary` для точного речення й пари мов; повторний
тап під час роботи не запускає дубль. Коли готовий glossary уже є, токени
відкриваються без AI-запиту. Це окрема операція без списання монеток за токени.

`ContextGlossaryUseCase` чекає `generation.status=generating` через
`GET /v1/search/context-glossary/generations/:id` (без повторного POST), максимум
45 хвилин (верхня межа, не очікуваний час); `failed`/недоступність показують помилку й дозволяють повторити тап.
Успішна відповідь звіряється з точним реченням, мовами й UTF-16 діапазонами,
після чого `WordDetails.contextGlossary` зберігається в Room і синхронізується у
звичайному `topic_words.metadata`. Якщо за час очікування слово або його приклад
змінилися, запізніла відповідь не перезаписує нову картку. Деталі контракту —
[doc 17](17-api-and-data-reference.md); ціль часткового кешу контекстних слів —
[канонічна задача](../../service/tasks/ai-lexicon-and-admin-reset.md) §6–7.

**[ЗАРАЗ] Додавання просто з контексту.** У розкритій картці слова блок «Контекстний приклад»
рендериться тим самим `ContextGlossarySentence`, що й тренування, але з дією
`ContextGlossaryTokenAction.AddToDictionary`: тап по нецільовому слові відкриває попап із
контекстним перекладом і кнопкою `+`, яка одразу кладе слово в **цей** словник за 1 монетку
(`onAddContextWord` → `spendTranslationBee` → `AddWord`). Слово, яке вже є у словнику,
показується жовтою заливкою, у попапі має `✓` і не клікається — повторного списання немає.
Опційні `token.translationId`/`token.senseKey` переходять через bookmark у
`WordDetails.translationId`/`senseKeys`, Room і sync; ключ закладки й ✓ також
відрізняють два однаково написані контекстні значення. Слово, створене з
речення, лишається **неповною** статтею для наступного повного пошуку (§10).
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

### 6.2 [НОВЕ, фаза 2 · D16] Дедуп ЗБЕРЕЖЕННЯ — один запис на сенс

`+`/`✓` вище — про **позначку на екрані** (пара слово+переклад). Окремо від нього
працює гейт **збереження**, і він рахує вже не пару, а **сенс**:

```kotlin
// VocabeeStore.addWord → isSenseAlreadySaved(topicId, source, details)
topic.words.any { word -> word.overlapsSenseGroup(source, details) }
```

| Правило | Деталі |
|---|---|
| Предикат | `WordEntry.overlapsSenseGroup(candidateSource, candidateDetails)`: збіг слова-джерела (`trim().lowercase()`) **плюс непорожній перетин** множин sense-ключів; за їх відсутності — той самий `translationId`. Та сама семантика, що в `groupBySense` — рівність підписів тут не годиться: збережений `[k1]` після ревізії лексикону зустріне кандидата `[k1, k2]`, і це той самий сенс |
| Перевіряються ВСІ записи групи | не лише представник: запис-місток `[k1, k2]` мусить блокувати і `[k1]`, і `[k2]` |
| Кандидат без атрибуції | Порожні `senseKeys` **і** `translationId` → гейт не спрацьовує (нема по чому судити), лишається старий по-парний дедуп у Room. Один лише `translationId` захищає від повторного збереження цього ID з іншим текстом перекладу |
| Скоуп | лише **поточний словник**: той самий сенс у двох різних словниках — легальні два записи |
| Порядок | гейт стоїть **після** економічних перевірок (`canAddWordToDictionary`) і **до** `addWordUseCase`, тож відмова не створює рядок і не списує монетку |
| Наслідок за дизайном | місток `[k1, k2]` блокує окреме збереження `[k2]` — сенси, які лексикон уже звів разом, у словнику лишаються однією карткою |

**Відмова видима, а не мовчазна.** Гейт кладе у стан `pendingMessage =
SenseAlreadySavedMessage` («Цей сенс уже у словнику»); екран деталей показує його
снекбаром **над IME** (снекбар-хост усередині `DictionaryDetailScreen`, інакше
клавіатура пошуку його б перекрила), маршрутний хост лишається фолбеком.
`consumePendingMessage()` знімає текст, щоб та сама відмова могла спливти ще раз.

> **Відома шорсткість (minor, у беклозі):** повідомлення споживається після показу,
> тож повернення на екран упродовж вікна снекбара може показати його вдруге; лікується
> one-shot каналом замість поля стану.

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

## 10. Серверний `/search`-пайплайн: поточний AI-only та legacy

**[ЗАРАЗ, локальна нова гілка]** За `LEXICON_AI_ONLY=true` (default) звичайний
пошук бере перевірену повну статтю з бази або запускає її цілісну AI-генерацію.
Контекстна/неповна стаття має добудовувати лише відсутні значення, не дублюючи
наявний сенс; сам prefix-ввід не запускає таку генерацію. Значення, переклади,
приклади та sense-level відношення мають утворювати один узгоджений запис із
стабільною атрибуцією; старий імпорт/суміш незалежних provider-метаданих не є
прихованим fallback. Див. [канонічну задачу](../../service/tasks/ai-lexicon-and-admin-reset.md)
§3–8 і [doc 17](17-api-and-data-reference.md) для стану серверного контракту.

**[LEGACY, вимкнено за `LEXICON_AI_ONLY=true`]** Нижченаведені кроки
`freshness/top-up → Hunspell → CompositeTranslationProvider →
CompositeDictionaryProvider → repair`, таблиця `providerReason`, провайдери
§11 та валідатор §12 описують збережений старий шлях. Вони застосовні лише
коли прапорець AI-only явно вимкнено; їх не слід читати як поведінку нового режиму.

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
| 2 | **Префікс-кеш lexicon** | `findPrefixMatches(detectedLang, normalized, otherLang, maxResults)` — `LIKE 'q%'`, exact-first, тоді primary, тоді коротші слова. Дає живі підказки «cir → circle/circus/circumstance». Якщо exact-збігу нема, сервер спершу обрізає список до 15 видимих варіантів, а тоді для кожного підтягує вже збережений snapshot IPA/PoS/senses/examples/forms/relations і V2 `senseKeys`. Це read-only hydration: dictionary/AI, sense-attribution і quality repair для часткового вводу не запускаються. Legacy sense без персистованого key повертає `senseKey=null`, а не вигаданий stable ID. | `lexicon.repository.ts`, `lexicon.service.ts` |
| 3 | **Freshness / top-up** | **[ЗАРАЗ]** Якщо є exact-cached і провайдер `isAvailable`: рядок свіжий, коли `translator.acceptsTier(row.providerTier)` **або** tier `seed-import`. Кураторський імпорт — не лише свіжий, а й **immutable у звичайному `/search`**: missing IPA/metadata, quality score або pending repair не запускають автоматичний provider top-up і не переписують reviewed-дані. AI-family (`openai-*`/`ai-*`/`audit-*`) приймає одна одну: зміна `OPENAI_MODEL` не знецінює runtime-кеш. | `lexicon.service.ts`, `provider-tier.ts` (`CURATED_IMPORT_TIER`) |
| 4 | **Word-validator (квота-гейт)** | Для слова — Hunspell, для фрази — phrase-validator; додатково пропускаються короткі/uppercase/dotted кандидати на абревіатуру (`btw`, `LOL`, `NATO`, `U.S.`), а остаточну валідність вирішує structured AI | `lexicon.service.ts`, word-validator §12 |
| 5 | **Провайдер перекладу + lexical metadata** | OpenAI класифікує обидві мовні сторони як `word/phrase/expression/abbreviation`, окремо дає register tags, розшифровки, значення, дослівний переклад і приклади; напрямок орієнтується так, щоб mobile завжди отримав metadata learning-side одиниці | `openai-translation.provider.ts`, `translation.provider.ts` |
| 6 | **Echo-гард** | Кандидати = провайдер-результати, де `normalize(text) != normalizedQuery`. Усі збіглися з вводом → `echo` (НЕ персистимо). `null` → `no_provider_data` | `lexicon.service.ts:209-242` |
| 7 | **Upsert lexicon + directional cache** | `persistAllVariants`: upsert source і target words, зв'язати `target_word_id`, зберегти тільки `detectedLang → otherLang`. Reviewed V2 import переносить IPA, якщо його однозначно дає Kaikki, зберігає stable `senseKey` і всі reviewed `translations[].senseKeys` у many-to-many `translation_senses`; перший зв'язок також проєктується в legacy `translations.sense_id`, тому чинний клієнт одразу отримує контекст без AI-атрибуції. Відсутній IPA не блокує імпорт і не запускає runtime AI. V1 endpoint лишається лише для сумісності. | `lexicon.service.ts`, `dictionary-lexicon-import.service.ts` |
| 8 | **Збагачення (IPA/audio/examples/senses)** | Для runtime/provider rows `enrichLearningEntry` (тільки не-фраза) може запустити dictionary-ланцюг (OpenAI → FreeDictionary). **Відсутній IPA не форсить runtime-enrichment:** exact-запис повертається з `ipa=null`, а IPA дозаповнюється окремим curated batch/re-import. Runtime-рефетч exact-кешу лишається для `form-of-only`. Для `seed-import` exact lookup лише збирає вже персистовані IPA/senses/examples/forms: відсутнє поле повертається порожнім і **не є дозволом на runtime-добудову**. Senses обмежуються union активних `translation_senses` саме для requested target language; examples читаються лише з `translation_lang = targetLang OR NULL`, target-specific рядок має пріоритет. Порожня directional attribution не відкриває всі word-global senses. Одночасно сервер надсилає `lexicon_curated_data_missing` з парою мов, source/target, `translationOrigin` і переліком прогалин — це черга для перегенерації нашого batch, не AI-fallback. | `lexicon.service.ts`, `lexicon.repository.ts`, `search-observability.ts`, `lexicon-core.module.ts` |
| 8.1 | **Runtime V2 sense attribution** | Кожен runtime sense одразу отримує детермінований stable `senseKey`; старі NULL-key senses ліниво апгрейдяться на exact search. OpenAI повертає для кожного перекладу один або кілька `senseKeys[]`, backend атомарно пише всі links у `translation_senses`, а `translations.sense_id`/`senseIndex` лишає як першу compatibility-проєкцію. Старі provider rows без V2 marker переатрибутуються один раз; miss має cooldown. | `sense-key.ts`, `openai-sense-attribution.provider.ts`, `lexicon.service.ts` |
| 9 | **Quality repair перед композицією** | Runtime translation rows із `qualityScore >= 100` можуть форсувати repair. `seed-import` не ремонтується автоматично через user search: сигнал лишається предметом явної кураторської правки й повторного імпорту. | `lexicon.service.ts`, `quality-feedback.service.ts` |
| 10 | **Композиція відповіді** | **[ЗАРАЗ]** Провайдер-хіти спершу, тоді не-дубль префікс-підказки, до `maxResults`. **Точний збіг перекриває підказки:** якщо введене слово є в базі (або його щойно переклав провайдер) — віддаємо лише його переклади, сусідів на ту саму букву відкидаємо. Точного збігу нема (часткове введення `cir`) → не більш як `PREFIX_SUGGESTION_LIMIT = 15` підказок, кожна з повним уже персистованим snapshot деталей. Однаковий source-example у кількох мовних імпортах дедуплікується на сервері за normalized `(senseId,text)` після target-language фільтра; mobile не маскує помилки даних локальним `distinct`. Під час успішного quality repair старі low-quality variants фільтруються; варіанти дедупляться по парі normalized known/learning text. | `lexicon.service.ts` |

### `providerReason` (meta — чому викликали/не викликали провайдер)

| Значення | Зміст | Код |
|---|---|---|
| `exact_cached` | вже є свіжий кеш — не дзвонили | `lexicon.service.ts:197-198` |
| `not_a_word` | не пройшов spell-чек — пропуск заради квоти | `:199-201`, `:241` |
| `echo` | провайдер віддав ввід дослівно — трактуємо як «нема перекладу», не зберігаємо | `:233-234` |
| `no_provider_data` | реальне слово, але ланцюг нічого не дав | `:235-239` |
| `translated` | провайдер дав реальний переклад → персист | `:214-216` |

### Sentry + PostHog спостереження за пошуком і витратами

**[ЗАРАЗ]** `dictionary-gateway` надсилає структуровані Sentry Logs (не Issues) для
трьох продуктово важливих результатів: `lexicon.search.cache_hit` для
`exact_cached`; `lexicon.search.no_translation` для `not_a_word`, `echo` і
`no_provider_data`; `lexicon.search.ai_translation_generated` для успішного
перекладу з AI-origin (`openai-*`/`ai-*`). Кожен запис має `search.language`,
`search.target_language`, `search.is_phrase`, `search.result_count` і, де доречно,
причину або origin. Сам текст запиту, user id, email та HTTP body навмисно не
надсилаються.

**[ЗАРАЗ]** Кожна provider-спроба, викликана прогалиною вже збереженого запису,
окремо надсилає personless PostHog event `lexicon_incomplete_data_fallback` і
Sentry log `lexicon.incomplete_data.provider_fallback`. Властивості: `operation`,
`gap_reasons`, `provider`, мови, `lexicon_word_id`, `curated_import` і
`potentially_billable`. Причини runtime-fallback охоплюють missing lexical metadata/variant,
form-of-only, missing sense examples/attribution та quality repair. Подія з
`curated_import=true` є regression-сигналом: нормальний `seed-import` заблокований
від таких fallback. Якщо curated exact-запит неповний, окремий personless event
`lexicon_curated_data_missing` не викликає провайдера й містить `missing_fields`,
`source_lang`, `target_lang`, `learning_lang`, source lemma/normalized, target text,
`translation_id`, `translation_origin` та `regeneration_key` для точного відбору
рядків під час перегенерації.

Runtime fallback telemetry не містить сирий user query/lemma: `lexicon_word_id` достатній,
щоб знайти cache row для діагностики, не відправляючи імена чи довільний введений текст у
Sentry/PostHog. Curated-missing event може містити lexeme/target із reviewed batch, бо це
не user input і саме вони утворюють regeneration key.

### Відповідь `SearchResponseDto` → клієнт

`SearchResponse` (моб, `SearchResponse.kt`) дзеркалить `SearchResponseDto`. Крім звичайних dictionary-полів, кожен `SearchVariant` має `lexicalUnitKind`, `registerTags`, `expansion`, `translatedExpansion`, `meaning`, `literalTranslation`, `usageExample`, `usageExampleTranslation`. `RemoteLexiconSearchUseCase.toOption` мапить їх у JSON-снапшот `WordDetails`.

---

## 11. Провайдери та ланцюги (DI: `TRANSLATION_PROVIDER` / `DICTIONARY_PROVIDER`)

### Провайдер перекладу (`CompositeTranslationProvider`)

**[LEGACY]** Цей DI-ланцюг збережений у коді, але за `LEXICON_AI_ONLY=true` не викликається.
У старому режимі він містить **OpenAI translation**. Модель конфігурується через
`OPENAI_MODEL`, default — `gpt-5.6-sol`; structured JSON повертає до 8 варіантів.
Кожен результат тегається `openai-<model>` — це provenance для тулінгу/аудиту, а
**не** критерій свіжості: `acceptsTier` приймає всю AI-родину, тож свап моделі не
тригерить масовий рефетч (§10, крок 3). Погані окремі рядки регенеруються через
quality-feedback і адмін-видалення (`translation_pair_repairs`), не через tier.

| Провайдер | tier-name | variants/call | Доступність | Код |
|---|---|---|---|---|
| Wiktionary (мультиваріант, per-sense таблиці) | `wiktionary` | 10 | `!!baseUrl`; знімає наголоси `́`, дедуп | `wiktionary-translation.provider.ts:45-104` |
| DeepL (1 переклад/запит) | `deepl-free`/`deepl-pro` (детект по суфіксу `:fx`) | 1 | `!!apiKey`; `EN-GB`/`PT-PT` overrides | `deepl.provider.ts:31-77` |
| MyMemory (TM-фолбек, без ключа) | `mymemory` | 1 | завжди | `mymemory.provider.ts:25-55` |
| OpenAI translation (structured JSON) | `openai-<model>` (default `openai-gpt-5.6-sol`) | 8 | `!!apiKey`; `reasoning_effort=none`, temp 0.2; repair запитує тільки `desiredVariants` і передає exclusions | `openai-translation.provider.ts` |
| Mock (тести/офлайн) | `mock` | 1 | завжди | `mock-translation.provider.ts:10-44` |

`CompositeTranslationProvider`: `acceptedTierNames` = union дітей, `variantsPerCall` = max, `isAvailable` = є хоч один usable (`composite-translation.provider.ts:19-58`).

### Ланцюг словника / збагачення (`CompositeDictionaryProvider`)

**[LEGACY, AI-only вимикає]** Порядок старого режиму: **OpenAI dictionary → FreeDictionary** (перший із даними виграє; кожен ставить свій `origin`).

| Провайдер | origin | Деталі | Код |
|---|---|---|---|
| FreeDictionary (Wiktionary-backed, `?translations=true`) | `freedictionaryapi.com` | IPA (`extractPhonemicIpa`), PoS, до 8 senses, синоніми/антоніми, форми; **form-of-only → `null`** (щоб AI добив) | `free-dictionary.provider.ts:68-242` |
| OpenAI dictionary | `openai-<model>` (default `openai-gpt-5.6-sol`) | structured JSON: ≤5 senses (≥1 приклад кожен), ≤5 синонімів, ≤4 антонімів, ≤6 форм; підтримує `en/uk/ru/pl/de/es` | `openai-dictionary.provider.ts` |

`CompositeDictionaryProvider.supports/lookup` — `composite-dictionary.provider.ts`. Для runtime/provider cache `cachedSensesAreFormOfOnly` може змусити рефетч, якщо кеш «form-of-only»; відсутній IPA не є тригером. Для `seed-import` runtime-евристики вимкнені повністю.

**[LEGACY]** FreeDictionary лишається fallback лише коли AI-only явно вимкнений;
окремого Wiktionary translation-провайдера в тому DI-ланцюгу немає.

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

**[LEGACY, AI-only вимикає]** `WordValidator` (Hunspell через `nspell`, словники `dictionary-en` / `dictionary-uk`) — `word-validator.ts`. У новому режимі формат, мови й структура все одно перевіряються технічно, але сторонній словник не відкидає слово до AI.

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

### [ЗАРАЗ, локально] Поточний шлях клієнта й асинхронна відповідь

```text
док словника → debounce → GET /v1/search
  ├─ повна стаття в базі → готові сенс-групи
  └─ генерація → meta.generation.id → безплатний GET /v1/search/generations/:id
                      (ті самі speak/learn; Loading до complete або помилки)
готовий результат → «+» → локальний WordEntry + sync
тап по власному нерозібраному прикладу → діалог → підтвердження
  → POST /v1/search/context-glossary → за потреби безплатний GET .../generations/:id
  → точний contextGlossary в Room/sync → офлайн-токени
```

### [АРХІВ] Старий runtime V2 / оверлей (видалена UI-поверхня та legacy backend)

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
   → enrich (OpenAI→FreeDictionary: IPA/audio/stable senses/syn/ant/forms)
   → V2 attribution (translation → senseKeys[] → translation_senses)
   → compose → SearchResponseDto{senses[].senseKey, results[].senseKeys[],
     legacy senseIndex, tier, maxResults, meta.beeBalance}
   ▼
RemoteLexiconSearchUseCase.toOption → List<TranslationOption>
   ▼
AddWordResultsList (слово+IPA+переклад+Sparkle, розгортання деталей, +/✓)
   │   дубль: живий savedWordKeys (пара слово+переклад); alreadyAdded — без споживача в UI
   ▼
onAdd → canAddWordToDictionary() → AddWord (learningWord, value, ipa, details + translationId) → sync
   │   auth sync: Dictionary target-scoped projector → schema+SHA-256 revision
   │   changed snapshot → повна заміна text/IPA/details; прогрес/context окремо (D15)
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
