import { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import { z } from 'zod';
import type { BackendClient } from '../backendClient.js';
import { run } from './result.js';

const TASK_STATE = z.enum([
  'new',
  'in-progress',
  'blocked',
  'in-review',
  'completed',
]);

export function registerTaskTools(server: McpServer, client: BackendClient) {
  server.tool(
    'list_tasks',
    'List tasks, optionally filtered to one project (or "inbox").',
    { project: z.string().optional().describe('Project id or "inbox"') },
    ({ project }) => run(() => client.listTasks(project)),
  );

  server.tool(
    'get_task',
    'Get a single task by id.',
    { id: z.string().describe('Task id, e.g. TASK-7') },
    ({ id }) => run(() => client.getTask(id)),
  );

  server.tool(
    'create_task',
    'Create a task in a project (defaults to the Inbox).',
    {
      name: z.string().describe('Task name'),
      description: z.string().optional().describe('Details (markdown)'),
      due: z.string().optional().describe('Due date YYYY-MM-DD'),
      state: TASK_STATE.optional(),
      projectId: z.string().optional().describe('Target project id, or "inbox"'),
    },
    (args) => run(() => client.createTask(args)),
  );

  server.tool(
    'update_task',
    'Update fields of a task. Only the provided fields change.',
    {
      id: z.string(),
      name: z.string().optional(),
      description: z.string().optional(),
      due: z.string().nullable().optional().describe('YYYY-MM-DD, or null to clear'),
      state: TASK_STATE.optional(),
    },
    ({ id, ...changes }) => run(() => client.updateTask(id, changes)),
  );

  server.tool(
    'set_task_state',
    'Set just the state of a task.',
    { id: z.string(), state: TASK_STATE },
    ({ id, state }) => run(() => client.updateTask(id, { state })),
  );

  server.tool(
    'move_task',
    'Move a task to another project (or "inbox").',
    { id: z.string(), projectId: z.string().describe('Target project id or "inbox"') },
    ({ id, projectId }) => run(() => client.moveTask(id, projectId)),
  );

  server.tool(
    'delete_task',
    'Delete a task.',
    { id: z.string() },
    ({ id }) => run(() => client.deleteTask(id)),
  );
}
