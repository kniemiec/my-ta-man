import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { DndContext } from '@dnd-kit/core';
import { describe, expect, it, vi } from 'vitest';
import type { Task } from '../api/types';
import { TaskCard } from './TaskCard';

const task: Task = {
  id: 'TASK-1',
  name: 'Fix login bug',
  state: 'new',
  due: '2026-07-01',
  projectId: 'PROJ-1',
};

function renderCard(onOpen = vi.fn()) {
  render(
    <DndContext>
      <TaskCard task={task} onOpen={onOpen} />
    </DndContext>,
  );
  return onOpen;
}

describe('TaskCard', () => {
  it('shows the task name, id and due date', () => {
    renderCard();
    expect(screen.getByText('Fix login bug')).toBeInTheDocument();
    expect(screen.getByText('TASK-1')).toBeInTheDocument();
    expect(screen.getByText(/due 2026-07-01/)).toBeInTheDocument();
  });

  it('calls onOpen when the title is clicked', async () => {
    const onOpen = renderCard();
    await userEvent.click(screen.getByText('Fix login bug'));
    expect(onOpen).toHaveBeenCalledWith(task);
  });
});
