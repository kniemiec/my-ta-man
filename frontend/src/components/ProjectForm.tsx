import { useState } from 'react';

interface Props {
  onCreate: (input: { name: string; description?: string; due?: string | null }) => void;
}

export function ProjectForm({ onCreate }: Props) {
  const [open, setOpen] = useState(false);
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');

  function submit(e: React.FormEvent) {
    e.preventDefault();
    if (!name.trim()) return;
    onCreate({ name: name.trim(), description: description.trim() || undefined });
    setName('');
    setDescription('');
    setOpen(false);
  }

  if (!open) {
    return (
      <button
        className="rounded-lg bg-sky-600 px-4 py-2 text-sm font-medium text-white hover:bg-sky-700"
        onClick={() => setOpen(true)}
      >
        + New project
      </button>
    );
  }

  return (
    <form onSubmit={submit} className="flex flex-col gap-2 rounded-xl border border-slate-200 bg-white p-4">
      <input
        autoFocus
        className="rounded border border-slate-300 px-2 py-1"
        placeholder="Project name"
        value={name}
        onChange={(e) => setName(e.target.value)}
        aria-label="Project name"
      />
      <textarea
        className="rounded border border-slate-300 px-2 py-1"
        placeholder="Description (optional)"
        value={description}
        onChange={(e) => setDescription(e.target.value)}
        aria-label="Project description"
      />
      <div className="flex gap-2">
        <button className="rounded bg-sky-600 px-3 py-1.5 text-sm font-medium text-white" type="submit">
          Create
        </button>
        <button
          type="button"
          className="rounded border border-slate-300 px-3 py-1.5 text-sm"
          onClick={() => setOpen(false)}
        >
          Cancel
        </button>
      </div>
    </form>
  );
}
