# Vocabee — Android App Specification

**Platform:** Android (native, Kotlin + Jetpack Compose recommended)
**UI language:** Ukrainian
**Language pair (MVP):** Ukrainian ↔ English (UA → EN)
**Design language:** Custom flat, minimal, spacious; platform-aware but with its own visual identity.

---

## 1. Overview

**Vocabee** is a mobile app for learning foreign words. Its core purpose: let users quickly and effortlessly save unknown words (for example, while reading a book in a foreign language) into their own themed dictionaries, and practice them later.

The product is built around one signature loop:

> **Read → save a word (text or voice, with AI translation) → organize into themed dictionaries → practice later.**

---

## 2. Architecture

The app follows **Clean Architecture** with a clear separation of layers. Nothing is "all in one place" — each concern lives where it belongs, so the codebase stays readable and testable.

### 2.1 Layers

```
┌─────────────────────────────────────────────┐
│  PRESENTATION (UI)                            │
│  Jetpack Compose screens + ViewModels         │
│  - Stateless composables                      │
│  - ViewModel exposes UI state (StateFlow)     │
│  - One-off events via Channel/SharedFlow      │
└───────────────────────┬───────────────────────┘
                        │  calls use cases
┌───────────────────────▼───────────────────────┐
│  DOMAIN (business logic)                      │
│  - Use cases (one responsibility each)        │
│  - Domain models (pure Kotlin, no framework)  │
│  - Repository interfaces                       │
└───────────────────────┬───────────────────────┘
                        │  implemented by
┌───────────────────────▼───────────────────────┐
│  DATA                                         │
│  - Repository implementations                  │
│  - Local: Room (DB), DataStore (prefs)        │
│  - Remote: Retrofit/Ktor (AI + auth)          │
│  - Mappers (DTO ↔ domain ↔ entity)            │
└─────────────────────────────────────────────┘
```

**Dependency rule:** dependencies point inward only. Presentation knows Domain; Data knows Domain; Domain knows nothing about the others.

### 2.2 Recommended stack

| Concern | Choice |
|---------|--------|
| UI | Jetpack Compose |
| Architecture pattern | MVVM + Clean Architecture (UI / Domain / Data) |
| State | `StateFlow` / `MutableStateFlow` in ViewModels |
| Navigation | Jetpack Navigation Compose (single-activity) |
| Dependency injection | Hilt |
| Local database | Room |
| Preferences / settings | DataStore (Preferences) |
| Networking | Retrofit (or Ktor) + kotlinx.serialization |
| Async | Kotlin Coroutines + Flow |
| Speech-to-text | Android `SpeechRecognizer` API |
| Text-to-speech | Android `TextToSpeech` API |

### 2.3 Suggested package structure (feature-based)

```
com.vocabee
├── app/                      // Application, DI graph, MainActivity, navigation host
├── core/
│   ├── ui/                   // theme, design system, reusable composables
│   ├── common/               // Result wrapper, extensions, dispatchers
│   └── data/                 // base remote/local infrastructure
├── feature/
│   ├── onboarding/           // splash, onboarding slides, language pick
│   ├── auth/                 // login / sign up / skip
│   ├── dictionaries/         // home grid, create-dictionary sheet
│   ├── dictionarydetail/     // word list, AI context
│   ├── addword/              // full-screen add-word flow (text + voice + AI)
│   ├── practice/             // flashcards (MVP 2)
│   └── profile/              // settings, languages, stats
```

Each `feature/*` module is self-contained with its own `ui`, `domain`, and `data` packages, so screens stay isolated and easy to read.

### 2.4 Design system module

All visual primitives live in `core/ui` (colors, typography, spacing, shapes, reusable components: `VocabeeButton`, `WordRow`, `DictionaryCard`, `BottomSheetScaffold`, etc.). Screens compose these primitives — no ad-hoc styling scattered across the app.

---

## 3. Domain Entities

