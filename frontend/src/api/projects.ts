import { api } from './client';
import type { Project, ProjectState } from './types';

export interface ProjectInput {
  name: string;
  description?: string;
  due?: string | null;
  state?: ProjectState;
}

export const projectsApi = {
  list: () => api.get<Project[]>('/projects'),
  get: (id: string) => api.get<Project>(`/projects/${id}`),
  create: (input: ProjectInput) => api.post<Project>('/projects', input),
  update: (id: string, changes: Partial<ProjectInput>) =>
    api.patch<Project>(`/projects/${id}`, changes),
  remove: (id: string) => api.delete(`/projects/${id}`),
};
