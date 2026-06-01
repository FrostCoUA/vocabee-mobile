# Vocabee Application Description

## Project Name

Vocabee

## Short Description

Vocabee is an app for people who are learning vocabulary in a foreign language. It helps users create personal vocabulary topics, add new words quickly, get translations, and practice them later.

## Purpose

The main goal of Vocabee is to make vocabulary collection and practice as simple as possible. The app is designed for learners who regularly meet unfamiliar words and want a fast way to save, translate, and review them.

## Target Audience

Vocabee is intended for people who are learning a foreign language and want a simple tool for building and reviewing their vocabulary.

## Core Functionality

Users can:

- Add words to their personal vocabulary
- Group vocabulary into topics
- Enter words using voice input
- Enter words manually with the keyboard
- Get translations for unfamiliar words
- Practice saved words
- Check whether they have memorized the words
- Configure their known language and learning language in the profile

## User Profile

In the user profile, the user can select:

- One language they already know
- One language they are currently learning

These language settings are used to support translation and vocabulary practice.

## Home Screen

On the home screen, the user sees a list of vocabulary topics they are currently learning.

If the user has not created any topics yet, the home screen shows an empty state.

When topics already exist, they are displayed as a grid of dictionary cards.

Each dictionary card contains:

- A small icon in the top-left corner that visually represents the topic
- The topic name, for example "Work", "Food", or "Cinema"
- The number of saved words in that topic
- A "Start" action with an arrow, used to begin practicing that topic

The card design is clean and minimal: white rounded cards on a light background, with compact typography and enough spacing to make each topic easy to scan.

The home screen also includes an "Add new topic" card. It is shown in the same grid as the dictionaries, but uses a dashed border and a centered plus icon with the label "New topic". This card is used to create another vocabulary topic.

## Navigation And Top Bar

Every page should have a top bar.

The top bar includes:

- The app icon on the left, represented by a bee
- The current page title next to the app icon
- An optional action on the right side, which can be either an icon or a text button

## Key Advantage

One of Vocabee's main advantages is fast word entry. Users can add new words using voice input, which makes vocabulary creation quicker and more convenient. Keyboard input is also available for users who prefer manual entry or need more precise control.

## Main Product Goal

The author's main focus is ease of use. Vocabee should let a language learner quickly enter an unfamiliar word, receive its translation, save it into a topic, and later test themselves to see if they remember it.

## Vocabulary Storage Roadmap

Vocabulary topics and words are stored per user. Until authorization exists, the app uses a local string user key. Topic and word records should keep local metadata that can support future backend synchronization: created/added timestamps, updated timestamps, and a sync status such as pending create, pending update, synced, or pending delete.

Future storage should add word usage examples after the first Room schema is stable. The planned model is a separate table linked to a word, with multiple example sentences per word. For verbs, the same feature should support storing several verb forms, including first, second, and third forms where the target language needs them. This should be implemented as a real persisted feature later, not as hardcoded UI text.
