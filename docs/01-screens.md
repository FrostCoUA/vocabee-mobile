# 01 — Карта екранів і шторок (Vocabee mobile)

Повна карта UI-шару клієнта (Kotlin Multiplatform / Compose). Документ описує **усі екрани**, **усі bottom sheets (шторки)**, оверлей додавання слова та навігацію. Для кожної поведінки явно вказано `[ЗАРАЗ]` (як працює в поточному коді) та `[НОВЕ]` (затверджена зміна за рішеннями D1–D9).

Основні файли:
- `presentation/App.kt` — `VocabeeApp`, `MainApp`, усі основні екрани, банери, частина шторок.
- `presentation/OnboardingFlow.kt` — `SplashScreen`, `OnboardingScreen`, `AuthScreen`, `LanguageSelectScreen`.
- `presentation/AddWordOverlay.kt` — оверлей `AddWordOverlay` (морф-анімація з пігулки) + спільні стани (loading/error/results), які перевикористовує екран деталей.
- `presentation/PrototypeBottomSheets.kt` — каркас `PrototypeBottomSheet`, `CreateDictionarySheet`, `LanguageSheet`.
- `presentation/navigation/VocabeeNavigation.kt` — маршрути, таби.

---

## Зведена таблиця

| Екран / шторка | Тип | Коли показується | Файл:рядок |
|---|---|---|---|
| Splash | full-screen | Кожен запуск (візуальний інтро) | `OnboardingFlow.kt:100` |
| Onboarding (3 слайди) | full-screen | Перший запуск, після Splash | `OnboardingFlow.kt:182` |
| Auth | full-screen | Перший запуск, після Onboarding | `OnboardingFlow.kt:403` |
| LanguageSelect | full-screen | Перший запуск, після Auth | `OnboardingFlow.kt:732` |
| DictionariesHome (Словники) | таб | Головний таб після входу в Main | `App.kt:1355` |
| DictionaryDetail (TopicDetail) | пуш | Тап по картці словника | `App.kt:2017` |
| AddWordOverlay | оверлей | Морф із пігулки «+» (окремий компонент) | `AddWordOverlay.kt:136` |
| Practice (Тренування) | таб | Таб «Тренування» | `App.kt:3327` |
| Profile (Профіль) | таб | Таб «Профіль» | `App.kt:4087` |
| MissingTopic | пуш-фолбек | TopicDetail з неіснуючим id | `App.kt:4651` |
| CreateDictionary | sheet | FAB / порожній стан Home | `PrototypeBottomSheets.kt:134` |
| DeleteDictionary | sheet | Свайп-видалення картки | `App.kt:999` |
| LanguageForDictionary | sheet (заглушка) | Тап на мовний індикатор у деталях | `App.kt:826` |
| LanguageForProfile | sheet | Тап на рядок мови в Профілі | `App.kt:843` |
| NeedBees (BeeRewardSheet) | sheet | Бракує монеток | `App.kt:1067` |
| AuthRequired | sheet | Анонім уперся в ліміт | `App.kt:1144` |
| SyncConflict | sheet | Вхід з конфліктом даних | `App.kt:1219` |
| Exit | sheet | Системний Back на корені | `App.kt:941` |

Загальний потік верхнього рівня — `VocabeeApp` (`App.kt:278`): машина станів `AppFlow { Splash, Onboarding, Auth, LanguageSelect, Main }` (`App.kt:155`).

---

## 1. Splash

`SplashScreen` — `OnboardingFlow.kt:100`.

- **Призначення.** Брендований інтро-екран на старті.
- **Коли показується.** `[ЗАРАЗ]` На **кожному** запуску (`flow` стартує з `AppFlow.Splash`, `App.kt:298`). Після нього — розвилка за прапором `hasCompletedOnboarding`:
  - `skipFirstLaunchFlow == true` → одразу `Main`;
  - інакше → `Onboarding` (`App.kt:303–307`).
- **Візуальні стани.** Лише один стан (статичний). Немає loading/error.
- **Ключові елементи.** Радіальний фіолетовий градієнт `SplashGradient`; логотип `PrototypeLogo` 96dp; вордмарк «voca**bee**» (bee — жовтим, `OnboardingFlow.kt:123–130`); підзаголовок «Збирай слова. Будуй словники.»; знизу «MADE WITH CARE».
- **Дії користувача.** Авто-перехід через `delay(1900)` (`OnboardingFlow.kt:101–104`); **тап будь-де** теж викликає `onDone` (екран увесь клікабельний, `OnboardingFlow.kt:110`).
- **Неочевидне.** `skipFirstLaunchFlow` читається **один раз** через `remember` на старті (`App.kt:297`), тож зміна прапора в межах сесії не впливає на поточну розвилку.

---

## 2. Onboarding (3 слайди)

`OnboardingScreen` — `OnboardingFlow.kt:182`. Дані слайдів — `onboardingSlides` (`OnboardingFlow.kt:163`).

- **Призначення.** Пояснити три головні фічі.
- **Коли показується.** `[ЗАРАЗ]` Тільки на першому запуску, після Splash.
- **Слайди** (індекс у локальному `index`, `OnboardingFlow.kt:183`):
  1. «Зберігай слова під час читання» — арт `Read`.
  2. «Створюй тематичні словники» — арт `Organize`.
  3. «Повторюй, коли зручно» — арт `Practice`.
