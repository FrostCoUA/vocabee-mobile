# 21 — Баланси й бонусні монетки в client-admin-web

> Статус усього документа: **[НОВЕ за D14]**.
>
> У поточному коді admin dashboard показує кількість rewarded-ad events, а
> user detail — лише поточний `beeBalance`. Загального wallet ledger,
> достеменної суми витрат і контрольованого admin bonus grant ще немає.

---

## 1. Що з'являється в адмінці

### 1.1 Загальний Overview

На головній сторінці `client-admin-web` додаються картки:

- **Монеток на балансах** — поточна сума балансів усіх користувачів;
- **Витрачено за весь час** — gross debit з wallet ledger;
- **Повернуто** — сума refund credits;
- **Чисті витрати** — gross spent мінус refunds;
- **Нараховано бонусами адміністратора**;
- **Загалом нараховано** — усі applied credit entries.

Дві перші картки — обов'язкові за D14. Решта потрібна, щоб число
«витрачено» не було оманливим після повернень і ручних бонусів.

Overview показує:

- стан **на зараз** для балансів;
- lifetime за замовчуванням для потокових показників;
- опційний date range `from/to` для credits/debits/refunds;
- breakdown за `ruleKey/source`;
- `completeFrom`, якщо історія ledger неповна до rollout.

### 1.2 Розділ «Економіка → Гаманці»

Окрема вкладка містить:

- ті самі headline metrics;
- графік credits/debits/refunds за період;
- таблицю wallet ledger;
- фільтри за типом операції, користувачем, policy version, датою;
- список admin bonus grants/jobs;
- CTA **«Нарахувати бонус»**.

### 1.3 Картка конкретного користувача

У `UserDetailPage` додається блок **«Гаманець»**:

| Поле | Значення |
|---|---|
| Поточний баланс | `currentBalanceBees` |
| Усього нараховано | applied credits |
| Усього витрачено | gross applied debits |
| Повернуто | refund credits |
| Чисті витрати | gross debits − refunds |
| Бонуси адміністратора | credits із `ruleKey=admin_bonus` |
| Повнота історії | `completeFrom` / legacy notice |

Нижче — cursor-table ledger користувача та дія **«Нарахувати бонусні
монетки»**, якщо scope дозволяє.

---

## 2. Визначення метрик

| Метрика | Формула |
|---|---|
| `currentOutstandingBees` | `SUM(users.bee_balance)` для вибраного scope користувачів |
| `totalCreditedBees` | `SUM(delta_bees)` для applied entries, де `delta_bees > 0` |
| `grossSpentBees` | `SUM(ABS(delta_bees))` для applied spend entries, де `delta_bees < 0` |
| `refundedBees` | `SUM(delta_bees)` для applied entries із `kind=refund` |
| `netSpentBees` | `grossSpentBees - refundedBees` |
| `adminBonusBees` | positive applied entries із `kind=admin_bonus` |

У `grossSpentBees` входять реальні покупки/списання:

- звичайний платний словник;
- збереження result;
- маркет-набір;
- інші майбутні paid actions.

Не входять:

- reserved/pending/failed/void entries;
- refunds;
- адміністративні бонуси;
- starting balance та rewards.

### 2.1 Scope користувачів

За замовчуванням headline **«Монеток на балансах»** охоплює всі user rows,
включно з banned/deactivated, бо монетки все ще числяться на їхніх рахунках.
Поруч доступний breakdown:

- active;
- banned;
- deactivated.

Admin UI явно підписує scope. Не можна мовчки виключати неактивні акаунти й
називати результат «сумарним балансом».

---

## 3. Append-only wallet ledger

Точні lifetime-витрати неможливо відновити лише з поточного
`users.beeBalance`. D14 додає єдиний append-only ledger для **всіх** рухів.

Мінімальний `wallet_ledger_entry`:

| Поле | Призначення |
|---|---|
| `id` | UUID |
| `userId` | Власник гаманця |
| `deltaBees` | Signed integer: credit `>0`, debit `<0` |
| `balanceBefore/After` | Snapshot для звірки |
| `kind` | Тип операції |
| `ruleKey` | Конкретне правило economy policy |
| `policyVersion` | Версія policy, якщо застосовна |
| `businessId` | charge/reward/purchase/grant id |
| `idempotencyKey` | Захист від повтору |
| `status` | `applied | pending | failed | void` |
| `actorType/actorSubject` | system/admin без email у public DTO |
| `metadata` | Строго allowlisted технічні поля |
| `createdAt` | UTC |

