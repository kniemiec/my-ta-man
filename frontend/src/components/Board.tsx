import { DndContext, type DragEndEvent, PointerSensor, useSensor, useSensors } from '@dnd-kit/core';
import type { Task, TaskState } from '../api/types';
import { TASK_STATES } from '../api/types';
import { Column } from './Column';

interface Props {
  tasks: Task[];
  onMove: (task: Task, state: TaskState) => void;
  onOpen: (task: Task) => void;
}

export function Board({ tasks, onMove, onOpen }: Props) {
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
        {TASK_STATES.map((state) => (
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
