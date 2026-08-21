# 18 · Продуктова аналітика (PostHog)

Аналітика живе в особистому проєкті PostHog (**us.posthog.com, project 519771**, хост інжесту `https://us.i.posthog.com`) — окремому від будь-яких робочих сервісів. Ключ — публічний клієнтський `phc_…` (не секрет, як і Sentry DSN). Мета: бачити **всі запити користувачів**, **що віддав бекенд** і **звідки взяті дані** — згенеровані AI чи прочитані з бази.

## [ЗАРАЗ] Архітектура

| Шар | SDK | Ініціалізація | Вимикач |
|---|---|---|---|
| `vocabee-gateway` (обидва гейтвеї) | `posthog-node` | лінивий singleton `src/analytics/posthog.ts` | порожній `POSTHOG_API_KEY` |
| `vocabee-mobile` (Android) | `com.posthog:posthog-android` | `VocabeeApplication.initPostHog()` | порожній `VOCABEE_POSTHOG_API_KEY` |
| iOS | — | немає (заглушка `NoAnalyticsTracker`) | — |

- Спільний код мобайла знає лише інтерфейс `core/analytics/AnalyticsTracker` (Koin: Android → `PostHogAnalyticsTracker`, iOS → no-op).
- **Одна персона на обох кінцях:** distinct id = серверний `users.id`. Клієнт викликає `identify(user.id)` після `ApplyAuthenticatedAccount`, бекенд шле події з тим самим id → PostHog зливає їх в одного користувача. Вихід з акаунта → `reset()`.
- Анонімні/сервісні події йдуть personless (`$process_person_profile: false`), щоб не плодити персон.
- Спільна властивість `app_environment` (`development`/`production`) на всіх подіях з обох боків.

## [ЗАРАЗ] Словник подій

**Бекенд (`vocabee-gateway`):**
| Подія | Де | Що всередині |
|---|---|---|
| `api_request` | глобальний інтерсептор обох гейтвеїв (`analytics/api-request-analytics.interceptor.ts`, реєстрація в `bootstrap-gateway.ts`) | gateway, method, route, path, status_code, duration_ms, request_id, user_id/is_anonymous_user, api_consumer_id (dictionary), error_type. Health-пінги пропускаються |
| `translation_search` | `client-search.controller.ts` після відповіді dictionary-gateway | **query**, мови, tier, bee_balance, results_count, provider_reason, translation_origin і головне — **`data_source`: `database` / `ai` / `provider` / `none`** |
| `lexicon_search_served` | `lexicon/search-observability.ts` (поруч із Sentry-логом; покриває й не-AI провайдерів) | provider_reason, data_source, translation_origin, мови, results_count — сервісна подія dictionary-gateway без персони |

**Мобайл (Android):**
| Подія | Де | Що всередині |
|---|---|---|
| `client_api_request` | Ktor-плагін `data/api/AnalyticsHttpPlugin.kt` (кожен запит до гейтвея) | path (без query string), method, status_code, duration_ms, error_type |
| `translation_search_result` / `translation_search_failed` | `RemoteLexiconSearchUseCase` (сирі `source`/`origin` ще не змаплені в UI-модель) | query, мови, tier, results_count, bee_balance, translation_origin, **data_source** |
| `practice_answer` (нова схема) | `VocabeeStore.adjustSenseGroupKnowledge` | topic_id, **word_id** (представник сенс-групи), **`sense_group_size`**, known |
| `dictionary_created` / `dictionary_deleted` / `dictionary_words_cleared` | `VocabeeStore` | title/topic_id, мови, charged_beecoins, words_removed |
| `word_added` / `word_deleted` | `VocabeeStore` | topic_id, source, translation, has_details |
| `practice_round_completed` | `VocabeeStore` | rounds_total, streak_days |
| `beecoins_added` / `beecoins_spent` | `VocabeeStore` | amount, balance |
| `signed_out` + `reset()` | `VocabeeStore.signOutKeepLastUserState` | — |
| авто-події SDK | lifecycle (`Application Opened`…) | екранні авто-події вимкнені (одна Activity + Compose) |

### [ЗАРАЗ→змінено фазами 2–4] `practice_answer` — нова схема

| Було (до сенс-груп) | Стало (D16) |
|---|---|
| одна подія на **рядок** словника; `word_id` — саме той рядок | одна подія на **картку-сенс**; `word_id` — **представник** групи (її обличчя), решта членів у події не перелічена |
| — | `sense_group_size` — скільки записів відповідь **реально** рушила (`updatedIds`, а не склад замороженої картки: член, видалений під час раунду, розмір не роздуває) |
| `known` | `known` — без змін |

> **Історія несумісна для мультиперекладних груп.** Раніше три збережені переклади
> одного значення давали **три** події `practice_answer` за раунд, тепер — **одну** з
> `sense_group_size = 3`. Тобто на межі релізу впаде «кількість відповідей» і зросте
> середній прогрес на подію, хоч поведінка користувача не змінилась. Для одноперекладних
> сенсів (`sense_group_size = 1`, більшість словників) схеми збігаються. Порівнювати
> періоди коректно лише через `sum(sense_group_size)`, а не `count()`; або фільтрувати
> `sense_group_size = 1`.

### [ЗАРАЗ→змінено фазами 2–4] Метрика показаних варіантів пошуку рахує СЕНСИ

