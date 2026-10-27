# Фаза 1: атомарність сенсів у скілах генерації — план імплементації

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Скіли генерації перекладів гарантують інваріант «переклади одного сенсу взаємозамінні в прикладовому реченні», а валідатор ловить порушення — щоб нові дані були придатні для сенс-групування (спека 2026-08-20-sense-group-dictionary-design.md, §7).

**Architecture:** Один блок правил «Sense atomicity gate» вставляється в три SKILL.md (primary, v2-migration, secondary) у `~/.codex/skills` (джерело істини); чотири нові перевірки додаються у `service/scripts/validate_reviewed_batch.py` (жорсткі — на дублікати, мʼякі/strict — на umbrella-глоси, порожні сенси, per-sense primary, щоб не зламати реваліацію легасі-батчів); потім набір клонується в `~/.claude/skills` за конвеєром власника.

**Tech Stack:** Markdown (SKILL.md), Python 3 (валідатор, без тест-фреймворку — доказ через фікстури).

## Global Constraints

- Джерело істини скілів: `~/.codex/skills/`; клон: `~/.claude/skills/` із замінами `.codex`→`.claude`, `AGENTS.md`→`CLAUDE.md`, «Codex»→«agent» (конвеєр власника).
- Мова нових правил у SKILL.md — англійська (як сусідній текст цих файлів).
- Валідатор: нові перевірки НЕ мають ламати реваліацію наявних легасі-батчів — жорсткими за замовчуванням є лише перевірки на дублікати; umbrella/порожні-сенси/primary — warning за замовчуванням, error під прапорцем `--strict-atomicity`.
- Git-комітів немає: `~/.codex` і `~/.claude` — поза git; `service/` — теж поза git (батьківська тека не репо).
- Точні рядки-якорі можуть зсунутися — шукати за наведеними фрагментами тексту.

---

### Task 1: Блок правил у трьох SKILL.md

**Files:**
- Modify: `~/.codex/skills/vocabee-primary-translate/SKILL.md`
- Modify: `~/.codex/skills/vocabee-translate-v2-migration/SKILL.md`
- Modify: `~/.codex/skills/vocabee-secondary-translate/SKILL.md`

**Interfaces:**
- Produces: заголовок `### Sense atomicity gate` — на нього посилається Task 2 (валідатор згадує правило в docstring) і Task 3 (клон).

- [ ] **Step 1: Вставити у `vocabee-primary-translate/SKILL.md`** — одразу ПІСЛЯ секції «Translation-to-sense gate» (шукати заголовок або текст «Every translation must carry one or more reviewed stable `senseKeys`», ~рядок 337) — новий блок ДОСЛІВНО:

```markdown
### Sense atomicity gate

These rules make sense-level grouping sound: every translation attributed to a
sense must be interchangeable with its siblings in that sense's example
sentence.

- One sense = one meaning. A definition that enumerates distinct meanings
  ("To move quickly on foot, operate, or manage something") is an umbrella
  gloss — split it into separate senses, each with its own definition and its
  own example.
- Interchangeability check: every translation attributed to a sense must be a
  natural translation of that sense's example sentence(s). If substituting the
  translation into the example's translation reads wrong, the attribution is
  wrong — split the sense or move the translation.
- Sense dedup: two candidate senses with the same meaning and the same example
  sentence are ONE sense. Merge them under a single senseKey and attribute all
  their translations to it. Never emit near-duplicate senses or duplicate the
  same example text under two senseKeys.
- Per-sense primary: among the translations attributed to one sense, mark
  exactly one as `isPrimary: true` (the most natural, most common rendering);
  order the rest by descending confidence. A record may therefore carry one
  primary per sense, not one per record.
```

- [ ] **Step 2: Вставити у `vocabee-translate-v2-migration/SKILL.md`** — у секцію про генерацію/regeneration полів запису (шукати згадку `fullRecord` або «generatedFields»); той самий блок з Step 1 плюс один додатковий рядок наприкінці списку:

```markdown
- Regeneration must never collapse meanings: when regenerating a record
  (`v2Migration.generatedFields: ["fullRecord"]`), preserve every distinct
  meaning of the reviewed V1 source as its own sense, preserve `ipa`, and
  preserve every reviewed example (the "run" regeneration that dropped the
  керувати example and ipa is the anti-pattern).
```

- [ ] **Step 3: Вставити у `vocabee-secondary-translate/SKILL.md`** — у секцію правил генерації батчів (шукати згадки senseKey/examples); той самий блок з Step 1 без змін.

- [ ] **Step 4: Перевірити вставки** — `grep -c "Sense atomicity gate" ~/.codex/skills/vocabee-primary-translate/SKILL.md ~/.codex/skills/vocabee-translate-v2-migration/SKILL.md ~/.codex/skills/vocabee-secondary-translate/SKILL.md` → по 1 у кожному.

---

### Task 2: Перевірки у валідаторі

**Files:**
- Modify: `/Users/artemmoroz/Work/pet/vocabee/service/scripts/validate_reviewed_batch.py`
- Create (фікстура): `/private/tmp/claude-501/-Users-artemmoroz-Work-pet-vocabee/d7384e2e-a571-4408-a5b5-465406c0cb14/scratchpad/atomicity-fixture.jsonl`

**Interfaces:**
- Consumes: наявну структуру валідатора (спершу прочитати файл: як він ітерує записи, як репортить помилки/ворнінги, чи має argparse).
- Produces: прапорець `--strict-atomicity`; функція `check_sense_atomicity(record) -> tuple[list[str], list[str]]` (errors, warnings).

