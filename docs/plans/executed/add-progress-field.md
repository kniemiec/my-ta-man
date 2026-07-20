# Add a `progress` field to Tasks and Projects

## Context

Tasks (`*.md`) and Projects (`_project.md`) are stored as Obsidian-style notes: YAML
frontmatter + a markdown body. Today the **entire body is the `description`** — there is
no section parsing anywhere in the stack.

The user wants a second free-text field, `progress`, for recording running notes on how a
task/project is going. It must live as its **own markdown section** in the `.md` file so it
can be hand-edited in Obsidian independently of the description. Per the user, the body
should be restructured into explicit `## Description` and `## Progress` headings so the
format is **extensible to further sections later** (e.g. `## Notes`).

The field flows through four layers — Java model + markdown body parsing (backend), Zod
schemas + TS interfaces (MCP server), TS types + UI (frontend) — all of which must change.

## Design decisions (confirmed with user)

1. **Sectioned body.** The body becomes a sequence of `## <Heading>` sections. Recognized
   sections map to model fields: `Description` → `description`, `Progress` → `progress`.
   A generic section parser is introduced so adding future sections is trivial and so
   **unknown/user-added sections are preserved** on round-trip (mirroring how `Frontmatter`
   already preserves unknown YAML keys).
2. **Backward compatibility.** Existing files have a heading-less raw body. When a body has
   no `## ` headings, the whole body is treated as `description` (and `progress` is null).
   On the next write it is rewrapped into `## Description`. No migration script needed.
3. **Frontend gets a project edit UI.** Projects currently have create-only UI; we add an
   editable project details panel (mirroring `TaskDrawer`) so `progress` (and `description`)
   can be edited in the app for both tasks and projects.

## Backend changes (Java)

### New: `storage/BodyParser.java`
A small utility, parallel in spirit to `Frontmatter`. Responsibilities:
- `Map<String,String> parse(String body)` — split a markdown body on top-level `## `
  headings into an **ordered** `LinkedHashMap` of `heading -> content` (trimmed).
  - If the body contains no `## ` heading, return `{"Description": <whole body trimmed>}`
    (backward compat). Any preamble text before the first heading is folded into
    `Description`.
- `String serialize(Map<String,String> sections)` — rebuild `## Heading\n\n<content>` blocks
  in map order, skipping sections whose content is null/blank, separated by blank lines.
- Helpers to read/update a single section while preserving all others and their order, so
  unknown sections (e.g. a user's `## Notes`) survive a patch that only touches `Progress`.

Add `BodyParserTest.java`: round-trip, heading-less backward-compat input, unknown-section
preservation, blank-section omission.

### Models — `model/Task.java`, `model/Project.java`
Add `private String progress;` with getter/setter, alongside `description`. (`@JsonInclude`
NON_NULL already drops it from JSON when absent.)

### `storage/EntityMapper.java`
Replace the two `setDescription(fm.body()...)` lines (`EntityMapper.java:30`, `:42`) with:
```java
Map<String,String> sections = BodyParser.parse(fm.body());
t.setDescription(blankToNull(sections.get("Description")));
t.setProgress(blankToNull(sections.get("Progress")));
```
Same for `toProject`.

### `service/TaskService.java`
- Extend `PATCHABLE` (`TaskService.java:27`) to include `"progress"`.
- In `create` (`TaskService.java:70`), replace `String body = req.description...` with a
  `BodyParser.serialize` of an ordered map `{Description: req.description, Progress: req.progress}`.
- In `patch` (`TaskService.java:110-113`), replace the whole-body description replacement
  with: parse the current body into sections, then update only the `Description` and/or
  `Progress` entries that appear in `changes`, then `serialize`. This keeps the existing
  field-merge isolation guarantee — patching `progress` must not disturb `description` (and
  vice versa), exactly like the existing `patchDescriptionLeavesMetadataUntouched` test.

### `service/ProjectService.java`
Mirror the `TaskService` changes (PATCHABLE set, `create` body build, `patch` section merge).

### Request DTOs — `service/CreateTaskRequest.java`, `service/CreateProjectRequest.java`
Add `public String progress;` (optional).

### Backend tests
- Extend `ServiceIntegrationTest.java`: create with progress writes a `## Progress` section
  on disk; patch progress leaves description/metadata untouched and vice versa; a legacy
  heading-less file reads as description-only and gains `## Description`/`## Progress` after a
  write.
- Extend `ApiTest.java` lifecycle to set and read back `progress` over HTTP.

## MCP server changes (TypeScript, `mcp-server/`)

- `src/backendClient.ts` — add `progress?: string` to the `Task` and `Project` interfaces.
- `src/tools/taskTools.ts` — add `progress: z.string().optional().describe(...)` to the
  `create_task` and `update_task` input schemas (the args are forwarded to the backend as-is,
  so no other wiring is needed).
- `src/tools/projectTools.ts` — same for `create_project` and `update_project`.
- `src/server.test.ts` — extend the existing arg-mapping assertions to confirm `progress` is
  forwarded on create/update for both tasks and projects.

## Frontend changes (React/TS, `frontend/src/`)

### Types & API clients
- `api/types.ts` — add `progress?: string` to `Task` and `Project`.
- `api/tasks.ts` — add `progress?: string` to `TaskInput`.
- `api/projects.ts` — add `progress?: string` to `ProjectInput`.

### Task UI — `components/TaskDrawer.tsx`
Add a `progress` section mirroring the existing `description` handling: `useState`
initialized from `task.progress`, synced in the `useEffect`, a markdown-rendered view block
(`ReactMarkdown`) with a "No progress updates" placeholder, a `<textarea>` in edit mode, and
`progress` included in the `onSave` payload. Update the `onSave` prop type and the
`saveTask` signature in `pages/ProjectBoardPage.tsx` to carry `progress`.

### Project edit UI (new)
Projects currently have no edit panel. Add one mirroring `TaskDrawer`:
- New `components/ProjectDrawer.tsx` (or an inline edit mode on the `ProjectBoardPage`
  header) that shows project `name`, `state`, `due`, `description` (now markdown-rendered),
  and `progress`, with an edit mode that calls `projectsApi.update(id, changes)` (the client
  method already exists).
- Wire an "Edit" affordance into `pages/ProjectBoardPage.tsx` header so the drawer opens for
  the current project and refreshes it on save.
- Optionally add a `progress` textarea to `components/ProjectForm.tsx` for set-at-creation.

### Frontend tests
- Extend `components/TaskDrawer.test.tsx` for progress render + edit/save (parallel to the
  description tests).
- Add a test for the new project edit panel covering progress edit/save.

## Verification

1. **Backend unit/integration:** `cd backend && ./gradlew test` (or `mvn test`). Confirms
   `BodyParser` round-trips, section-isolated patches, and the HTTP lifecycle with `progress`.
2. **Disk check:** create a task via the API with description + progress, then open the
   `.md` in the vault and confirm distinct `## Description` and `## Progress` sections; edit
   `## Progress` by hand and confirm `GET /api/tasks/{id}` reflects it (no caching).
3. **Backward compat:** point at a vault with a pre-existing heading-less task, confirm it
   reads as description-only, then patch it and confirm it rewraps into the two sections
   while preserving any unknown `## Notes`-style section.
4. **MCP:** `cd mcp-server && npm test`; manually call `create_task`/`update_task` and
   `*_project` tools with `progress` and confirm it persists via `get_task`/`get_project`.
5. **Frontend:** `cd frontend && npm test`, then `npm run dev` — edit a task's progress in
   the TaskDrawer and a project's progress in the new project edit panel; confirm both
   render markdown and persist across reload.
