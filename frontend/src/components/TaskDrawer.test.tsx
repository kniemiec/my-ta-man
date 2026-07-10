import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import type { Task } from '../api/types';
import { TaskDrawer } from './TaskDrawer';

const task: Task = {
  id: 'TASK-1',
  name: 'Write docs',
  description: 'Original body.',
  progress: 'Halfway there.',
  state: 'new',
  projectId: 'PROJ-1',
};

describe('TaskDrawer', () => {
  it('renders the description as markdown', () => {
    render(<TaskDrawer task={task} onClose={vi.fn()} onSave={vi.fn()} onDelete={vi.fn()} />);
    expect(screen.getByText('Original body.')).toBeInTheDocument();
  });

  it('edits and saves the description', async () => {
    const onSave = vi.fn();
    render(<TaskDrawer task={task} onClose={vi.fn()} onSave={onSave} onDelete={vi.fn()} />);

    await userEvent.click(screen.getByRole('button', { name: 'Edit' }));
    const textarea = screen.getByLabelText('Description');
    await userEvent.clear(textarea);
    await userEvent.type(textarea, 'Updated body.');
    await userEvent.click(screen.getByRole('button', { name: 'Save' }));

    expect(onSave).toHaveBeenCalledWith(
      expect.objectContaining({ description: 'Updated body.' }),
    );
  });

  it('renders the progress as markdown', () => {
    render(<TaskDrawer task={task} onClose={vi.fn()} onSave={vi.fn()} onDelete={vi.fn()} />);
    expect(screen.getByText('Halfway there.')).toBeInTheDocument();
  });

  it('edits and saves the progress', async () => {
    const onSave = vi.fn();
    render(<TaskDrawer task={task} onClose={vi.fn()} onSave={onSave} onDelete={vi.fn()} />);

    await userEvent.click(screen.getByRole('button', { name: 'Edit' }));
    const textarea = screen.getByLabelText('Progress');
    await userEvent.clear(textarea);
    await userEvent.type(textarea, 'Now in review.');
    await userEvent.click(screen.getByRole('button', { name: 'Save' }));

    expect(onSave).toHaveBeenCalledWith(
      expect.objectContaining({ progress: 'Now in review.' }),
    );
  });

  it('changes state via the dropdown', async () => {
    const onSave = vi.fn();
    render(<TaskDrawer task={task} onClose={vi.fn()} onSave={onSave} onDelete={vi.fn()} />);
    await userEvent.selectOptions(screen.getByLabelText('State'), 'in-progress');
    expect(onSave).toHaveBeenCalledWith({ state: 'in-progress' });
  });

  it('fires onDelete', async () => {
    const onDelete = vi.fn();
    render(<TaskDrawer task={task} onClose={vi.fn()} onSave={vi.fn()} onDelete={onDelete} />);
    await userEvent.click(screen.getByRole('button', { name: 'Delete' }));
    expect(onDelete).toHaveBeenCalled();
  });
});
