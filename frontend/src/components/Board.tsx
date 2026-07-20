import { DndContext, type DragEndEvent, PointerSensor, useSensor, useSensors } from '@dnd-kit/core';
import type { Task, TaskState } from '../api/types';
import { TASK_STATES, TASK_STATES_WITH_ARCHIVED } from '../api/types';
import { Column } from './Column';

interface Props {
  tasks: Task[];
  onMove: (task: Task, state: TaskState) => void;
  onOpen: (task: Task) => void;
  showArchived?: boolean;
}

export function Board({ tasks, onMove, onOpen, showArchived = false }: Props) {
  // The archived column is only rendered when toggled on, so archived tasks
  // (which land in that column) stay hidden from the active board by default.
  const columns = showArchived ? TASK_STATES_WITH_ARCHIVED : TASK_STATES;
  // A small activation distance so clicks aren't treated as drags.
  const sensors = useSensors(useSensor(PointerSensor, { activationConstraint: { distance: 5 } }));

  function handleDragEnd(event: DragEndEvent) {
    const { active, over } = event;
    if (!over) return;
    const task = tasks.find((t) => t.id === active.id);
    const target = over.id as TaskState;
    if (task && task.state !== target) {
      onMove(task, target);
    }
  }

  return (
    <DndContext sensors={sensors} onDragEnd={handleDragEnd}>
      <div className="flex gap-4 overflow-x-auto pb-4">
        {columns.map((state) => (
          <Column
            key={state}
            state={state}
            tasks={tasks.filter((t) => t.state === state)}
            onOpen={onOpen}
          />
        ))}
      </div>
    </DndContext>
  );
}
