import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import type { Project } from '../api/types';
import { ProjectDrawer } from './ProjectDrawer';

const project: Project = {
  id: 'PROJ-1',
  name: 'Alpha',
  description: 'Project rationale.',
  progress: 'Kickoff done.',
  state: 'new',
};

describe('ProjectDrawer', () => {
  it('renders description and progress as markdown', () => {
    render(<ProjectDrawer project={project} onClose={vi.fn()} onSave={vi.fn()} />);
    expect(screen.getByText('Project rationale.')).toBeInTheDocument();
    expect(screen.getByText('Kickoff done.')).toBeInTheDocument();
  });

  it('edits and saves the progress', async () => {
    const onSave = vi.fn();
    render(<ProjectDrawer project={project} onClose={vi.fn()} onSave={onSave} />);

    await userEvent.click(screen.getByRole('button', { name: 'Edit' }));
    const textarea = screen.getByLabelText('Progress');
    await userEvent.clear(textarea);
    await userEvent.type(textarea, 'Milestone 1 reached.');
    await userEvent.click(screen.getByRole('button', { name: 'Save' }));

    expect(onSave).toHaveBeenCalledWith(
      expect.objectContaining({ progress: 'Milestone 1 reached.' }),
    );
  });

  it('changes state via the dropdown', async () => {
    const onSave = vi.fn();
    render(<ProjectDrawer project={project} onClose={vi.fn()} onSave={onSave} />);
    await userEvent.selectOptions(screen.getByLabelText('State'), 'in-progress');
    expect(onSave).toHaveBeenCalledWith({ state: 'in-progress' });
  });

  it('archives via the Archive button', async () => {
    const onSave = vi.fn();
    render(<ProjectDrawer project={project} onClose={vi.fn()} onSave={onSave} />);
    await userEvent.click(screen.getByRole('button', { name: 'Archive' }));
    expect(onSave).toHaveBeenCalledWith({ state: 'archived' });
  });

  it('offers Unarchive for an archived project', async () => {
    const onSave = vi.fn();
    render(
      <ProjectDrawer
        project={{ ...project, state: 'archived' }}
        onClose={vi.fn()}
        onSave={onSave}
      />,
    );
    await userEvent.click(screen.getByRole('button', { name: 'Unarchive' }));
    expect(onSave).toHaveBeenCalledWith({ state: 'new' });
  });
});
