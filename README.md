# MyTaMan

A personal, single-user, simplified Linear: **Projects** that group **Tasks**, managed
through a clean web UI *and* through an LLM via MCP. The datastore is a folder of markdown
files that doubles as an **Obsidian vault** — the same data can be edited by hand in
Obsidian and by the app, with no divergence.

## Architecture

| Component | Tech | Role |
|-----------|------|------|
| **backend** | Java 21 + Javalin | REST API over the vault (markdown files as the source of truth) |
| **frontend** | React + Vite + TS + Tailwind | Kanban UI, served by nginx (also proxies `/api`) |
| **mcp-server** | TypeScript + MCP SDK | Streamable-HTTP MCP tools that forward to the backend |

```
┌──────────┐      /api       ┌──────────┐     files     ┌────────────┐
│ frontend │ ───(nginx)────▶ │ backend  │ ───────────▶ │ Obsidian   │
│  (nginx) │                 │ (Javalin)│ ◀─────────── │ vault dir  │
└──────────┘                 └──────────┘               └────────────┘
      ▲                          ▲
   browser                  ┌────────────┐
                            │ mcp-server │ ◀── LLM (Claude, etc.)
                            └────────────┘
```

## Data layout (the vault)

```
$VAULT_PATH/
  .mytaman/counters.json     hidden id counters (Obsidian ignores dot-folders)
  ProjectAlpha/
    _project.md              project metadata + description
    fix-login-bug.md         one file per task
  Inbox/                     tasks with no project
    buy-domain.md
```

Each file is YAML frontmatter + a markdown body:

```markdown
---
id: TASK-7
name: Fix login bug
state: in-progress
due: 2026-07-01
created: 2026-06-23
---

The login form throws a 500 when the email contains a `+`…
```

- **Folder location is authoritative** for project membership — there is no `project:`
  field that could contradict it. Move a file's folder to move it between projects.
- The `id` in frontmatter is stable; the filename is just a human label and may be renamed
  freely in Obsidian.
- The backend reads from disk on every request (no cache), so Obsidian edits show up
  immediately, and writes are field-level merges (changing state never touches the body).

## Quick start (everything via Docker)

```bash
cp .env.example .env        # set VAULT_PATH to your vault folder
docker compose up --build
```

- UI: <http://localhost:8080>
- MCP endpoint: <http://localhost:3001/mcp>

Point Obsidian at the same `VAULT_PATH` folder to edit the data by hand.

### Register the MCP server with an MCP client

```bash
claude mcp add --transport http mytaman http://localhost:3001/mcp
```

Tools: `list_projects`, `get_project`, `create_project`, `update_project`,
`delete_project`, `list_tasks`, `get_task`, `create_task`, `update_task`,
`set_task_state`, `move_task`, `delete_task`.

## REST API

| Method | Path | Action |
|--------|------|--------|
| GET | `/api/projects` | list projects (with task counts) |
| POST | `/api/projects` | create project |
| GET | `/api/projects/{id}` | project + its tasks |
| PATCH | `/api/projects/{id}` | field-merge update |
| DELETE | `/api/projects/{id}` | delete (409 if it has tasks) |
| GET | `/api/tasks` | all tasks; `?project=PROJ-3` or `?project=inbox` |
| POST | `/api/tasks` | create task |
| GET | `/api/tasks/{id}` | get task |
| PATCH | `/api/tasks/{id}` | field-merge update |
| PATCH | `/api/tasks/{id}/move` | move to another project / inbox |
| DELETE | `/api/tasks/{id}` | delete task |

## Development & tests

```bash
# Backend (unit + temp-vault integration + API over an ephemeral port)
cd backend && mvn test

# Frontend (Vitest + React Testing Library)
cd frontend && npm install && npm test

# MCP server (Vitest, backend stubbed)
cd mcp-server && npm install && npm test

# Full-stack e2e (brings up docker-compose with a scratch vault, drives the UI)
cd e2e && npm install && npx playwright install --with-deps chromium && npm test
```

Run the frontend or MCP server against a local backend during development:

```bash
cd backend && VAULT_DIR=./vault mvn compile exec:java   # or run Main from your IDE
cd frontend && npm run dev          # proxies /api to http://localhost:7000
cd mcp-server && npm run dev        # BACKEND_URL defaults to http://localhost:7000
```
