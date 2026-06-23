import type { ProjectState, TaskState } from '../api/types';

const COLORS: Record<string, string> = {
  new: 'bg-slate-200 text-slate-700',
  'in-progress': 'bg-amber-200 text-amber-900',
  blocked: 'bg-rose-200 text-rose-800',
  'in-review': 'bg-violet-200 text-violet-800',
  completed: 'bg-emerald-200 text-emerald-800',
};

const LABELS: Record<string, string> = {
  new: 'New',
  'in-progress': 'In Progress',
  blocked: 'Blocked',
  'in-review': 'In Review',
  completed: 'Completed',
};

export function StateTag({ state }: { state: TaskState | ProjectState }) {
  return (
    <span
      className={`inline-block rounded-full px-2 py-0.5 text-xs font-medium ${
        COLORS[state] ?? 'bg-slate-200 text-slate-700'
      }`}
    >
      {LABELS[state] ?? state}
    </span>
  );
}

export const stateLabel = (state: string) => LABELS[state] ?? state;
