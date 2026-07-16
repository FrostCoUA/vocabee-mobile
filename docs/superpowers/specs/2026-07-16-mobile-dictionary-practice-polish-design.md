# Mobile dictionary and practice polish

Date: 2026-07-16  
Status: approved design

## Goal

Polish the dictionary detail and training flows so that compact headers are aligned,
incomplete translations cannot create empty dictionary cards, active practice is a
focused full-screen experience, and context-practice content remains readable on
narrow screens.

This is a targeted mobile change. It keeps the existing root routes and storage
model, introduces small testable policy/layout helpers, and does not require a
backend or database migration.

## Scope

The change covers:

- compact dictionary-detail header alignment;
- completeness validation and enrichment before saving a translation;
- safe handling of previously saved incomplete translations;
- full-screen classic and context sessions, including exit confirmation;
- classic card/CTA placement and clipped touch indications;
- equal-height practice-mode cards;
- adaptive context answer chips;
- simplification of the context-practice deck to recognition and matching.

## 1. Dictionary detail header

### Current problem

`DetailHeader` positions the collapsing title with a manually calculated top offset
that is independent of the compact controls row. Font metrics can therefore leave
the title visibly above or below the back and language controls.

### Design

- The compact title anchor belongs to the same 40 dp controls row as the back button
  and language pill and is vertically centered in that row.
- The expanded title keeps its current visual position.
- Collapse animation interpolates between the expanded title anchor and the compact
  row anchor; it no longer derives the compact vertical position from a magic top
  offset.
- Horizontal placement and the current header artwork remain unchanged.

## 2. Complete translations only

### Save-ready policy

Autocomplete continues to show lean prefix suggestions, but a translation can be
persisted only when all of the following are available:

- non-blank source and translated text;
- non-blank IPA;
- non-empty part of speech;
- a valid attributed sense (`senseIndex` resolves to a sense);
- a non-blank definition for that sense;
- at least one non-blank example for that sense.

Forms, synonyms, and antonyms are optional enrichment and do not block saving.

### Add flow

1. The user taps `+` on a translation option.
2. If the option is already save-ready, it is added normally.
3. Otherwise the client performs an exact lexicon search for the selected source
   word and matches the selected translated text in the exact result.
4. The client adds the matched option only if the enriched candidate passes the
   save-ready policy.
5. If no complete match is returned, the add overlay stays open and shows the
   transient message `Не вдалося завантажити повну інформацію. Спробуйте ще раз.`

The domain/store add boundary applies the same policy defensively so that another
presentation caller cannot persist an incomplete option directly.

### Existing incomplete records

Previously saved records are never deleted automatically. During a dictionary-detail
screen lifetime, the client silently performs at most one exact enrichment attempt
per incomplete record and updates successful matches through the existing enrichment
path. Until enrichment succeeds:

- the base word/translation row remains visible to avoid data loss;
- the expand affordance and empty rounded details container are hidden;
- a details object containing only non-rendered metadata such as part of speech is
  not treated as renderable content.

## 3. Full-screen practice session

### Chrome state

Practice setup remains the root `Practice` tab and continues to show the bottom tab
bar. Starting either classic or context practice marks the practice session as
active in the parent app chrome state. While the session or result is active, the
bottom tab bar is hidden.

The existing route remains in place; a separate navigation destination is not
introduced. `PracticeScreen` reports session-active changes to `MainApp`, and it
clears the flag when leaving or disposing the session.

### Exit behavior

- Active classic and context headers show a close icon in the top-right corner.
- The close icon and system Back invoke the same exit request.
- An active round opens a bottom sheet titled `Перервати тренування?` with actions
  `Продовжити` and `Перервати`.
- The body explains that the round will end while already recorded answers remain
  saved.
- `Продовжити` dismisses the sheet without changing the card or progress.
- `Перервати` returns to setup, preserving the selected practice mode and selected
  dictionaries.
- On an already completed result screen, close/back returns to setup directly because
  there is no active round to interrupt.

The practice-level Back handler takes precedence over the app-level exit sheet while
a session is active.

### Classic layout and interaction

- The card fills all available space between the progress header and the bottom CTA,
  while retaining its horizontal margin and rounded shape.
- The bottom CTA is anchored immediately above the system navigation-bar inset.
- In `Далі`, the label precedes the circular chevron icon.
- Interactive cards and CTAs use the clickable `Surface` overload with the same
  rounded shape, so ripple never paints outside the border.

## 4. Practice-mode cards

- Both mode cards are equal in height. Their row equalizes to the tallest intrinsic
  card instead of allowing each card to size independently.
