# FUNCTIONAL-OVERVIEW — майстер-інвентар функціоналу Vocabee

> **Єдиний верхньорівневий опис «весь функціонал» Vocabee.** Цей документ — карта-зміст усіх можливостей застосунку, згрупованих за доменами. Він **не дублює** деталі — для кожної функції є посилання на профільний документ `00–17`.
>
> Позначки стану в усьому файлі: **[ЗАРАЗ]** — поведінка в поточному коді; **[НОВЕ]** — затверджена зміна (журнал рішень D1–D10); **[МАЙБУТНЄ]** — заплановано пізніше / TBD.
>
> Джерело істини рішень — журнал у [00-overview-and-decisions.md](00-overview-and-decisions.md) (формалізовано **D1–D9**). **D10** (гібрид-тренування) у цьому інвентарі та в `13`/`15`/`17` уживається як робочий номер; канон поведінки — [11-practice-training.md](11-practice-training.md), а окремий запис «D10» у журналі `00` ще треба додати (див. лишок наприкінці). Технічний контракт — [17-api-and-data-reference.md](17-api-and-data-reference.md).

---

## 1. Що таке продукт і сигнатурний цикл

**Vocabee** — застосунок для вивчення іноземних слів (Kotlin Multiplatform / Compose клієнт + NestJS gateway + Postgres). Користувач читає тексти (книги, статті, субтитри, дописи) і «на ходу» зберігає незнайомі слова — вводить текстом або диктує голосом, а застосунок одразу підтягує AI-переклад зі збагаченням (IPA, значення, приклади, синоніми, форми). Зібрані слова впорядковуються у тематичні словники (серіал, подорож, робота) — кожен зі своєю парою мов, кольором та іконкою — після чого тренуються вправами на запам'ятовування. Економіка побудована на внутрішній валюті **монетки (beecoins)**, якими оплачуються платні дії; анонім має безкоштовний пробний режим, а повна економіка вмикається після входу через Google. **Сигнатурний цикл:** `Читаю → зберігаю слово (текст/голос + AI-переклад) → організовую в тематичні словники → тренуюсь`.

---

## 2. Карта станів користувача (анонім / авторизований)

Vocabee розрізняє **два стани**: `anonymous` (без акаунта, лише локально) та `authenticated` (вхід через Google). Реєстрація — **момент апгрейду**: саме після входу вмикаються монетки, реклама, промо й синхронізація. На сервері анонім — це «відсутність JWT», а не рядок у БД (D2; [16](16-auth-and-account-lifecycle.md) §16.1).

| Аспект | `anonymous` (без акаунта) | `authenticated` (Google) | Рішення / док |
|---|---|---|---|
| Основний функціонал | Збереження слів, словники, тренування | Повний доступ | [00](00-overview-and-decisions.md) §2 |
| Ліміт словників | Жорсткий **2** (`FREE_DICTIONARY_LIMIT`) | 2 безкоштовні + по −10 за кожен наступний | D2 / [04](04-coins-economy.md) §3 |
| Ліміт слів | Жорсткий **50 усього** (`AnonymousFreeWordLimit`) | Без жорсткого ліміту слів | D2 |
| Монетки (beecoins) | **Немає** (early-return у `addBees`/`spendBees`) | Є; старт `INITIAL_BEE_BALANCE = 50` | D2 / [04](04-coins-economy.md) |
| Пошук перекладу | Доступний (у межах 50 слів), **без списання** | Платний: −1 монетка (`TRANSLATION_SEARCH_BEE_COST`) | D1, D2 / [13](13-add-word-and-ai-search.md) §7 |
| Реклама (rewarded ad) | **Недоступна** | Доступна: база +10 (`REWARDED_AD_BEE_AMOUNT`) | D2 / [04](04-coins-economy.md) §5 |
| Промо / акції | **Недоступні** (`GET /promos` → `[]`) | Доступні (config-driven, бонус зверху) | D4 / [05](05-promo-api-and-banners.md) |
| Синхронізація | Немає — дані лише локально | Повна (push/pull LWW, мерж при вході) | D9 / [06](06-sync-and-account-merge.md) |
| Зберігання | Локально (Room `user_key = 'local-user'`) | Сервер — джерело істини; локально кеш | [03](03-data-caching.md) §2 |
| Видалення | **Hard-delete** (незворотне) | **Soft-delete** (`PendingDelete`) | D3 / [07](07-deletion.md) §1 |

> **premium** — третій tier у схемі (`UserTier = anonymous \| registered \| premium`), але зараз `premium ≡ registered` (усі ліміти 50, жодного монетизаційного важеля). Слот зарезервовано під майбутнє ([16](16-auth-and-account-lifecycle.md) §16.1, [10](10-edge-cases-and-open-items.md) O1). **[МАЙБУТНЄ].**

---

## 3. Інвентар функціоналу за доменами

Для кожної функції: **Функція | Короткий опис | Стан | Детальний доку**.

### 3.1 Словники (Dictionaries / Topics)

