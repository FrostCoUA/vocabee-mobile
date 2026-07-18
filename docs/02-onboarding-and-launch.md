# 02 — Онбординг і запуск (Launch decision tree)

Документ описує дерево рішень при старті застосунку Vocabee та потік онбордингу.
Для кожної поведінки явно позначено **[ЗАРАЗ]** (як працює в поточному коді) і
**[НОВЕ]** (затверджена зміна за рішенням D5, джерело — власник продукту).

Ключові файли:
- `App.kt` — `VocabeeApp` / `MainApp`, гейт першого запуску, `runStartupSync`, `startGoogleSignIn`.
- `OnboardingFlow.kt` — `SplashScreen`, `OnboardingScreen`, `AuthScreen`, `LanguageSelectScreen`.
- `PreferencesManager.kt` — персистентні прапори (`hasCompletedOnboarding`, токени, мови, курсори синку).
- `VocabeeStore.kt` — `applyAuthenticatedAccount`, `signOutKeepLastUserState`.

---

## 1. [ЗАРАЗ] Поточний жорсткий потік запуску

### 1.1 Стани потоку

`AppFlow` (`App.kt:155`) — лінійний скінченний автомат екранів першого запуску:

```kotlin
internal enum class AppFlow { Splash, Onboarding, Auth, LanguageSelect, Main }
```

Гейт першого запуску в `VocabeeApp` (`App.kt:297–339`):

```kotlin
val skipFirstLaunchFlow = remember { preferencesManager.hasCompletedOnboarding }
var flow by remember { mutableStateOf(AppFlow.Splash) }
```

### 1.2 Жорстка послідовність екранів

| Крок | Екран | Файл/рядок | Перехід далі | Як завершується |
|------|-------|------------|--------------|-----------------|
| 1 | `Splash` | `OnboardingFlow.kt:100` (`SplashScreen`) | `Onboarding` (або `Main`, якщо `skipFirstLaunchFlow`) | `delay(1900)` або тап по екрану → `onDone` (`App.kt:304–306`) |
| 2 | `Onboarding` | `OnboardingFlow.kt:182` (`OnboardingScreen`) | `Auth` | 3 слайди (Read/Organize/Practice); «Далі»/«Почати» або «Пропустити» → `onDone` (`App.kt:308`) |
| 3 | `Auth` | `OnboardingFlow.kt` (`AuthScreen`) | `LanguageSelect` | **бутафорський** — Google і гостьовий режим кличуть `onDone` (`App.kt:309`) |
| 4 | `LanguageSelect` | `OnboardingFlow.kt` (`LanguageSelectScreen`) | `Main` | «Готово» → зберігає мови в `store` + ставить `hasCompletedOnboarding = true` (`App.kt:310–326`) |
| 5 | `Main` | `App.kt:327` (`MainApp`) | — | основний застосунок |

### 1.3 [ЗАРАЗ] Auth-екран — спрощений і поки бутафорський

`AuthScreen` (`OnboardingFlow.kt`) за редизайном спрощено до двох дій: «Продовжити з Google»
і «Продовжити без акаунта» (+ «Пропустити» вгорі). Поля email/пароль, кнопку Facebook і перемикач
«вхід ↔ реєстрація» **прибрано**. Під кнопками — картка про ліміти гостя («до 2 словників і 50 слів,
дані лише на цьому пристрої») і футер про Умови користування; дії притиснуті до низу екрана.

**Жодна кнопка поки нічого не автентифікує** — усі викликають той самий `onDone` і просто перемикають
`flow` на `LanguageSelect`. Це чистий UI без виклику `GoogleAuthController` чи `VocabeeApi`.

Справжній Google-вхід наразі живе **тільки** всередині `Main` — на екрані профілю
(`startGoogleSignIn`, `App.kt:493`), а не в онбордингу.

### 1.4 [ЗАРАЗ] Прапор `hasCompletedOnboarding`

- Тип/сховище: `PreferencesManager.hasCompletedOnboarding: Boolean` (`PreferencesManager.kt:22`),
  на Android — SharedPreferences; у превʼю/тестах — `InMemoryPreferencesManager` (завжди `false`).
- Виставляється в `true` **лише** наприкінці `LanguageSelect.onDone` (`App.kt:323`).
- Читається один раз на композицію `VocabeeApp` у `skipFirstLaunchFlow` (`App.kt:297`).
- **Device-global**: привʼязаний до встановлення застосунку, а не до акаунта. Sign-out його **не скидає** (див. розділ 5.3).

