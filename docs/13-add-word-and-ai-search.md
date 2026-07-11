# 13 — Додавання слова + AI-пошук / переклад

Сигнатурний флов Vocabee: морф-оверлей «Додати слово» → дебаунсений текстовий/голосовий ввід → серверний `/search`-пайплайн (детект мови → кеш lexicon → провайдери перекладу → AI-збагачення) → список результатів із кнопкою `+`/`✓`.

Суміжні документи (не дублюються тут): онбординг — `02`, дані/кеш — `03`, економіка/монетки — `04`, sync+мерж — `06`, мови/мовлення/теми (деталі STT) — `08`, крайові випадки — `10`, motion-бриф (морф-анімація) — `12`.

Легенда: **[ЗАРАЗ]** — як у коді сьогодні; **[НОВЕ]** — затверджена зміна (D1–D10); **[МАЙБУТНЄ]** — відкладено.

---

## 1. Вхідна точка: морф-оверлей із пігулки

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

---

## 4. Стани оверлея: Idle / Recording / Loading / Error / Results

Перемикач у `AddWordOverlay.kt:300-332` (`when`-ланцюг, порядок важливий):

| Стан | Умова входу | Що показано | Код |
|---|---|---|---|
| **Idle (Mic)** | `cleanedQuery.isBlank()` і не слухаємо | `MicStage`: «Продиктуй слово / або почни вводити…» + кнопка мікрофона | `AddWordOverlay.kt:301`, `:495-551` |
| **Recording** | `isListening == true` | `MicStage` зі станом «Слухаю…», помаранчева хвиля `VoiceWaveform` (28 барів), «торкнись, щоб зупинити» | `AddWordOverlay.kt:301`, `:521-526`, `:611-653` |
| **Loading** | `searchState.isLoading` | `AddWordLoadingState`: спінер + «Шукаю переклад…» | `AddWordOverlay.kt:308`, `:655-675` |
| **Error** | `searchState.errorMessage != null` | `AddWordErrorState`: «Не вдалось отримати переклад» + текст помилки (з `VocabeeApiException`) | `AddWordOverlay.kt:309-312`, `:677-707` |
| **Results** | інакше | `AddWordResultsList` (порожній → «Нічого не знайдено для «{q}»»; інакше — список) | `AddWordResultsList`, `:709-789` |

> `AddWordMode { Idle, Recording, Results }` (`:82`) — декларований enum; фактично станами керують `isListening` / `searchState.isLoading` / `searchState.errorMessage` напряму. `AddWordSearchState` (`:126-133`) — носій loading/results/error + `tier`/`maxResults`.

Recording-слот хвилі має фіксовану висоту (80 dp + 30 dp spacer), порожній в Idle — щоб мікрофон не «стрибав» між станами (`:519-527`).

---

## 5. Список результатів

Кожен рядок — `AddWordResultRow` (`AddWordOverlay.kt:798-921`), джерело — `TranslationOption` (мапиться з `SearchVariant` у `RemoteLexiconSearchUseCase.kt:82-115`).

| Елемент | Джерело | Код |
|---|---|---|
| **Вихідне слово** (learning-side, канонічне) | `option.learningWord` — **не сире введення** (тож «circumstanc» → «circumstance») | `AddWordOverlay.kt:835-848`, коментар `:836-837` |
| **IPA** | `option.ipa` (праворуч від слова, якщо є) | `AddWordOverlay.kt:849-858` |
| **Переклад** (known-side) | `option.value` (== `knownWord`) | `AddWordOverlay.kt:866-877` |
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

Видалення з рядка `+`/`✓` — **без повернення монеток** + Undo-снекбар (D3), деталі — `07-deletion.md`.

---

## 7. Ціна 1 монетка за пошук — ДЕ enforced

Константа: `TRANSLATION_SEARCH_BEE_COST = 1` (gateway `wallet.constants.ts:5`) == `TranslationSearchBeeCost` (моб).

### [ЗАРАЗ] — подвійне списання (клієнт + сервер)