- **Візуальні стани.** Три статичні слайди; немає loading/empty/error. Арт малюється на `Canvas` (`OnboardingArtwork`, `OnboardingFlow.kt:297`).
- **Ключові елементи.** Кнопка «Пропустити» (верх-право); великий заголовок + підзаголовок; індикатор-точки (активна — жовта розтягнута пігулка, `OnboardingFlow.kt:248–262`); кнопка «Далі» / на останньому слайді «Почати».
- **Дії користувача.**
  - «Пропустити» → `onDone` → перехід до Auth (`OnboardingFlow.kt:202`).
  - «Далі» → `index += 1`; на останньому слайді кнопка стає «Почати» і викликає `onDone` (`OnboardingFlow.kt:267–269`).
- **Куди веде.** `onDone` у `VocabeeApp` веде на `AppFlow.Auth` (`App.kt:308`).
- **Неочевидне.** Перемикання слайдів — без свайпу, лише кнопкою/точок без кліку (точки не клікабельні).

---

## 3. Auth

`AuthScreen` — `OnboardingFlow.kt:403`.

- **Призначення.** Реєстрація / вхід в онбордингу.
- **Коли показується.** `[ЗАРАЗ]` Після Onboarding на першому запуску.
- **⚠ КЛЮЧОВЕ — екран ЗАРАЗ бутафорський.** Усі способи продовження ведуть в **той самий `onDone`** без реальної автентифікації:
  - кнопка «Зареєструватися/Увійти» → `onClick = onDone` (`OnboardingFlow.kt:474`);
  - «Продовжити з Google» → `onClick = onDone` (`OnboardingFlow.kt:479`);
  - «Продовжити з Facebook» → `onClick = onDone` (`OnboardingFlow.kt:490`);
  - «Пропустити» → `onDone` (`OnboardingFlow.kt:423`).
  Тобто email/пароль не валідуються й нікуди не відправляються; натискання будь-чого = пропуск.
- **`[НОВЕ]` (D5).** Екран стає **справжнім Google-входом**. З'являється розумна розвилка після входу:
  - увійшов і на сервері Є дані → пропускаємо вибір мов, тягнемо профіль із сервера → Main;
  - увійшов, акаунт новий → показуємо вибір мов → пушимо на сервер → Main;
  - скіпнув вхід → вибір мов → зберігаємо локально (анонім) → Main.
  Повне дерево та обробка конфлікту — у документі про онбординг/синхронізацію; тут лише оглядово.
- **Візуальні стани.** Перемикач `isSignup` (`OnboardingFlow.kt:404`) міняє заголовки/тексти/підпис кнопки між «Створи акаунт» і «З поверненням». Немає loading/error станів (бо немає реального запиту).
- **Ключові елементи.** Логотип; поля «Електронна пошта» / «Пароль» (`VocabeeInputField`, пароль під `PasswordVisualTransformation`); основна пілюля; розділювач «або»; дві соц-кнопки (Google/Facebook glyph — намальовані на Canvas); нижній рядок-перемикач «Вже маєш акаунт? → Увійти» ↔ «Ще немає акаунта? → Зареєструватися» (`OnboardingFlow.kt:511–525`).
- **Дії користувача.** Усе → `onDone` (див. вище), окрім тапа на рядок-перемикач, який лише міняє `isSignup`.
- **Куди веде.** `onDone` у `VocabeeApp` → `AppFlow.LanguageSelect` (`App.kt:309`).

---

## 4. LanguageSelect

`LanguageSelectScreen` — `OnboardingFlow.kt:732`.

- **Призначення.** Вибрати пару мов «я розмовляю» → «я вивчаю» (дефолт для нових словників).
- **Коли показується.** `[ЗАРАЗ]` Після Auth на першому запуску. `[НОВЕ]` (D5) Показується умовно — пропускається, якщо увійшов і на сервері вже є дані.
- **Візуальні стани.** Один стан (контент). Дефолти: `initialSpeak = "uk"`, `initialLearn = "en"` (`OnboardingFlow.kt:734–735`); реально передаються поточні мови зі стейту (`App.kt:312–313`).
- **Ключові елементи.**
  - Прев'ю-пілюлі обраної пари з іконкою-стрілкою (`LanguagePill`, `OnboardingFlow.kt:787–795`);
  - дві секції «Я РОЗМОВЛЯЮ» / «Я ВИВЧАЮ», кожна — сітка `LanguageGrid` 2-в-ряд (`LanguageCard`);
  - перелік мов фільтрується по `supportedLanguages`; якщо перетин порожній — fallback на повний `PrototypeLanguages` (`OnboardingFlow.kt:744–747`);
  - кожна секція **виключає** мову, обрану в іншій (`exclude`, `OnboardingFlow.kt:800/809`), щоб не можна було обрати однакову пару;
  - закріплена внизу кнопка «Готово».
- **Дії користувача.** Тап на картку мови → оновлює `speak`/`learn`; «Готово» → `onDone(speak, learn)`.
- **Куди веде / побічні ефекти.** `onDone` у `VocabeeApp` (`App.kt:314–325`): застосовує `SelectSpeakingLanguage` / `SelectLearningLanguage`, **виставляє `preferencesManager.hasCompletedOnboarding = true`** і переходить у `AppFlow.Main`.
- **Неочевидне.** Саме тут фіксується «онбординг завершено» — наступний холодний старт піде Splash→Main.

