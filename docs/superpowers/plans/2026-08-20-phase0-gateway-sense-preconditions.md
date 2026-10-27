# Фаза 0: gateway-передумови сенс-груп — план імплементації

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Пошук віддає кожному варіанту деталі його власного сенсу, curated-дані не деградують і перекривають translator-кеш, сміттєві wiktextract-форми відфільтровані на записі, читанні та в БД.

**Architecture:** Чотири незалежні правки в vocabee-gateway: (1) спільний фільтр сміттєвих форм, застосований у FreeDictionary-провайдері (write-path), в `assembleEnrichment` (read-path) і одноразовою міграцією (БД); (2) guard у `confirmTier`, щоб provider-кеш не перетирав tier curated-рядків; (3) curated-first у `ORDER BY` префікс-пошуку, щоб при дублікатах target-тексту serve-дедуп лишав curated-рядок; (4) чиста функція `projectEnrichmentForVariant`, яка після мемоізованого word-level enrichment проєктує у варіант лише сенси з його `senseKeys`/legacy `senseIndex` (без атрибуції — легасі-поведінка без змін).

**Tech Stack:** NestJS 11, Drizzle ORM, PostgreSQL 16 (партиціонований lexicon), Jest 29 + ts-jest.

## Global Constraints

- Робоча тека: `/Users/artemmoroz/Work/pet/vocabee/vocabee-gateway`. Гілка: `feature/phase0-sense-scoped-search` від `develop` (створити в Task 1, step 1).
- У робочому дереві може бути чужий WIP: `git add` — ЛИШЕ явні шляхи, ніколи `git add -A`/`git add .`.
- Тести: один файл — `npx jest <pattern>` (rootDir=src, короткий патерн типу `lexicon.service` працює); повний прогін перед кожним комітом задачі: `npm run lint:backend && npm run typecheck && npm run test:backend`.
- `CURATED_IMPORT_TIER = 'seed-import'` імпортується з `src/lexicon/provider-tier.ts:54`.
- Наступний вільний номер міграції — **0022** (`src/db/migrations/`, формат `NNNN_snake_case.sql`; 0001–0021 зайняті).
- Wire-формат `SearchVariant` не змінюється: жодних нових/видалених полів DTO — міняється лише вміст `senses`/`synonyms`/`antonyms`/`examples`/`senseIndex` для варіантів З атрибуцією.
- Коментарі в коді — українською, у стилі сусіднього коду; технічні ідентифікатори англійською.
- Інтеграційних тестів з реальною БД у репо немає — все на fake-ах/моках (патерни: `lexicon.service.spec.ts` FakeLexiconRepository; `lexicon.repository.spec.ts` scripted-білдери + `rendered()`; міграційні `*.spec.ts` асертять текст SQL).

---

### Task 1: Спільний фільтр сміттєвих форм (`isJunkWordForm`)

**Files:**
- Create: `src/lexicon/word-form-filter.ts`
- Test: `src/lexicon/word-form-filter.spec.ts`

**Interfaces:**
- Produces: `export function isJunkWordForm(formText: string, tags?: readonly string[]): boolean` — консумується Task 2 (провайдер + assembleEnrichment).

- [ ] **Step 1: Створити гілку**

```bash
cd /Users/artemmoroz/Work/pet/vocabee/vocabee-gateway
git checkout -b feature/phase0-sense-scoped-search develop
```

- [ ] **Step 2: Написати падаючий тест**

`src/lexicon/word-form-filter.spec.ts`:

```ts
import { isJunkWordForm } from './word-form-filter';

describe('isJunkWordForm', () => {
  it('відсіює wiktextract-дебрис за текстом', () => {
    expect(isJunkWordForm('no-table-tags', ['table-tags'])).toBe(true);
    expect(isJunkWordForm('glossary', ['inflection-template'])).toBe(true);
    expect(isJunkWordForm('No-Table-Tags', [])).toBe(true);
  });

  it('відсіює службові теги таблиці відмінювання', () => {
    expect(isJunkWordForm('whatever', ['table-tags'])).toBe(true);
    expect(isJunkWordForm('whatever', ['past', 'inflection-template'])).toBe(true);
  });

  it('відсіює форми без жодної літери чи цифри', () => {
    expect(isJunkWordForm('-', ['imperative', 'past'])).toBe(true);
    expect(isJunkWordForm('—', [])).toBe(true);
    expect(isJunkWordForm('  ', [])).toBe(true);
    expect(isJunkWordForm('', [])).toBe(true);
  });

  it('пропускає справжні форми, включно з кирилицею й архаїкою', () => {
    expect(isJunkWordForm('runs', ['present', 'singular', 'third-person'])).toBe(false);
    expect(isJunkWordForm('ran', ['past'])).toBe(false);
    expect(isJunkWordForm('бігла', ['past', 'feminine'])).toBe(false);
    expect(isJunkWordForm('runneth', ['archaic', 'present'])).toBe(false);
  });
});
```