| Сторона | Що робить | Код |
|---|---|---|
| **Сервер** (авторитетний) | `LexiconController.search` для **залогінених** викликає `walletService.spendBees(user.id, 1)` ДО пошуку, повертає `meta.beeBalance` | `lexicon.controller.ts:42-49`, `:56` |
| **Клієнт** (локальний) | `App.kt` має `onSpendSearchBee = { store.spendTranslationBee() }`; `spendTranslationBee` для auth викликає `spendBees(1)` локально | `App.kt:686`, `VocabeeStore.kt:241-244`, `:362-370` |
| Звірка | `searchRemotely` бере серверний `result.beeBalance` → `onBeeBalanceChanged` → `VocabeeEvent.SetBeeBalance` → клієнт **приймає серверне значення** як істину | `App.kt:175-177`, `:701-703`, `VocabeeStore.kt:372-376` |

> Ризик: клієнт може списати локально **і** сервер списує своє — без звірки це −2 за один пошук. Зараз серверний `beeBalance` затирає локальний баланс, тож кінцеве значення = серверне, але порядок подій крихкий.

### [НОВЕ] D1 — економіка СЕРВЕР-АВТОРИТЕТНА

- Списання рахує **лише сервер** (`spendBees` у контролері), клієнт — **оптимістичний показ + звірка** з `meta.beeBalance`.
- Прибрати локальне `spendBees` як джерело істини; `spendTranslationBee`/`onSpendSearchBee` стають оптимістичним preview, що завжди підтверджується серверним балансом.
- Усуває подвійне списання. Економіка лишається тільки для **authenticated** (D2: анонім без монеток).

### Гостьовий режим (анонім, D2)

- Сервер для аноніма **не списує** (`if (user)` — `lexicon.controller.ts:43`), `beeBalance=null`.
- Клієнтський `spendTranslationBee` для не-auth повертає `canSearchTranslation()` без списання (`VocabeeStore.kt:242`).
- Замість монеток — ліміт 50 слів (див. §9).

---

## 8. Гейти: `canSearchTranslation` / `canAddWordToDictionary`

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

> [НОВЕ] D1: фінальне рішення «достатньо монеток / списання» — за сервером (`spendBees` кине, якщо балансу нема). Клієнтські гейти лишаються для миттєвого UX (показати шит, не слати запит), але **не** є джерелом істини.

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

## 10. Серверний `/search`-пайплайн

`GET /v1/search?q=&speak=&learn=` (`KtorVocabeeApi.kt:28-48` → `LexiconController.search` → `LexiconService.search`).

DTO запиту `SearchQueryDto` (`search.dto.ts`): `q` (1–200, trim), `speak`/`learn` (ISO-639-1 з `SUPPORTED_LANGUAGE_CODES`). Токен — опціональний bearer; без нього tier `anonymous` (`OptionalJwtAccessGuard`).

### Кроки пайплайну (`lexicon.service.ts:132-284`)

