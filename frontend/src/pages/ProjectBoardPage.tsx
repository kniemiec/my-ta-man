import { useCallback, useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { projectsApi } from '../api/projects';
import { tasksApi } from '../api/tasks';
import type { Project, Task, TaskState } from '../api/types';
import { INBOX_ID } from '../api/types';
import { Board } from '../components/Board';
import { StateTag } from '../components/StateTag';
import { TaskDrawer } from '../components/TaskDrawer';

export function ProjectBoardPage({ inbox = false }: { inbox?: boolean }) {
  const params = useParams();
  const projectId = inbox ? INBOX_ID : (params.id as string);

  const [project, setProject] = useState<Project | null>(null);
  const [tasks, setTasks] = useState<Task[]>([]);
  const [selected, setSelected] = useState<Task | null>(null);
  const [newName, setNewName] = useState('');
  const [error, setError] = useState<string | null>(null);

  const reload = useCallback(async () => {
    try {
      setTasks(await tasksApi.list(projectId));
      if (!inbox) setProject(await projectsApi.get(projectId));
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to load');
    }
  }, [projectId, inbox]);

  useEffect(() => {
    reload();
  }, [reload]);

  async function addTask(e: React.FormEvent) {
    e.preventDefault();
    if (!newName.trim()) return;
    await tasksApi.create({ name: newName.trim(), projectId });
    setNewName('');
    reload();
  }

  async function moveState(task: Task, state: TaskState) {
    // Optimistic update for a snappy board.
    setTasks((prev) => prev.map((t) => (t.id === task.id ? { ...t, state } : t)));
    await tasksApi.setState(task.id, state);
    reload();
  }

  async function saveTask(changes: {
    name?: string;
    description?: string;
    due?: string | null;
    state?: TaskState;
  }) {
    if (!selected) return;
    const updated = await tasksApi.update(selected.id, changes);
    setSelected(updated);
    reload();
  }

  async function deleteTask() {
    if (!selected) return;
    await tasksApi.remove(selected.id);
    setSelected(null);
    reload();
  }

  return (
    <div className="mx-auto max-w-7xl p-6">
      <Link to="/" className="text-sm text-sky-700 hover:underline">
        ← Projects
      </Link>

      <header className="mb-4 mt-2 flex items-center gap-3">
        <h1 className="text-2xl font-bold text-slate-800">
          {inbox ? 'Inbox' : (project?.name ?? projectId)}
        </h1>
        {project && <StateTag state={project.state} />}
      </header>

      {!inbox && project?.description && (
        <p className="mb-4 max-w-2xl text-sm text-slate-600">{project.description}</p>
      )}

      {error && <div className="mb-4 rounded bg-rose-100 p-3 text-sm text-rose-800">{error}</div>}

      <form onSubmit={addTask} className="mb-6 flex gap-2">
        <input
          className="w-80 rounded border border-slate-300 px-3 py-2 text-sm"
          placeholder="New task name…"
          value={newName}
          onChange={(e) => setNewName(e.target.value)}
          aria-label="New task name"
        />
        <button className="rounded bg-sky-600 px-4 py-2 text-sm font-medium text-white hover:bg-sky-700">
          Add task
        </button>
      </form>

      <Board tasks={tasks} onMove={moveState} onOpen={setSelected} />

      {selected && (
        <TaskDrawer
          task={selected}
          onClose={() => setSelected(null)}
          onSave={saveTask}
          onDelete={deleteTask}
        />
      )}
    </div>
  );
}