| Entity | Fields |
|--------|--------|
| **User** | id, name, email, nativeLanguage, learningLanguage (default), notificationsEnabled, darkTheme |
| **Dictionary** | id, name, themeColor, languagePair (defaults to user's), wordCount, lastUsedAt, createdAt |
| **Word** | id, dictionaryId, original, transcription, translation, contexts (list), createdAt |
| **WordContext** | exampleSentence, exampleTranslation, isAiGenerated |

---

## 4. Navigation

Single-activity app with a **bottom navigation bar (3 tabs)** available from all primary screens:

1. **Словники (Dictionaries)** — home, list of dictionaries (start destination).
2. **Тренування (Practice)** — flashcards (fully designed; logic is MVP 2).
3. **Профіль (Profile)** — user settings.

Full-screen flows (onboarding, auth, add-word overlay) are presented **above** the tab bar and hide it.

---

## 5. Launch & Onboarding Flow

### 5.1 Splash screen
- Shown on launch; Vocabee logo on purple background.
- Auto-advances after 1–2 s (decides destination: onboarding vs. home).

### 5.2 Onboarding (2–3 slides)
- Intro to the core idea: "save words while reading" → "organize themed dictionaries" → "practice later".
- Each slide: illustration + title + subtitle.
- Page-dot indicator, **Skip** and **Next / Start** buttons.
- Shown only on first launch (persisted in DataStore).

### 5.3 Login / Sign up
- Fields: email + password.
- Buttons: "Continue with Google", "Continue with Facebook".
- Toggle between Login ↔ Sign up.
- **Skip** button — app is usable without an account.
- **Requirement:** login is NOT mandatory for core usage in MVP.

### 5.4 Language selection (onboarding)
- User picks **native language** and **learning language** (UA → EN).
- Clear cards/pickers.
- **Requirement:** this sets the default language pair used everywhere afterwards.

---

## 6. Screen: Dictionaries (Home)

### 6.1 With dictionaries
- Grid/list of dictionary cards.
- Each card: theme name, theme color, word count, "today / last used" badge.
- **Sorting:** by last used (newest on top).
- **FAB (+)** to create a new dictionary.

### 6.2 Empty state
- Friendly illustration + hint text + **"Create your first dictionary"** button.

### 6.3 Actions
| Action | Result |
|--------|--------|
| Tap a dictionary card | Navigate to Dictionary Detail |
| Tap FAB (+) | Open create-dictionary bottom sheet |

---

## 7. Create Dictionary (bottom sheet)

- Opens over the home screen (no navigation to a new screen).
- Fields: **dictionary name** + **theme color** (brand-palette swatches).
- **Create** button.
- **Requirement:** new dictionary automatically inherits the default language from the profile — no extra language step.
- On create: sheet closes, new card appears instantly on home.

---

## 8. Screen: Dictionary Detail

### 8.1 Structure
- Header: theme name + word count.
- List of saved words. Each word row contains:
  - original word;
  - transcription / pronunciation;
  - translation;
  - speaker icon (text-to-speech);
  - **AI context** — example sentence with the word + its translation, in a subtle styled block with a light AI tag.
- Pinned **"+ Додати слово (Add word)"** flat button at the bottom.

### 8.2 Empty state
- If the dictionary has no words: soft hint guiding to the add button.

### 8.3 Change dictionary language
- Available as a **secondary** action (small control in the header).
- Defaults to the profile language; override is optional and de-emphasized.

---

## 9. Add Word (full-screen flow) — signature interaction

### 9.1 Launch & animation
- Trigger: tap **"+ Add word"** in Dictionary Detail.
- **Animation (container transform):** the button-pill **morphs** and expands to full screen — the button *becomes* the surface, not replaced by a new screen.
  - Duration ~300–400 ms, ease-out.
  - Purple fill expands from the button position, then transitions to the white overlay background.
- **Content entrance (staggered, after ~60% of the morph):**
  - search field slides down from top (fade + slide);
  - microphone button appears centered (fade + scale up);
  - ~50–80 ms delay between them.

### 9.2 Text input
- Tap field → keyboard appears.
- As the user types, **AI translation suggestions** appear in real time below the field.
- Each result row: **word + translation + "+" button**.
- AI suggests translations even for words not in any dictionary.
- Each result animates in (fade + slide up, ~150 ms).

### 9.3 Voice input
- Tap/hold the mic button → start recording (Android `SpeechRecognizer`).
- **Recording state:**
  - animated waveform (orange `#F76400`);
  - "Слухаю… (Listening…)" indicator;
  - "торкнись, щоб зупинити (tap to stop)" hint.
- Speech converts to text → the same suggestion list appears.
- Tapping mic again → re-record (if speech-to-text misheard).

### 9.4 Adding a word
- Tap the "+" on a result row:
  - row gives a quick scale-pulse;
  - "+" briefly becomes a checkmark;
  - the word visually "drops" into the dictionary (subtle downward fade).
- **Requirement:** the overlay stays open for fast consecutive additions.

### 9.5 Word already exists
- If the typed/spoken word is already saved in this dictionary — show its saved translation in an "already added" state instead of a duplicate add option.

### 9.6 Dismiss
- Closing reverses the animation: the full-screen surface **collapses back into the pill** (~300 ms); content fades out first.
- User returns exactly where they were, no reload.

---

## 10. Screen: Practice

- **Fully designed**, but functionally **MVP 2**.
- Concept: flashcards for review.
  - card shows the word;
  - tap/flip reveals translation + context;
  - "know / don't know" actions;
  - light progress indicator at the top.
- **Requirement:** do not implement complex spaced-repetition logic in the first iteration; the screen must look finished and on-brand.

---

## 11. Screen: Profile

### 11.1 Content
- Avatar + name + email.
- Minimal visual stats row: streak / saved words / tests.
- **Language settings (source of truth):**
  - "Я розмовляю (I speak)" — native language;
  - "Я вивчаю (I'm learning)" — default language for new dictionaries.
- Notifications toggle.
- Dark theme toggle (first iteration — visual toggle only, full theming is MVP 2).
- Access to: edit profile, help, invite friends.

### 11.2 Language logic
- The language chosen here is the **default** for all new dictionaries.
- Changing a single dictionary's language does not affect the profile default.

---

## 12. AI Functionality

| Function | Behavior |
|----------|----------|
| Translation suggestions | On demand during text/voice input, including words outside any dictionary |
| Context generation | For saved words: example sentence + its translation |
| AI-content marking | A light visual tag (sparkle / AI label) so the content reads as a helpful hint, not noise |

> **Implementation note:** for the design prototype, AI suggestions are simulated with realistic, instant, stable data. Real model integration comes during development. The architecture isolates AI behind a repository interface, so the simulated source can be swapped for a real backend without touching the UI.

---

## 13. Edge Cases & Error States

| Case | Expected behavior |
|------|-------------------|
| No internet (AI/auth) | Show a non-blocking message; allow offline browsing of saved dictionaries/words |
| Microphone permission denied | Explain why it's needed; offer to open settings; fall back to text input |
| Speech not recognized | Show "didn't catch that" + easy re-record |
| AI found no translation | Show "no suggestion" state + allow manual entry |
| Duplicate dictionary name | Allow, or warn gently (no hard block) — to be decided |
| Empty input submitted | Disable add action until there's valid input |

---

## 14. Non-Functional Requirements

- **UX principle:** minimum actions, maximum clarity. Spacious, no visual noise, correct accents.
- **One clear primary action** per screen.
- **Consistent components** (cards, buttons, fields, tab bar, sheets) across the app, sourced from the design-system module.
- **Bottom sheets** used to avoid unnecessary navigation (create dictionary, change language).
- **State handling:** every screen has explicit loading / content / empty / error states.
- **Offline-first reads:** saved data is available without a network connection (Room as source of truth).
- **Accessibility:** sufficient touch targets, content descriptions for icons, readable contrast.

---

## 15. Palette & Typography

**Colors**
- Primary purple: `#4F46E5` (deep `#410FA3`)
- Secondary blue: `#5B7BFE`, light tint `#E0E7FF`
- Accents: yellow `#FFCC00`, orange `#F76400` (used sparingly — streaks, CTAs, highlights)
- Neutrals: white `#FFFFFF`, text `#111827`, muted `#6B7280` / `#9CA3AF`

**Typeface:** Manrope (geometric, friendly, full Cyrillic support).

---

## 16. MVP Scope

| Priority | Includes |
|----------|----------|
| **MVP 1** | Splash, onboarding, login, language selection, Dictionaries home, create dictionary, dictionary detail, add word (text + voice + AI suggestions), profile |
| **MVP 2** | Full Practice (flashcards with review logic), full dark theme, real AI backend |