- Both cards reserve exactly two text lines for the description.
- Descriptions use `maxLines = 2` and end ellipsis when longer.
- A one-line description still occupies the same two-line content region.
- The title stays on one line using the existing bounded shrink behavior.

## 5. Context answer layout

The fixed two-column `chunked(2)` layout is replaced with an adaptive answer flow.

- A chip first measures to the width required by its text, horizontal padding, and
  optional correct/wrong icon.
- Chips stay on the current row only when the complete next chip fits.
- A chip that does not fit moves intact to the next row.
- A chip wider than an otherwise empty row uses the full available width and grows
  vertically to two or more lines.
- Answer text is never ellipsized and is always fully visible.
- Chips keep a 48 dp minimum height and rounded pill-like corners; multi-line content
  increases height rather than reducing touch target or text size.

## 6. Context-practice formats

### Removed presentation

- Remove the right-side format labels `впізнавання` and `вживання`; the direction
  badge on the left remains.
- Remove the pre-answer translation hint used by the UK-to-EN exercise.
- Remove the post-error `ПІДКАЗКА` panel and its confused-pair sentence.

### Removed format

The recall/self-assessment format is removed completely from context practice:

- remove `ContextCard.Recall` and its deck-selection threshold;
- remove the flip-card UI and `Знаю` / `Не знаю` actions;
- remove its progress/result branches and dead supporting code.

Context practice retains:

- recognition with visible answer options;
- UK-to-EN option selection for mature pairs;
- matching for eligible mature multi-sense groups.

For an EN-to-UK recognition pair, wrong options come only from other distinct,
trainable senses of the same source word. If no honest wrong option exists, that pair
is omitted rather than padded with an unrelated translation. Matching is selected
before recognition, so a matching-eligible group remains trainable even when one of
its standalone pairs lacks a distractor. Setup pair counts and empty states use the
same eligibility policy as the deck, so the UI never promises unbuildable cards.

For UK-to-EN option selection, at least one distinct source-word distractor must be
available; otherwise the card is omitted.

## 7. State, errors, and persistence

- Knowledge updates already recorded before interruption remain persisted through
  the existing adjustment use case.
- Dismissing the interruption sheet does not rebuild or reshuffle the deck.
- Returning to setup after interruption clears only the current deck/session state.
- Exact translation enrichment does not spend an additional user search coin; it is
  completion of the selected paid/allowed search action.
- Network or lexicon failures do not create partial saved entries.

## 8. Test strategy

Add or extract pure, internal policies so behavior can be covered without relying on
pixel tests:

- save-ready candidate accepts complete attributed data and rejects missing IPA,
  part of speech, definition, example, or valid sense attribution;
- an incomplete autocomplete option is resolved through exact enrichment before add;
- a failed/incomplete exact result is not persisted;
- renderability rejects metadata-only details and prevents an empty details block;
- practice app chrome is visible for setup and hidden for active/result states;
- context deck contains no recall cards;
- pairs without honest distractors are omitted and setup counts match deck
  eligibility;
- context deck anti-repeat and matching behavior remain intact.

Build verification:

- `./gradlew :app:testDebugUnitTest`;
- `./gradlew :app:assembleDebug`;
- Android visual/interaction check for compact header alignment, full-screen practice,
  interruption sheet, CTA placement, clipped ripple, equal mode-card height, and
  narrow-screen multi-line answer chips.

## 9. Documentation updates

Update the living behavior documentation after implementation:

- `docs/01-screens.md`;
- `docs/11-practice-training.md`;
- `docs/12-motion-and-interaction-brief.md`;
- `docs/13-add-word-and-ai-search.md`;
- `docs/14-word-details-and-audio.md`.

## Out of scope

- introducing a dedicated practice navigation route;
- changing the training economy or D10 knowledge increments;
- adding new backend lexicon providers or schema fields;
- deleting legacy user words automatically;
- redesigning classic practice content beyond the requested layout and interaction
  fixes.

## Acceptance criteria

1. `Common` and any other compact dictionary title are vertically aligned with the
   compact controls row.
2. A translation without the save-ready data cannot be newly persisted.
3. A legacy incomplete word never expands into an empty details container.
4. Setup shows the bottom tab bar; active and completed practice do not.
5. Close/back during a round opens the interruption sheet and preserves the session
   when cancelled.
6. The practice card fills available content height, `Далі` sits above the system
   navigation inset, and its arrow is after the label.
7. Card/button ripple stays within rounded bounds.
8. Practice-mode cards have equal height and a two-line ellipsized description area.
9. Context answers wrap as whole chips and display every answer in full.
10. Context practice shows no hint blocks, right-side format label, recall flip card,
    or `Знаю` / `Не знаю` actions.
11. Pairs without honest distractors are not counted or placed in the deck.
