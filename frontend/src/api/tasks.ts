import { api } from './client';
import type { Task, TaskState } from './types';

export interface TaskInput {
  name: string;
  description?: string;
  progress?: string;
  due?: string | null;
  state?: TaskState;
  projectId?: string;
}

export const tasksApi = {
  list: (projectId?: string) =>
    api.get<Task[]>(projectId ? `/tasks?project=${encodeURIComponent(projectId)}` : '/tasks'),
  get: (id: string) => api.get<Task>(`/tasks/${id}`),
  create: (input: TaskInput) => api.post<Task>('/tasks', input),
  update: (id: string, changes: Partial<Omit<TaskInput, 'projectId'>>) =>
    api.patch<Task>(`/tasks/${id}`, changes),
  setState: (id: string, state: TaskState) => api.patch<Task>(`/tasks/${id}`, { state }),
  move: (id: string, projectId: string) =>
    api.patch<Task>(`/tasks/${id}/move`, { projectId }),
  remove: (id: string) => api.delete(`/tasks/${id}`),
};