### 1.5 [ЗАРАЗ] `runStartupSync` при холодному старті

Викликається з `MainApp` через `LaunchedEffect(Unit) { runStartupSync() }` (`App.kt:556–558`),
тобто **тільки коли вже у `Main`**. Сама функція — `App.kt:451–491`:

1. Early-return, якщо немає `api` або немає жодного токена (`accessToken == null && refreshToken == null`) (`App.kt:452–453`).
2. **Refresh токена**: якщо є `refreshToken` → `backend.refreshSession(refreshToken)` (`App.kt:456–459`).
3. **`currentUser()`** → `VocabeeEvent.ApplyAuthenticatedAccount(...)` (`App.kt:460–472`): підтягує
   userId, displayName/email, `speakLang`/`learnLang`, `notificationsEnabled`, `darkThemeEnabled`, `beeBalance`.
4. **Розвилка дельта-синку** (`App.kt:473–486`):
   - якщо є незалиті локальні зміни (`localRevisionEpochMillis > 0L`) → `syncVocabularyNow()` (push+merge через `applySync`);
   - інакше дельта-синк: `backend.syncTopics(since = lastSyncAt)`; якщо є зміни → `applyServerSnapshot(...)`.
5. Будь-яка помилка ковтається (`catch { }`) → застосунок лишається локальним/офлайн (`App.kt:487–489`).

> **Важливо [ЗАРАЗ]:** `runStartupSync` запускається **після** того, як користувач опинився в `Main`.
> При першому запуску це відбувається лише наприкінці жорсткого ланцюга (крок 4→5). Мови з сервера
> можуть «перезаписати» локально обрані, бо `ApplyAuthenticatedAccount` виставляє `userLanguage`/`learningLanguage`
> із серверного профілю (`VocabeeStore.kt:419–447`) — але вже **після** показу екрана вибору мов, що й створює
> зайвий екран для юзера, який насправді має акаунт. Саме це усуває D5.

---

## 2. [НОВЕ] Повне дерево рішень за D5

Мета D5: Auth-екран в онбордингу стає **справжнім** Google-входом, а після входу — розумна
розвилка: якщо на сервері вже є дані юзера, **пропускаємо** вибір мов і тягнемо все з сервера.

### 2.1 Псевдо-ASCII-схема

```
[Splash]  (програється завжди, ~1.9с)
   │
   ├─ hasCompletedOnboarding == true ───────────────► [Main] + фоновий runStartupSync   (розділ 4)
   │
   └─ hasCompletedOnboarding == false
            │
            ▼
       [Onboarding]  (3 слайди: Read / Organize / Practice)
            │  «Далі»×N → «Почати»  (або «Пропустити»)
            ▼
       [Auth]  ◄── [НОВЕ] СПРАВЖНІЙ Google-вхід (а не бутафорський onDone)
            │
            ├──────────────── (A) «Увійти через Google» ───────────────┐
            │                                                            ▼
            │                                              signInWithGoogle() → currentUser()
            │                                              + перевірка «сервер має дані?» (розділ 3)
            │                                                            │
            │                          ┌─────────────────────────────────┴───────────────────────┐
            │                          ▼                                                           ▼
            │              (а) СЕРВЕР МАЄ ДАНІ юзера                          (б) НОВИЙ акаунт (даних нема)
            │              ── ПРОПУСКАЄМО вибір мов ──                        ── показуємо вибір мов ──
            │                          │                                                           │
            │              тягнемо мови/баланс/тему/                          юзер обирає пару мов
            │              словники з сервера                                            │
            │              (applyAuthenticatedAccount                          push на сервер
            │               + applyServerSnapshot)                            (updateCurrentUser +
            │                          │                                       initial applySync)
            │                          ▼                                                  ▼
            │              hasCompletedOnboarding = true                    hasCompletedOnboarding = true
            │                          │                                                  │
            │                          └──────────────────────┬───────────────────────────┘
            │                                                  ▼
            │                                               [Main]
            │
            └──────────────── (B) «Пропустити» вхід (анонім) ──────────────┐
                                                                            ▼
                                                              [LanguageSelect]  (вибір мов)
                                                                            │
                                                              зберігаємо ЛОКАЛЬНО (анонім),
                                                              hasCompletedOnboarding = true
                                                                            ▼
                                                                         [Main]
```

### 2.2 Таблиця «умова → наступний екран → джерело мов»

