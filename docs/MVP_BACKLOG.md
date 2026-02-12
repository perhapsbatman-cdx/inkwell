# Inkwell MVP Backlog

This backlog turns the product blueprint into buildable MVP slices.

## Epic 1 — Project and Binder Foundation

### Story 1.1: Create/open local project
- User can create a project with title and save location.
- App opens the most recent project on launch.
- Acceptance criteria:
  - Project metadata persisted locally.
  - Empty project shows binder with starter document.

### Story 1.2: Binder tree operations
- Add folder/document.
- Rename/delete/move.
- Drag and drop reorder + nesting.
- Acceptance criteria:
  - Binder ordering persists after app restart.
  - Move operations update parent and index correctly.

### Story 1.3: Linked notes per document
- Each document has plain-text notes.
- Notes load/switch with selected document.
- Acceptance criteria:
  - Notes persist independently from editor body.

## Epic 2 — Editor and Layout

### Story 2.1: Three-pane workspace
- 20/60/20 default layout in landscape/tablet.
- Pane collapse and adjustable widths with persisted preferences.
- Acceptance criteria:
  - User can hide outline/notes panes.
  - Editor-only mode is available.

### Story 2.2: Rich text baseline
- Title/Heading/H1-H4 styles.
- Bold/italic/underline/sup/sub/strikethrough.
- Lists, alignment, line spacing, ornamental break.
- Acceptance criteria:
  - Formatting survives save/load.
  - Export pipeline can map core formatting.

## Epic 3 — Writing Modes and Timer

### Story 3.1: Standard writing mode timer
- 5..60 minutes, 5-minute increments.
- Auto-start on first keypress.
- Acceptance criteria:
  - Timer pause/resume works.
  - Zero-state prompt offers continue/reset/change.

### Story 3.2: Sprint mode
- 5,10,15,20,25,30 minutes only.
- Focus UI: timer + current paragraph.
- Acceptance criteria:
  - Non-essential chrome hidden during sprint.

## Epic 4 — Interop and Export

### Story 4.1: Single-document import/export
- Import: txt/md/docx/rtf/html/epub
- Export: txt/md/docx/rtf/pdf/epub
- Acceptance criteria:
  - Unsupported styles gracefully downgraded.

### Story 4.2: Project compile export
- Export full manuscript by binder order.
- Chapter/scene breaks from folder/doc structure.
- Acceptance criteria:
  - Generate one output per selected format.

### Story 4.3: Markdown bridge
- Copy as markdown / paste from markdown
- Acceptance criteria:
  - Round-trip basic formatting with stable output.

## Epic 5 — Google Drive Sync

### Story 5.1: OAuth and project backup
- Connect/disconnect Google account.
- Upload/download project snapshots.
- Acceptance criteria:
  - Manual sync with success/failure reporting.

### Story 5.2: Conflict handling
- Detect divergent versions.
- Offer keep-local / keep-remote / duplicate.
- Acceptance criteria:
  - No silent overwrite.

## Epic 6 — Setup and Accessibility

### Story 6.1: First-run preferences
- Orientation preference and theme profile.
- Timer defaults.
- Acceptance criteria:
  - Preferences editable and persisted.

### Story 6.2: E-ink lite mode
- Reduced animations and high contrast.
- Acceptance criteria:
  - Lite mode can be toggled without restart.

## Definition of MVP Done
- User can manage a binder tree, write in a 3-pane editor, use Standard/Sprint timers, and export at least txt/md/docx/pdf.
- Google Drive manual sync works for complete project backups.
- App runs on Android 10+ and supports an e-ink lite profile.
