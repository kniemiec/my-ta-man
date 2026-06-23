// Typed fetch wrapper over the MyTaMan backend REST API. No business logic here —
// the backend owns all validation and storage rules.

export interface Task {
  id: string;
  name: string;
  description?: string;
  due?: string;
  state: string;
  created?: string;
  projectId: string;
}

export interface Project {
  id: string;
  name: string;
  description?: string;
  due?: string;
  state: string;
  created?: string;
  taskCount?: number;
  tasks?: Task[];
}

export class BackendError extends Error {
  status: number;
  constructor(status: number, message: string) {
    super(message);
    this.status = status;
  }
}

export class BackendClient {
  constructor(private readonly baseUrl: string) {}

  private async request<T>(method: string, path: string, body?: unknown): Promise<T> {
    const res = await fetch(this.baseUrl + path, {
      method,
      headers: body !== undefined ? { 'Content-Type': 'application/json' } : undefined,
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
    if (!res.ok) {
      let message = `${res.status} ${res.statusText}`;
      try {
        const data = (await res.json()) as { error?: string };
        if (data?.error) message = data.error;
      } catch {
        /* ignore non-JSON */
      }
      throw new BackendError(res.status, message);
    }
    if (res.status === 204) return undefined as T;
    return (await res.json()) as T;
  }

  // Projects
  listProjects = () => this.request<Project[]>('GET', '/api/projects');
  getProject = (id: string) => this.request<Project>('GET', `/api/projects/${id}`);
  createProject = (input: Record<string, unknown>) =>
    this.request<Project>('POST', '/api/projects', input);
  updateProject = (id: string, changes: Record<string, unknown>) =>
    this.request<Project>('PATCH', `/api/projects/${id}`, changes);
  deleteProject = (id: string) => this.request<void>('DELETE', `/api/projects/${id}`);

  // Tasks
  listTasks = (projectId?: string) =>
    this.request<Task[]>(
      'GET',
      projectId ? `/api/tasks?project=${encodeURIComponent(projectId)}` : '/api/tasks',
    );
  getTask = (id: string) => this.request<Task>('GET', `/api/tasks/${id}`);
  createTask = (input: Record<string, unknown>) =>
    this.request<Task>('POST', '/api/tasks', input);
  updateTask = (id: string, changes: Record<string, unknown>) =>
    this.request<Task>('PATCH', `/api/tasks/${id}`, changes);
  moveTask = (id: string, projectId: string) =>
    this.request<Task>('PATCH', `/api/tasks/${id}/move`, { projectId });
  deleteTask = (id: string) => this.request<void>('DELETE', `/api/tasks/${id}`);
}
