# 20 — Конфігурація економіки в client-admin-web

> Статус усього документа: **[НОВЕ за D13]**.
>
> У поточному коді `client-admin-web` уже керує користувачами, преміумом,
> сесіями й аудитом, але окремої сторінки економіки немає. Частина сум
> захардкоджена в gateway/mobile (`INITIAL_BEE_BALANCE`,
> `REWARDED_AD_BEE_AMOUNT`, `REFERRAL_REWARD_BEES`), а цільова policy D11 ще
> не має повного admin UI.

---

## 1. Принцип

Усі суми, які користувач **витрачає** або **заробляє**, конфігуруються в
`client-admin-web` і виконуються `client-gateway`.

- Мобільний клієнт не є джерелом цін або винагород.
- Активна конфігурація версійна й незмінна після публікації.
- Зміни не діють ретроактивно на вже створені charge/reward.
- Кожне списання або нарахування зберігає `policyVersion` і фактичну суму.
- Адміністратор редагує чернетку, переглядає diff і лише потім публікує її.
- Кожна публікація має автора, обов'язкову причину та audit event.

---

## 2. Місце в адмінці

У лівій навігації `client-admin-web` додається пункт
**«Економіка»** з іконкою монетки/гаманця.

Сторінка має шість розділів:

1. **Огляд** — активна версія, дата набуття чинності, основні суми та
   попередження;
2. **Витрати** — словники, збереження слів, refund window;
3. **Винагороди** — реєстрація, реклама, інвайти та інші earn options;
4. **Маркет** — ціни й доступність готових наборів;
5. **Гаманці** — сумарні баланси/витрати, ledger та бонусні нарахування;
6. **Версії** — чернетки, заплановані/активні/архівні policy та diff.

Policy/config описує цей документ. Операційний wallet dashboard і bonus
grants — [21-client-admin-wallet-operations.md](21-client-admin-wallet-operations.md).

Активна версія read-only. Основні дії:

- **«Створити чернетку»** — копія активної policy;
- **«Зберегти чернетку»**;
- **«Перевірити»** — серверна валідація без публікації;
- **«Опублікувати»** або **«Запланувати»**;
- **«Створити rollback-чернетку»** з вибраної старої версії.

---

## 3. Що конфігурується

### 3.1 Витрати

| Поле | Призначення |
|---|---|
| `freeTopicLimit` | Кількість безкоштовних звичайних словників |
| `topicCreationCost` | Вартість наступного звичайного словника |
| `searchCost` | Вартість lookup; за D11 цільове значення 0 |
| `wordAdditionCost` | Вартість збереження одного конкретного result |
| `refundWindowSeconds` | Вікно повернення для eligible topic/word charge |

Маркет-набори не використовують `topicCreationCost` або `wordAdditionCost`:
їхня повна ціна задається окремим offer у розділі «Маркет» (D12).

### 3.2 Реєстрація та старт акаунта

Це дві різні сутності, які UI не повинен змішувати:

| Поле | Призначення |
|---|---|
| `startingBalanceBees` | Початковий баланс нового authenticated-акаунта |
| `registrationReward.enabled` | Чи є окремий одноразовий бонус за реєстрацію |
| `registrationReward.amountBees` | Сума окремого бонусу |
| `registrationReward.startsAt/endsAt` | Опційне вікно кампанії |
| `registrationReward.eligibility` | Які нові акаунти беруть участь |

Поруч показується розрахунок:

`Новий користувач отримає: startingBalanceBees + registrationReward`.

Якщо обидві суми ненульові, перед публікацією адмінка показує помітне
попередження, щоб випадково не подвоїти welcome grant.

### 3.3 Rewarded ad

| Поле | Призначення |
|---|---|
| `rewardedAd.enabled` | Чи доступний спосіб заробітку |
| `rewardedAd.rewardBees` | Базова винагорода за підтверджений перегляд |
| `rewardedAd.dailyLimit` | Опційний ліміт успішних винагород на добу |
| `rewardedAd.cooldownSeconds` | Опційна пауза між винагородами |

Сума фіксується в ad nonce/transaction snapshot на момент початку показу.
Зміна policy під час реклами не змінює вже виданий nonce. SSV та
ідемпотентність D1/D4 обов'язкові незалежно від суми.

### 3.4 Інвайти друзів / referral

| Поле | Призначення |
|---|---|
| `referral.enabled` | Чи є реферал активним earn option |
| `referral.inviterRewardBees` | Скільки отримує той, хто запросив |
| `referral.inviteeRewardBees` | Скільки отримує запрошений |
| `referral.qualifyingEvent` | Подія, після якої винагорода стає eligible |
| `referral.attributionWindowDays` | Вікно прив'язки invite-коду |
| `referral.maxRewardsPerInviter` | Опційний lifetime/campaign cap |
| `referral.startsAt/endsAt` | Опційне вікно кампанії |