| # | Умова (після Splash/Onboarding) | Наступний екран | Джерело мов (speak/learn) | Баланс/тема | Прапор |
|---|--------------------------------|-----------------|---------------------------|-------------|--------|
| (а) | Увійшов через Google **І** на сервері Є дані юзера | **Пропустити LanguageSelect** → `Main` | Сервер (`UserResponse.speakLang/learnLang` через `ApplyAuthenticatedAccount`) | Сервер (`beeBalance`, `darkThemeEnabled`) | `hasCompletedOnboarding = true` |
| (б) | Увійшов через Google, але акаунт **НОВИЙ** (даних нема) | `LanguageSelect` → push → `Main` | Локальний вибір юзера → **push** на сервер (`updateCurrentUser` + initial `applySync`) | Стартові константи (`INITIAL_BEE_BALANCE = 50`), серверні після push | `hasCompletedOnboarding = true` |
| (в) | **Скіпнув** вхід (лишається анонімом) | `LanguageSelect` → `Main` | Локальний вибір, зберігається **локально** (`PreferencesManager` + `store`); сервер не торкаємо | Аноніму баланс не показуємо (D2: anonymous без монеток) | `hasCompletedOnboarding = true` |
| — | Вхід завершився **помилкою** (cancel/мережа/not configured) | Лишаємось на `Auth`, показуємо нотіс, юзер може повторити або «Пропустити» | — | — | не змінюється |

> Реюз існуючого коду: гілки (а)/(б) спираються на вже наявні `signInWithGoogle` (`App.kt:212`),
> `ApplyAuthenticatedAccount` (`App.kt:236`), `applyServerSnapshot`/`syncVocabularyNow`
> (`App.kt:377–403`). Гілка (в) — той самий шлях, що й поточний `LanguageSelect.onDone` (`App.kt:314–325`).
> Нове по суті — **перенести точку входу до онбордингу** і додати розвилку «сервер має дані?».

### 2.3 Конфлікт даних при вході в онбордингу

Якщо на момент входу вже є **локальні анонімні** словники (юзер устиг щось зібрати до входу) **І**
сервер теж має дані — спрацьовує **sync-конфлікт D9** (мерж/затерти/відкинути/інший email). Деталі
конфлікту — у файлі `06-sync-and-account-merge.md` та в коді `PendingSyncConflict` + `SyncConflictSheet` (`App.kt:198`, `516–553`).
У типовому онбординг-сценарії локальних анонімних даних ще нема, тож конфлікт не виникає, і працює
проста гілка (а)/(б).

---

## 3. Як визначається «сервер має дані юзера»

### 3.1 [ЗАРАЗ] Що вже є в коді

У `startGoogleSignIn` (профіль, `App.kt:516–521`) уже є точний предикат «у акаунта є контент»:

```kotlin
val serverSnapshot = api.syncTopics(null)
val serverHasVocabulary = serverSnapshot.topics.isNotEmpty() ||
    serverSnapshot.words.isNotEmpty()
```

Тобто **наявність бодай одного topic або одного word** у повному снапшоті (`syncTopics(null)`)
= «сервер має дані». `SyncResponse` несе `topics`, `words`, `serverTime` (`SyncDtos.kt:25–30`).

### 3.2 [НОВЕ] Розширений предикат для розвилки D5 (а)/(б)

Для розвилки в онбордингу «сервер має дані юзера» = виконано **хоча б одну** з умов:

| Сигнал | Звідки | Значення |
|--------|--------|----------|
| Є словники/слова | `syncTopics(null)`: `topics.isNotEmpty() \|\| words.isNotEmpty()` | контент уже існує → гілка (а) |
| Заданий профіль мов | `UserResponse.speakLang`/`learnLang` непорожні і не дефолтні | юзер раніше проходив вибір мов → можна пропустити |

- **Гілка (а)** (пропустити вибір мов): спрацьовує, якщо є контент **або** на сервері збережена
  валідна мовна пара профілю. У такому разі мови беремо з `UserResponse` (через `ApplyAuthenticatedAccount`),
  а контент — з `applyServerSnapshot(syncTopics(null))`.
- **Гілка (б)** (новий акаунт): немає ні контенту, ні осмисленого мовного профілю → показуємо `LanguageSelect`.

> **Припущення (уточнити):** для гілки (б) бекенд для свіжого акаунта повертає `speakLang`/`learnLang`
> із дефолтами логіну (на вході `loginWithGoogle` передає поточні локальні `speakLang/learnLang`,
> `App.kt:230–234`). Тому ознака «новий акаунт» надійніше базується на **відсутності topics/words**,
> а не лише на мовах. Точний серверний сигнал «акаунт щойно створено» (напр. прапор `isNew` у
> відповіді `loginWithGoogle`) бажано додати на gateway, щоб не покладатись на евристику.