- [ ] **Step 1: Прочитати валідатор** і зʼясувати: механізм репорту помилок (список? лічильник? формат рядка), наявність argparse, як дістаються senses/translations/examples запису (V1 і V2 формати — перевірки застосовувати ЛИШЕ до V2-записів: `formatVersion == "v2"` або наявність `senseKey` у senses).

- [ ] **Step 2: Додати функцію** (адаптувати сигнатуру під стиль файла, логіка — дослівно):

```python
def check_sense_atomicity(record):
    """Sense atomicity gate (див. SKILL.md): дублікати — errors,
    umbrella-глоси / порожні сенси / per-sense primary — warnings
    (errors під --strict-atomicity)."""
    errors, warnings = [], []
    senses = record.get("senses") or []
    if not any(s.get("senseKey") for s in senses):
        return errors, warnings  # V1-запис — поза скоупом
    word = record.get("word", "?")

    def norm(text):
        return " ".join((text or "").strip().lower().split())

    # 1. Дублікати нормалізованих дефініцій (error)
    seen_defs = {}
    for s in senses:
        d = norm(s.get("definition"))
        if d and d in seen_defs:
            errors.append(f"{word}: duplicate sense definition {d!r}")
        seen_defs[d] = True

    # 2. Один і той самий приклад під двома senseKey (error)
    seen_examples = {}
    for ex in record.get("examples") or []:
        t = norm(ex.get("text"))
        key = ex.get("senseKey")
        if t and t in seen_examples and seen_examples[t] != key:
            errors.append(f"{word}: example {t!r} attributed to two senses")
        seen_examples.setdefault(t, key)

    translations = record.get("translations") or []
    by_sense = {}
    for tr in translations:
        for sk in tr.get("senseKeys") or []:
            by_sense.setdefault(sk, []).append(tr)

    for s in senses:
        sk = s.get("senseKey")
        attributed = by_sense.get(sk, [])
        # 3. Umbrella-глос: " or " у дефініції при >=2 перекладах (warning)
        if " or " in norm(s.get("definition")) and len(attributed) >= 2:
            warnings.append(
                f"{word}: possible umbrella gloss "
                f"{norm(s.get('definition'))!r} with {len(attributed)} translations"
            )
        # 4a. Сенс без жодного перекладу (warning)
        if not attributed:
            warnings.append(f"{word}: sense {sk} has no attributed translations")
        # 4b. Per-sense primary: >1 primary — warning; 0 primary — warning
        primaries = [tr for tr in attributed if tr.get("isPrimary")]
        if len(primaries) > 1:
            warnings.append(f"{word}: sense {sk} has {len(primaries)} primary translations")

    return errors, warnings
```

- [ ] **Step 3: Вплести у головний цикл**: викликати для кожного запису; errors додаються до наявного механізму помилок; warnings друкуються з префіксом `WARN`; під `--strict-atomicity` warnings стають errors. Додати прапорець у argparse (або аналог, якщо парсинг інший).

- [ ] **Step 4: Довести red/green фікстурою**: створити `atomicity-fixture.jsonl` з двома V2-записами — чистим (усі перевірки мовчать) і брудним (дубль-дефініція, дубль-приклад під двома ключами, umbrella-глос із 2 перекладами, сенс без перекладів, 2 primary в одному сенсі; metadata як у справжніх батчів: `needsReview: false`, `review.status: "approved"`). Запустити валідатор на фікстурі без прапорця (очікувано: 2 errors + 4 warnings по брудному запису, чистий — чистий) і з `--strict-atomicity` (очікувано: 6 errors). Якщо валідатор вимагає інші обовʼязкові поля запису (rank, origin тощо) — доповнити фікстуру до прохідності РЕШТИ перевірок, щоб нові було видно ізольовано.

- [ ] **Step 5: Реваліація реального батча** — прогнати валідатор БЕЗ прапорця на `/Users/artemmoroz/Work/pet/vocabee/service/translation-import/v2/en_uk/core_0251_0300_v2.jsonl`: помилок бути не повинно (warnings — очікувані, зокрема umbrella на «run»); це доказ, що легасі-реваліація не зламана.

---

### Task 3: Клон у ~/.claude

**Files:**
- Modify: `~/.claude/skills/vocabee-primary-translate/SKILL.md`, `~/.claude/skills/vocabee-translate-v2-migration/SKILL.md`, `~/.claude/skills/vocabee-secondary-translate/SKILL.md`

- [ ] **Step 1:** Для кожного з трьох скілів скопіювати оновлений SKILL.md з `~/.codex/skills/...` у `~/.claude/skills/...` із замінами в тексті: `.codex`→`.claude`, `AGENTS.md`→`CLAUDE.md`, слово «Codex» (як назва агента)→«agent» (sed або ручна правка; перевірити, що заміни не зачепили шляхи, які й мають лишитись).
- [ ] **Step 2:** `diff` кожної пари файлів: відмінності мають бути ЛИШЕ у зазначених замінах (і в раніше наявних розбіжностях клону, якщо такі були — їх зберегти; якщо клон і джерело розійшлися суттєво — перенести лише новий блок «Sense atomicity gate» + migration-рядок, не перезаписуючи решту, і зазначити розходження у звіті).

## Self-review чекліст

- Спека §7: п.1–2 → Task 1 Step 1; п.3 → Task 1 (per-sense primary) + Task 2 (4b); п.4 → Task 2; п.5 → Task 1 Steps 2–3; п.6 (регенерація верхівки) — це фаза 5, не тут.
- Легасі-безпека: жорсткі лише дублікат-перевірки; Task 2 Step 5 доводить на реальному батчі.
- Контракт не міняється: нових полів у V2-форматі немає (per-sense primary використовує наявний isPrimary).
