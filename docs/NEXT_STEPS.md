# Inkwell — What’s Next (Post-Scaffold Plan)

This plan answers “what should we build next?” now that the Android/Compose scaffold exists.

## Guiding principle

Prioritize a **vertical slice** that proves the app’s core loop end-to-end:
1. Create/open project
2. Organize binder
3. Write in editor + notes
4. Persist and restore

If this loop is solid, every advanced feature (rich text, export, sync) becomes incremental.

## Immediate next sprint (Sprint A)

### A1. Introduce project domain + persistence foundation
- Add Room database with the first entities:
  - `ProjectEntity`
  - `BinderNodeEntity` (`Folder` / `Document`, parentId, orderIndex)
  - `DocumentContentEntity`
  - `DocumentNoteEntity`
- Add DAO interfaces for:
  - project load/create
  - binder CRUD + reorder operations
  - document body/note read-write
- Add app-layer repository (`ProjectRepository`) with suspend APIs.

**Done when:** A project can be created and reopened with binder/content state intact after app restart.

### A2. Refactor UI state management (view model + unidirectional state)
- Replace activity-local mutable state with `MainViewModel`.
- Define `EditorUiState` (selected document, mode, timer state, pane visibility).
- Move timer logic to view model and make it deterministic/testable.

**Done when:** UI survives process recreation without losing core state.

### A3. Binder interactions: real CRUD
- Add “new folder” and “new document” actions.
- Add rename and delete for binder nodes.
- Persist reorder indexes (drag/drop can be iterative if needed).

**Done when:** Binder changes persist and reload correctly.

### A4. Basic settings persistence
- Add DataStore preferences for:
  - orientation preference
  - default writing mode
  - default timer duration
  - pane collapse state

**Done when:** Preferences are remembered across launches.

## Sprint B (next)

### B1. Responsive pane system
- Replace static weights with user-adjustable split ratios.
- Add collapsible side panes with remembered layout profiles.
- Add editor-only mode as a first-class layout profile.

### B2. Rich text editor decision spike
- Evaluate options:
  - Compose-native attributed text approach
  - embedding a WebView-based editor
  - third-party rich text engine integration
- Produce decision doc with tradeoffs: formatting fidelity, performance, Android 10 compatibility.

### B3. Export v0
- Implement plain export for `.txt` and `.md` first.
- Project compile concatenation by binder order.

## Sprint C (after core loop hardening)

### C1. Import pipeline v0
- Implement `.txt` and `.md` import first.
- Map imported files into binder nodes.

### C2. Sprint mode UX hardening
- Paragraph-focused visual mode.
- End-of-timer choice dialog (`Continue`, `Restart`, `Change`).

### C3. E-ink Lite profile v0
- Reduced motion + high contrast + simplified update frequency.

## Architectural checkpoints (must decide early)

1. **Document storage shape:** DB text columns vs file blobs + DB metadata.
2. **Rich text representation:** HTML/Markdown/Delta/Spans for internal canonical format.
3. **Sync strategy:** snapshot-based first, then merge-aware revisions.
4. **Export pipeline:** single canonical model transformed per target format.

## Risks to actively mitigate next

- Rich text implementation complexity in Compose.
- Binder drag/drop correctness with nested hierarchy.
- Format fidelity drift between editor, storage, and export.

## Suggested ticket breakdown (first 10 tickets)

1. Set up Room + migrations baseline
2. Create entities + DAOs for project/binder/content/notes
3. Implement `ProjectRepository`
4. Add `MainViewModel` + `EditorUiState`
5. Wire Compose screen to ViewModel state/actions
6. Persist selected document + mode
7. Add create document/folder actions
8. Add rename/delete node actions
9. Add DataStore preferences integration
10. Add instrumentation smoke test for cold start/open project

## Success criteria for “MVP phase 1 complete”

- A user can:
  - create/open a project,
  - organize at least a simple binder,
  - write and attach notes,
  - close and reopen app without losing work.

Once this is complete, proceed with rich text and export/import expansion.