### 3.3 Що відбувається з балансом і темою при вході

Незалежно від гілки, при успішному вході викликається `VocabeeEvent.ApplyAuthenticatedAccount`
(`App.kt:236–247`, обробник — `VocabeeStore.kt:419–447`). Він:

| Поле стану | Джерело | Куди пишеться |
|-----------|---------|---------------|
| `account` | `UserResponse.id/displayName/email` | `VocabeeAccountState.Authenticated` |
| `userLanguage` / `learningLanguage` | `speakLang` / `learnLang` (з фолбеком на поточні, якщо код невідомий) | `state` + `PreferencesManager.userLanguageCode/learningLanguageCode` |
| `notificationsEnabled` | `UserResponse.notificationsEnabled` | `state` |
| `darkThemeEnabled` | `UserResponse.darkThemeEnabled` | `state` + `PreferencesManager.darkThemeEnabled` (керує темою застосунку) |
| `beeBalance` | `UserResponse.beeBalance` (coerce ≥ 0) | `state` + `PreferencesManager.beeBalance` |
| `topics` | `loadUserTopicsUseCase()` | перезавантажуються під нового userId |

Далі для гілки (а) поверх цього лягає **контент** через `applyServerSnapshot(serverSnapshot)`
(`App.kt:377–382` → `replaceCurrentSyncSnapshot` + `markCurrentVocabularySynced(serverTime)`).

> **[НОВЕ] Тема застосовується одразу:** оскільки `darkThemeEnabled` керує `VocabeeTheme`
> (`App.kt:292`), при вході з гілки (а) тема перемикається на серверну ще на переході в `Main`.
> За D2 баланс монеток показуємо лише автентифікованому юзеру; аноніму (гілка в) баланс/реклама/промо
> вимкнені.

---

## 4. Повторні запуски (`hasCompletedOnboarding == true`)

### 4.1 [ЗАРАЗ] Потік

```
[Splash] (програється завжди) ──► [Main] ──► LaunchedEffect → runStartupSync (фоном)
```

- `skipFirstLaunchFlow == true` → `SplashScreen.onDone` ставить `flow = AppFlow.Main` напряму
  (`App.kt:305`), повністю минаючи `Onboarding`/`Auth`/`LanguageSelect`.
- Сплеш **усе одно програється** на кожному запуску як візуальний інтро (`App.kt:296`).
- У `Main` спрацьовує `LaunchedEffect(Unit) { runStartupSync() }` (`App.kt:556–558`):
  refresh токена → `currentUser` → `ApplyAuthenticatedAccount` → дельта-/повний синк (розділ 1.5).

### 4.2 [НОВЕ] Узгодженість із D5

Дерево D5 нічого не змінює для повторних запусків: гілка `hasCompletedOnboarding == true` лишається
`Splash → Main + фоновий startup-sync`. Уся розумна розвилка (а)/(б)/(в) застосовується **тільки**
під час першого проходження онбордингу.

---

## 5. Крайові випадки

### 5.1 Офлайн на старті

| Сценарій | [ЗАРАЗ] поведінка | [НОВЕ] |
|----------|-------------------|--------|
| Повторний запуск, немає мережі | `runStartupSync` кидає виняток на `refreshSession`/`currentUser`, він ковтається (`App.kt:487–489`) → застосунок працює з локальним станом (останній відомий профіль/словники з `PreferencesManager` + Room). | Без змін. Жодного блокувального екрана; синк повториться при наступному успішному старті/дії. |
| Перший запуск, офлайн, юзер тисне Google-вхід | Auth бутафорський — просто йде далі (`onDone`). | Справжній вхід впаде (`GoogleAuthResult.Failure`/`VocabeeApiException`) → показуємо нотіс на `Auth`, лишаємось на екрані; юзер може «Пропустити» і піти анонімом (гілка в). |
| Перший запуск, офлайн, «Пропустити» | Працює — все локально. | Без змін: гілка (в) повністю офлайн-дружня. |

### 5.2 Токен прострочений

- [ЗАРАЗ] `runStartupSync` спершу робить `refreshSession(refreshToken)` (`App.kt:456–459`). Якщо
  **refresh успішний** — далі `currentUser` працює з новим access-токеном.
- Якщо **refresh теж невалідний** (протух/відкликаний) — `refreshSession`/`currentUser` кидають
  виняток, він ковтається → застосунок лишається з локальним станом, але **формально** ще вважає
  себе автентифікованим у UI (account зі стейту/останнього синку).