`results_count` у `translation_search_result` береться з довжини списку опцій, який
рендериться на екрані. Відколи варіанти одного значення зливаються в один айтем
(`toSenseGroupedOptions`, [13](13-add-word-and-ai-search.md) §5.1), це вже **кількість
показаних сенсів**, а не кількість перекладів у відповіді сервера: `run` із 32
варіантами дає ~8 айтемів, решта живе в `alternatives` розгорнутого рядка. Серверні
`results_count` у `translation_search` / `lexicon_search_served` НЕ змінились — вони й
далі рахують варіанти, тож клієнтське й серверне число тепер розходяться **за
дизайном**. Для «скільки провайдер дав» дивись серверну подію.

> **[ВІДКРИТЕ] Відмови дедупу не видно в аналітиці.** Гейт «цей сенс уже у словнику»
> ([13](13-add-word-and-ai-search.md) §6.2) робить `return` до `addWordUseCase`, тож
> `word_added` не летить — і не летить нічого замість нього. Скільки разів користувачі
> впираються у відмову (і чи не сприймають її як баг) наразі **невідомо**. Потрібна
> подія `word_add_rejected_same_sense` з `topic_id` / `source` / чи був кандидат
> альтернативою — без неї не оцінити, чи не варто натомість підсвічувати вже збережену
> картку.

`data_source` виводиться так: `exact_cached` → **database**; `translated` + origin `openai-*`/`ai-*` → **ai**; `translated` + інший origin (deepl/mymemory/словник) → **provider**; `not_a_word`/`echo`/`no_provider_data` → **none**.

> **Увага, головна пастка:** поле `origin` рядка каже, **хто колись його створив**, а не звідки він прийшов **зараз**. Переклад із origin `openai-gpt-5.6-sol` і `cached=true` прочитано з Postgres — виклику AI не було. Авторитетна ознака — `meta.triedProvider` / `meta.providerReason` від сервера.

## [ЗАРАЗ] Швидка перевірка з телефону

Кожен пошук пише один рядок у logcat:

```bash
adb logcat -s VocabeeSearch:D
# q='run' source=database ms=2368 n=32 cached=32/32 triedProvider=false reason=exact_cached origin=vocabee-translate/en_uk/core_0251_0300
# q='flabbergasted' source=ai ms=10647 n=4 cached=0/4 triedProvider=true reason=translated origin=openai-gpt-5.6-sol
# q='quokka' source=none ms=205 n=0 cached=0/0 triedProvider=false reason=not_a_word
```

Орієнтири латентності (заміряно на dev): з бази — 0.2–1.5 с, генерація AI — 10–12 с, відсіяне спелчекером слово — 0.2 с.

**Що визначає латентність видачі з бази.** Не кількість результатів, а кількість унікальних слів, що реально потребують runtime-добудови. **Відсутній IPA більше не запускає dictionary/AI-виклик:** exact-запис повертається з `ipa=null`, а поле дозаповнюється окремим curated re-import. `dictionaryMissAt` локалізує повтори після спроби, де весь dictionary-ланцюг не дав корисних даних.

Подія `lexicon_search_served` тепер несе `total_ms` / `prefix_ms` / `variants_ms` — якщо латентність знову виросте, видно, яка саме фаза винна.

Перший заміряний розклад на dev (`run`, exact_cached): `total=1959ms`, `prefix=9ms`, `variants=1942ms` — 99% часу жило у збірці варіантів, а не в запиті до Postgres.

**Що це було і як полагоджено.** Збагачення exact-збігу (senses/relations/forms/приклади) залежить лише від слова, але виконувалось у кожному exact-варіанті окремо. У `run` вісім перекладів → ті самі сім запитів у Postgres відпрацьовували вісім разів. Тепер результат шериться одним промісом на слово за запит (як `senseDataJobs`), а рядки, прочитані під час form-of перевірки, перевикористовуються замість повторного читання.

Заміряно на dev до/після (теплі виклики):

| слово | до | після |
|---|---|---|
| run (32 результати) | 1.5–2.0 с | **0.50–0.83 с** |
| cat (46 результатів) | 1.3–2.0 с | **0.22–0.28 с** |
| honey, beehive | 0.27 с | 0.18–0.31 с |

Відповідь не змінилась: ті самі 32 результати, 11 з IPA/senses/прикладами, `senseIndex` на місці.

## [ЗАРАЗ] Конфігурація

- Gateway: `POSTHOG_API_KEY` + `POSTHOG_HOST` у `.env` (локально), `docker-compose.yml` прокидає в обидва гейтвеї. **Coolify:** змінні треба додати вручну на кожен застосунок (client-gateway і dictionary-gateway) — compose-маппінги там не діють.
- Mobile: `vocabee.posthog.apiKey`/`vocabee.posthog.host` у `local.properties` або `VOCABEE_POSTHOG_API_KEY`/`VOCABEE_POSTHOG_HOST` в env; дефолт зашитий у `app/build.gradle.kts` (як Sentry DSN).

## [МАЙБУТНЄ]

- PostHog iOS SDK замість `NoAnalyticsTracker`.
- Промо/лідерборд-події (D4) після появи Promo API.
- Дашборди: воронка додавання слова, розподіл `data_source` (скільки пошуків реально ходить в AI), вартість AI-запитів.