---

## 5. DictionariesHome — «Словники»

`DictionariesHomeScreen` — `App.kt:1355`. Маршрут `VocabeeRoute.DictionaryHome`; таб `AppTab.Dictionary`.

- **Призначення.** Головний екран: список тематичних словників, банер-слот стану акаунта, точка створення.
- **Коли показується.** Стартовий елемент бекстеку (`App.kt:356–358`), таб «Словники».

### Візуальні стани
- **Порожній (`topics.isEmpty()`).** `EmptyHomeState` (`App.kt:1404–1409`): ілюстрація, «Поки що порожньо», підказка, кнопка «Створити словник». FAB у цьому стані **не показується** (`App.kt:1424`), створення йде з центральної кнопки.
- **Непорожній.** Сітка 2-в-колонку (`LazyVerticalGrid`, `App.kt:1373`); картки `DictionaryCard` рендеряться в **реверсному** порядку (`topics.reversed()`, `App.kt:1412`) — найновіший зверху. Показується FAB (`App.kt:1424–1442`).
- Немає окремих loading/error станів (дані беруться зі `store.state`).

### Банер-слот (повноширинний, завжди під хедером, `App.kt:1386–1401`)
- `[ЗАРАЗ]` **Анонім** → `AnonymousFreeLimitBanner` (`App.kt:1503`): показує «Словники X/2 · слова Y/50», іконка User. `critical = topics.size >= FreeDictionaryLimit || totalWords >= AnonymousFreeWordLimit` (`App.kt:1367`) → заголовок «Ліміт гостя вичерпано», помаранчевий акцент; інакше «Гостьовий режим», фіолетовий. Тап → `onAuthBannerClick` → відкриває `AuthRequired(WordLimit)` (`App.kt:645`).
- `[ЗАРАЗ]` **Авторизований** → `BeeWalletBanner` (`App.kt:1563`): бейдж балансу монеток (`BeeBalanceBadge`, іконка Sparkle), «1 пошук коштує 1 монетку. Подивись відео і отримай +10». `critical = beeBalance <= CriticalBeeThreshold (3)` → «Монетки майже закінчились», помаранчевий. Тап → `onBeeBannerClick` → `showRewardedAdForBees()` (`App.kt:644`).
- `[НОВЕ]` (D2) Узгоджено: анонім **не має монеток/реклами/промо** — банер монеток показується лише авторизованому. Поточний код вже розводить ці два банери по типу акаунта.
- `[НОВЕ]` (D4) Промо: банер-слот має ставати **config-driven** (бекенд повертає копірайт банера title/subtitle/icon/accent/priority). Зараз вся копія банера захардкоджена.

### Хедер
`HomeHeader` (`App.kt:1447`): великий заголовок «Словники» + логотип; метрики «N словники · M слів зібрано» (тільки коли `topicCount > 0`).

### Картка словника
`DictionaryCard` (`App.kt:1792`), обгорнута у `SwipeRevealDeleteContainer` (`App.kt:1675`):
- колір з теми (`prototypeTopicTheme(coverIndex)`); фон-заливка за `knowledgePercent` (`KnowledgeBackgroundFill`, `App.kt:1813`); watermark стільників; бейдж «сьогодні» (`TopicUpdatedLabel.Today`); назва (до 2 рядків); чип «N слів»; мітка «оновлено» (вчора / N днів тому / N тижнів тому — `updatedLabelText`, `App.kt:1886`).
- **Свайп-видалення:** жест вліво відкриває червону кнопку «Видалити» (`SwipeRevealDeleteContainer`, поріг/інерція по velocity, `App.kt:1774–1781`). Тап по кнопці → `onTopicDeleteRequest` → шторка `DeleteDictionary` (`App.kt:661`).

### Дії та переходи
- Тап по картці → `onTopicClick` → push `TopicDetail(topicId)` (`App.kt:658`).
- FAB / кнопка в empty → `onCreateClick` (`App.kt:648–657`) з **тришляховою гілкою**:
  1. `store.anonymousDictionaryLimitReached()` → `AuthRequired(DictionaryLimit)`;
  2. `store.canCreateTopic()` → `CreateDictionary`;
  3. інакше → `NeedBees(DictionaryCreate)`.
- **Окремий плаваючий банер** `CriticalBeeBanner` (`App.kt:1611`) показується **поверх контенту** (не на Home), коли авторизований і `beeBalance <= 3` і поточний маршрут ≠ Home (`App.kt:772–785`). Тап → реклама.

---

## 6. DictionaryDetail (TopicDetail)

`DictionaryDetailScreen` — `App.kt:2017`. Маршрут `VocabeeRoute.TopicDetail(topicId)`. Якщо словник не знайдено — `MissingTopicScreen` (`App.kt:4651`).

- **Призначення.** Перегляд слів словника, додавання нових (інлайн-панель пошуку перекладу), озвучення, тренувальний прогрес, перемикач напрямку STT.