- [ ] **Step 3: Переконатися, що тест падає**

Run: `npx jest word-form-filter`
Expected: FAIL — `Cannot find module './word-form-filter'`

- [ ] **Step 4: Мінімальна імплементація**

`src/lexicon/word-form-filter.ts`:

```ts
/**
 * Wiktextract-дампи містять службові псевдо-форми таблиці відмінювання
 * («no-table-tags» з тегом table-tags, «glossary» з inflection-template,
 * рядок «-» для відсутньої клітинки). Це метадані парсера, не словоформи —
 * фільтруємо на записі (провайдер), на читанні (assembleEnrichment) і в БД
 * (міграція 0022).
 */
const JUNK_FORM_TAGS = new Set(['table-tags', 'inflection-template']);
const JUNK_FORM_TEXTS = new Set(['no-table-tags', 'glossary']);

export function isJunkWordForm(
  formText: string,
  tags: readonly string[] = [],
): boolean {
  const text = formText.trim().toLowerCase();
  if (!text) return true;
  if (JUNK_FORM_TEXTS.has(text)) return true;
  if (tags.some((tag) => JUNK_FORM_TAGS.has(tag.trim().toLowerCase()))) {
    return true;
  }
  // Без жодної літери/цифри («-», «—», пунктуація) — не форма.
  if (!/[\p{L}\p{N}]/u.test(text)) return true;
  return false;
}
```

- [ ] **Step 5: Тест зелений**

Run: `npx jest word-form-filter`
Expected: PASS (4 tests)

- [ ] **Step 6: Коміт**

```bash
git add src/lexicon/word-form-filter.ts src/lexicon/word-form-filter.spec.ts
git commit -m "feat(lexicon): спільний фільтр сміттєвих wiktextract-форм"
```

---

### Task 2: Застосувати фільтр у провайдері (write) і в assembleEnrichment (read)

**Files:**
- Modify: `src/lexicon/providers/free-dictionary.provider.ts:135-143` (цикл forms)
- Modify: `src/lexicon/lexicon.service.ts:1928-1931` (мапінг forms в `assembleEnrichment`)
- Test: `src/lexicon/lexicon.service.spec.ts` (новий describe для `assembleEnrichment`)

**Interfaces:**
- Consumes: `isJunkWordForm` з Task 1.
- Produces: `assembleEnrichment(...).forms` більше не містить сміттєвих рядків — на це спираються тести Task 6.

- [ ] **Step 1: Написати падаючий тест на read-шлях**

`assembleEnrichment` — експортована чиста функція (`lexicon.service.ts:1813`), тестується напряму. Додати в `src/lexicon/lexicon.service.spec.ts` (у кінець файла, поруч з іншими describe; фабрики рядків — за патерном `senseRow(patch)` з `lexicon.repository.spec.ts:432-448`):

```ts
describe('assembleEnrichment форми', () => {
  const entry = {
    lang: 'en',
    normalized: 'run',
    ipa: '/ɹʌn/',
    audioUrl: null,
    partOfSpeech: ['verb'],
  };
  const formRow = (formText: string, tags: string[]) =>
    ({
      id: '00000000-0000-0000-0000-000000000001',
      wordLang: 'en',
      wordId: '00000000-0000-0000-0000-000000000002',
      formText,
      tags,
      source: 'dictionary',
      origin: 'freedictionaryapi.com',
      createdAt: new Date('2026-01-01T00:00:00Z'),
    }) as never;

  it('відсіює wiktextract-дебрис, лишаючи справжні форми', () => {
    const result = assembleEnrichment(entry, [], [], [
      formRow('runs', ['present', 'singular', 'third-person']),
      formRow('no-table-tags', ['table-tags']),
      formRow('glossary', ['inflection-template']),
      formRow('-', ['imperative', 'past']),
      formRow('ran', ['past']),
    ], []);

    expect(result.forms.map((form) => form.text)).toEqual(['runs', 'ran']);
  });
});
```

Імпорт `assembleEnrichment` у spec уже може бути — перевірити верх файла; якщо ні, додати до наявного import з `'./lexicon.service'`.

- [ ] **Step 2: Тест падає**

Run: `npx jest lexicon.service -t "відсіює wiktextract-дебрис"`
Expected: FAIL — отримано `['runs', 'no-table-tags', 'glossary', '-', 'ran']`

- [ ] **Step 3: Імплементація read-шляху**

У `src/lexicon/lexicon.service.ts` додати import (верх файла, поруч з іншими локальними імпортами):

```ts
import { isJunkWordForm } from './word-form-filter';
```

Замінити мапінг forms у `assembleEnrichment` (рядки 1928-1931):

```ts
    forms: forms
      .filter((form) => !isJunkWordForm(form.formText, form.tags ?? []))
      .map((form) => ({
        text: form.formText,
        tags: form.tags ?? [],
      })),
```