- **Припущення (уточнити) / [НОВЕ]:** при стійкому 401 на refresh варто переводити в анонімний стан
  (`signOutKeepLastUserState`) і чистити токени (`accessToken/refreshToken = null`), щоб не показувати
  «залогінений» UI без валідної сесії. Зараз явної гілки «refresh failed → sign-out» немає — це треба додати.

### 5.3 Юзер вийшов (sign-out)

Кнопка виходу в профілі (`App.kt:760–764`):

```kotlin
preferencesManager.accessToken = null
preferencesManager.refreshToken = null
store.signOutKeepLastUserState()
```

`signOutKeepLastUserState` (`VocabeeStore.kt:449–455`): чистить `currentUserId`, переводить
`account = Anonymous`, перевантажує `topics`. **Не чіпає** `hasCompletedOnboarding`,
`userLanguageCode`, `learningLanguageCode`, `darkThemeEnabled`, `lastAuthenticatedUserId`.

| Аспект | Поведінка |
|--------|-----------|
| Splash/онбординг після sign-out | **НЕ показуються знову.** `hasCompletedOnboarding` device-global і не скидається → наступний (і поточний) перехід лишається `Splash → Main`. |
| Стан користувача | Стає `Anonymous` (D2: без монеток/реклами/промо), але збережені мови/тема лишаються з останньої сесії. |
| Повторний вхід | Через профіль (`startGoogleSignIn`) — той самий справжній Google-вхід, що й у [НОВЕ] онбордингу, з тією ж розвилкою «сервер має дані?» і можливим sync-конфліктом D9. |

> **Увага (за умовою задачі):** `hasCompletedOnboarding` — **device-global, не скидається на вихід**.
> Тому юзер після sign-out **ніколи повторно не побачить онбординг/слайди**; повторний вхід можливий
> лише з екрана профілю в `Main`, а не через онбординговий `Auth`. Це свідома поведінка (онбординг —
> одноразовий інтро на пристрій), але її варто памʼятати при тестуванні «чистого» входу.

---

## 6. Точки інлайн-довідки «?» (тултіпи)

Перелік місць, де доречні «?»-тултіпи в онбордингу та виборі мов. **Детальний текст підказок — у
файлі `09`**, тут лише перелік точок-якорів.

| # | Екран / елемент | Файл/рядок | Що пояснює тултіп |
|---|-----------------|------------|-------------------|
| 1 | `Auth` — кнопка «Увійти через Google» | `OnboardingFlow.kt:479` | Навіщо входити: монетки, реклама, промо, синк між пристроями (вмикаються лише після входу — D2). |
| 2 | `Auth` — «Пропустити» (анонім) | `OnboardingFlow.kt:421` | Що дає гостьовий режим і його ліміти: макс. 2 словники, макс. 50 слів; монеток нема. |
| 3 | `LanguageSelect` — підзаголовок «за замовчуванням для нових словників» | `OnboardingFlow.kt:771–778` | Що пара мов — це **дефолт** для нових словників; існуючі не змінюються; пару можна оверрайднути при створенні словника (D6). |
| 4 | `LanguageSelect` — секція «Я РОЗМОВЛЯЮ» | `OnboardingFlow.kt:798` | Яка мова вважається «відомою» / мовою інтерфейсу-перекладу. |
| 5 | `LanguageSelect` — секція «Я ВИВЧАЮ» | `OnboardingFlow.kt:806` | Яка мова — цільова для словників і STT-напрямку (повʼязано з D8). |
| 6 | [НОВЕ] `Auth` — індикатор «вхід виконано, але новий акаунт» (гілка б) | новий UI | Чому далі показуємо вибір мов навіть після входу (акаунт ще порожній). |

---

## Зведення позначок

- **[ЗАРАЗ]:** лінійний `Splash→Onboarding→Auth(бутафорський)→LanguageSelect→Main`; `Auth` нічого не
  автентифікує (усі кнопки `onDone`); справжній вхід лише у профілі (`startGoogleSignIn`);
  `runStartupSync` стартує вже в `Main`; `hasCompletedOnboarding` ставиться наприкінці вибору мов і є
  device-global.
- **[НОВЕ] (D5):** `Auth` стає справжнім Google-входом; після входу розвилка (а) сервер має дані →
  пропустити вибір мов, тягнути все з сервера; (б) новий акаунт → вибір мов → push; (в) скіп → вибір
  мов локально (анонім). Повторні запуски лишаються `Splash→Main` + фоновий startup-sync.