### Візуальні стани
- **Empty (`topic.words.isEmpty()`).** `DetailEmptyState` (`App.kt:3162`): ілюстрація, «Ще немає слів», підказка ввести/продиктувати, стрілка вниз до інпут-доку.
- **Content.** `LazyColumn` зі згрупованими словами; колапсуючий хедер `DetailHeader` зверху; інлайн-панель перекладу + інпут-док знизу.
- **Loading (пошук перекладу).** `AddWordLoadingState` (`AddWordOverlay.kt:656`): спінер + «Шукаю переклад…». Показується в `InlineTranslationPanel` коли `searchState.isLoading` (`App.kt:2592`).
- **Error.** `AddWordErrorState` (`AddWordOverlay.kt:678`): «Не вдалось отримати переклад» + повідомлення. Також помилка-гейту монеток/ліміту прокидається в `searchState.errorMessage` (`App.kt:2112–2116`).
- **No results.** `AddWordResultsList` з порожнім списком → «Нічого не знайдено для «X»» (`AddWordOverlay.kt:720`).

### Групування слів
`[ЗАРАЗ]` Слова однакового source-слова (case-insensitive) **схлопуються в одну картку** `WordGroupRow` (`App.kt:2771`); групи будуються `groupBySourceWord()` (`AddWordOverlay.kt:116`). Картка показує source + IPA + перелік перекладів через кому; details (значення/приклади/синоніми/форми) беруться з першого entry, де вони є. Є старий `WordRow` (`App.kt:2901`) для одиночного слова — наразі в екрані використовується саме `WordGroupRow`.

### Knowledge %
- Фонова заливка картки за середнім `knowledgePercent` групи (`averageKnowledgePercent`, `AddWordOverlay.kt:106`) — не як явний бейдж, а як прогрес-заливка (`KnowledgeBackgroundFill`).

### Кнопка озвучення (TTS)
- На кожній картці — квадратна кнопка з іконкою `Sound` (`App.kt:2861–2876`). Тап → `onSpeak(group.sourceWord, topic.sourceLanguage.speechTag)` → `speechOutputController.speak(...)` (`App.kt:718`).

### Перемикач напрямку STT (D8)
- У хедері — пілюля з двома прапорами та стрілкою (`DetailHeader`, `App.kt:2710–2729`). Тап → `onToggleSpeechDirection` → інвертує локальний `speechDirectionReversed` (`App.kt:2250`). Це міняє пріоритетну мову розпізнавача: `speechInputLanguage` / `speechOutputLanguage` (`App.kt:2061–2062`).
- `[ЗАРАЗ]` Стан напрямку **лише в пам'яті екрана** — `remember(topic.id, …) { mutableStateOf(false) }` (`App.kt:2045`), тож скидається при кожному відкритті словника.
- `[НОВЕ]` (D8) Має **зберігатися по словнику** (персистити напрямок per-topic).

### Хедер (колапсуючий)
`DetailHeader` (`App.kt:2649`): кнопка «назад»; пілюля напрямку STT; назва + «N слів · оновлено …»; згортається по скролу (`detailHeaderCollapseFraction`, `App.kt:2086`), інтерполюючи висоту/розмір шрифту/позиції.

### Інпут-док і панель перекладу (низ екрана)
- `InlineAddWordBar` (`App.kt:2368`): поле «Введи слово або фразу…» + кнопка mic/cancel. Утримання кнопки запускає голосове введення (`detectTapGestures` з затримкою `VoicePressStartDelayMillis = 250ms`, `App.kt:2478–2500`); під час слухання — хвиля `VoiceWaveform`.
- `InlineTranslationPanel` (`App.kt:2524`): слайд-ін панель з результатами; з'являється коли `showTranslationPanel` (є запит / loading / error / speechError, `App.kt:2056`).
- Складна геометрія під клавіатуру: `keyboardVisible`, паддінги дока, `InputDockBackdrop` (градієнт + blur, `App.kt:2336`).

### Дебаунс і списання монеток
- `[ЗАРАЗ]` Дебаунс пошуку — `delay(700)` після паузи у вводі (`App.kt:2109`). Перед запитом викликається `onSpendSearchBee()` (`App.kt:2111`); якщо повертає `false` → помилка-гейт + `onTranslationSearchBlocked()` (`NeedBees` або `AuthRequired`).
- `[НОВЕ]` (D1) Економіка стає **серверно-авторитетною**: клієнт списує оптимістично, але сервер валідує квоти в `applySync`; усуває подвійне списання.

### Дії та переходи
- «Назад» → `onBack` → `backStack.removeLastOrNull()`.
- Додати слово (`+` у результатах) → `onAddWord`; якщо `store.canAddWordToDictionary()` хибне → `AuthRequired(WordLimit)` (`App.kt:706–712`); після успіху — `syncVocabularyNow()`.
- Прибрати слово (`✓` → remove) → `onRemoveWord` → `RemoveWord` + sync (`App.kt:714–717`).
- Тап на пілюлю напрямку → перемикання STT (див. D8).
- Мовний індикатор у хедері відкриває `LanguageForDictionary` через `onOpenLanguageSheet` (`App.kt:676`) — **наразі заглушка** (див. шторку нижче).
- Системний Back при відкритій панелі/слуханні/фокусі → `cancelSearch()` (локальний `BackHandler`, `App.kt:2197`).

---