`GET /v1/referral/me` повертає значення з активної policy, а не константу
`REFERRAL_REWARD_BEES`. Якщо `enabled=false` або credit-механіка ще не
розгорнута, mobile не називає інвайт способом заробітку.

`qualifyingEvent` не має бути простим відкриттям лінка. Підтримувані значення
визначає сервер, наприклад:

- `first_authenticated_sign_in`;
- `onboarding_completed`;
- `first_word_saved`.

Вибір має відповідати реалізованому anti-fraud флоу; адмінка не дозволяє
опублікувати значення, якого gateway не підтримує.

### 3.5 Промо та інші винагороди

Сторінка економіки посилається на конфігурацію Promo API (D4) або показує її
як вкладену секцію. Для кожної кампанії керуються:

- enabled/disabled;
- `reward.bees`;
- умови й target;
- початок/кінець і reset window;
- priority;
- CTA та safe deeplink.

`registration`, `milestone`, `daily_streak`, `leaderboard` та майбутні earn
types використовують той самий wallet ledger і policy snapshot. Клієнт
рендерить лише активні можливості.

---

## 4. Маркет: ціни готових словників

`client-admin-web` керує **комерційною пропозицією**, а не самим словниковим
контентом. Контент/revision готує vocabulary pipeline/dictionary domain;
client admin визначає, чи продається ревізія і за скільки.

### 4.1 Таблиця offer-ів

| Колонка | Значення |
|---|---|
| Набір | `packId`, назва, категорія |
| Ревізія | `revisionId`, кількість елементів |
| Мовна пара | default для всіх готових пар або конкретний override |
| Ціна | `priceBees`, ціле число ≥0 |
| Доступність | active/inactive |
| Чинність | `startsAt` / `endsAt` |
| Остання зміна | хто, коли, policy version |

Для масштабу використовується дворівнева ціна:

1. **default price** для `packId`;
2. опційний **pair override** для
   `packId + learningLang + knownLang`.

Pair override має пріоритет. Якщо немає ні override, ні default price, набір
для цієї пари не продається й mobile отримує `unavailable`.

### 4.2 Дії

- змінити default price;
- додати/видалити pair override у чернетці;
- масово застосувати ціну до вибраних пар;
- увімкнути/вимкнути offer;
- запланувати початок/кінець;
- переглянути effective mobile payload;
- побачити кількість чинних entitlement і покупок перед деактивацією.

Деактивація впливає лише на **нові покупки**. Наявні entitlement та повторне
встановлення придбаних наборів лишаються доступними.

---

## 5. Workflow публікації

```text
Активна policy → Створити чернетку → Редагувати → Validate
→ Переглянути diff/вплив → Причина → Publish now / Schedule
```

Правила:

- одночасно може бути кілька чернеток, але лише одна активна policy;
- scheduled policy активується сервером атомарно за `effectiveAt`;
- активну/архівну policy не редагують in-place;
- rollback = нова версія з копією старого config;
- publish вимагає повторного читання поточної active version; stale draft
  отримує 409 і мусить бути rebased;
- `reason` після trim обов'язково 3..500;
- diff показує old/new для кожного поля й market offer;
- публікація створює один append-only audit event із sanitized before/after;
- secrets, anti-fraud signals і raw provider payload у audit/UI не потрапляють.

---

## 6. Валідація

Блокуючі правила:

- усі суми — цілі числа `>= 0`;
- `freeTopicLimit >= 0`, `refundWindowSeconds >= 0`;
- якщо reward disabled, його amount не використовується;
- `startsAt < endsAt`, якщо обидва задані;
- мови pair override підтримуються й різні;
- немає двох active override для одного `packId + pair + time window`;
- `packId/revisionId` існують і сумісні з парою;
- `qualifyingEvent` входить у серверний allowlist;
- market offer не можна активувати без готової revision;
- scheduled version не може починатися в минулому.

Попередження, які вимагають окремого підтвердження:

- paid actions активні, але всі earn options вимкнені;
- одночасно ненульові `startingBalanceBees` і registration reward;
- ціна або винагорода змінилася більш ніж на заданий safety threshold;
- rewarded-ad reward активний без daily cap;
- реферал увімкнений без розгорнутого attribution/anti-fraud capability;
- деактивується offer із наявними покупками.

---

## 7. Доступи

Додаються окремі scopes:

| Scope | Можливість |
|---|---|
| `client:economy:read` | Перегляд active/draft/history і preview |
| `client:economy:write` | Створення та редагування чернеток |
| `client:economy:publish` | Publish/schedule/archive/rollback |

