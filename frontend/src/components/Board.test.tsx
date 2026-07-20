import { render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import type { Task } from '../api/types';
import { Board } from './Board';

const tasks: Task[] = [
  { id: 'TASK-1', name: 'Active task', state: 'new', projectId: 'PROJ-1' },
  { id: 'TASK-2', name: 'Done task', state: 'archived', projectId: 'PROJ-1' },
];

describe('Board', () => {
  it('hides the archived column by default', () => {
    render(<Board tasks={tasks} onMove={vi.fn()} onOpen={vi.fn()} />);
    expect(screen.queryByTestId('column-archived')).not.toBeInTheDocument();
    // The archived task, which only lives in that column, is not shown.
    expect(screen.queryByText('Done task')).not.toBeInTheDocument();
    expect(screen.getByText('Active task')).toBeInTheDocument();
  });

  it('shows the archived column and its tasks when toggled on', () => {
    render(<Board tasks={tasks} onMove={vi.fn()} onOpen={vi.fn()} showArchived />);
    expect(screen.getByTestId('column-archived')).toBeInTheDocument();
    expect(screen.getByText('Done task')).toBeInTheDocument();
  });
});
