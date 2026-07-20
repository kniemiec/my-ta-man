import { useEffect, useState } from 'react';
import ReactMarkdown from 'react-markdown';
import type { Project, ProjectState } from '../api/types';
import { PROJECT_STATES_WITH_ARCHIVED } from '../api/types';
import { stateLabel } from './StateTag';

interface Props {
  project: Project;
  onClose: () => void;
  onSave: (changes: {
    name?: string;
    description?: string;
    progress?: string;
    due?: string | null;
    state?: ProjectState;
  }) => void;
}

export function ProjectDrawer({ project, onClose, onSave }: Props) {
  const [editing, setEditing] = useState(false);
  const [name, setName] = useState(project.name);
  const [description, setDescription] = useState(project.description ?? '');
  const [progress, setProgress] = useState(project.progress ?? '');
  const [due, setDue] = useState(project.due ?? '');

  useEffect(() => {
    setName(project.name);
    setDescription(project.description ?? '');
    setProgress(project.progress ?? '');
    setDue(project.due ?? '');
    setEditing(false);
  }, [project]);

  function save() {
    onSave({
      name: name.trim(),
      description,
      progress,
      due: due.trim() === '' ? null : due.trim(),
    });
    setEditing(false);
  }

  return (
    <div className="fixed inset-0 z-20 flex justify-end bg-black/30" onClick={onClose}>
      <div
        className="flex h-full w-full max-w-md flex-col gap-4 overflow-y-auto bg-white p-6 shadow-xl"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex items-start justify-between">
          {editing ? (
            <input
              className="w-full rounded border border-slate-300 px-2 py-1 text-lg font-semibold"
              value={name}
              onChange={(e) => setName(e.target.value)}
              aria-label="Project name"
            />
          ) : (
            <h2 className="text-lg font-semibold">{project.name}</h2>
          )}
          <button className="ml-2 text-slate-400 hover:text-slate-700" onClick={onClose}>
            ✕
          </button>
        </div>

        <div className="text-xs text-slate-500">{project.id}</div>

        <label className="flex flex-col gap-1 text-sm">
          <span className="font-medium text-slate-600">State</span>
          <select
            className="rounded border border-slate-300 px-2 py-1"
            value={project.state}
            onChange={(e) => onSave({ state: e.target.value as ProjectState })}
            aria-label="State"
          >
            {PROJECT_STATES_WITH_ARCHIVED.map((s) => (
              <option key={s} value={s}>
                {stateLabel(s)}
              </option>
            ))}
          </select>
        </label>

        <label className="flex flex-col gap-1 text-sm">
          <span className="font-medium text-slate-600">Due date</span>
          {editing ? (
            <input
              type="date"
              className="rounded border border-slate-300 px-2 py-1"
              value={due}
              onChange={(e) => setDue(e.target.value)}
            />
          ) : (
            <span className="text-slate-700">{project.due ?? '—'}</span>
          )}
        </label>

        <div className="flex flex-col gap-1 text-sm">
          <span className="font-medium text-slate-600">Description</span>
          {editing ? (
            <textarea
              className="min-h-40 rounded border border-slate-300 p-2 font-mono text-sm"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              aria-label="Description"
            />
          ) : (
            <div className="prose prose-sm max-w-none rounded border border-slate-100 bg-slate-50 p-3">
              {project.description ? (
                <ReactMarkdown>{project.description}</ReactMarkdown>
              ) : (
                <span className="text-slate-400">No description</span>
              )}
            </div>
          )}
        </div>

        <div className="flex flex-col gap-1 text-sm">
          <span className="font-medium text-slate-600">Progress</span>
          {editing ? (
            <textarea
              className="min-h-24 rounded border border-slate-300 p-2 font-mono text-sm"
              value={progress}
              onChange={(e) => setProgress(e.target.value)}
              aria-label="Progress"
            />
          ) : (
            <div className="prose prose-sm max-w-none rounded border border-slate-100 bg-slate-50 p-3">
              {project.progress ? (
                <ReactMarkdown>{project.progress}</ReactMarkdown>
              ) : (
                <span className="text-slate-400">No progress updates</span>
              )}
            </div>
          )}
        </div>

        <div className="mt-auto flex items-center gap-2">
          {editing ? (
            <>
              <button
                className="rounded bg-sky-600 px-3 py-1.5 text-sm font-medium text-white hover:bg-sky-700"
                onClick={save}
              >
                Save
              </button>
              <button
                className="rounded border border-slate-300 px-3 py-1.5 text-sm"
                onClick={() => setEditing(false)}
              >
                Cancel
              </button>
            </>
          ) : (
            <>
              <button
                className="rounded border border-slate-300 px-3 py-1.5 text-sm hover:bg-slate-50"
                onClick={() => setEditing(true)}
              >
                Edit
              </button>
              <button
                className="rounded border border-slate-300 px-3 py-1.5 text-sm hover:bg-slate-50"
                onClick={() => onSave({ state: project.state === 'archived' ? 'new' : 'archived' })}
              >
                {project.state === 'archived' ? 'Unarchive' : 'Archive'}
              </button>
            </>
          )}
        </div>
      </div>
    </div>
  );
}