- [ ] **Step 4: Імплементація write-шляху (провайдер)**

У `src/lexicon/providers/free-dictionary.provider.ts` додати import:

```ts
import { isJunkWordForm } from '../word-form-filter';
```

У циклі forms (рядки 135-143) — фільтр перед записом у мапу:

```ts
        for (const form of entry.forms ?? []) {
          const text = form.word?.trim();
          if (!text) continue;
          if (isJunkWordForm(text, form.tags ?? [])) continue;
          const key = text.toLowerCase();
          if (formMap.has(key)) continue;
          if (formMap.size >= MAX_FORMS) break;
          formMap.set(key, { text, tags: dedupeTags(form.tags) });
        }
```

(Окремого spec на провайдер немає в репо; write-шлях покритий юніт-тестами хелпера з Task 1 + typecheck. Якщо під час роботи знайдеться готовий seam для провайдер-тесту — можна додати за патерном `openai-dictionary`-спеків, але це не обовʼязково для завершення задачі.)

- [ ] **Step 5: Тести зелені**

Run: `npx jest lexicon.service && npx jest word-form-filter`
Expected: PASS, включно з новим describe.

- [ ] **Step 6: Повний прогін і коміт**

```bash
npm run lint:backend && npm run typecheck && npm run test:backend
git add src/lexicon/lexicon.service.ts src/lexicon/lexicon.service.spec.ts src/lexicon/providers/free-dictionary.provider.ts
git commit -m "fix(lexicon): не пропускати wiktextract-дебрис у форми (провайдер + видача)"
```

---

### Task 3: Міграція 0022 — вичистити сміттєві форми з БД

**Files:**
- Create: `src/db/migrations/0022_prune_junk_word_forms.sql`
- Test: `src/db/migrations/0022_prune_junk_word_forms.spec.ts`

**Interfaces:**
- Consumes: критерії сміття з Task 1 (продубльовані в SQL — це свідома денормалізація: міграція має бути самодостатнім SQL).

- [ ] **Step 1: Написати падаючий тест**

Патерн — наявні міграційні спеки (напр. `0021`-spec): читають SQL текстом і асертять зміст. `src/db/migrations/0022_prune_junk_word_forms.spec.ts`:

```ts
import { readFileSync } from 'node:fs';
import { join } from 'node:path';

describe('0022_prune_junk_word_forms', () => {
  const sql = readFileSync(
    join(__dirname, '0022_prune_junk_word_forms.sql'),
    'utf8',
  );

  it('видаляє форми зі службовими тегами таблиці відмінювання', () => {
    expect(sql).toContain('DELETE FROM lexicon_word_forms');
    expect(sql).toContain("ARRAY['table-tags', 'inflection-template']");
  });

  it('видаляє відомі сміттєві тексти й порожні псевдо-форми', () => {
    expect(sql).toContain("lower(form_text) IN ('no-table-tags', 'glossary')");
    expect(sql).toContain('[[:alnum:]]');
  });
});
```

- [ ] **Step 2: Тест падає**

Run: `npx jest 0022_prune_junk_word_forms`
Expected: FAIL — файл SQL не існує (`ENOENT`).

- [ ] **Step 3: Написати міграцію**

`src/db/migrations/0022_prune_junk_word_forms.sql` (прецедент DELETE-міграцій: `0014:18-19`, `0019:10`):

```sql
-- Wiktextract-дебрис із таблиць відмінювання потрапляв у lexicon_word_forms
-- як псевдо-форми (no-table-tags/glossary/«-»). Запис і видача вже фільтрують
-- (word-form-filter.ts); ця міграція чистить накопичене.
DELETE FROM lexicon_word_forms
WHERE tags && ARRAY['table-tags', 'inflection-template']::text[]
   OR lower(form_text) IN ('no-table-tags', 'glossary')
   OR form_text !~ '[[:alnum:]]';
```

- [ ] **Step 4: Тест зелений**

Run: `npx jest 0022_prune_junk_word_forms`
Expected: PASS.

- [ ] **Step 5: Локальна перевірка міграції (якщо локальний Postgres :5433 запущений)**

```bash
npm run db:migrate
```

Expected: `0022_prune_junk_word_forms.sql` застосована без помилок (раннер трекає в `_migrations`, кожен файл у своїй транзакції). Якщо локальний Postgres не запущений — пропустити, міграція перевіриться на dev-деплої.

- [ ] **Step 6: Коміт**

```bash
git add src/db/migrations/0022_prune_junk_word_forms.sql src/db/migrations/0022_prune_junk_word_forms.spec.ts
git commit -m "chore(db): міграція 0022 — вичистити wiktextract-дебрис із lexicon_word_forms"
```

---

### Task 4: `confirmTier` не деградує curated-рядки