| # | Крок | Деталі | Код |
|---|---|---|---|
| 0 | **Нормалізація** | `trim` → `normalize()` = NFKC + trim + lowercase; `isPhrase = слів > 1` | `lexicon.service.ts:137-138`, `:860-862` |
| 1 | **Детект мови (speak/learn)** | `LanguageDetector.detectBetween`: (a) скрипт-евристика Кирилиця vs Латиниця, (b) `franc-min` якщо ≥4 символи, (c) fallback → `learnLang`. `otherLang` = протилежна detected | `lang-detect.ts:30-55`, `lexicon.service.ts:140-147` |
| 2 | **Префікс-кеш lexicon** | `findPrefixMatches(detectedLang, normalized, otherLang, maxResults)` — `LIKE 'q%'`, exact-first, тоді primary, тоді коротші слова. Дає живі підказки «cir → circle/circus/circumstance» | `lexicon.repository.ts:308-358`, `lexicon.service.ts:151-161` |
| 3 | **Freshness / top-up** | Якщо є exact-cached і провайдер `isAvailable`: перевірити, чи є рядок із поточного tier (`acceptedTierNames`). Нема → `needsTopUp` (авто-рефетч при DeepL Free→Pro, зміні AI-моделі) | `lexicon.service.ts:171-191` |
| 4 | **Word-validator (квота-гейт)** | `isPlausibleProviderInput`: для слова — `isPlausibleWord`, для фрази — `isPlausiblePhrase`. Не пройшло → `providerReason='not_a_word'`, провайдер НЕ викликається (бережемо квоту) | `lexicon.service.ts:199-202`, `:286-294`, word-validator §12 |
| 5 | **Провайдер перекладу** | Якщо `!exactCached ∨ needsTopUp` і слово плаузибельне → `translator.translate({text, detectedLang→otherLang})`. Композит-ланцюг (§11) | `lexicon.service.ts:203-208` |
| 6 | **Echo-гард** | Кандидати = провайдер-результати, де `normalize(text) != normalizedQuery`. Усі збіглися з вводом → `echo` (НЕ персистимо). `null` → `no_provider_data` | `lexicon.service.ts:209-242` |
| 7 | **Upsert lexicon + translations cache** | `persistAllVariants`: upsert source-word, `addTranslation` (+ зворотний mirror target→source), `confirmTier` для вже наявних. Кеш зберігає лише підтверджені переклади | `lexicon.service.ts:440-575` |
| 8 | **Збагачення (IPA/audio/examples/senses)** | `enrichLearningEntry` (тільки не-фраза): dictionary-ланцюг (FreeDictionary → OpenAI) дає IPA, audio, senses+приклади, синоніми/антоніми, форми; усе ідемпотентно персиститься; heal-on-read для старого IPA | `lexicon.service.ts:476-477`, `:583-727` |
| 9 | **Композиція відповіді** | Провайдер-хіти спершу, тоді не-дубль префікс-підказки, до `maxResults`; дедуп по `normalize(knownWord)` | `lexicon.service.ts:245-264` |

### `providerReason` (meta — чому викликали/не викликали провайдер)

| Значення | Зміст | Код |
|---|---|---|
| `exact_cached` | вже є свіжий кеш — не дзвонили | `lexicon.service.ts:197-198` |
| `not_a_word` | не пройшов spell-чек — пропуск заради квоти | `:199-201`, `:241` |
| `echo` | провайдер віддав ввід дослівно — трактуємо як «нема перекладу», не зберігаємо | `:233-234` |
| `no_provider_data` | реальне слово, але ланцюг нічого не дав | `:235-239` |
| `translated` | провайдер дав реальний переклад → персист | `:214-216` |

### Відповідь `SearchResponseDto` → клієнт

`SearchResponse` (моб, `SearchResponse.kt`) дзеркалить `SearchResponseDto`. Ключове: `results: SearchVariant[]` (`knownWord`, `learningWord`, `ipa`, `audioUrl`, `partOfSpeech`, `examples`, `senses`, `synonyms`, `antonyms`, `forms`, `source`, `origin`, `isPrimary`, `cached`, `match`), `tier`, `maxResults`, `meta.beeBalance`. `RemoteLexiconSearchUseCase.toOption` мапить це в `TranslationOption` (`RemoteLexiconSearchUseCase.kt:82-115`).

---

## 11. Провайдери та ланцюги (DI: `TRANSLATION_PROVIDER` / `DICTIONARY_PROVIDER`)

### Ланцюг перекладу (`CompositeTranslationProvider` — перший непорожній виграє)

Порядок DI: **Wiktionary → DeepL → MyMemory → (OpenAI як AI-фолбек)**. Кожен результат тегається `providerName` дочірнього → рядок кешу лягає під правильний `provider_tier`.

| Провайдер | tier-name | variants/call | Доступність | Код |
|---|---|---|---|---|
| Wiktionary (мультиваріант, per-sense таблиці) | `wiktionary` | 10 | `!!baseUrl`; знімає наголоси `́`, дедуп | `wiktionary-translation.provider.ts:45-104` |
| DeepL (1 переклад/запит) | `deepl-free`/`deepl-pro` (детект по суфіксу `:fx`) | 1 | `!!apiKey`; `EN-GB`/`PT-PT` overrides | `deepl.provider.ts:31-77` |
| MyMemory (TM-фолбек, без ключа) | `mymemory` | 1 | завжди | `mymemory.provider.ts:25-55` |
| OpenAI translation (AI-фолбек, structured JSON) | `openai-<model>` (напр. `ai-gpt-4o-mini`) | 8 | `!!apiKey`; temp 0.2; до 8 варіантів | `openai-translation.provider.ts:54-161` |
| Mock (тести/офлайн) | `mock` | 1 | завжди | `mock-translation.provider.ts:10-44` |

