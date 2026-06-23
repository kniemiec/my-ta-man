import { Client } from '@modelcontextprotocol/sdk/client/index.js';
import { InMemoryTransport } from '@modelcontextprotocol/sdk/inMemory.js';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import type { BackendClient } from './backendClient.js';
import { createMcpServer } from './server.js';

// A fully stubbed backend so we assert tool->client mapping, not real HTTP.
function fakeBackend() {
  return {
    listProjects: vi.fn().mockResolvedValue([]),
    getProject: vi.fn().mockResolvedValue({ id: 'PROJ-1' }),
    createProject: vi.fn().mockResolvedValue({ id: 'PROJ-1', name: 'Alpha' }),
    updateProject: vi.fn().mockResolvedValue({ id: 'PROJ-1' }),
    deleteProject: vi.fn().mockResolvedValue(undefined),
    listTasks: vi.fn().mockResolvedValue([]),
    getTask: vi.fn().mockResolvedValue({ id: 'TASK-1' }),
    createTask: vi.fn().mockResolvedValue({ id: 'TASK-1', name: 'Fix' }),
    updateTask: vi.fn().mockResolvedValue({ id: 'TASK-1', state: 'in-progress' }),
    moveTask: vi.fn().mockResolvedValue({ id: 'TASK-1', projectId: 'PROJ-2' }),
    deleteTask: vi.fn().mockResolvedValue(undefined),
  };
}

let backend: ReturnType<typeof fakeBackend>;
let client: Client;

beforeEach(async () => {
  backend = fakeBackend();
  const server = createMcpServer(backend as unknown as BackendClient);
  client = new Client({ name: 'test', version: '1.0.0' });
  const [clientTransport, serverTransport] = InMemoryTransport.createLinkedPair();
  await Promise.all([server.connect(serverTransport), client.connect(clientTransport)]);
});

afterEach(async () => {
  await client.close();
});

async function call(name: string, args: Record<string, unknown> = {}) {
  return client.callTool({ name, arguments: args });
}

describe('MCP tools', () => {
  it('exposes all project and task tools', async () => {
    const { tools } = await client.listTools();
    const names = tools.map((t) => t.name);
    expect(names).toEqual(
      expect.arrayContaining([
        'list_projects',
        'create_project',
        'get_project',
        'update_project',
        'delete_project',
        'list_tasks',
        'create_task',
        'set_task_state',
        'move_task',
        'delete_task',
      ]),
    );
  });

  it('create_task forwards args to the backend', async () => {
    await call('create_task', { name: 'Fix', projectId: 'PROJ-1' });
    expect(backend.createTask).toHaveBeenCalledWith({ name: 'Fix', projectId: 'PROJ-1' });
  });

  it('list_tasks passes the project filter', async () => {
    await call('list_tasks', { project: 'PROJ-3' });
    expect(backend.listTasks).toHaveBeenCalledWith('PROJ-3');
  });

  it('set_task_state maps to an update with only the state', async () => {
    await call('set_task_state', { id: 'TASK-1', state: 'in-progress' });
    expect(backend.updateTask).toHaveBeenCalledWith('TASK-1', { state: 'in-progress' });
  });

  it('update_task strips id and forwards the rest', async () => {
    await call('update_task', { id: 'TASK-1', name: 'New name' });
    expect(backend.updateTask).toHaveBeenCalledWith('TASK-1', { name: 'New name' });
  });

  it('move_task forwards id and target project', async () => {
    await call('move_task', { id: 'TASK-1', projectId: 'PROJ-2' });
    expect(backend.moveTask).toHaveBeenCalledWith('TASK-1', 'PROJ-2');
  });

  it('create_project returns the created project as JSON text', async () => {
    const res = await call('create_project', { name: 'Alpha' });
    const text = (res.content as { type: string; text: string }[])[0].text;
    expect(JSON.parse(text)).toMatchObject({ id: 'PROJ-1', name: 'Alpha' });
  });

  it('surfaces backend errors as tool errors', async () => {
    backend.deleteProject.mockRejectedValueOnce(
      Object.assign(new Error('Project still has tasks'), { status: 409 }),
    );
    const res = await call('delete_project', { id: 'PROJ-1' });
    expect(res.isError).toBe(true);
  });
});