**Files:**
- Modify: `src/lexicon/lexicon.repository.ts:1359-1382` (`confirmTier`)
- Test: `src/lexicon/lexicon.repository.spec.ts`

**Interfaces:**
- Consumes: `CURATED_IMPORT_TIER` з `./provider-tier`.
- Produces: `confirmTier` гарантовано не змінює рядки з `provider_tier = 'seed-import'` — на це спирається Task 5 (curated-рядок лишається curated назавжди, крім явного адмін-імпорту).

Контекст дефекту: `persistAllVariants` (`lexicon.service.ts:1143-1157`) для вже наявного target-тексту викликає `confirmTier`, який безумовно перезаписує `provider_tier` (+`metadata`) — тобто повторний виклик перекладача деградує промоутнутий curated-рядок назад у provider-tier, вимикаючи всі curated-захисти (`isCuratedImportRow`, freshness, quality-exemption).

- [ ] **Step 1: Написати падаючий тест**

Патерн scripted-білдерів — `lexicon.repository.spec.ts:318-399` (`scriptedUpdate`, `rendered()`; у цьому файлі подвійні лапки). Додати:

```ts
describe("LexiconRepository confirmTier", () => {
  it("не чіпає curated-рядки: guard по provider_tier у WHERE", async () => {
    const update = scriptedUpdate();
    const repository = new LexiconRepository({
      update: update.builder,
    } as unknown as Database);

    await repository.confirmTier(
      "en",
      WORD_ID,
      "uk",
      "бігти",
      "openai-gpt-5.6-sol",
    );

    const where = rendered(update.captured.where);
    expect(where.sql).toContain("is distinct from");
    expect(where.params).toContain("seed-import");
  });
});
```

(Точні імена хелперів/геттерів звірити з наявними тестами `confirmTier`-сусідів у файлі — `scriptedUpdate` повертає обʼєкт із захопленим `where`; якщо в наявній версії геттер називається інакше, використати той самий, що в сусідньому describe для update-методів.)

- [ ] **Step 2: Тест падає**

Run: `npx jest lexicon.repository -t "не чіпає curated"`
Expected: FAIL — у відрендереному WHERE немає `is distinct from`.

- [ ] **Step 3: Імплементація**

У `src/lexicon/lexicon.repository.ts`: додати import `CURATED_IMPORT_TIER` з `./provider-tier` (якщо ще не імпортовано), і в `confirmTier` розширити WHERE:

```ts
      .where(
        and(
          eq(translations.sourceLang, sourceLang),
          eq(translations.sourceWordId, sourceWordId),
          eq(translations.targetLang, targetLang),
          sql`lower(${translations.targetText}) = ${normalizedTargetText}`,
          isNull(translations.deletedAt),
          // Провайдерське «підтвердження» ніколи не деградує curated-імпорт:
          // seed-import — найвищий tier, його міняє лише адмінський import-v2.
          sql`${translations.providerTier} IS DISTINCT FROM ${CURATED_IMPORT_TIER}`,
        ),
      );
```

Оновити docstring над методом (рядки 1350-1358): додати речення «Curated-рядки (`seed-import`) виключені — їхній tier недоторканний для провайдерів.»

- [ ] **Step 4: Тест зелений + повний прогін**

Run: `npx jest lexicon.repository`
Expected: PASS. Потім `npm run lint:backend && npm run typecheck && npm run test:backend`.

- [ ] **Step 5: Коміт**

```bash
git add src/lexicon/lexicon.repository.ts src/lexicon/lexicon.repository.spec.ts
git commit -m "fix(lexicon): confirmTier більше не деградує curated seed-import рядки"
```

---

### Task 5: Curated-рядки перемагають у видачі при дублікатах

**Files:**
- Modify: `src/lexicon/lexicon.repository.ts:890-899` (`findPrefixMatches` ORDER BY)
- Modify: `src/lexicon/lexicon.service.spec.ts` (FakeLexiconRepository — дзеркало нового порядку, якщо його `findPrefixMatches` сортує)
- Test: `src/lexicon/lexicon.repository.spec.ts` + `src/lexicon/lexicon.service.spec.ts`

**Interfaces:**
- Consumes: гарантію Task 4 (tier curated-рядка стабільний).
- Produces: при двох рядках з одним target_text перший у `suggestionVariants` — curated ⇒ serve-дедуп (`lexicon.service.ts:575-607`, перший доданий перемагає) лишає curated.

Контекст: унікального індексу на `(source_word_id, lower(target_text))` немає (лише не-unique `translations_source_idx` та ін.) — дублікати можливі; поточний ORDER BY (`exact-first → is_primary DESC → length ASC → normalized ASC`) не враховує tier, тож дублікат-translator може перекрити curated.

- [ ] **Step 1: Падаючий тест на SQL-порядок**

У `lexicon.repository.spec.ts`, за патерном `rendered()`:

```ts
describe("LexiconRepository findPrefixMatches", () => {
  it("ставить curated-рядки перед provider-кешем у ORDER BY", async () => {
    const select = scriptedSelect([[]]);
    const repository = new LexiconRepository({
      select: select.builder,
    } as unknown as Database);

    await repository.findPrefixMatches("en", "run", "uk", 50);

    const orderBy = rendered(select.captured.orderBy);
    expect(orderBy.sql).toContain("provider_tier");
    expect(orderBy.params).toContain("seed-import");
  });
});
```

(Як і в Task 4 — точну форму захоплення `orderBy` звірити зі scripted-хелперами файла; якщо `scriptedSelect` не захоплює orderBy, розширити хелпер за аналогією з `where`-геттером.)

- [ ] **Step 2: Тест падає**

Run: `npx jest lexicon.repository -t "curated-рядки перед provider"`
Expected: FAIL.

- [ ] **Step 3: Імплементація ORDER BY**

`src/lexicon/lexicon.repository.ts`, `findPrefixMatches` (рядки 890-899) — додати tier-критерій одразу після exact-first:

```ts
      .orderBy(
        // exact matches first
        sql`CASE WHEN ${lexiconWords.normalized} = ${queryNormalized} THEN 0 ELSE 1 END`,
        // curated import beats any provider cache on duplicate target rows
        sql`CASE WHEN ${translations.providerTier} = ${CURATED_IMPORT_TIER} THEN 0 ELSE 1 END`,
        // then primary translations
        desc(translations.isPrimary),
        // then shorter words (more likely to be the user's actual target)
        asc(sql`length(${lexiconWords.normalized})`),
        // tie-break alphabetically
        asc(lexiconWords.normalized),
      )
```

- [ ] **Step 4: Поведінковий тест на рівні сервісу**

У `lexicon.service.spec.ts` (патерн `FakeLexiconRepository` + `seedDirectionalTranslations`, `:1274`):

```ts
describe('LexiconService curated пріоритет при дублікатах', () => {
  it('serve-дедуп лишає curated-рядок, а не свіжіший translator-дубль', async () => {
    const repository = new FakeLexiconRepository();
    repository.seedDirectionalTranslations('en', 'run', 'uk', [
      { text: 'бігти', providerTier: 'openai-gpt-5.6-sol' },
      { text: 'бігти', providerTier: 'seed-import' },
    ]);
    const service = createService(repository, new FakeTranslationProvider({}));

    const result = await service.search({
      query: 'run',
      speakLang: 'uk',
      learnLang: 'en',
    });

    const variants = result.results.filter((v) => v.knownWord === 'бігти');
    expect(variants).toHaveLength(1);
    expect(variants[0].origin).toBe(
      repository.translationOrigin('seed-import', 'бігти'),
    );
  });
});
```

Примітка виконавцю: якщо `FakeLexiconRepository.findPrefixMatches` реалізує сортування — віддзеркалити curated-first там само; якщо фейк повертає в порядку вставки — асерт вище формулюється через порядок сіду (спершу translator, потім curated) і БЕЗ дзеркала тест не пройде — це і є сигнал додати сортування у фейк. Хелпера `translationOrigin` у фейку може не бути — тоді асертити стабільне поле, яке фейк уже зберігає (наприклад, зробити curated-сід з іншим `metadata` і перевірити `variants[0].source === 'seed'`, якщо фейк мапить source із сіду). Ключова вимога незмінна: **єдиний варіант «бігти», і це curated-рядок**.

- [ ] **Step 5: Тести зелені + повний прогін**

Run: `npx jest lexicon.repository && npx jest lexicon.service`, потім `npm run lint:backend && npm run typecheck && npm run test:backend`
Expected: PASS.

- [ ] **Step 6: Коміт**

```bash
git add src/lexicon/lexicon.repository.ts src/lexicon/lexicon.repository.spec.ts src/lexicon/lexicon.service.spec.ts
git commit -m "fix(lexicon): curated seed-import перемагає translator-кеш при дублікатах видачі"
```

---

### Task 6: Per-variant проєкція деталей (`projectEnrichmentForVariant`)

**Files:**
- Modify: `src/lexicon/lexicon.service.ts` (нова експортована функція + виклик у `prefixMatchToVariant` перед return `:859-893`)
- Test: `src/lexicon/lexicon.service.spec.ts`

**Interfaces:**
- Consumes: `attributedSenseKeys`/`legacySenseIndex`, уже обчислені в `prefixMatchToVariant` (`lexicon.service.ts:818-857`); `SearchSense` тип (`:57-66`); `dedupePreserveOrder` (`:1960-1970`).
- Produces: `export function projectEnrichmentForVariant(enrichment, attributedSenseKeys, legacySenseIndex)` → `{ senses, synonyms, antonyms, examples, senseIndex }` — контракт для мобільних фаз 2–4 (варіант З атрибуцією несе лише свої сенси).

