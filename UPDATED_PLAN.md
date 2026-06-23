# MyTaMan — Implementation Plan

## Context

A personal, single-user, simplified Linear: **Projects** that group **Tasks**, managed through a clean web UI and also through an LLM via MCP. The defining constraint is that the **datastore is a folder of markdown files that doubles as an Obsidian vault** — so the same data can be edited by hand in Obsidian and by the app, with no divergence. Everything must run from a single `docker-compose up`, be decomposed into small testable units, and let the storage location be configured.

### Domain model
- **Task**: `id`, `name`, `description`, `due` (optional), `state` ∈ {`new`, `in-progress`, `blocked`, `in-review`, `completed`}. All transitions allowed. No subtasks.
- **Project**: `id`, `name`, `description`, `due` (optional), `state` ∈ {`new`, `in-progress`, `completed`}, plus its list of tasks (derived from folder contents).
- No assignees, no auth (local single-user).

---

## Decisions

| Area | Decision |
|------|----------|
| **File layout** | Folder per project at vault root; one `.md` file per task inside it. `_project.md` holds project metadata/description. An `Inbox/` folder holds project-less tasks. |
| **File format** | YAML frontmatter (metadata) + markdown body (description) — Obsidian-native "properties". |
| **Identity** | Stable `id` (e.g. `TASK-7`, `PROJ-3`) in frontmatter; filename is a human label derived from the name and may be renamed freely in Obsidian. Backend locates entities by scanning frontmatter `id`, never by filename. |
| **Project membership** | **Folder location is authoritative.** No `project:` frontmatter field — nothing can contradict the folder. Moving a file's folder = moving projects. |
| **Sync model** | **Read from disk on every request, no cache.** Filesystem is the single source of truth; Obsidian edits are reflected instantly. |
| **Write safety** | **Field-level merge**: on update, re-read the file, change only the touched field(s), atomic-rewrite (temp file + `Files.move` ATOMIC). Changing state never disturbs the body and vice versa. |
| **Deletion** | Delete task = remove file. Delete project = **409 if folder still has tasks** (no accidental mass loss). |
| **Backend** | Java 21 + **Javalin** (embedded Jetty) + Jackson (JSON) + SnakeYAML (frontmatter). Fat jar, `eclipse-temurin:21-jre-alpine`, sub-second startup. |
| **MCP server** | **TypeScript**, official MCP SDK, **Streamable HTTP** transport, runs as a docker-compose service. Thin adapter → backend REST. |
| **Frontend** | **React + Vite + TypeScript + Tailwind**, **Kanban board** (columns = states, drag to change state). Description: rendered markdown + plain-textarea edit. |
| **Network/auth** | No auth. **nginx** (frontend container) serves the static UI and reverse-proxies `/api/*` to the backend over the compose network — single origin, no CORS, backend port unpublished. MCP reaches backend internally. |
| **Storage config** | `VAULT_PATH` in `.env`, bind-mounted into the **backend only** at `/vault`. Point Obsidian at the same host folder. |
| **Tests** | Per-component (backend JUnit5 unit + integration on temp vault; frontend Vitest + RTL; MCP tool→REST tests) **+ one Playwright e2e** over the running compose stack. |

---

## Repository layout

```
MyTaMan/
  backend/            # Java 21 + Javalin (Maven)
  frontend/           # React + Vite + TS + Tailwind, served by nginx
  mcp-server/         # TypeScript + MCP SDK (Streamable HTTP)
  e2e/                # Playwright full-stack test
  docker-compose.yml
  .env.example        # VAULT_PATH=...
  README.md
```

### Vault on disk
```
$VAULT_PATH/
  .mytaman/counters.json        # hidden id counters (Obsidian ignores dot-folders)
  ProjectAlpha/
    _project.md                 # project metadata + description
    fix-login-bug.md            # task
    add-dark-mode.md
  Inbox/                        # tasks with no project
    buy-domain.md
```

Task file example:
```markdown
---
id: TASK-7
name: Fix login bug
state: in-progress
due: 2026-07-01
created: 2026-06-23
---

The login form throws a 500 when the email contains a `+`...
```
`_project.md` is identical but with project `id`/`state`.

---

## 1. Backend (Java 21 + Javalin)

```
backend/src/main/java/com/mytaman/
  Main.java                       # wire config + Javalin app, register routes
  config/AppConfig.java           # reads VAULT_DIR env (default /vault), PORT
  model/
    Task.java  TaskState.java     # enums serialize kebab-case
    Project.java  ProjectState.java
  storage/
    Frontmatter.java              # split/parse/serialize YAML+body (SnakeYAML)
    VaultPaths.java               # slug, folder/file resolution, Inbox handling
    IdSequence.java               # .mytaman/counters.json, in-process ReentrantLock
    TaskRepository.java           # scan vault, find-by-id, atomic write, move, delete
    ProjectRepository.java
  service/
    TaskService.java              # validation, state changes, field-merge updates
    ProjectService.java           # block-delete-if-nonempty rule
  api/
    TaskController.java  ProjectController.java
    ErrorMapper.java              # 400/404/409 -> JSON problem body
```

### REST API
| Method | Path | Action |
|--------|------|--------|
| GET | `/api/projects` | list projects (with task counts) |
| POST | `/api/projects` | create project (new folder + `_project.md`) |
| GET | `/api/projects/{id}` | project + its tasks |
| PATCH | `/api/projects/{id}` | field-merge update (name/description/due/state) |
| DELETE | `/api/projects/{id}` | delete folder; **409 if it has tasks** |
| GET | `/api/tasks` | all tasks; `?project=PROJ-3` or `?project=inbox` filter |
| POST | `/api/tasks` | create task (in a project folder or Inbox) |
| GET | `/api/tasks/{id}` | get task |
| PATCH | `/api/tasks/{id}` | field-merge update (name/description/due/state) |
| PATCH | `/api/tasks/{id}/move` | move to another project/Inbox (move file) |
| DELETE | `/api/tasks/{id}` | delete file |