`kind` мінімально:

`opening_balance | starting_balance | registration_reward | rewarded_ad |
referral_inviter | referral_invitee | promo_reward | admin_bonus |
topic_charge | word_charge | market_purchase | refund`.

Баланс і applied ledger entry створюються **в одній транзакції**. Прямий
UPDATE `users.beeBalance` поза WalletService/ledger забороняється code policy,
а за можливості — DB permissions/trigger.

---

## 4. Індивідуальне нарахування бонусу

У `UserDetailPage → Гаманець` адміністратор натискає
**«Нарахувати бонусні монетки»**.

Форма:

- сума `amountBees` — ціле число `>0`;
- обов'язкова внутрішня причина `reason` — 3..500 символів;
- опційний user-facing текст `message` — до 160 символів;
- preview: поточний баланс → новий баланс;
- підтвердження.

Правила:

- бонус — лише positive credit; від'ємна сума й пряме встановлення балансу
  заборонені;
- за замовчуванням бонус можна дати лише active user; inactive → 409;
- server-side max amount береться з admin safety config;
- понад safety threshold потрібне посилене підтвердження;
- `Idempotency-Key` обов'язковий;
- повтор із тим самим ключем повертає той самий grant без нового credit;
- ledger entry, balance update, grant row і admin audit event — одна
  PostgreSQL transaction;
- відповідь повертає `previousBalance`, `grantedBees`, `newBalance`,
  `ledgerEntryId`.

Після успіху user detail одразу оновлює баланс та історію. Mobile побачить
новий баланс при наступному wallet/profile refresh; окремий push не є
обов'язковим для першої версії.

---

## 5. Масове нарахування бонусів

Щоб підтримати кампанії для кількох користувачів, вкладка «Гаманці» має
двоетапний bulk flow.

### 5.1 Preview

Адміністратор задає:

- явний список user ids **або** серверний фільтр-аудиторію;
- `amountBees` на одного користувача;
- reason;
- опційний user-facing message.

Сервер резолвить immutable recipient snapshot і повертає:

- eligible recipient count;
- excluded count із безпечним breakdown причин;
- total grant amount;
- sample recipients без зайвих PII;
- `previewToken` з коротким TTL;
- warnings.

Фільтр-аудиторія може використовувати лише allowlisted поля
(`accountStatus`, registration date, premium, language). Довільний SQL або
client-provided recipient count заборонені.

### 5.2 Execute

Execute вимагає:

- чинний `previewToken`;
- `Idempotency-Key`;
- повторне підтвердження recipient count і total amount;
- scope `client:wallet:grant`.

Велика аудиторія виконується як background job:

- кожен recipient має власний deterministic idempotency key;
- повтор job не дублює вже applied credits;
- job показує `queued/running/completed/partial_failed/failed`;
- retry працює лише для failed recipients;
- після першого applied credit job не можна «скасувати» шляхом видалення
  історії;
- компенсація, якщо колись потрібна, є окремою audited операцією й не
  маскується негативним bonus.

Перша реалізація може обмежити batch size. Ліміт повертає сервер і показує
admin UI.

---

## 6. Доступи й аудит

Нові scopes:

| Scope | Можливість |
|---|---|
| `client:wallet:read` | Агрегати, user wallet summary, ledger, bonus history |
| `client:wallet:grant` | Individual/bulk bonus preview та execute |

Початковий розподіл:

- `client_admin` — `client:wallet:read`;
- `super_admin` — read + grant;
- `dictionary_admin` — без wallet-доступу.

Bonus grant — важлива зовнішня дія:

- reason обов'язкова;
- audit event append-only;
- `beforeValue/afterValue` містить баланс, суму, grant/job id і recipient
  count, але не секрети або повний bulk recipient list;
- повний recipient snapshot зберігається в захищених grant tables;
- admin UI не має кнопки видалення ledger/grant history.

---

## 7. Admin API `[НОВЕ]`

### 7.1 Read

