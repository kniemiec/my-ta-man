// Shared types mirroring the backend DTOs.

export type TaskState =
  | 'new'
  | 'in-progress'
  | 'blocked'
  | 'in-review'
  | 'completed';

export type ProjectState = 'new' | 'in-progress' | 'completed';

export const TASK_STATES: TaskState[] = [
  'new',
  'in-progress',
  'blocked',
  'in-review',
  'completed',
];

export const PROJECT_STATES: ProjectState[] = ['new', 'in-progress', 'completed'];

export interface Task {
  id: string;
  name: string;
  description?: string;
  due?: string;
  state: TaskState;
  created?: string;
  projectId: string;
}

export interface Project {
  id: string;
  name: string;
  description?: string;
  due?: string;
  state: ProjectState;
  created?: string;
  taskCount?: number;
  tasks?: Task[];
}

/** Logical id of the project-less Inbox. */
export const INBOX_ID = 'inbox';
