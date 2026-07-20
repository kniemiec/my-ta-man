# Add an `archived` state to Tasks and Projects

## Context

Users need a way to get finished or abandoned tasks/projects out of their active
working view without deleting them (which is irreversible) and without losing the
work's history. The chosen model is a **new lifecycle state value** `archived`, added
to the existing `state` enum for both tasks and projects.

Decisions confirmed with the user:

1. **Model = new state value.** `archived` becomes a 6th `TaskState` and a 4th
   `ProjectState`, alongside `completed`. An entity is *either* archived *or* in another
   state — archiving overwrites the previous state (there is no separate boolean flag).
   This deliberately reuses the entire existing `state` pipeline (frontmatter key,
   PATCH, create, enum parsing) rather than introducing a new field.
2. **Visibility = hidden with a toggle.** Archived items are hidden from the active views
   by default: the board omits the `archived` column and the projects grid omits archived
   projects. A **"Show archived"** toggle reveals them. Because the board renders one
   column per state and filters tasks into their state's column, hiding the `archived`
   column automatically hides archived tasks — no per-card filtering needed.
3. **No cascade.** Archiving a project sets only the project's own state. Its tasks keep
   their own states (state is inherently per-entity here, so this is the natural behavior).

Because `state` already flows end-to-end, the backend and MCP changes are almost entirely
"add one enum value." The substantive new work is the frontend toggle/visibility behavior.

## Backend changes (Java)

### `model/TaskState.java` and `model/ProjectState.java`
Add one enum constant to each:
- `TaskState`: `ARCHIVED("archived")` (after `COMPLETED`).
- `ProjectState`: `ARCHIVED("archived")` (after `COMPLETED`).

That is the **only** backend production change required. Everything downstream already
handles arbitrary states generically:
- Create accepts `state` (`TaskService.java:60`, `ProjectService.java:58`).
- PATCH already lists `"state"` in `PATCHABLE` and writes it via `parseTaskState`/
  `parseProjectState` (`TaskService.java:102`, `ProjectService.java:101`).
- `EntityMapper` parses `state` from frontmatter on read.
- List/get endpoints return whatever state is on disk, so archived items are still
  returned by the API — the UI decides visibility. (MCP/API stay unfiltered; the toggle
  lives in the frontend, matching the "filesystem is source of truth" design.)

### Backend tests
- `model` enum round-trip: extend any state-mapping assertions to cover `archived`
  (`fromWire("archived")` ↔ `wire()`), and confirm `fromWire` is case-insensitive as today.
- `service/ServiceIntegrationTest.java`: patch a task and a project to `state=archived`
  and assert the `.md` frontmatter shows `state: archived`, and that unarchiving (patch to
  another state) works and leaves body/description untouched (reuses the existing
  field-merge isolation guarantee).
- `api/ApiTest.java`: extend the lifecycle to PATCH `state=archived` over HTTP and read it
  back; confirm an invalid state still yields 400 via `parse*State` → `ValidationException`.

## MCP server changes (TypeScript, `mcp-server/`)

- `src/tools/taskTools.ts` — add `'archived'` to the `TASK_STATE` z.enum (line 6-12).
- `src/tools/projectTools.ts` — add `'archived'` to the `PROJECT_STATE` z.enum (line 6).
  This makes `create_*`, `update_*`, and `set_task_state` accept/forward the new state.
  No client wiring changes: `backendClient` forwards `state` generically as a string.
- `src/server.test.ts` — extend the arg-mapping assertions so `set_task_state`/`update_task`
  and `update_project` accept and forward `state: 'archived'`.

## Frontend changes (React/TS, `frontend/src/`)

### `api/types.ts`
- Add `'archived'` to both the `TaskState` and `ProjectState` unions.
- Keep the existing `TASK_STATES` / `PROJECT_STATES` arrays as the **active** states (the
  default board columns / active project set) — do **not** add `archived` to them.
- Add two new exports for the "with archived" case used by dropdowns and the toggled board:
  - `TASK_STATES_WITH_ARCHIVED = [...TASK_STATES, 'archived']`
  - `PROJECT_STATES_WITH_ARCHIVED = [...PROJECT_STATES, 'archived']`

### `components/StateTag.tsx`
Add an `archived` entry to `COLORS` (a muted grey, e.g. `bg-slate-300 text-slate-600`) and
to `LABELS` (`'Archived'`). `stateLabel` then works for the new state automatically.

### `components/Board.tsx`
- Add a `showArchived?: boolean` prop.
- Render columns from `showArchived ? TASK_STATES_WITH_ARCHIVED : TASK_STATES`. Archived
  tasks fall into the `archived` column, which is absent unless toggled — so they vanish
  from the active board with no extra filtering. Dragging a card onto the `archived`
  column (when shown) archives it; dragging it out to another column unarchives it.

### `pages/ProjectBoardPage.tsx`
- Add `const [showArchived, setShowArchived] = useState(false)` and a small toggle
  (checkbox/button) in the header; pass `showArchived` to `<Board>`.
- The `TaskDrawer` state dropdown should list `archived` so a task can be archived without
  first revealing the column (see below).

### `components/TaskDrawer.tsx` and `components/ProjectDrawer.tsx`
- Change the state `<select>` to iterate `TASK_STATES_WITH_ARCHIVED` /
  `PROJECT_STATES_WITH_ARCHIVED` so `Archived` is a selectable option.
- (Optional, nicer UX) add an explicit **Archive / Unarchive** button that calls
  `onSave({ state: 'archived' })` or `onSave({ state: 'new' })` — because the archived
  board column is hidden by default, a direct button is clearer than drag-to-archive.

### `pages/ProjectsPage.tsx` and `components/ProjectCard.tsx`
- `ProjectsPage`: add a `showArchived` toggle; by default filter out projects whose
  `state === 'archived'` before rendering the grid (`projects.filter(...)`).
- `ProjectCard` (optional): visually de-emphasize an archived project (muted border/opacity)
  when it is shown via the toggle. The `StateTag` already conveys the state.

### Frontend tests
- `components/TaskDrawer.test.tsx` / add a `ProjectDrawer` state-option test: assert the
  dropdown offers `Archived` and that selecting it calls `onSave({ state: 'archived' })`.
- Board test: with `showArchived={false}` there is no `column-archived`; with
  `showArchived={true}` the archived column renders and holds archived tasks.
- `ProjectsPage` test (if one exists / add light coverage): archived projects are hidden by
  default and appear once the toggle is on.

## Documentation

- Update `UPDATED_PLAN.md` domain model (lines 8-9) to list `archived` in the task and
  project state sets.

## Verification

1. **Backend:** `cd backend && mvn test` — enum round-trip, `state=archived` PATCH persists
   to frontmatter, invalid state still 400s.
2. **Disk check:** PATCH a task to `archived` via the API, open the `.md` in the vault,
   confirm `state: archived`; hand-edit it back in Obsidian and confirm `GET` reflects it
   (no caching).
3. **MCP:** `cd mcp-server && npm test`; manually call `set_task_state` / `update_project`
   with `state: "archived"` and confirm via `get_*`.
4. **Frontend:** `cd frontend && npm test`, then `npm run dev` — archive a task from the
   drawer, confirm it disappears from the board, toggle "Show archived" to see the archived
   column, drag it back to unarchive; archive a project and confirm it drops out of the grid
   until "Show archived" is on.
5. **E2E (optional):** extend `e2e/tests/flow.spec.ts` to archive a task and assert it is
   hidden until the toggle is enabled.