Початковий безпечний розподіл:

- `client_admin` — read + write draft;
- `super_admin` — read + write + publish;
- `dictionary_admin` — без доступу до економіки.

Розподіл ролей можна змінити окремим рішенням, але publish не слід
автоматично прирівнювати до звичайного user moderation.

---

## 8. Admin API `[НОВЕ]`

| Метод | Шлях | Scope | Призначення |
|---|---|---|---|
| GET | `/v1/admin/economy/policies/active` | `client:economy:read` | Активна policy |
| GET | `/v1/admin/economy/policies` | `client:economy:read` | Cursor-list версій/чернеток |
| GET | `/v1/admin/economy/policies/:id` | `client:economy:read` | Повний sanitized config + validation |
| POST | `/v1/admin/economy/policies` | `client:economy:write` | Створити draft із active або вибраної version |
| PATCH | `/v1/admin/economy/policies/:id` | `client:economy:write` | Змінити лише draft |
| POST | `/v1/admin/economy/policies/:id/validate` | `client:economy:write` | Серверна перевірка + warnings |
| POST | `/v1/admin/economy/policies/:id/publish` | `client:economy:publish` | Publish now або schedule; `{effectiveAt?,reason}` |
| POST | `/v1/admin/economy/policies/:id/archive` | `client:economy:publish` | Архівувати лише draft/scheduled |

Відповіді не містять advertising secrets, SSV keys, raw anti-fraud signals або
приватні provider credentials.

### 8.1 Client-safe runtime config

`GET /v1/economy/config` повертає мобільному клієнту лише безпечну активну
частину:

- `policyVersion`;
- актуальні topic/word/refund значення;
- `startingBalanceBees` лише як інформаційне значення, якщо потрібне UI;
- доступні earn options із label/reward/availability;
- referral presentation;
- timestamps/cache metadata.

Market price повертається самим `GET /v1/market/packs`, а promo reward —
`GET /v1/promos`, але обидва формуються з тієї самої active policy/version.

---

## 9. Runtime-правила

| Дія | Яку версію/суму фіксуємо |
|---|---|
| Створення акаунта | active policy на початку server transaction |
| Registration reward | policy + campaign version у reward ledger |
| Початок rewarded ad | amount/policy у виданому nonce |
| Referral attribution | policy, що відповідає визначеному qualifying моменту |
| Створення звичайного словника | active policy у topic charge |
| Збереження result | active policy у word charge |
| Market purchase | offer/policy/revision у market purchase |
| Refund | історична сума оригінального charge, не current policy |

Activation policy має інвалідувати gateway cache. Якщо інстанси бачать різні
версії, мутація не повинна мовчки брати локальний stale config: version
перевіряється у транзакції або через узгоджений active-policy store.

---

## 10. Дані

Мінімальна модель:

- `economy_policy_versions`
  - `id`, monotonic `version`;
  - `status = draft | scheduled | active | archived`;
  - `config` JSONB із versioned schema;
  - `baseVersion`, `effectiveAt`;
  - `createdBy`, `publishedBy`, timestamps;
- `economy_policy_validations`
  - normalized errors/warnings або validation snapshot;
- market offer-и є частиною immutable policy config або мають власні rows,
  snapshot яких входить у published version;
- wallet ledger/charge/reward rows зберігають
  `policyVersion`, `ruleKey`, `amount` і business idempotency key.

Активна версія одна — DB constraint/transaction не дозволяє дві одночасно.

---

## 11. Міграція чинних констант

Першу policy створює міграція/seed із **фактично активних на момент rollout**
значень, щоб оновлення адмінки саме по собі не змінювало економіку:

- starting balance із `INITIAL_BEE_BALANCE`;
- rewarded ad із `REWARDED_AD_BEE_AMOUNT`;
- free topic limit/topic cost;
- актуальний v1 або v2 lookup/save cost відповідно до rollout D11;
- referral presentation із `REFERRAL_REWARD_BEES`, але
  `referral.enabled=false`, доки credit/anti-fraud не реалізовані;
- registration reward відповідно до реально активної Promo API кампанії;
- market offers inactive, доки для них не задані ціни й готові revisions.

Після міграції runtime перестає читати ці суми з дубльованих mobile
констант. Старі constants можна лишити лише як compatibility fallback на
обмежений rollout-період із явною метрикою fallback usage.

Wallet ledger, aggregate spend і admin bonus rollout виконуються разом або
після policy rollout за [D14](21-client-admin-wallet-operations.md); policy
сама по собі не може відновити історичні витрати, яких код раніше не
записував.