Семантика (узгоджена зі спекою §3 і з мобільним `displayContent`, який має ті самі фолбеки):
1. Є `attributedSenseKeys` → лишаємо тільки сенси з цими ключами.
2. Ключів нема, є `legacySenseIndex` у межах списку → лишаємо той один сенс; `senseIndex` у відповіді стає `0`.
3. Атрибуції нема взагалі → повертаємо enrichment без змін (легасі-поведінка, мобільний фолбек `senses.take(3)` продовжує працювати).
4. Синоніми/антоніми: з відібраних сенсів; якщо у відібраних порожньо — word-level пул (як робить мобільний `ifEmpty`). Приклади: з відібраних сенсів; порожньо — flat-список.

- [ ] **Step 1: Падаючі юніт-тести чистої функції**

```ts
describe('projectEnrichmentForVariant', () => {
  const sense = (senseKey: string | null, patch: Partial<SearchSense> = {}): SearchSense => ({
    senseKey,
    definition: 'To move quickly on foot.',
    partOfSpeech: 'verb',
    tags: [],
    examples: [{ text: 'I run every morning.', translation: 'Я бігаю щоранку.' }],
    synonyms: [],
    antonyms: [],
    ...patch,
  });
  const enrichment = {
    senses: [
      sense('sense_aaa', { synonyms: ['dash'] }),
      sense('sense_bbb', {
        definition: 'An act of running.',
        partOfSpeech: 'noun',
        examples: [{ text: "Let's go for a run.", translation: 'Ходімо на пробіжку.' }],
      }),
      sense(null, { definition: '(intransitive) To move briskly.' }),
    ],
    synonyms: ['gallop', 'ladder', 'execute'],
    antonyms: ['rise', 'riser'],
    examples: [{ text: 'flat example', translation: null }],
  };

  it('з senseKeys лишає тільки атрибутовані сенси та їхні приклади/синоніми', () => {
    const projected = projectEnrichmentForVariant(enrichment, ['sense_aaa'], null);
    expect(projected.senses.map((s) => s.senseKey)).toEqual(['sense_aaa']);
    expect(projected.synonyms).toEqual(['dash']);
    expect(projected.examples).toEqual([
      { text: 'I run every morning.', translation: 'Я бігаю щоранку.' },
    ]);
    expect(projected.senseIndex).toBe(0);
  });

  it('без ключів, але з legacy senseIndex — лишає той сенс і нормалізує індекс у 0', () => {
    const projected = projectEnrichmentForVariant(enrichment, [], 2);
    expect(projected.senses.map((s) => s.definition)).toEqual([
      '(intransitive) To move briskly.',
    ]);
    expect(projected.senseIndex).toBe(0);
  });

  it('у відібраного сенсу нема синонімів — фолбек на word-level пул', () => {
    const projected = projectEnrichmentForVariant(enrichment, ['sense_bbb'], null);
    expect(projected.synonyms).toEqual(['gallop', 'ladder', 'execute']);
    expect(projected.antonyms).toEqual(['rise', 'riser']);
  });

  it('без атрибуції повертає все як є (легасі)', () => {
    const projected = projectEnrichmentForVariant(enrichment, [], null);
    expect(projected.senses).toHaveLength(3);
    expect(projected.examples).toEqual([{ text: 'flat example', translation: null }]);
    expect(projected.senseIndex).toBeNull();
  });
});
```

- [ ] **Step 2: Тести падають**

Run: `npx jest lexicon.service -t "projectEnrichmentForVariant"`
Expected: FAIL — функція не існує.

- [ ] **Step 3: Імплементація чистої функції**

У `src/lexicon/lexicon.service.ts`, поруч з `assembleEnrichment`:

```ts
/**
 * Проєкція word-level enrichment у конкретний варіант перекладу: варіант
 * з атрибуцією несе ЛИШЕ свої сенси (і їхні приклади/синоніми), а не блоб
 * усього слова. Без атрибуції — легасі-поведінка без змін, мобільний фолбек
 * (`senses.take(3)`) продовжує працювати.
 */
export function projectEnrichmentForVariant(
  enrichment: {
    senses: SearchSense[];
    synonyms: string[];
    antonyms: string[];
    examples: { text: string; translation: string | null }[];
  },
  attributedSenseKeys: string[],
  legacySenseIndex: number | null,
): {
  senses: SearchSense[];
  synonyms: string[];
  antonyms: string[];
  examples: { text: string; translation: string | null }[];
  senseIndex: number | null;
} {
  let selected: SearchSense[] = [];
  if (attributedSenseKeys.length > 0) {
    const keys = new Set(attributedSenseKeys);
    selected = enrichment.senses.filter(
      (sense) => sense.senseKey !== null && keys.has(sense.senseKey),
    );
  }
  if (selected.length === 0 && legacySenseIndex !== null) {
    const byIndex = enrichment.senses[legacySenseIndex];
    if (byIndex) selected = [byIndex];
  }
  if (selected.length === 0) {
    return { ...enrichment, senseIndex: legacySenseIndex };
  }
  const senseSynonyms = dedupePreserveOrder(
    selected.flatMap((sense) => sense.synonyms),
  );
  const senseAntonyms = dedupePreserveOrder(
    selected.flatMap((sense) => sense.antonyms),
  );
  const senseExamples = selected.flatMap((sense) => sense.examples);
  return {
    senses: selected,
    synonyms: senseSynonyms.length > 0 ? senseSynonyms : enrichment.synonyms,
    antonyms: senseAntonyms.length > 0 ? senseAntonyms : enrichment.antonyms,
    examples: senseExamples.length > 0 ? senseExamples : enrichment.examples,
    senseIndex: 0,
  };
}
```

