import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { BackendClient, BackendError } from './backendClient.js';

function mockFetch(status: number, body: unknown) {
  return vi.fn().mockResolvedValue({
    ok: status >= 200 && status < 300,
    status,
    statusText: 'OK',
    json: async () => body,
  } as Response);
}

const client = new BackendClient('http://backend:7000');

afterEach(() => vi.unstubAllGlobals());

describe('BackendClient', () => {
  beforeEach(() => vi.stubGlobal('fetch', mockFetch(200, {})));

  it('builds the list-tasks query string', async () => {
    await client.listTasks('PROJ-3');
    expect(fetch).toHaveBeenCalledWith(
      'http://backend:7000/api/tasks?project=PROJ-3',
      expect.anything(),
    );
  });

  it('moves a task via PATCH /tasks/{id}/move', async () => {
    await client.moveTask('TASK-1', 'PROJ-2');
    const [url, opts] = (fetch as ReturnType<typeof vi.fn>).mock.calls[0];
    expect(url).toBe('http://backend:7000/api/tasks/TASK-1/move');
    expect(opts.method).toBe('PATCH');
    expect(JSON.parse(opts.body)).toEqual({ projectId: 'PROJ-2' });
  });

  it('throws BackendError carrying the status and message', async () => {
    vi.stubGlobal('fetch', mockFetch(409, { error: 'Project still has tasks' }));
    await expect(client.deleteProject('PROJ-1')).rejects.toMatchObject({
      status: 409,
      message: 'Project still has tasks',
    } satisfies Partial<BackendError>);
  });
});