## 7. AddWordOverlay (оверлей з морф-анімацією)

`AddWordOverlay` — `AddWordOverlay.kt:136`. Окремий компонент (не екран навігації). Морфиться з пігулки-джерела (`AddWordOrigin`, `AddWordOverlay.kt:75`).

> Примітка: екран деталей реалізує додавання слова **інлайн-панеллю** (`InlineTranslationPanel`/`InlineAddWordBar`), а `AddWordOverlay` — повноекранний морф-варіант із тих самих будівельних блоків (`AddWordResultsList`, `AddWordLoadingState`, `AddWordErrorState`, `MicStage`). Обидва живуть у тому самому файлі та шерять стани.

- **Призначення.** Повноекранне додавання слова: голос або текст → результати перекладу → toggle «+ / ✓».
- **Морф-анімація.** `[ЗАРАЗ]` При відкритті `morph` 0→1 за `tween(420)`, тоді `content` 0→1 за `tween(280)` (`AddWordOverlay.kt:168–171`). Поверхня інтерполюється від прямокутника-пігулки (origin) до фул-скрін через `graphicsLayer` scale/translate (`AddWordOverlay.kt:253–265`), колір лерпиться `accent → White` (`lerpColor`, `AddWordOverlay.kt:923`). Закриття — у зворотному порядку (`close()`, `AddWordOverlay.kt:173`).

### Стани (enum `AddWordMode { Idle, Recording, Results }`, `AddWordOverlay.kt:82`)
- **Idle / порожній запит.** `MicStage` (`AddWordOverlay.kt:496`): велика кнопка mic, «Продиктуй слово» / «або почни вводити його у поле вгорі». Слот хвилі завжди зарезервований (фіксована висота 80dp), щоб mic не «стрибав».
- **Recording (waveform).** `MicStage` із `isListening = true`: анімована хвиля `VoiceWaveform` (28 смужок, фазовий зсув, `AddWordOverlay.kt:611`), помаранчевий mic, «Слухаю… / торкнись, щоб зупинити».
- **Loading.** `AddWordLoadingState` — спінер + «Шукаю переклад…».
- **Error.** `AddWordErrorState` — «Не вдалось отримати переклад» + текст помилки.
- **Results.** `AddWordResultsList` (`AddWordOverlay.kt:710`) — список `AddWordResultRow`.

### Список результатів і рядок «+/✓»
- Кожен рядок `AddWordResultRow` (`AddWordOverlay.kt:799`): canonical `learningWord` (а не сире введення — навмисно, `AddWordOverlay.kt:838`), IPA, іконка Sparkle (AI), переклад; розкривний блок деталей (`WordDetailsBlock`) якщо є.
- Toggle-кнопка 44×44 (`AddWordOverlay.kt:898–912`): якщо слово вже в словнику — фіолетовий `✓` (тап = remove); інакше accent `+` (тап = add). Геометрія однакова, щоб рядок не стрибав.
- «Доданість» рахується наживо: `option.alreadyAdded || existingTranslations.contains(...)` (`AddWordOverlay.kt:754`) — і серверний прапор, і миттєвий локальний апдейт.

### AI-атрибуція
- Футер списку: іконка Sparkle + «Переклади та приклади згенеровано AI» (`footerCaptionFor`, `AddWordOverlay.kt:791`). `[ЗАРАЗ]` Пер-тірні підписи («до N варіантів», «увійди для більше») прибрані — серверні ліміти зняті.

### Дебаунс 1с
- `[ЗАРАЗ]` У цьому оверлеї дебаунс — `delay(1000)` після паузи у вводі, `isLoading=true` ставиться одразу (`AddWordOverlay.kt:191–199`). (На інлайн-екрані деталей дебаунс — 700ms.)

### Лічильник доданих
- `AddedCountBar` (`AddWordOverlay.kt:933`) знизу: «N слів додано» + «Готово» (`close()`), показується коли `addedCount > 0`.

---

## 8. Practice — «Тренування»

`PracticeScreen` — `App.kt:3327`. Маршрут `VocabeeRoute.Practice`; таб `AppTab.Practice`. Дві фази: вибір тем → флеш-картки.

### Фаза 1 — вибір тем (`PracticeSetupScreen`, `App.kt:3531`)
- **Empty.** Якщо немає тренувальних тем (жодного слова) → `PracticeEmptyState` (`App.kt:3764`): «Немає слів для повторення».
- **Content.** Список `PracticeTopicPickerRow` (`App.kt:3673`) з чекбоксами; кожен рядок: іконка теми, назва, «N слів · X% знаю». Кнопка «Вибрати всі / Очистити» (`App.kt:3587`). Закріплений низ: «Вибрано: K тем · M слів» + кнопка «Почати тренування» (enabled коли `selectedWords > 0`).
- **Дії.** Тап по рядку → toggle теми; «Почати» → `practiceStarted = true` (якщо вибір не порожній).

