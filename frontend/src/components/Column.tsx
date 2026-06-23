import { useDroppable } from '@dnd-kit/core';
import type { Task, TaskState } from '../api/types';
import { stateLabel } from './StateTag';
import { TaskCard } from './TaskCard';

interface Props {
  state: TaskState;
  tasks: Task[];
  onOpen: (task: Task) => void;
}

export function Column({ state, tasks, onOpen }: Props) {
  const { setNodeRef, isOver } = useDroppable({ id: state });

  return (
    <div
      ref={setNodeRef}
      data-testid={`column-${state}`}
      className={`flex w-72 shrink-0 flex-col rounded-xl bg-slate-100 p-3 ${
        isOver ? 'ring-2 ring-sky-400' : ''
      }`}
    >
      <div className="mb-2 flex items-center justify-between px-1">
        <h3 className="text-sm font-semibold text-slate-600">{stateLabel(state)}</h3>
        <span className="text-xs text-slate-400">{tasks.length}</span>
      </div>
      <div className="flex flex-1 flex-col gap-2">
        {tasks.map((t) => (
          <TaskCard key={t.id} task={t} onOpen={onOpen} />
        ))}
      </div>
    </div>
  );
}