| Функція | Короткий опис | Стан | Док |
|---|---|---|---|
| Перегляд списку словників | Головний екран `DictionariesHome`: сітка 2-в-колонку карток у реверсному порядку (найновіший зверху); метрики «N словників · M слів». | [ЗАРАЗ] | [01](01-screens.md) §5 |
| Порожній стан Home | `EmptyHomeState` з ілюстрацією й кнопкою «Створити словник» (FAB прихований). | [ЗАРАЗ] | [01](01-screens.md) §5 |
| Картка словника | Колір теми, заливка-фон за `knowledgePercent`, watermark стільників, чип «N слів», мітка «оновлено». | [ЗАРАЗ] | [01](01-screens.md) §5 |
| Створення словника (назва + колір) | Шторка `CreateDictionarySheet`: поле назви (ліміт 28), палітра кольорів (`coverIndex`), авто-перехід у новий словник. | [ЗАРАЗ] | [01](01-screens.md) CreateDictionary |
| Пікер іконок теми (14 іконок) | Набір іконок (серіал/книга/подорожі/їжа/робота/школа/спорт/музика/природа/техніка/шопінг/діти/здоров'я/загальна) поряд із кольорами. | [НОВЕ] D7 | [08](08-languages-speech-themes.md) §6.2 |
| Оверрайд пари мов при створенні | `LanguageInfoStrip` стає інтерактивним: пару мов для цього словника можна змінити (зараз read-only). | [НОВЕ] D6 | [08](08-languages-speech-themes.md) §2 |
| Перегляд словника (TopicDetail) | Список згрупованих слів, колапсуючий хедер, інлайн-панель додавання, прогрес. | [ЗАРАЗ] | [01](01-screens.md) §6 |
| Свайп-видалення словника | Свайп вліво відкриває «Видалити» → шторка підтвердження (каскад на слова). | [ЗАРАЗ] | [01](01-screens.md), [12](12-motion-and-interaction-brief.md) §E |
| Undo-снекбар видалення словника | Снекбар «Скасувати» ~6с після видалення; soft-delete на сервері, pending локально. | [НОВЕ] D3 | [07](07-deletion.md) §4 |
| Без повернення монеток за видалення | Видалення платного словника монетки **не повертає** (анти-фарм). | [ЗАРАЗ]/[НОВЕ] D3 | [07](07-deletion.md) §3 |
| Ліміт словників (анонім 2) | Жорсткий гейт `anonymousDictionaryLimitReached()` → `AuthRequired(DictionaryLimit)`. | [ЗАРАЗ] | [04](04-coins-economy.md) §3 |
| Ліміт/вартість словників (auth) | 1–2 безкоштовні; 3-й+ коштує 10 монеток (`canCreateTopic()`). | [ЗАРАЗ] | [04](04-coins-economy.md) §3 |
| Лічильники словників/слів | Хедер і банери показують «N словників · M слів»; для аноніма «X/2 · Y/50». | [ЗАРАЗ] | [01](01-screens.md) §5 |
| Фіксація пари мов на словник | `source_lang`/`target_lang` фіксуються на момент створення; зміна профілю не «їде» за існуючими. | [ЗАРАЗ]/[НОВЕ] D6 | [08](08-languages-speech-themes.md) §2.2 |
| Контекст-меню картки (довге утримання) | Перейменувати / змінити іконку / видалити — другий обережніший шлях. | [МАЙБУТНЄ] | [12](12-motion-and-interaction-brief.md) §E.2 |
| MissingTopic-фолбек | Екран «Словник не знайдено» при відкритті неіснуючого id. | [ЗАРАЗ] | [01](01-screens.md) §10 |

### 3.2 Слова (Words)

| Функція | Короткий опис | Стан | Док |
|---|---|---|---|
| Додавання слова текстом | Поле вводу з дебаунсом (≈700ms інлайн / 1000ms оверлей) → `/search`. | [ЗАРАЗ] | [13](13-add-word-and-ai-search.md) §2 |
| Додавання слова голосом (STT) | Утримання mic → розпізнавання → той самий дебаунс-пайплайн. | [ЗАРАЗ] | [13](13-add-word-and-ai-search.md) §3 |
| Морф-оверлей «Додати слово» | `AddWordOverlay` морфиться з пігулки «+» у повний екран (tween ~420ms). | [ЗАРАЗ] | [01](01-screens.md) §7, [13](13-add-word-and-ai-search.md) §1 |
| Список результатів із `+`/`✓` | Кожен рядок: канонічне слово, IPA, переклад, Sparkle (AI), toggle add/remove. | [ЗАРАЗ] | [13](13-add-word-and-ai-search.md) §5 |
| Виявлення дубліката | `alreadyAdded` (сервер) ∨ локальний `Set` перекладів → миттєвий toggle `+`↔`✓`. | [ЗАРАЗ] | [13](13-add-word-and-ai-search.md) §6 |
| Групування слів | Однакові source-слова схлопуються в одну картку `WordGroupRow`. | [ЗАРАЗ] | [01](01-screens.md) §6 |
| Розгортувані деталі слова | Тап розкриває тип/регістр, значення/розшифровку/дослівний переклад/приклад, далі senses, синоніми, антоніми й форми. | [ЗАРАЗ] | [14](14-word-details-and-audio.md) §3 |
| Типи lexical unit | `Word / Phrase / Expression / Abbreviation`; регістр окремо (`Slang`, `Informal`, `Internet` тощо), тому `LOL` = abbreviation+slang. | [ЗАРАЗ] | [14](14-word-details-and-audio.md) §1.2 |
| Багата модель деталей | `WordDetails`: lexical metadata + senses (definition/PoS/tags/examples/syn/ant), word-level syn/ant/forms/partOfSpeech. | [ЗАРАЗ] | [14](14-word-details-and-audio.md) §1 |
| Фоновий словничок прикладу | Після локального save один безкоштовний batch розкладає exact речення на токени й зберігає їх контекстні переклади в `WordDetails.contextGlossary`; для авторизованого юзера пара `слово+конкретний переклад+напрямок` також пишеться в приватний server glossary, а offline/anonymous снапшот проєктується туди через `applySync`. | [ЗАРАЗ] | [13](13-add-word-and-ai-search.md) §6.1, [17](17-api-and-data-reference.md) §1.5 |
| Снапшот перекладу/IPA/деталей | Збережене слово — заморожений знімок провайдера; зміни lexicon його не оновлюють. | [ЗАРАЗ] | [14](14-word-details-and-audio.md) §5 |
| Озвучення слова (TTS) | Кнопка `Sound` на картці → озвучує source-слово source-мовою. | [ЗАРАЗ] | [14](14-word-details-and-audio.md) §4, [08](08-languages-speech-themes.md) §4 |
| Свайп-видалення слова | Reveal-кнопка «Видалити» (88dp) → видалення одразу (без діалогу). | [ЗАРАЗ] | [12](12-motion-and-interaction-brief.md) §D |
| Full-swipe-to-delete | Далекий свайп одразу комітить видалення (як Gmail). | [НОВЕ] | [12](12-motion-and-interaction-brief.md) §D.2 |
| Undo-снекбар видалення слова | Снекбар «Скасувати» ~6с; для аноніма — відкладене застосування (hard-delete незворотний). | [НОВЕ] D3 | [07](07-deletion.md) §4 |
| Видалення слова по `translation` | Слово ідентифікується за текстом перекладу (UI не тримає id). | [ЗАРАЗ] | [03](03-data-caching.md) §6.5, [07](07-deletion.md) §1 |
| Дублікати перекладів дозволені | Один source може мати кілька різних перекладів; дедуп лише по парі `source+translation`. | [ЗАРАЗ] | [03](03-data-caching.md) §6.5 |
| Порожній словник лишається | Видалення останнього слова не видаляє словник. | [ЗАРАЗ] | [07](07-deletion.md) §1 |
| Озвучення target-слова target-мовою | Можливість озвучити переклад (зараз лише source). | [МАЙБУТНЄ] | [08](08-languages-speech-themes.md) §4.2 |
| Ре-енричмент збережених слів | Підтягнути свіжі senses/forms за `sourceWordId`. | [МАЙБУТНЄ] | [14](14-word-details-and-audio.md) §5.3 |

### 3.3 AI-пошук / переклад (`/search`-пайплайн)

| Функція | Короткий опис | Стан | Док |
|---|---|---|---|
| Серверний `/search` ендпоінт | `GET /v1/search?q=&speak=&learn=`, опційний JWT (анонім або auth). | [ЗАРАЗ] | [17](17-api-and-data-reference.md) §1.5, [13](13-add-word-and-ai-search.md) §10 |
| Нормалізація запиту | NFKC + trim + lowercase; визначення фраза vs слово. | [ЗАРАЗ] | [13](13-add-word-and-ai-search.md) §10 |
| Детект мови вводу | `LanguageDetector.detectBetween`: скрипт-евристика → franc-min → fallback `learnLang`. | [ЗАРАЗ] | [08](08-languages-speech-themes.md) §5, [13](13-add-word-and-ai-search.md) §10 |
| Префікс-кеш lexicon | `LIKE 'q%'` живі підказки (exact-first → primary → коротші). | [ЗАРАЗ] | [13](13-add-word-and-ai-search.md) §10 |
| Word-validator (квота-гейт) | Hunspell перевіряє плаузибельність перед провайдером (бережемо квоту). | [ЗАРАЗ] | [13](13-add-word-and-ai-search.md) §12 |
| Провайдер перекладу | OpenAI structured output: переклади + тип/регістр/розшифровка/значення/дослівний переклад/приклад; лише запитаний напрямок. | [ЗАРАЗ] | [13](13-add-word-and-ai-search.md) §10–11 |
| Echo-гард | Якщо провайдер віддав ввід дослівно → «нема перекладу», не персистимо. | [ЗАРАЗ] | [13](13-add-word-and-ai-search.md) §10 |
| Збагачення (dictionary-ланцюг) | `OpenAI → FreeDictionary`: IPA, audio, senses+приклади, синоніми/антоніми, форми. | [ЗАРАЗ] | [13](13-add-word-and-ai-search.md) §11, [14](14-word-details-and-audio.md) §2 |
| Напрямний кеш без mirror | Upsert source/target lexicon + translation cache лише для `detectedLang → otherLang`; зворотний напрямок генерується окремим запитом. | [ЗАРАЗ] | [13](13-add-word-and-ai-search.md) §10 |
| Остання відновлювана ревізія | Dictionary admin може soft-delete переклад з причиною та відновити той самий рядок; пошук не показує видалені варіанти й дозаповнює тільки відсутній слот. | [ЗАРАЗ] | [13](13-add-word-and-ai-search.md) §10, [17](17-api-and-data-reference.md) §1.3/3.2 |
| `providerReason` (meta) | `exact_cached / not_a_word / echo / no_provider_data / translated`. | [ЗАРАЗ] | [13](13-add-word-and-ai-search.md) §10 |
| AI-атрибуція | Футер «Переклади та приклади згенеровано AI» + Sparkle per-row. | [ЗАРАЗ] | [13](13-add-word-and-ai-search.md) §5 |
| Ціна 1 монетка за пошук (auth) | Сервер списує `TRANSLATION_SEARCH_BEE_COST`, повертає `meta.beeBalance`. | [ЗАРАЗ]/[НОВЕ] D1 | [13](13-add-word-and-ai-search.md) §7 |
| Тіри й `maxResults` | `TIER_MAX_RESULTS` = 50/50/50 (капи знято); backend Swagger синхронізовано з цим контрактом. | [ЗАРАЗ]; майбутня різниця tier — відкрита | [13](13-add-word-and-ai-search.md) §13, [10](10-edge-cases-and-open-items.md) O1/O3 |
| Не брати монетку за 0 результатів | Списувати лише за успішну видачу (зараз списується до пошуку). | [НОВЕ] D1 | [10](10-edge-cases-and-open-items.md) #6 |
| Search по парі словника | Пошук має брати пару зі словника, а не профілю (після D6). | [НОВЕ] D6/D8 | [08](08-languages-speech-themes.md) §3.5 |
| Офлайн/empty/error-стани пошуку | «Немає звʼязку, спробуй ще раз» + «Повторити»; «Переклад не знайдено». | [НОВЕ] | [10](10-edge-cases-and-open-items.md) #1, #6 |

### 3.4 Тренування (Practice)

| Функція | Короткий опис | Стан | Док |
|---|---|---|---|
| Вибір тем (setup-екран) | Мультивибір словників із чекбоксами; єдина колода без перемикача «Класика / Слово в контексті»; «Вибрано: K тем · M слів». | [ЗАРАЗ] | [01](01-screens.md) §8, [11](11-practice-training.md) §1, §7 |
| Колода флеш-карток | До 10 pager-карток з незалежним flip-станом; slide через «Знаю»/«Далі»; sense/usage-контекст; на звороті metadata вислову/абревіатури. | [ЗАРАЗ] | [01](01-screens.md) §8 |
| Клікабельні слова контексту | Кожен збагачений токен речення відкриває popup із локально збереженим контекстним перекладом; без мережі та монеток у тесті. | [ЗАРАЗ] | [11](11-practice-training.md) §1 |
| Цільове слово без підказки | Усі входження цільового слова/словозміни (lemma-aware) жовті, але не відкривають popup; підказки доступні лише для інших токенів. | [ЗАРАЗ] | [11](11-practice-training.md) §1 |
| Закладки слів із речення | Popup додає/знімає сесійну закладку; badge у topbar; фінал дозволяє додати одне/всі слова через picker словника. 1 монетка/нове слово, недостатній баланс → rewarded ad без втрати списку. | [ЗАРАЗ] | [11](11-practice-training.md) §1 |
| Бінарна оцінка «Знаю / Не знаю» | Ручне відкриття перекладу автоматично = «Не знаю»; далі лишається «Далі» (стрілка праворуч); «Знаю» — одразу наступна. | [ЗАРАЗ] | [11](11-practice-training.md) §1, §6 |
| Поточний відбір (найслабші) | shuffle(seed) → sort by knowledge% → take(10). Причина «ті самі 10». | [ЗАРАЗ] | [11](11-practice-training.md) §1 |
| Гібрид пріоритет+Leitner+анти-повтор | Новий алгоритм: пріоритет-бал + Leitner-бокси + зважений рандом + кулдаун + нові. | [НОВЕ] D10 | [11](11-practice-training.md) §2–§4 |
| Сесія 10 (≈7 повтор + ≈3 нові) | `SESSION_SIZE=10`, `NEW_PER_SESSION=3`; адаптивні квоти. | [НОВЕ] D10 | [11](11-practice-training.md) §4.2 |
| Нові поля знань | `timesCorrect/timesWrong/boxLevel/lastReviewedAt/dueAt` (синкаються). | [НОВЕ] D10 | [11](11-practice-training.md) §3, [17](17-api-and-data-reference.md) §3 |
| Leitner-інтервали (бокси 0–5) | 10хв / 1д / 3д / 7д / 16д — дозрівання `dueAt`. | [НОВЕ] D10 | [11](11-practice-training.md) §5 |
| Анти-повтор (кулдаун + рандом) | Нещодавно показані виключаються; зважений рандом → щоразу інший набір. | [НОВЕ] D10 | [11](11-practice-training.md) §8 |
| Перечергування «Не знаю» | Невідоме слово повертається в кінець колоди цієї ж сесії. | [НОВЕ] D10 | [11](11-practice-training.md) §6, [12](12-motion-and-interaction-brief.md) §B |
| Прогрес сесії | «X / N · правильно K» + смужка (ціль — сегментований бар). | [ЗАРАЗ]/[НОВЕ] | [01](01-screens.md) §8, [12](12-motion-and-interaction-brief.md) §C |
| Done-стан раунду | Кільце відсотка, «Правильних K із N», «Ще раунд» / «Обрати теми». | [ЗАРАЗ] | [01](01-screens.md) §8 |
| Тренування безкоштовне | Переклад під час тренування монеток не списує. | [ЗАРАЗ] | [11](11-practice-training.md) §11 |
| Empty-стан тренування | «Немає слів для повторення» коли немає тренувальних тем. | [ЗАРАЗ] | [01](01-screens.md) §8 |
| Режим «іспиту» / монетки за тренування | Окрема кнопка «усі слова поспіль»; XP/монетки за тренування. | [МАЙБУТНЄ] | [11](11-practice-training.md) §11 |

### 3.5 Економіка монеток (beecoins)

| Функція | Короткий опис | Стан | Док |
|---|---|---|---|
| Стартовий баланс +50 | `INITIAL_BEE_BALANCE = 50` при реєстрації. | [ЗАРАЗ] | [04](04-coins-economy.md) §1 |
| Заробіток за рекламу +10 | `REWARDED_AD_BEE_AMOUNT = 10` за перегляд. | [ЗАРАЗ] | [04](04-coins-economy.md) §1 |
| Витрата −10 за словник понад ліміт | `DICTIONARY_CREATION_BEE_COST` за 3-й+ словник. | [ЗАРАЗ] | [04](04-coins-economy.md) §1 |
| Витрата −1 за пошук | `TRANSLATION_SEARCH_BEE_COST` за пошук перекладу. | [ЗАРАЗ] | [04](04-coins-economy.md) §1 |
| Гейти доступу | `canCreateTopic` / `canSearchTranslation` / `canAddWordToDictionary`. | [ЗАРАЗ] | [04](04-coins-economy.md) §3 |
| Анонім без монеток (early-return) | `addBees`/`spendBees` повертаються рано, якщо `!isAuthenticated`. | [ЗАРАЗ] D2 | [04](04-coins-economy.md) §3 |
| Серверна авторитетність балансу | Сервер — джерело істини; клієнт оптимістично + звірка (`SetBeeBalance`). | [НОВЕ] D1 | [04](04-coins-economy.md) §4, [03](03-data-caching.md) §7 |
| Фікс подвійного списання за словник | Прибрати клієнтське списання; списує лише сервер (`create` + `applySync`). | [НОВЕ] D1 | [04](04-coins-economy.md) §4 |
| `applySync` валідує квоти й списує | Enforcement квот словників + списання + відхилення понадлімітного. | [НОВЕ] D1 | [06](06-sync-and-account-merge.md) §3, [04](04-coins-economy.md) §4 |
| Критичний поріг ≤3 | `CriticalBeeThreshold = 3` → банер «майже закінчились монетки». | [ЗАРАЗ] | [04](04-coins-economy.md) §6 |
| Стан «0 монеток» | Блокування платних дій; шторка NeedBees / 402 `not_enough_bees`. | [ЗАРАЗ] | [04](04-coins-economy.md) §6, [10](10-edge-cases-and-open-items.md) #15 |
| Баланс не йде в мінус | `coerceAtLeast(0)` на клієнті; guard `gte` на сервері. | [ЗАРАЗ] | [04](04-coins-economy.md) §6 |
| Бейдж/банер балансу | `BeeBalanceBadge` (Sparkle/стільники); `BeeWalletBanner`. | [ЗАРАЗ] | [01](01-screens.md) §5 |

### 3.6 Промо / винагороди (Promo API)

| Функція | Короткий опис | Стан | Док |
|---|---|---|---|
| Config-driven Promo API | `GET /v1/promos` повертає активні промо з прогресом; клієнт лише рендерить. | [НОВЕ] D4 | [05](05-promo-api-and-banners.md) §1–§2 |
| Промо-банер (стани) | Один банерний слот; прогрес / claimable / completed; `icon/accent/priority`. | [НОВЕ] D4 | [05](05-promo-api-and-banners.md) §4 |
| Промо-ботомшит | Бейдж винагороди + пояснення + візуалізація прогресу + CTA-кнопки з конфіга. | [НОВЕ] D4 | [05](05-promo-api-and-banners.md) §5 |
| Кампанія milestone (10 реклам→+20) | Повторювана; скидається після claim. | [НОВЕ] D4 | [05](05-promo-api-and-banners.md) §3a |
| Кампанія daily_streak (5 днів→+50) | ≥3 реклами/день 5 днів поспіль; стрік скидається при пропуску. | [НОВЕ] D4 | [05](05-promo-api-and-banners.md) §3b |
| Кампанія one_time (реєстрація→+50) | Разовий вітальний бонус; для аноніма — `locked` із `sign_in`. | [НОВЕ] D4 | [05](05-promo-api-and-banners.md) §3c |
| Кампанія leaderboard (топ-10→+50) | Щотижневий рейтинг переглядів реклами; скидання щопонеділка. | [НОВЕ] D4 | [05](05-promo-api-and-banners.md) §3d, §6 |
| Лідерборд топ-10 (екран) | Топ-10 + «моє місце» + таймер до скидання; видача +50. | [НОВЕ] D4 | [05](05-promo-api-and-banners.md) §6 |
| Claim призу (ідемпотентний) | `POST /v1/promos/{id}/claim` — сервер перевіряє прогрес, нараховує `reward.bees`. | [НОВЕ] D4 | [05](05-promo-api-and-banners.md) §7 |
| Rewarded ad (база +10) | `POST /v1/wallet/rewarded-ad` — база +10, не верифікований/не ідемпотентний. | [ЗАРАЗ] | [04](04-coins-economy.md) §5, [17](17-api-and-data-reference.md) §1.4 |
| Верифікована/ідемпотентна реклама | SSV / разовий nonce; просування ad-лічильників у тій самій транзакції. | [НОВЕ] D1/D4 | [04](04-coins-economy.md) §5, [05](05-promo-api-and-banners.md) §7 |
| Серверні лічильники промо | `adsTotal/adsToday/streakDays/adsThisWeek` + стан видачі (нова підсистема). | [НОВЕ] D4 | [05](05-promo-api-and-banners.md) §8 |
| Анонім промо не бачить | `GET /promos` для аноніма → `promos: []`; натомість банер гостьового режиму. | [НОВЕ] D2/D4 | [05](05-promo-api-and-banners.md) §3 |
| «Запросити друзів» / реферал | Персональна лінка й QR; Android Share передає текст+PNG; advertised bonus +50 з `/referral/me`. Фактична атрибуція/credit — майбутнє. | [ЗАРАЗ]/[МАЙБУТНЄ] | [15](15-profile-and-settings.md) §5 |

### 3.7 Авторизація / акаунт

| Функція | Короткий опис | Стан | Док |
|---|---|---|---|
| Анонім = відсутність JWT | Анонім не має серверного рядка; `is_anonymous` спляче поле. | [ЗАРАЗ] D2 | [16](16-auth-and-account-lifecycle.md) §16.1 |
| Google ID-token вхід | `POST /v1/auth/google` (verify через tokeninfo); existing email шукається через `lower(email)`, а linked/existing рядок має бути `active` до link/token issue. | [ЗАРАЗ] | [16](16-auth-and-account-lifecycle.md) §16.2 |
| Email+пароль (register/login) | Існують на бекенді, клієнтом **не** використовуються; login завжди робить один cost-12 bcrypt compare (real/dummy). | [ЗАРАЗ] | [16](16-auth-and-account-lifecycle.md) §16.2, [17](17-api-and-data-reference.md) §1.2 |
| Account-status auth gate | Після valid credentials `active` проходить; missing/`banned`/`deactivated` → generic internal 401. Internal auth messages глобальний фільтр нормалізує у wire `errorType=unauthorized` + «Потрібна повторна авторизація.». | [ЗАРАЗ] | [16](16-auth-and-account-lifecycle.md) §16.1–16.4, [17](17-api-and-data-reference.md) §1.2 |
| Видача/ротація токенів | Access JWT (15m) + refresh JWT (30d, SHA-256 у БД); обидва runtime-валідують UUID `sub` + `kind=user` до DB, rotation на refresh. | [ЗАРАЗ] | [16](16-auth-and-account-lifecycle.md) §16.3 |
| Refresh-сесії | `POST /v1/auth/refresh` — active-check до revoke старого + нової пари; inactive refresh не revoke/rotate. | [ЗАРАЗ] | [16](16-auth-and-account-lifecycle.md) §16.3 |
| Зберігання токенів (клієнт) | `AuthTokenStore` — реактивний `StateFlow` access; refresh у prefs. | [ЗАРАЗ] | [16](16-auth-and-account-lifecycle.md) §16.5 |
| Реальний Google-вхід в онбордингу | Auth-екран стає справжнім входом (зараз бутафорський `onDone`). | [НОВЕ] D5 | [02](02-onboarding-and-launch.md) §1.3, [01](01-screens.md) §3 |
| Вхід із профілю | `startGoogleSignIn()` — реальний потік + перевірка конфлікту. | [ЗАРАЗ] | [15](15-profile-and-settings.md) §2, [16](16-auth-and-account-lifecycle.md) §16.6 |
| Анонім → зареєстрований | `moveUserVocabulary` (UPDATE user_key) + перезапис балансу/мов/теми. | [ЗАРАЗ] | [03](03-data-caching.md) §5, [16](16-auth-and-account-lifecycle.md) §16.6 |
| Вихід (sign-out) | Чистить токени + `signOutKeepLastUserState` (дані лишаються). | [ЗАРАЗ] | [15](15-profile-and-settings.md) §6, [16](16-auth-and-account-lifecycle.md) §16.8 |
| Колізія email (Google на password-юзера) | Реюз існуючого рядка; deployed migration має unique `lower(email)`, але Drizzle metadata описує plain index (schema drift). | [ЗАРАЗ] / ризик | [10](10-edge-cases-and-open-items.md) #12 |
| Клієнт кличе `/auth/logout` | Revoke refresh на сервері при виході (зараз не кличе). | [НОВЕ] / уточнити | [16](16-auth-and-account-lifecycle.md) §16.8 O4 |
| Refresh-failed → sign-out | При стійкому 401 на refresh переводити в анонім. | [НОВЕ] / уточнити | [02](02-onboarding-and-launch.md) §5.2 |
| Картка ідентичності (профіль) | Аватар (ініціали «НК» захардкоджено), displayName, email, Edit (мертва). | [ЗАРАЗ] | [15](15-profile-and-settings.md) §2.2 |
| Редагування `displayName` / фото | Кнопка Edit активна; ініціали з імені; фото Google. | [МАЙБУТНЄ] | [15](15-profile-and-settings.md) §2.2 |
| Мерж акаунтів (різні Google) | Перенесення словників між акаунтами за монетки. | [МАЙБУТНЄ] D9 | [06](06-sync-and-account-merge.md) §5 |
| Гостьовий серверний рядок | Справжній анонімний акаунт (`is_anonymous`, `RegisteredUserGuard`). | [МАЙБУТНЄ] | [16](16-auth-and-account-lifecycle.md) §16.10 |

### 3.8 Синхронізація / офлайн

| Функція | Короткий опис | Стан | Док |
|---|---|---|---|
| Sync-статуси | `PendingCreate / PendingUpdate / Synced / PendingDelete` на кожному рядку. | [ЗАРАЗ] | [06](06-sync-and-account-merge.md) §1.1, [03](03-data-caching.md) §1.3 |
| PUSH — `applySync` | Повний снапшот → `POST /v1/topics/sync/apply` → серверний снапшот назад. | [ЗАРАЗ] | [06](06-sync-and-account-merge.md) §1.2, [17](17-api-and-data-reference.md) §4.2 |
| PULL — `syncTopics(since)` | Delta-pull змінених після курсора; `deletedTopicIds/deletedWordIds`. | [ЗАРАЗ] | [06](06-sync-and-account-merge.md) §1.3, [17](17-api-and-data-reference.md) §4.1 |
| LWW-розв'язання конфліктів | Last-Write-Wins по рядку за таймстемпами. | [ЗАРАЗ] | [06](06-sync-and-account-merge.md) §1.6 |
| Курсор синку | `lastSyncAt` (серверний час) + `localRevisionEpochMillis` (dirty-лічильник). | [ЗАРАЗ] | [06](06-sync-and-account-merge.md) §1.4 |
| Startup-sync (холодний старт) | refresh → `currentUser` → дельта/повний синк (фоном у Main). | [ЗАРАЗ] | [02](02-onboarding-and-launch.md) §1.5, [16](16-auth-and-account-lifecycle.md) §16.7 |
| Офлайн-стійкість | Локальні зміни оптимістично в Room; тихий ретрай при появі мережі. | [ЗАРАЗ] | [10](10-edge-cases-and-open-items.md) #3 |
| Обхід лімітів (поточна діра) | `applySync` не валідує квоти → 20 словників заливаються безкоштовно. | [ЗАРАЗ] вразливість | [06](06-sync-and-account-merge.md) §2 |
| Серверне enforcement у `applySync` | Квоти словників, валідація id/власності, довірені таймстемпи, дедуп, rate-limit, ліміт payload. | [НОВЕ] D1 | [06](06-sync-and-account-merge.md) §3 |
| Розширений контракт відповіді sync | `newBeeBalance` + `applied`/`rejected` (з кодами помилок). | [НОВЕ] D1 | [06](06-sync-and-account-merge.md) §3.2 |
| Sync-конфлікт при вході (мерж) | Локальні + серверні дані → справжній мерж із вартістю в монетках. | [НОВЕ] D9 | [06](06-sync-and-account-merge.md) §4 |
| Шторка SyncConflict (3 варіанти зараз) | Взяти серверне / залити локальне / інший email. | [ЗАРАЗ] | [01](01-screens.md) SyncConflict |
| Шторка мержу (5 варіантів) | Погодитись (мерж) / затерти серверне / відкинути локальне / скасувати / інший email. | [НОВЕ] D9 | [06](06-sync-and-account-merge.md) §4.3 |
| Обчислення вартості мержу | `billable × DICTIONARY_CREATION_BEE_COST`; нестача монеток → лише не-мерж. | [НОВЕ] D9 | [06](06-sync-and-account-merge.md) §4.4 |
| `replaceSyncSnapshot` (затерти) | Повне перезатирання локальної бази серверним снапшотом. | [ЗАРАЗ] | [03](03-data-caching.md) §4 |

### 3.9 Мови / мовлення (Languages / Speech)

| Функція | Короткий опис | Стан | Док |
|---|---|---|---|
| Профільна пара мов | «Я розмовляю» / «Я вивчаю» — дефолт для нових словників. | [ЗАРАЗ] | [08](08-languages-speech-themes.md) §1 |
| Інваріант «дві мови різні» | Авто-корекція в селекторах + виключення в UI. | [ЗАРАЗ] | [08](08-languages-speech-themes.md) §1.2 |
| Мова словника vs профілю | Словник фіксує пару; профіль — лише дефолт нових (оверрайд при створенні). | [ЗАРАЗ]/[НОВЕ] D6 | [08](08-languages-speech-themes.md) §2 |
| Напрямок перекладу (перемикач) | Пілюля з двома прапорами в хедері словника інвертує напрямок. | [ЗАРАЗ] | [01](01-screens.md) §6, [08](08-languages-speech-themes.md) §3 |
| Напрямок → STT | Виставляє пріоритетну мову розпізнавача (primary) + alternative. | [ЗАРАЗ] | [08](08-languages-speech-themes.md) §3.1–§3.2 |
| Напрямок STT по словнику | Зберігати напрямок per-topic (зараз — лише в пам'яті екрана). | [НОВЕ] D8 | [08](08-languages-speech-themes.md) §3.4, [10](10-edge-cases-and-open-items.md) O8 |
| STT (розпізнавання) | `AndroidSpeechInputController` з primary + alternative тегами. | [ЗАРАЗ] | [08](08-languages-speech-themes.md) §3 |
| Android 14+ детект/switch мови | Авто-визначення мови вводу серед пари (тільки SDK≥34). | [ЗАРАЗ] | [08](08-languages-speech-themes.md) §3.3 |
| TTS (озвучення) | `AndroidSpeechOutputController`; асинхронна ініціалізація (перший тап може мовчати). | [ЗАРАЗ] | [08](08-languages-speech-themes.md) §4, [14](14-word-details-and-audio.md) §4 |
| Бекенд `lang-detect` | Визначення мови вводу між двома мовами (скрипт + franc-min). | [ЗАРАЗ] | [08](08-languages-speech-themes.md) §5 |
| Підтримувані мови (13) | `uk, en, de, es, fr, pl, it, pt, tr, he, ar, lt, cs` — `GET /v1/languages`. | [ЗАРАЗ] | [08](08-languages-speech-themes.md) §0, [17](17-api-and-data-reference.md) §1.7 |
| `speechTag` (BCP-47) | `uk-UA`, `en-US` для STT/TTS; ≠ ISO-код. | [ЗАРАЗ] | [08](08-languages-speech-themes.md) §7 |

### 3.10 Персоналізація (теми / налаштування)

| Функція | Короткий опис | Стан | Док |
|---|---|---|---|
| Кольори-теми словників (8) | `PrototypeTopicThemes` — 8 кольорів через `coverIndex`. | [ЗАРАЗ] | [08](08-languages-speech-themes.md) §6.1 |
| Набір іконок тем (14) | D7-іконки (серіал/книга/.../загальна), окреме поле `iconKey`. | [НОВЕ] D7 | [08](08-languages-speech-themes.md) §6.2 |
| Темна тема (тогл) | `SetDarkThemeEnabled` → M3-тема; локальне + серверне збереження. | [ЗАРАЗ] | [15](15-profile-and-settings.md) §4.2 |
| Дарк прототипних кольорів | Токенізувати `PrototypeColor` під тему (зараз частково). | [МАЙБУТНЄ] | [15](15-profile-and-settings.md) §4.2 |
| Сповіщення (тогл) | `SetNotificationsEnabled` зберігає булеве; реальної доставки немає. | [ЗАРАЗ] / [МАЙБУТНЄ] | [15](15-profile-and-settings.md) §4.1 |
| Мовні дефолти (профіль) | Рядки «Я розмовляю» / «Я вивчаю» → `LanguageForProfile`. | [ЗАРАЗ] | [15](15-profile-and-settings.md) §3 |
| Статистика профілю | Стрік «7» і тренування «12» захардкоджені; «слів збережено» реальне. | [ЗАРАЗ] / [МАЙБУТНЄ] | [15](15-profile-and-settings.md) §7 |
| Версія застосунку | «Vocabee · v1.0.0» захардкоджений літерал. | [ЗАРАЗ] | [15](15-profile-and-settings.md) §8 |
| «Допомога та підтримка» | Мертвий рядок (`onClick={}`); може стати входом у довідку. | [ЗАРАЗ] мертве | [15](15-profile-and-settings.md) §5 |

### 3.11 Онбординг / запуск

| Функція | Короткий опис | Стан | Док |
|---|---|---|---|
| Splash | Брендований інтро на кожному запуску (delay 1.9с / тап). | [ЗАРАЗ] | [01](01-screens.md) §1, [02](02-onboarding-and-launch.md) §1 |
| Onboarding (3 слайди) | Read / Organize / Practice; «Далі»/«Почати»/«Пропустити». | [ЗАРАЗ] | [01](01-screens.md) §2 |
| Auth-екран (бутафорський) | Усі кнопки → `onDone` без реальної автентифікації. | [ЗАРАЗ] | [02](02-onboarding-and-launch.md) §1.3 |
| LanguageSelect | Вибір пари мов «розмовляю»→«вивчаю»; ставить `hasCompletedOnboarding`. | [ЗАРАЗ] | [01](01-screens.md) §4 |
| Розвилка входу D5 | (а) є дані→пропуск мов; (б) новий→вибір мов→push; (в) скіп→локально. | [НОВЕ] D5 | [02](02-onboarding-and-launch.md) §2 |
| Предикат «сервер має дані» | `syncTopics(null)` непорожній або заданий мовний профіль. | [ЗАРАЗ]/[НОВЕ] D5 | [02](02-onboarding-and-launch.md) §3 |
| Прапор `hasCompletedOnboarding` | Device-global; не скидається на sign-out. | [ЗАРАЗ] | [02](02-onboarding-and-launch.md) §1.4, §5.3 |
| Повторні запуски | `Splash → Main` + фоновий startup-sync. | [ЗАРАЗ] | [02](02-onboarding-and-launch.md) §4 |
| Офлайн на старті | Без блокувального екрана; синк повториться пізніше. | [ЗАРАЗ] | [02](02-onboarding-and-launch.md) §5.1 |
| Помилка входу в онбордингу | Лишаємось на Auth + нотіс; «Пропустити» доступне. | [НОВЕ] D5 | [02](02-onboarding-and-launch.md) §5.1 |

### 3.12 Довідка / інлайн «?»

| Функція | Короткий опис | Стан | Док |
|---|---|---|---|
| Патерн «?» → ботомшит | Іконка «?» відкриває `PrototypeBottomSheet` із коротким поясненням. | [НОВЕ] | [09](09-inline-help-tooltips.md) §2 |
| Інвентар тултіпів (20) | Готовий копірайт: монетки, ліміти, реклама, акції, стрік, milestone, leaderboard, гостьовий режим, реєстрація, напрямок STT, мова словника, тема, синхронізація, мерж, видалення, тренування, темна тема, сповіщення, низький баланс. | [НОВЕ] | [09](09-inline-help-tooltips.md) §3 |
| Опційні CTA в шторці довідки | «Дивитись рекламу» / «Увійти через Google» де доречно. | [НОВЕ] | [09](09-inline-help-tooltips.md) §2 |
| Точка входу «всі довідки» | Рядок «Допомога та підтримка» → список довідок. | [МАЙБУТНЄ] / уточнити | [09](09-inline-help-tooltips.md) §5 |

### 3.13 Бекенд / інфраструктура

| Функція | Короткий опис | Стан | Док |
|---|---|---|---|
| Auth-ендпоінти | `/v1/auth/{register,login,google,refresh,logout,me}`. | [ЗАРАЗ] | [17](17-api-and-data-reference.md) §1.2 |
| Users-ендпоінти | `GET/PATCH /v1/me` — профіль, мови, тогли. | [ЗАРАЗ] | [17](17-api-and-data-reference.md) §1.3, [15](15-profile-and-settings.md) §3 |
| Wallet-ендпоінти | `GET /v1/wallet`, `POST /v1/wallet/rewarded-ad`. | [ЗАРАЗ] | [17](17-api-and-data-reference.md) §1.4 |
| Search/Lexicon-ендпоінт | `GET /v1/search` (OptionalJwt); OpenAPI показує anonymous **або** bearer. | [ЗАРАЗ] | [17](17-api-and-data-reference.md) §1.5 |
| Context glossary batch | `POST /v1/search/context-glossary` на client (OptionalJwt, no wallet) і dictionary gateway (`X-API-Key`); один provider/quota call на речення. | [ЗАРАЗ] | [17](17-api-and-data-reference.md) §1.5 |
| Topics-ендпоінти | `/v1/topics`, `/sync`, `/sync/apply`, `:id/words` (CRUD + sync). | [ЗАРАЗ] | [17](17-api-and-data-reference.md) §1.6 |
| Languages-ендпоінт | `GET /v1/languages` (13 мов). | [ЗАРАЗ] | [17](17-api-and-data-reference.md) §1.7 |
| Client admin: фільтр словників | `sourceLang?`/`targetLang?` фільтрують словники користувача на сервері; web default `en → uk`, кожен dropdown має `Усі`. | [ЗАРАЗ] | [17](17-api-and-data-reference.md) §1.3.1 |
| Dictionary admin: фільтри перекладів | Мови — dropdown-и з canonical language API; Origin/model — динамічні dropdown-и з БД у «Розширених фільтрах». | [ЗАРАЗ] | [17](17-api-and-data-reference.md) §1.3.2 |
| Promo-ендпоінти | `/v1/promos`, `/{id}/claim`, `/leaderboard/ad-watchers`. | [НОВЕ] D4 | [17](17-api-and-data-reference.md) §1.8, [05](05-promo-api-and-banners.md) §7 |
| Guard-стратегія | `JwtAccessGuard` вимагає runtime-valid user claims + active user; `OptionalJwtAccessGuard` дає anonymous 200 лише без `Authorization`, а supplied invalid/expired/malformed/inactive → 401; `RegisteredUserGuard` мертвий. | [ЗАРАЗ] | [16](16-auth-and-account-lifecycle.md) §16.4 |
| Room-схема (клієнт) | `vocabulary_topics` + `vocabulary_words`, version 4, конвертери. | [ЗАРАЗ] | [03](03-data-caching.md) §1, [17](17-api-and-data-reference.md) §3.1 |
| Per-user партиціювання (Room) | `user_key` (`local-user`/userId) ізолює акаунти в одній базі. | [ЗАРАЗ] | [03](03-data-caching.md) §2 |
| Postgres-схема | `users` з `account_status`/moderation audit fields, `refresh_tokens/oauth_accounts`, admin audit, topics/topic_words/languages/lexicon*. | [ЗАРАЗ] | [17](17-api-and-data-reference.md) §3.2 |
| Партиціювання lexicon (LIST за мовою) | `lexicon_*` партиціоновані по `word_lang` (7 партицій). | [ЗАРАЗ] | [17](17-api-and-data-reference.md) §3.2, [14](14-word-details-and-audio.md) §2.1 |
| Soft-delete + ретеншн | `deleted_at` всюди; GC/hard-delete немає (GDPR-питання). | [ЗАРАЗ] / [МАЙБУТНЄ] | [07](07-deletion.md) §7, [10](10-edge-cases-and-open-items.md) O4 |
| Провайдери перекладу/словника | Wiktionary/DeepL/MyMemory/OpenAI; FreeDictionary/OpenAI. | [ЗАРАЗ] | [13](13-add-word-and-ai-search.md) §11 |
| Міграції БД | `0001–0015` наявні; `0015_user_context_glossary.sql` додає приватний contextual glossary; майбутня `0016_training_fields.sql` для D10 ще [НОВЕ] і не створена. | [ЗАРАЗ]/[НОВЕ] | [17](17-api-and-data-reference.md) §3.3 |
| Cron лідерборда | Щопонеділка 00:00 UTC: нарахування топ-10 + обнулення тижневого агрегату. | [НОВЕ] D4 | [05](05-promo-api-and-banners.md) §6 |
| Bump Room до v5 (поля D10) | Нові колонки тренування + Room-міграція. | [НОВЕ] D10 | [17](17-api-and-data-reference.md) §3.1 |

### 3.14 Motion / взаємодії

| Функція | Короткий опис | Стан | Док |
|---|---|---|---|
| Глобальні токени руху | Easing/тривалості (S/M/L)/haptics/палітра — застосувати всюди. | [НОВЕ] ціль | [12](12-motion-and-interaction-brief.md) §0 |
| Переворот картки тренування (flip) | rotationY 0→180°; ціль — spring із підйомом, idle-погойдування, кросфейд. | [ЗАРАЗ]/[НОВЕ] | [12](12-motion-and-interaction-brief.md) §A |
| Флов «Не знаю» (анімація) | Червоний пульс → авто-flip → морф 2 кнопок в «Далі» → перечергування → вихід. | [ЗАРАЗ]/[НОВЕ] | [12](12-motion-and-interaction-brief.md) §B |
| Прогрес сесії (анімація) | Сегментований бар (10), count-up, колірна мапа, перехід у Done. | [НОВЕ] ціль | [12](12-motion-and-interaction-brief.md) §C |
| Свайп-видалення слова (motion) | Прогресивний реквіл + full-swipe + колапс висоти + reflow + Undo 6с. | [ЗАРАЗ]/[НОВЕ] | [12](12-motion-and-interaction-brief.md) §D |
| Свайп-видалення словника (motion) | Реквіл + шторка + згортання картки + reflow сітки + Undo + примітка про монетки. | [ЗАРАЗ]/[НОВЕ] | [12](12-motion-and-interaction-brief.md) §E |
| Морф додавання слова | Морф-оверлей із пігулки (tween ~420ms) — узгодити зі spring-набором. | [ЗАРАЗ]/[НОВЕ] | [12](12-motion-and-interaction-brief.md) §F |
| Перемикач напрямку (swap-флип) | Горизонтальний swap двох мовних пігулок + мікро-haptic. | [НОВЕ] ціль | [12](12-motion-and-interaction-brief.md) §F |
| Claimable-промо (пульсація) | Пульсація бейджа винагороди + світіння рамки. | [НОВЕ] ціль | [12](12-motion-and-interaction-brief.md) §F |
| Каскад BackHandler / навігація | 3 таби, single-activity NavDisplay; пріоритет Back: шторка→стек→Exit. | [ЗАРАЗ] | [01](01-screens.md) Навігація |

---

## 4. Нове / заплановане (backlog)

### 4.1 [НОВЕ] — затверджені зміни (D1–D10)

| # | Зміна | Рішення | Док |
|---|---|---|---|
| N1 | Серверно-авторитетна економіка; фікс подвійного списання | D1 | [04](04-coins-economy.md) |
| N2 | `applySync` валідує квоти, списує, повертає `newBeeBalance`/`applied`/`rejected` | D1 | [06](06-sync-and-account-merge.md) §3 |
| N3 | Серверне списання −1 на `/search`; не брати монетку за 0 результатів | D1 | [13](13-add-word-and-ai-search.md) §7, [10](10-edge-cases-and-open-items.md) #6 |
| N4 | Верифікована/ідемпотентна реклама (SSV/nonce) + просування ad-лічильників | D1/D4 | [04](04-coins-economy.md) §5 |
| N5 | Config-driven Promo API + 4 кампанії + банери/ботомшит + лідерборд | D4 | [05](05-promo-api-and-banners.md) |
| N6 | Реальний Google-вхід в онбордингу + розвилка (а)/(б)/(в) | D5 | [02](02-onboarding-and-launch.md) §2 |
| N7 | Оверрайд пари мов словника при створенні; search по парі словника | D6 | [08](08-languages-speech-themes.md) §2 |
| N8 | Пікер іконок тем (14) + поле `iconKey` + синк | D7 | [08](08-languages-speech-themes.md) §6.2 |
| N9 | Напрямок STT зберігається по словнику (Room-поле + sync) | D8 | [08](08-languages-speech-themes.md) §3.4 |
| N10 | Справжній мерж при вході (5 варіантів + вартість у монетках) | D9 | [06](06-sync-and-account-merge.md) §4 |
| N11 | Гібрид-тренування (пріоритет+Leitner+анти-повтор) + поля знань + майбутня `0016_training_fields.sql` **[НОВЕ, ще не створена]** + Room v5 | D10 | [11](11-practice-training.md) |
| N12 | Undo-снекбар (~6с) для слів і словників; без повернення монеток | D3 | [07](07-deletion.md) §4, [12](12-motion-and-interaction-brief.md) §D/E |
| N13 | Інлайн-довідка «?» (20 тултіпів) | — | [09](09-inline-help-tooltips.md) |
| N14 | Токени руху + апгрейд анімацій (flip/свайп/прогрес/морф) | — | [12](12-motion-and-interaction-brief.md) |
| N15 | Клієнт кличе `/auth/logout`; refresh-failed → sign-out | — / уточнити | [16](16-auth-and-account-lifecycle.md) §16.8 |
| N16 | Унікальний email-індекс; явне лінкування Google за email | — / уточнити | [10](10-edge-cases-and-open-items.md) #12 |
| N17 | Error/empty-стани (офлайн-пошук, «не знайдено», TTS/STT/мікрофон) | — | [10](10-edge-cases-and-open-items.md) §1 |
| N18 | Прибрати `sourceLang/targetLang` з `UpdateTopicDto` (D6 «існуючі незмінні») | D6 | [17](17-api-and-data-reference.md) §1.6 |

### 4.2 [МАЙБУТНЄ] — відкладено / TBD

| # | Елемент | Док |
|---|---|---|
| F1 | Мерж акаунтів (різні Google) — перенесення словників за монетки | [06](06-sync-and-account-merge.md) §5 |
| F2 | premium-tier із реальним монетизаційним важелем | [16](16-auth-and-account-lifecycle.md) O1, [10](10-edge-cases-and-open-items.md) O1 |
| F3 | Гостьовий серверний рядок (`is_anonymous`, `RegisteredUserGuard`) | [16](16-auth-and-account-lifecycle.md) O2/O3 |
| F4 | Озвучення target-слова target-мовою (TTS) | [08](08-languages-speech-themes.md) §4.2 |
| F5 | Ре-енричмент збережених слів за `sourceWordId` | [14](14-word-details-and-audio.md) §5.3 |
| F6 | Реальна доставка нагадувань про тренування (push/local notif) | [15](15-profile-and-settings.md) §4.1 |
| F7 | Реальний стрік / лічильник тренувань у профілі | [15](15-profile-and-settings.md) §7, [11](11-practice-training.md) §11 |
| F8 | Редагування `displayName` + аватар Google | [15](15-profile-and-settings.md) §2.2 |
| F9 | «Запросити друзів» / реферал-флоу | [15](15-profile-and-settings.md) §5 |
| F10 | GDPR-GC soft-видалених рядків (ретеншн-джоб) | [07](07-deletion.md) §7, [10](10-edge-cases-and-open-items.md) O4 |
| F11 | Reuse-detection для refresh (каскадне відкликання при крадіжці) | [16](16-auth-and-account-lifecycle.md) O5 |
| F12 | Режим «іспиту» в тренуванні; монетки/XP за тренування | [11](11-practice-training.md) §11 |
| F13 | Ліміт слів на словник (продуктивність) | [10](10-edge-cases-and-open-items.md) O5 |
| F14 | Контекст-меню картки словника (довге утримання) | [12](12-motion-and-interaction-brief.md) §E.2 |
| F15 | Токенізація `PrototypeColor` під темну тему | [15](15-profile-and-settings.md) §4.2 |

---

## 5. Матриця «Функція → стан користувача»

Колонки: **Анонім** / **Авторизований** / **Premium**. (`✓` доступно · `✗` недоступно · `≈` як у авторизованого · `[Н]` нове/заплановане.)

| Можливість | Анонім | Авторизований | Premium |
|---|---|---|---|
| Створення словників | ✓ (макс. 2) | ✓ (2 безкошт. + платні) | ≈ |
| Додавання слів | ✓ (макс. 50 усього) | ✓ (без жорсткого ліміту) | ≈ |
| AI-пошук / переклад | ✓ (без списання) | ✓ (−1 монетка) | ≈ |
| `maxResults` пошуку | 50 (капи знято) | 50 | 50 (`[Н]` майб. lever) |
| Озвучення (TTS) | ✓ | ✓ | ≈ |
| Голосовий ввід (STT) | ✓ | ✓ | ≈ |
| Тренування | ✓ | ✓ | ≈ |
| Монетки (beecoins) | ✗ | ✓ (старт 50) | ≈ |
| Реклама (rewarded ad) | ✗ | ✓ (+10) | ≈ |
| Промо / акції | ✗ | ✓ | ≈ |
| Лідерборд | ✗ | ✓ `[Н]` D4 | ≈ |
| Синхронізація з сервером | ✗ | ✓ | ≈ |
| Бекап даних | ✗ | ✓ | ≈ |
| Видалення | hard (незворотне) | soft + Undo `[Н]` D3 | ≈ |
| Мерж при вході | n/a (джерело мержу) | ✓ `[Н]` D9 | ≈ |
| Профіль (Google identity) | ✗ (локальний профіль) | ✓ | ≈ |
| Темна тема / сповіщення | ✓ (локально) | ✓ (синк) | ≈ |
| Мова словника (оверрайд) | ✓ `[Н]` D6 | ✓ `[Н]` D6 | ≈ |

> **Примітка про premium:** у поточному коді **premium ≡ registered** — окремих можливостей немає (усі ліміти 50, жодного важеля). Колонка лишена як зарезервований слот під майбутню монетизацію (F2). До її реалізації авторизований і premium функціонально ідентичні.

---

## 6. Карта документів (швидке посилання)

| # | Док | Домени |
|---|---|---|
| 00 | [Огляд і рішення](00-overview-and-decisions.md) | Призначення, стани, глосарій, константи, D1–D9 |
| 01 | [Екрани](01-screens.md) | Усі екрани + bottom sheets + навігація |
| 02 | [Онбординг і запуск](02-onboarding-and-launch.md) | Launch tree, розвилка D5, повторні запуски |
| 03 | [Дані й кешування](03-data-caching.md) | Room, per-user партиціювання, кеш |
| 04 | [Економіка монеток](04-coins-economy.md) | Beecoins, серверна авторитетність, фікс списання |
| 05 | [Promo API і банери](05-promo-api-and-banners.md) | Config-driven промо, 4 кампанії, лідерборд |
| 06 | [Синхронізація і мерж](06-sync-and-account-merge.md) | Sync, enforcement, мерж при вході |
| 07 | [Видалення](07-deletion.md) | Soft/hard, Undo, без повернення монеток |
| 08 | [Мови, мовлення, теми](08-languages-speech-themes.md) | Профіль vs словник, STT/TTS, іконки тем |
| 09 | [Інлайн-довідка «?»](09-inline-help-tooltips.md) | Інвентар тултіпів |
| 10 | [Крайові випадки](10-edge-cases-and-open-items.md) | Error-стани, відкриті питання, backlog |
| 11 | [Тренування](11-practice-training.md) | Гібрид-відбір, Leitner, анти-повтор |
| 12 | [Motion-бриф](12-motion-and-interaction-brief.md) | Анімації, свайпи, Undo, морф |
| 13 | [Додавання слова + AI-пошук](13-add-word-and-ai-search.md) | Пайплайн `/search`, провайдери, гейти |
| 14 | [Деталі слова та озвучення](14-word-details-and-audio.md) | Senses/форми/синоніми, TTS, снапшот |
| 15 | [Профіль і налаштування](15-profile-and-settings.md) | Акаунт, мови, тогли, статистика, вихід |
| 16 | [Авторизація і життєвий цикл](16-auth-and-account-lifecycle.md) | JWT, Google, refresh, анонім→акаунт |
| 17 | [Довідник API та даних](17-api-and-data-reference.md) | Ендпоінти, моделі, схема, sync-контракт |
