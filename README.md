# Inkwell (Android)

Inkwell is an Android-first long-form writing app inspired by Scrivener, focused on structured drafting, notes, and distraction-managed writing sessions.

## Current repo status

This repository now includes:
- A **bootstrap Android app scaffold** using Kotlin + Jetpack Compose (`app/`).
- A **functional MVP prototype screen** with:
  - 3-pane layout (outline/editor/notes)
  - mode switching (Standard, Sprint, Editing)
  - an unobtrusive writing timer with sprint constraints
- A **delivery backlog** in `docs/MVP_BACKLOG.md` that breaks the vision into implementation stories.

## Product vision

Build a tablet/phone writing studio for Android 10+ that supports:
- A Scrivener-like multi-pane workspace.
- Project-oriented writing (folders, chapters, scenes, notes).
- Lightweight writing timers for focus sessions.
- Broad import/export and cloud sync.
- Optional e-ink-friendly experience.

## Platform and technical baseline

- **Target OS:** Android 10+
- **Primary stack:** Kotlin + Jetpack Compose
- **Architecture:** Offline-first, single-project local database, background sync queue
- **Storage:** Room (metadata/tree/notes), file-backed document blobs per project
- **Sync provider (v1):** Google Drive via OAuth (user-consented only)
- **Document model:** Tree-based project binder with ordered nodes (`Folder`, `Document`)

## Core UX specification (v1)

### 1) Three-pane editor layout (default)

Default split:
- **Pane 1 (Outline):** 20% (resizable, collapsible)
- **Pane 2 (Editor):** 60% (resizable, can be full-screen for distraction-free)
- **Pane 3 (Notes):** 20% (resizable, collapsible)

#### Pane 1: Outline/Binder
- Hierarchical project tree with folders and text documents.
- Drag-and-drop reorder and nesting.
- Quick add actions for folder/document.
- Context actions: rename, duplicate, move, archive/delete.
- Collapsible panel state persisted per project.

#### Pane 2: Writing/Editing surface
Rich text (WYSIWYG baseline):
- Paragraph styles: Title, Heading, H1–H4, Body
- Inline styles: bold, italic, underline, superscript, subscript, strikethrough
- Alignment: left, center, right, justified
- Line spacing presets
- Font picker (app-safe fonts)
- Bulleted and numbered lists
- Ornamental break insertion

Additional behavior:
- Distraction-free mode = editor-only layout.
- Selection-based formatting toolbar (compact and floating where possible).

#### Pane 3: Document-linked notes
- Notes are bound to currently selected document.
- Plain-text entry with URL auto-linking.
- Independent scroll position per document.
- Optional panel collapse.

## Writing modes (v1)

### Standard Writing Mode
- Optional Pomodoro-like timer.
- Duration in 5-minute steps from **5 to 60 minutes**.
- Timer auto-starts on first keypress in current session (if enabled).
- At zero: modal options `Continue`, `Restart same`, `Change duration`.
- Timer is visible but unobtrusive (small top/bottom chip).

### Sprint Writing Mode
- Allowed durations: **5, 10, 15, 20, 25, 30** minutes.
- Minimal interface: current paragraph focus + timer.
- Suppress binder/notes and most formatting controls until sprint ends or user exits.

### Editing Mode
- Explicit non-timed mode reserved for revision workflows.
- Timer-free and feature-extensible (tracked changes, comments, etc. in future).

## Import/Export and interoperability

### Export targets (v1 goal)
- Single document export and full-project compile export.
- Formats: `.docx`, `.pdf`, `.txt`, `.rtf`, `.md`, `.epub`
- Full-project export should support logical breaks (chapter/scene) based on binder hierarchy.

### Import targets (v1 goal)
- `.txt`, `.md`, `.docx`, `.rtf`, `.html`, `.epub`
- `.scriv` import as **best-effort** (if format limitations prevent full fidelity, import structure + plain text first).

### Markdown bridge
- `Copy as Markdown`
- `Paste from Markdown`
- Supported in any text document.

## Google Drive sync

- User-initiated sign-in and consent only.
- Project-level sync with conflict-safe revision strategy.
- Manual sync and background periodic sync options.
- No monetization or unnecessary identity capture.

## Templates

Planned:
- Built-in templates: `Blank Document`, `Short Story`, `Novel`
- User can save any project as reusable template.
- Template preserves binder structure, metadata, and starter notes.

## First-run setup

Initial onboarding choices:
- Preferred orientation (portrait / landscape)
- Default writing mode (standard/editing)
- Optional timer defaults
- Theme profile (`Standard` / `E-ink Lite`)

Preferences are editable later and persisted.

## E-ink Lite mode

Optimized profile for devices like Onyx Boox:
- Reduced animations and transitions
- High-contrast monochrome-friendly palette
- Simpler cursor/selection rendering
- Lower refresh-frequency UI updates
- Optional always-on minimal toolbar

## Suggested implementation roadmap

- `docs/MVP_BACKLOG.md` contains epics, stories, and acceptance criteria.
- `docs/NEXT_STEPS.md` contains the immediate post-scaffold execution plan and sprint sequencing.

## Risks and open questions

- `.scriv` compatibility may require staged support and partial fidelity in v1.
- Android rich text editing can be complex; evaluate editor engine early.
- Full-fidelity `.docx`/`.epub` round-trip needs strict formatting test suite.
