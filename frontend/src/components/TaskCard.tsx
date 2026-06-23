import { useDraggable } from '@dnd-kit/core';
import type { Task } from '../api/types';

interface Props {
  task: Task;
  onOpen: (task: Task) => void;
}

export function TaskCard({ task, onOpen }: Props) {
  const { attributes, listeners, setNodeRef, transform, isDragging } = useDraggable({
    id: task.id,
  });

  const style = transform
    ? { transform: `translate3d(${transform.x}px, ${transform.y}px, 0)` }
    : undefined;

  return (
    <div
      ref={setNodeRef}
      style={style}
      className={`cursor-grab rounded-lg border border-slate-200 bg-white p-3 shadow-sm hover:border-slate-300 ${
        isDragging ? 'opacity-50' : ''
      }`}
      {...listeners}
      {...attributes}
      data-testid={`task-card-${task.id}`}
    >
      <button
        type="button"
        className="block w-full text-left font-medium text-slate-800"
        onClick={() => onOpen(task)}
        // Don't let the drag listeners swallow the click.
        onPointerDown={(e) => e.stopPropagation()}
      >
        {task.name}
      </button>
      <div className="mt-1 flex items-center gap-2 text-xs text-slate-500">
        <span>{task.id}</span>
        {task.due && <span>· due {task.due}</span>}
      </div>
    </div>
  );
}