### Фаза 2 — колода карток
- **Формування колоди (`App.kt:3364–3385`).** Зі слів обраних тем; shuffle із сидом `shuffleSeed`, сортування за зростанням `knowledgePercent` (спершу гірше вивчені), `take(10)` — максимум 10 карток на раунд.
- **`PracticeFlipCard` (`App.kt:3801`).** Flip-анімація по осі Y (`rotationY`, `tween(460)`). Лице: чип теми, source-слово, IPA, рівень засвоєння `PracticeKnowledgeLevel` (5 поділок), іконка озвучення, підказка «Торкнись, щоб побачити переклад». Зворот: переклад (`App.kt:3846–3853`).
- **4 кнопки відповіді / стани кнопок (`App.kt:3490–3524`).**
  - До відповіді: дві кнопки — «Не знаю» (`answerUnknown` → `-20%`, перевертає картку, чекає «Далі») і «Знаю» (`answerKnown` → `+20%`, наступна).
  - Після «Не знаю»: одна кнопка «Далі» (`moveNext`).
  (Тобто фактично 2 пари станів кнопок, а не статичні 4.)
- **Прогрес.** Зверху «X / N · правильно K» + смужка (`App.kt:3436–3459`).
- **Done.** `PracticeDoneState` (`App.kt:4005`): кільце відсотка, «Раунд завершено», «Правильних: K із N», кнопки «Ще раунд» (`onRestart` — новий seed) і «Обрати теми» (`onChooseTopics` → назад у фазу 1).
- **Побічний ефект.** Кожна відповідь → `onAnswerWord` → `AdjustWordKnowledge` + `syncVocabularyNow()` (`App.kt:728–737`).

---

## 9. Profile — «Профіль»

`ProfileScreen` — `App.kt:4087`. Маршрут `VocabeeRoute.Settings`; таб `AppTab.Settings`.

- **Призначення.** Стан акаунта, вхід Google, статистика, мови за замовчуванням, налаштування, вихід.

### Стан акаунта (картка зверху)
- `[ЗАРАЗ]` **Анонім** → `ProfileSignInCard` (`App.kt:4294`): «Локальний профіль», «Увійди, щоб синхронізувати словники», кнопка «Продовжити з Google». Loading-підпис «Підключаю Google...» коли `isGoogleAuthLoading`. Notice-рядок (`authNotice`) з помилкою, якщо є.
- `[ЗАРАЗ]` **Авторизований** → `ProfileIdentityCard` (`App.kt:4378`): аватар (градієнт + ініціали — **захардкоджено «НК»**, `App.kt:4398`), displayName, email, іконка Edit (без дії).
- **Вхід Google.** `onGoogleSignInClick` → `startGoogleSignIn()` (`App.kt:493`): реальний потік Google + перевірка конфлікту даних (можливе відкриття `SyncConflict`).

### Статистика
- `[ЗАРАЗ]` Три плитки `ProfileStat` (`App.kt:4129–4155`): «7 днів поспіль» (**захардкоджено**), «{totalWords} слів збережено» (реальне), «12 тренувань» (**захардкоджено**). Стрік і тренування поки фейкові.

### Мови за замовчуванням (D6)
- Дві кнопки-рядки «Я розмовляю» / «Я вивчаю» з прапором і назвою (`App.kt:4158–4173`). Тап → `LanguageForProfile(Speaking/Learning)`.
- Підпис: «Нові словники створюються з цією парою мов автоматично.»
- `[НОВЕ]` (D6) Зміна пари впливає **лише на майбутні** словники; існуючі фіксують пару на момент створення; при створенні пару можна оверрайднути.

### Налаштування (тогли)
- «Сповіщення» (`onNotificationsChanged` → `SetNotificationsEnabled` + `pushProfileSettings`, `App.kt:752–755`).
- «Темна тема» (`onDarkThemeChanged` → `SetDarkThemeEnabled` + push, `App.kt:756–759`). Тема застосовується через `VocabeeTheme` (`App.kt:1304`).

### Мертві рядки (D — поза скоупом)
- `[ЗАРАЗ]` «Запросити друзів» — `onClick = {}` (`App.kt:4240`).
- `[ЗАРАЗ]` «Допомога та підтримка» — `onClick = {}` (`App.kt:4254`).
  Обидва візуально активні, але **нічого не роблять**.

### Вихід
- `[ЗАРАЗ]` Кнопка «Вийти» лише для авторизованого (`App.kt:4258`). Тап → чистить токени та `store.signOutKeepLastUserState()` (`App.kt:760–764`).
- Низ: «Vocabee · v1.0.0».

---

## 10. MissingTopic (фолбек)

`MissingTopicScreen` — `App.kt:4651`. Показується, коли `TopicDetail` відкрито з id, якого немає у `state.topics` (`App.kt:669`). Кнопка «назад», «Словник не знайдено», підказка повернутись.

---

# Bottom sheets (шторки)

Усі шторки будуються через `PrototypeBottomSheet` (`PrototypeBottomSheets.kt:60`): `ModalBottomSheet` зі `skipPartiallyExpanded = true`, drag-handle, заголовок + кнопка закриття, опційний `foot`. Стан керується `var sheet: PrototypeSheet?` у `MainApp` (`App.kt:364`), типи — `PrototypeSheet` (`App.kt:262`). Вихідна шторка має окремий прапор `exitSheetVisible` (`App.kt:365`).