Notes:
- **PATCH everywhere** (not PUT) to match the field-level-merge write model — the body carries only changed fields.
- ID generation: `IdSequence` persists monotonic counters in `.mytaman/counters.json` under a `ReentrantLock` so deleting the highest id never causes reuse.
- Reads scan the vault fresh each call (`Files.walk`), parsing only `.md` files; `find-by-id` matches frontmatter, tolerating renamed filenames.
- Atomic writes: write temp file in same dir, `Files.move(..., ATOMIC_MOVE)`.

### Tests
- Unit: `Frontmatter` round-trip, `VaultPaths` slug/collision, `IdSequence`, state/enum mapping.
- Integration: start each repository/service against a JUnit5 `@TempDir` vault; exercise create→list→update→move→delete and the 409-on-nonempty rule with real file I/O.
- API: boot Javalin on an ephemeral port over a temp vault; hit endpoints with an HTTP client; assert status codes and on-disk file contents.

---

## 2. Frontend (React + Vite + TS + Tailwind)

```
frontend/src/
  api/client.ts                   # fetch wrapper -> /api (same origin via nginx)
  api/projects.ts  api/tasks.ts   # typed calls, shared TS types mirroring DTOs
  components/
    Board.tsx                     # Kanban columns by state, dnd-kit drag
    Column.tsx  TaskCard.tsx
    StateTag.tsx
    TaskDrawer.tsx                # view rendered markdown + textarea edit, due, delete
    ProjectCard.tsx  ProjectForm.tsx
  pages/
    ProjectsPage.tsx              # grid of project cards
    ProjectBoardPage.tsx          # board for one project (+ Inbox board)
  App.tsx  main.tsx
```
- **Drag**: `@dnd-kit/core` — dropping a card in a column issues `PATCH /api/tasks/{id} {state}`.
- **Markdown**: render description with `react-markdown`; "Edit" swaps to a `<textarea>` that saves the body via `PATCH`.
- API base is `/api` (relative) so the browser only ever talks to nginx.
- **Tests**: Vitest + React Testing Library on `TaskCard`, `Board` (drag → calls API mock), `TaskDrawer` (edit→save), `ProjectForm`.

---

## 3. MCP server (TypeScript, Streamable HTTP)

```
mcp-server/src/
  index.ts                # create McpServer, Streamable HTTP transport on :3001/mcp
  backendClient.ts        # typed fetch wrapper over BACKEND_URL
  tools/projectTools.ts   # list/get/create/update/delete project
  tools/taskTools.ts      # list/get/create/update/move/set-state/delete task
```
- Each tool has a zod input schema and simply forwards to the backend REST API; **no business logic** here.
- Reads `BACKEND_URL` (e.g. `http://backend:7000`) from env.
- Client connects to `http://localhost:3001/mcp` (e.g. `claude mcp add --transport http mytaman http://localhost:3001/mcp`).
- **Tests**: Vitest with `backendClient` stubbed — assert each tool maps args→correct HTTP call and shapes the result/errors.

---

## 4. Docker Compose

```yaml
services:
  backend:
    build: ./backend
    expose: ["7000"]                       # internal only
    volumes: [ "${VAULT_PATH}:/vault" ]
    environment: { VAULT_DIR: /vault, PORT: "7000" }

  frontend:
    build: ./frontend                      # multi-stage: vite build -> nginx
    ports: [ "127.0.0.1:8080:80" ]         # UI + /api proxy
    depends_on: [ backend ]

  mcp-server:
    build: ./mcp-server
    ports: [ "127.0.0.1:3001:3001" ]
    environment: { BACKEND_URL: "http://backend:7000" }
    depends_on: [ backend ]
```
- nginx config: serve `/` from built assets; `location /api/ { proxy_pass http://backend:7000; }`.
- `.env.example` documents `VAULT_PATH`. Published ports bound to `127.0.0.1` (local only).
- Backend image multi-stage (Maven build → `eclipse-temurin:21-jre-alpine` runtime, fat jar).

---

## 5. Build order

1. **Backend** — model → `Frontmatter`/`VaultPaths`/`IdSequence` → repositories → services → controllers → unit/integration/API tests → Dockerfile.
2. **Frontend** — API client + types → `Board`/`TaskCard`/`TaskDrawer` → pages → Vitest tests → Dockerfile (vite build + nginx + proxy).
3. **MCP server** — `backendClient` → tools → Vitest tests → Dockerfile.
4. **Compose + e2e** — wire all three, mount a scratch vault, write the Playwright e2e.

---

## Verification

- **Backend**: `mvn test` (unit + temp-vault integration + API). Manual: `curl` create/list/patch/delete against a local vault dir and confirm the `.md` files appear/update correctly and remain valid Obsidian notes.
- **Frontend**: `npm test` (Vitest). Manual: `npm run dev` against the backend, drag a card between columns, confirm the file's `state` changes on disk.
- **MCP**: `npm test`. Manual: register the server in an MCP client and call `create_task`, confirm the file appears in the vault.
- **Full stack**: `cp .env.example .env` (set `VAULT_PATH`), `docker-compose up --build`, open `http://localhost:8080`. Then **Playwright e2e** in `e2e/`: launch the compose stack, create a project → add a task → drag it to `in-progress` → assert via the API that state changed, and (bonus) assert the corresponding `.md` file on the mounted vault reflects it.
- **Obsidian check**: open `VAULT_PATH` as a vault, edit a task's body, then `GET /api/tasks/{id}` and confirm the edit is served (validates the no-cache, filesystem-as-truth model).
