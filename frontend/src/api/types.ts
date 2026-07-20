// Shared types mirroring the backend DTOs.

export type TaskState =
  | 'new'
  | 'in-progress'
  | 'blocked'
  | 'in-review'
  | 'completed'
  | 'archived';

export type ProjectState = 'new' | 'in-progress' | 'completed' | 'archived';

/** Active states — the default board columns / visible project set (archived excluded). */
export const TASK_STATES: TaskState[] = [
  'new',
  'in-progress',
  'blocked',
  'in-review',
  'completed',
];

export const PROJECT_STATES: ProjectState[] = ['new', 'in-progress', 'completed'];

/** All states including `archived` — used by state dropdowns and the toggled board. */
export const TASK_STATES_WITH_ARCHIVED: TaskState[] = [...TASK_STATES, 'archived'];

export const PROJECT_STATES_WITH_ARCHIVED: ProjectState[] = [...PROJECT_STATES, 'archived'];

export interface Task {
  id: string;
  name: string;
  description?: string;
  progress?: string;
  due?: string;
  state: TaskState;
  created?: string;
  projectId: string;
}

export interface Project {
  id: string;
  name: string;
  description?: string;
  progress?: string;
  due?: string;
  state: ProjectState;
  created?: string;
  taskCount?: number;
  tasks?: Task[];
}

/** Logical id of the project-less Inbox. */
export const INBOX_ID = 'inbox';