## CreateDictionary
`CreateDictionarySheet` — `PrototypeBottomSheets.kt:134`. Відкривається з FAB/empty Home (через гілку `onCreateClick`, `App.kt:790`).
- **Елементи.** Поле «Назва теми» (autofocus, ліміт 28 символів, `PrototypeBottomSheets.kt:165–196`); палітра кольорів `SwatchPalette` (вибір `coverIndex`); інфо-стрічка пари мов `LanguageInfoStrip` (read-only, з профілю: `learningLanguage→userLanguage`, `App.kt:791–792`); кнопка «Створити» (disabled поки назва порожня).
- **Ліміт.** Якщо `existingDictionariesCount >= 5` → `LimitNote` «Ти створив(ла) максимум 5 словників…» (`PrototypeBottomSheets.kt:146,310`). `[ЗАРАЗ]` Тут локальний ліміт 5 з рекламним натяком; основний гейт ліміту/монеток виконує `onCreateClick` ще до відкриття шторки.
- **Дія.** «Створити» → `CreateTopic(title, coverIndex)` + `syncVocabularyNow()` + закриття + **авто-перехід** у новий `TopicDetail` (`App.kt:795–802`).
- `[НОВЕ]` (D7) Додати **пікер іконок** теми (серіал/книга/подорожі/їжа/робота/школа/спорт/музика/природа/техніка/шопінг/діти/здоров'я/загальна) поряд із кольорами.
- `[НОВЕ]` (D6) Дозволити **оверрайд пари мов** для цього конкретного словника (зараз стрічка лише read-only).

## DeleteDictionary
`DeleteDictionaryConfirmationSheet` — `App.kt:999`. Відкривається тапом на «Видалити» у свайпі картки (`App.kt:805`).
- **Елементи.** «Видалити словник?», «Словник «X» і N слів буде видалено», червоне «Цю дію не можна скасувати», кнопки «Скасувати» / «Видалити» (червона).
- **Дія.** «Видалити» → `RemoveTopic(id)` + sync + закриття; якщо стоїмо в деталях цього словника — поп бекстеку (`App.kt:811–818`).
- `[НОВЕ]` (D3) За видалення **платного** словника монетки **не повертаються**; має показуватись **Undo-снекбар (~5–10с)** для слів і словників; сервер — soft-delete, локально — pending-delete. Поточний текст «не можна скасувати» суперечить D3 і має бути замінений на Undo-флоу.

## LanguageForDictionary (заглушка)
`LanguageSheet` через гілку `PrototypeSheet.LanguageForDictionary` — `App.kt:826`. Відкривається з мовного індикатора в деталях.
- **Елементи.** `LanguageSheet` (`PrototypeBottomSheets.kt:337`): «Мова словника», підзаголовок «Лише для цього словника…», список мов із виключенням target-мови.
- `[ЗАРАЗ]` **Заглушка:** `onPick` нічого не змінює — лише `sheet = null` (`App.kt:835–839`); вибір мови ігнорується.
- `[НОВЕ]` (D6) Має реально оверрайдити пару мов для цього словника.

## LanguageForProfile
`LanguageSheet` через гілку `PrototypeSheet.LanguageForProfile` — `App.kt:843`. Відкривається з рядків мов у Профілі.
- **Елементи.** Заголовок «Я розмовляю» / «Я вивчаю» залежно від `target`; список мов із виключенням другої мови пари.
- **Дія.** `onPick(code)` → `SelectSpeakingLanguage` / `SelectLearningLanguage` + `pushProfileSettings()` + закриття (`App.kt:853–862`). Працює (на відміну від LanguageForDictionary).

## NeedBees (BeeRewardSheet)
`BeeRewardSheet` — `App.kt:1067`. Відкривається коли бракує монеток (`NeedBees(DictionaryCreate | TranslationSearch)`).
- **Елементи.** Банер `BeeWalletBanner` з балансом; текст залежить від причини: для створення «Перші 2 словники безкоштовні. Новий коштує 10 монеток», для пошуку «1 пошук слова або фрази коштує 1 монетку»; кнопки «Пізніше» / «Відео за +10».
- **Дія.** «Відео за +10» → `onWatchAd` → `showRewardedAdForBees()` (`App.kt:560`). Якщо не авторизований — замість реклами відкриває `AuthRequired(WordLimit)` (`App.kt:562–565`). Loading-стан «Готую рекламу...».
- `[НОВЕ]` (D1) Нарахування реклами стає **верифікованим/ідемпотентним** на сервері (SSV / nonce), не «голий POST». (D4) Базові +10 лишаються; промо — бонус зверху.

## AuthRequired
`AuthRequiredSheet` — `App.kt:1144`. Відкривається коли анонім уперся в ліміт (`AuthRequired(DictionaryLimit | WordLimit)`).
- **Елементи.** Банер `AnonymousFreeLimitBanner` (critical); текст залежно від причини: ліміт словників (2) або ліміт слів (50); кнопки «Пізніше» / «Увійти Google».
- **Дія.** «Увійти Google» → `onSignIn` → `startGoogleSignIn()` (`App.kt:881`). Loading «Входимо...».
- `[НОВЕ]` (D2) Реєстрація — момент апгрейду до монеток/реклами/промо.

## SyncConflict
`SyncConflictSheet` — `App.kt:1219`. Відкривається коли при вході є **і** локальні анонімні дані, **і** дані на сервері (`startGoogleSignIn`, `App.kt:521–529`).
- **Елементи.** Пояснення, чому не можна авто-мержити; рядок «Локально: X слів · На бекенді: Y слів»; три дії: «Взяти стан з бекенда», «Залити локальний стан», «Увійти іншим email».
- **Дії (`App.kt:894–922`).**
  - `onUseServer` → застосувати серверний снапшот, відкинути локальне.
  - `onUseLocal` → залити локальне (`replaceServerState = true`).
  - `onOtherEmail` → `clearAuthenticatedSessionForAnotherEmail()`.
  - `onDismiss` — **порожній** (`App.kt:893`): шторку не можна просто змахнути, бо `onDismissRequest = {}` — треба зробити вибір.
- `[ЗАРАЗ]` Реалізовано **3 варіанти** (взяти серверне / залити локальне / інший email).
- `[НОВЕ]` (D9) **Справжній мерж** із підрахунком **вартості в монетках** за зайві словники понад ліміт і чесним повідомленням суми списання. Повний набір варіантів: (1) погодитись на списання → мерж; (2) затерти серверні локальними; (3) відкинути локальне; (4) скасувати вхід; (запасний) інший email. Якщо монеток на мерж бракує — лишаються тільки не-мерж-варіанти. МАЙБУТНЄ: окрема функція «мерж акаунтів» (TBD).

## Exit
`ExitConfirmationSheet` — `App.kt:941`. Відкривається системним Back на корені бекстеку.
- **Елементи.** «Закрити Vocabee?», «Прогрес збережено локально…», кнопки «Залишитися» / «Закрити».
- **Дія.** «Закрити» → `onExitApp()` (`App.kt:931–933`).

---

# Навігація

Файл маршрутів: `navigation/VocabeeNavigation.kt`. Хост: `MainApp` (`App.kt:342`).

- **Single-activity, NavDisplay backstack.** `[ЗАРАЗ]` Один `NavDisplay` (Navigation3) з `rememberNavBackStack(...)` стартує з `DictionaryHome` (`App.kt:356–358`, `632`). Маршрути — `sealed interface VocabeeRoute` (`VocabeeNavigation.kt:11`): `DictionaryHome`, `TopicDetail(topicId)`, `Practice`, `Settings`. `entryProvider` мапить кожен маршрут на екран (`App.kt:638`).
- **3 таби.** `enum AppTab { Dictionary→DictionaryHome, Practice→Practice, Settings→Settings }` (`VocabeeNavigation.kt:36`). Нижній бар `VocabeeBottomBar` (`App.kt:3237`): «Словники» (Book), «Тренування» (Dumbbell), «Профіль» (User). Активний таб визначає `selectedTabFor(currentRoute)` (`VocabeeNavigation.kt:42`) — будь-що крім Practice/Settings вважається Dictionary (тобто TopicDetail підсвічує таб «Словники»).
- **Перемикання таба** → `openRoot(route)`: `backStack.clear()` + `add(route)` (`App.kt:372–375`), тобто кожен таб — це скидання стеку на корінь (не збереження окремих стеків на таб).
- **Коли ховається bottom bar.** `showBottomBar = AppTab.entries.any { it.route == currentRoute }` (`App.kt:362`). Тобто бар видно лише на трьох кореневих маршрутах; на `TopicDetail` (та будь-якому не-табовому маршруті) бар **прихований** (`App.kt:619–625`).
- **Каскад BackHandler.** Глобальний `BackHandler` у `MainApp` (`App.kt:610–616`) за пріоритетом:
  1. відкрита шторка (`sheet != null`) → закрити шторку;
  2. стек глибше за корінь (`backStack.size > 1`) → поп один запис;
  3. на корені → показати `ExitConfirmationSheet`.
  - `[ЗАРАЗ]` Зауваження з коментаря коду: `ModalBottomSheet` ставить **власний** внутрішній `BackHandler`, який перехоплює Back, коли шторка видима; гілка `sheet != null` у глобальному — як «safety net».
  - На екрані `TopicDetail` є **додатковий локальний** `BackHandler` (`App.kt:2197`), що перехоплює Back, коли відкрита панель пошуку/йде слухання/інпут у фокусі → `cancelSearch()` (закриває панель, а не екран).
  - `NavDisplay.onBack` теж поппить стек, якщо `size > 1` (`App.kt:635–637`).

---

## Зведення [НОВЕ] правок по екранах (швидке посилання на D1–D9)

| Екран/шторка | Рішення | Суть зміни |
|---|---|---|
| Auth (онбординг) | D5 | Зробити справжній Google-вхід + розумна розвилка (зараз бутафорський) |
| LanguageSelect | D5 | Пропуск, якщо є серверні дані |
| Home банер | D2, D4 | Анонім без монеток; банер промо config-driven |
| Detail (STT) | D8 | Напрямок зберігати по словнику |
| Detail (економіка) | D1 | Серверно-авторитетне списання, без подвійного |
| CreateDictionary | D6, D7 | Оверрайд пари мов + пікер іконок |
| DeleteDictionary | D3 | Без повернення монеток + Undo-снекбар, soft-delete |
| LanguageForDictionary | D6 | Реалізувати оверрайд (зараз заглушка) |
| NeedBees | D1, D4 | Верифікована/ідемпотентна реклама; промо зверху |
| SyncConflict | D9 | Справжній мерж із вартістю в монетках |