| Метод | Шлях | Scope | Призначення |
|---|---|---|---|
| GET | `/v1/admin/wallet/summary?from=&to=&status=` | `client:wallet:read` | Aggregate balances/credits/spend/refunds/admin bonuses |
| GET | `/v1/admin/wallet/ledger` | `client:wallet:read` | Cursor-list глобального ledger з фільтрами |
| GET | `/v1/admin/users/:userId/wallet/summary` | `client:wallet:read` | User wallet summary |
| GET | `/v1/admin/users/:userId/wallet/ledger` | `client:wallet:read` | Cursor-list ledger користувача |
| GET | `/v1/admin/wallet/bonus-jobs` | `client:wallet:read` | Bulk job history/status |

`WalletSummaryDto`:

```json
{
  "currentOutstandingBees": 125000,
  "totalCreditedBees": 230000,
  "grossSpentBees": 118000,
  "refundedBees": 13000,
  "netSpentBees": 105000,
  "adminBonusBees": 7000,
  "completeFrom": "2026-08-01T00:00:00Z",
  "isLifetimeComplete": false
}
```

Числа в прикладі ілюстративні.

### 7.2 Individual bonus

`POST /v1/admin/users/:userId/wallet/bonus`

- scope: `client:wallet:grant`;
- header: `Idempotency-Key`;
- body:

```json
{
  "amountBees": 50,
  "reason": "Компенсація за підтверджену помилку",
  "message": "Дякуємо за повідомлення — бонус уже на балансі."
}
```

### 7.3 Bulk bonus

| Метод | Шлях | Scope | Призначення |
|---|---|---|---|
| POST | `/v1/admin/wallet/bonuses/preview` | `client:wallet:grant` | Resolve immutable recipient snapshot і total |
| POST | `/v1/admin/wallet/bonuses` | `client:wallet:grant` | Створити job за previewToken + Idempotency-Key |
| GET | `/v1/admin/wallet/bonus-jobs/:jobId` | `client:wallet:read` | Статус і безпечні totals |
| POST | `/v1/admin/wallet/bonus-jobs/:jobId/retry` | `client:wallet:grant` | Retry лише failed recipients |

---

## 8. Дані

Мінімальні таблиці:

- `wallet_ledger_entries` — append-only рухи;
- `admin_bonus_grants` — individual grant business record;
- `admin_bonus_jobs` — bulk header/audience snapshot/status/totals;
- `admin_bonus_job_recipients` — per-user applied/failed state та
  deterministic idempotency key.

Індекси:

- `(user_id, created_at DESC, id DESC)`;
- `(kind, created_at DESC)`;
- unique `(user_id, idempotency_key)` або відповідний business scope;
- unique grant/job idempotency keys;
- status/time для job worker.

`users.beeBalance` лишається швидким поточним snapshot, але ledger є джерелом
історії та звірки. Періодичний reconciliation порівнює
`opening + SUM(applied delta)` із `beeBalance` і сигналізує про розбіжності,
не виправляючи їх мовчки.

---

## 9. Legacy rollout і повнота історії

До появи ledger код зберігає лише:

- поточний `users.beeBalance`;
- частину rewarded-ad credits;
- не повну історію всіх spend/refund.

Тому міграція:

1. створює для кожного користувача `opening_balance` на чинний баланс;
2. задає глобальний `completeFrom = rollout timestamp`;
3. може імпортувати достовірні legacy events лише без дублювання;
4. **не вигадує** історичні витрати як різницю між стартовим і поточним
   балансом.

Наслідок:

- **«Монеток на балансах»** точне відразу після rollout;
- spend/credit/refund totals точні **з `completeFrom`**;
- «за весь час» не показується без позначки
  `Історія повна з <date>`, доки старі операції не можна достовірно
  реконструювати.

---

## 10. Крайові випадки

- Concurrent purchase і admin bonus серіалізуються на user wallet row; жодне
  оновлення не губиться.
- Bonus не обходить максимальне integer/DB обмеження балансу.
- Повтор після timeout безпечний лише з тим самим `Idempotency-Key`.
- User став inactive між preview та execute → individual 409 / bulk excluded
  або failed за зафіксованим правилом job.
- Видалення/deactivation user не видаляє ledger; GDPR cleanup має окрему
  retention/anonymization policy.
- Зміна economy policy не змінює вже створений admin bonus.
- Нульова сума bonus invalid; для безкоштовної кампанії bonus grant не
  створюється.
- Admin не може видати bonus самому собі через mobile identity: admin
  credential domain і user wallet target лишаються окремими.