- [ ] **Step 4: Вплітання у prefixMatchToVariant**

У `prefixMatchToVariant`, після обчислення `legacySenseIndex` (`:857`) і перед return (`:859`):

```ts
    const projected = projectEnrichmentForVariant(
      { senses, synonyms, antonyms, examples: exampleList },
      attributedSenseKeys,
      legacySenseIndex,
    );
```

У самому return замінити чотири поля і senseIndex:

```ts
      examples: projected.examples,
      senses: projected.senses,
      synonyms: projected.synonyms,
      antonyms: projected.antonyms,
      senseKeys: attributedSenseKeys,
      senseIndex: projected.senseIndex,
```

(Рядки `senseIndex: legacySenseIndex !== null && legacySenseIndex >= 0 ? legacySenseIndex : null` — прибрати: нормалізація тепер у проєкції.)

- [ ] **Step 5: Поведінковий тест на рівні search()**

За патерном сідів `lexicon.runtime-v2.spec.ts` (V2-атрибуція вже тестується там — узяти його спосіб лінкування translation→sense через фейк/сіди; якщо той spec сідить через `FakeLexiconRepository`, повторити його арранж). Обовʼязкові асерти:

```ts
    // слово з 2 сенсами, translation A → sense1, translation B → sense2,
    // translation C — без лінків (легасі)
    const byWord = Object.fromEntries(
      result.results.map((v) => [v.knownWord, v]),
    );
    expect(byWord['бігти'].senses.map((s) => s.senseKey)).toEqual(['sense_run_verb']);
    expect(byWord['пробіжка'].senses.map((s) => s.senseKey)).toEqual(['sense_run_noun']);
    expect(byWord['тривати'].senses).toHaveLength(2); // легасі: повний список
```

(Ключі сенсів у тесті — ті, що згенерує сід; головне — перший варіант не бачить сенсів другого і навпаки, а нелінкований бачить усі.)

- [ ] **Step 6: Тести зелені + повний прогін**

Run: `npx jest lexicon.service && npx jest lexicon.runtime-v2`, потім `npm run lint:backend && npm run typecheck && npm run test:backend`
Expected: PASS. Якщо існуючі тести асертили «всі варіанти несуть повний senses-блоб» — оновити їх під нову семантику (це очікувана зміна контракту, зафіксована в спеці §3/§6).

- [ ] **Step 7: Коміт**

```bash
git add src/lexicon/lexicon.service.ts src/lexicon/lexicon.service.spec.ts src/lexicon/lexicon.runtime-v2.spec.ts
git commit -m "feat(lexicon): варіант пошуку несе деталі лише власного сенсу"
```

---

### Task 7: Жива документація

**Files:**
- Modify: `/Users/artemmoroz/Work/pet/vocabee/vocabee-mobile/docs/17-api-and-data-reference.md` (контракт /v1/search: per-variant senses)
- Modify: `/Users/artemmoroz/Work/pet/vocabee/vocabee-mobile/docs/14-word-details-and-audio.md` (деталі варіанта — скоуплені)
- Modify: `/Users/artemmoroz/Work/pet/vocabee/vocabee-mobile/docs/10-edge-cases-and-open-items.md` (новий кейс: дублікати перекладів одного сенсу + затінення curated)
- Modify: `/Users/artemmoroz/Work/pet/vocabee/CLAUDE.md` (застаріле «0008=поля тренування» → «0022+»)