`CompositeTranslationProvider`: `acceptedTierNames` = union дітей, `variantsPerCall` = max, `isAvailable` = є хоч один usable (`composite-translation.provider.ts:19-58`).

### Ланцюг словника / збагачення (`CompositeDictionaryProvider`)

Порядок: **FreeDictionary → OpenAI dictionary** (перший із даними виграє; кожен ставить свій `origin`).

| Провайдер | origin | Деталі | Код |
|---|---|---|---|
| FreeDictionary (Wiktionary-backed, `?translations=true`) | `freedictionaryapi.com` | IPA (`extractPhonemicIpa`), PoS, до 8 senses, синоніми/антоніми, форми; **form-of-only → `null`** (щоб AI добив) | `free-dictionary.provider.ts:68-242` |
| OpenAI dictionary (генеративний фолбек) | `openai-<model>` | structured JSON: ≤5 senses (≥1 приклад кожен), ≤5 синонімів, ≤4 антонімів, ≤6 форм; підтримує `en/uk/ru/pl/de/es` | `openai-dictionary.provider.ts:116-245` |

`CompositeDictionaryProvider.supports/lookup` — `composite-dictionary.provider.ts:20-49`. Сервісний `cacheLooksRich`/`cachedSensesAreFormOfOnly` змушує рефетч, якщо кеш «form-of-only» або без IPA (`lexicon.service.ts:607-619`, `:870-885`).

> Один HTTP-виклик `?translations=true` годує **і** dictionary-збагачення, **і** Wiktionary-translation (коментар `free-dictionary.provider.ts:85-91`).

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

## 13. Тіри та `maxResults` — ВІДКРИТЕ ПИТАННЯ

Розбіжність між кодом і коментарями — **позначено як відкрите**:

| Джерело | Заявлене | Код |
|---|---|---|
| `TIER_MAX_RESULTS` (істина в коді) | `anonymous=50, registered=50, premium=50` — капи **знято** для всіх | `user-tier.ts:14-18` |
| Swagger-опис контролера | «anonymous tier, max 3 results … registered → 5 … premium → 10» | `lexicon.controller.ts:32-34` |
| Док-стрінг `VocabeeApi.search` (моб) | «Without a token the gateway responds with up to 3 variants» | `VocabeeApi.kt:8-10` |
| Моб footer-caption | per-tier капи знято; «до N варіантів»/«увійди для більше» **прибрано**, лишилась лише AI-атрибуція | `AddWordOverlay.kt:791-796` |

**Стан:** фактично всі tier-и бачать `min(50, variantsPerCall провайдера)` — без штучного гейтингу (`user-tier.ts:5-13`: «нема монетизаційного важеля, поки не вийде premium»). Коментарі контролера й моб-API **застарілі** (3/5/10).

**Припущення (уточнити):** чи лишається `50/50/50` довгостроково, чи `premium` згодом отримає реальний lever (тоді `maxResults` стане tier-залежним і `footerCaptionFor` поверне copy «увійди для більше»)? `tierFromUserRow`/`maxResultsForTier` (`user-tier.ts:25-33`) вже готові під tier-залежність — слот зарезервовано. **Рекомендація:** синхронізувати Swagger-опис і `VocabeeApi.search` KDoc з реальним `TIER_MAX_RESULTS`, або реалізувати капи.

---

## 14. Підсумок флову (E2E)

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
   → word-validator → translate(Wiktionary→DeepL→MyMemory→OpenAI)
   → echo-гард → upsert lexicon + translations + mirror
   → enrich (FreeDictionary→OpenAI: IPA/audio/senses/syn/ant/forms)
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
