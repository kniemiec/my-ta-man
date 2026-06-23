import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { tasksApi } from './tasks';

function mockFetch(status: number, body: unknown) {
  return vi.fn().mockResolvedValue({
    ok: status >= 200 && status < 300,
    status,
    statusText: 'OK',
    json: async () => body,
  } as Response);
}

describe('tasksApi', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', mockFetch(200, {}));
  });
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('list with a project filter builds the query string', async () => {
    await tasksApi.list('PROJ-3');
    expect(fetch).toHaveBeenCalledWith('/api/tasks?project=PROJ-3', expect.anything());
  });

  it('setState issues a PATCH with the state body', async () => {
    await tasksApi.setState('TASK-1', 'in-progress');
    const [url, opts] = (fetch as ReturnType<typeof vi.fn>).mock.calls[0];
    expect(url).toBe('/api/tasks/TASK-1');
    expect(opts.method).toBe('PATCH');
    expect(JSON.parse(opts.body)).toEqual({ state: 'in-progress' });
  });

  it('move posts to the move endpoint', async () => {
    await tasksApi.move('TASK-1', 'PROJ-2');
    const [url, opts] = (fetch as ReturnType<typeof vi.fn>).mock.calls[0];
    expect(url).toBe('/api/tasks/TASK-1/move');
    expect(JSON.parse(opts.body)).toEqual({ projectId: 'PROJ-2' });
  });

  it('create posts to /tasks', async () => {
    await tasksApi.create({ name: 'New', projectId: 'inbox' });
    const [url, opts] = (fetch as ReturnType<typeof vi.fn>).mock.calls[0];
    expect(url).toBe('/api/tasks');
    expect(opts.method).toBe('POST');
  });
});