- [ ] **Step 1: docs/17** — у розділі про /v1/search додати блок `[НОВЕ]`: «Варіант з атрибуцією (`senseKeys`≠[] або legacy `senseIndex`) несе лише власні сенси, їхні приклади й sense-scoped синоніми/антоніми; фолбек на word-level пул, коли sense-списки порожні; без атрибуції — повний блоб (легасі). `senseIndex` тепер вказує в МЕЖАХ масиву `senses` варіанта (для атрибутованого — 0). Форми фільтруються від wiktextract-дебрису.»
- [ ] **Step 2: docs/14** — у секції про деталі варіанта позначити стару поведінку «однаковий блоб у всіх» як `[ЗАРАЗ→виправлено фазою 0]`, нову — `[НОВЕ]`.
- [ ] **Step 3: docs/10** — додати в реєстр кейс: «Легасі translator-кеш дублює target-тексти curated-імпорту; serve тепер віддає curated-рядок; повна історія деградації — форензик Task 8».
- [ ] **Step 4: CLAUDE.md (корінь)** — рядок про `db/schema/` виправити: `0007=bee_balance; [НОВЕ] 0022+=поля тренування (слот 0008 зайнятий referral_support)`.
- [ ] **Step 5: Коміт mobile-доків** (репо vocabee-mobile, main; лише явні шляхи):

```bash
cd /Users/artemmoroz/Work/pet/vocabee/vocabee-mobile
git add docs/17-api-and-data-reference.md docs/14-word-details-and-audio.md docs/10-edge-cases-and-open-items.md
git commit -m "docs: per-variant сенси у /v1/search, curated-пріоритет, фільтр форм (фаза 0)"
```

Кореневий `CLAUDE.md` — поза git (батьківська тека не є репо), просто зберегти файл.

---

### Task 8: Форензик dev-БД + відновлення curated-даних (за участі власника)

**Files:**
- Create: `/Users/artemmoroz/Work/pet/vocabee/review/phase0-dev-forensics.md` (результати)

Це НЕ код-задача: доступ до dev-БД (Coolify) є лише у власника. Виконати після деплою Task 1–6 на dev.

- [ ] **Step 1: SQL-форензик** (виконує власник у psql dev-БД; результат вставити у файл):

```sql
SELECT id, target_text, source, origin, provider_tier, confidence,
       is_primary, quality_score, created_at, deleted_at
FROM translations
WHERE source_lang = 'en' AND target_lang = 'uk'
  AND source_word_id = (SELECT id FROM lexicon_words
                        WHERE lang = 'en' AND normalized = 'run')
ORDER BY target_text, created_at;
```

Читання результату: дублікати `target_text` (seed + translator) ⇒ історія «дубль перекрив curated» — Task 5 уже лікує видачу; рядки лише translator із tier `openai-*` при існуючому receipt промоуту ⇒ історія «confirmTier/інший шлях деградував tier» — Task 4 уже лікує причину; `deleted_at`-мітки на seed-рядках ⇒ окремий шлях (quality-repair soft-delete) — зафіксувати у файлі як нову знахідку для фази 5.

- [ ] **Step 1b: Перевірка lc_ctype перед db:migrate на dev** (знахідка фінального ревʼю): `SELECT 'бігла' ~ '[[:alnum:]]';` мусить повернути `true`. На C-ctype кластері міграція 0022 знесла б УСІ не-ASCII (кириличні) форми — у такому разі СТОП, переписати критерій міграції перед застосуванням. Додатково у форензик: перевірити дублі `(source_word_id, lower(target_text))` серед активних рядків і розглянути partial unique index (Task 5 лікує видачу, не джерело дублів).

- [ ] **Step 2: Реімпорт en_uk** — після деплою фіксів перезалити V2-батчі en_uk через наявний идемпотентний конвеєр (`import-v2` промоутить існуючі рядки заново): скіл `vocabee-upload-translations`; receipts для en_uk у `service/translation-upload-state/development/receipts/primary/en_uk/` чексум-байндні — для форс-перезаливки цього напряму видалити його receipt-файли (лише en_uk!) і запустити аплоадер.
- [ ] **Step 3: Контрольний запит** — `curl "https://dev-api.vocabee.online/v1/search?q=run&speak=uk&learn=en"`: очікувано — варіанти з `source=seed`, у кожного свої сенси, без `no-table-tags`/`glossary` у формах.

---

## Порядок виконання і залежності

Task 1 → 2 → 3 (фільтр: хелпер → wiring → БД) — незалежні від 4–6.
Task 4 → 5 (guard tier-а, потім пріоритет у видачі).
Task 6 — незалежний від 4–5, найбільший.
Task 7 — після 1–6. Task 8 — після деплою на dev, за участі власника.

## Self-review чекліст (виконано при написанні)

- Спека §6 (передумови): п.1 → Task 6; п.2 → Task 4+5+8; п.3 → Task 1+2+3. IPA-втрату в V2-міграції (спека §7.6) фаза 0 не чіпає — це скіл-фаза 1.
- Типи узгоджені: `projectEnrichmentForVariant` консумує `SearchSense`/`dedupePreserveOrder`, визначені в тому самому файлі; `isJunkWordForm` — один контракт у трьох місцях.
- Місця, де точні сигнатури тест-хелперів невідомі з дослідження (scripted-геттери в Task 4/5, сіди runtime-v2 у Task 6), позначені явними інструкціями «звірити з наявним патерном файла» + жорсткими інваріантами, які тест мусить перевірити.
