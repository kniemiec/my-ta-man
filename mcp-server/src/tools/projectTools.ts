import { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import { z } from 'zod';
import type { BackendClient } from '../backendClient.js';
import { run } from './result.js';

const PROJECT_STATE = z.enum(['new', 'in-progress', 'completed']);

export function registerProjectTools(server: McpServer, client: BackendClient) {
  server.tool(
    'list_projects',
    'List all projects with their state and task counts.',
    {},
    () => run(() => client.listProjects()),
  );

  server.tool(
    'get_project',
    'Get a single project by id, including its tasks.',
    { id: z.string().describe('Project id, e.g. PROJ-3') },
    ({ id }) => run(() => client.getProject(id)),
  );

  server.tool(
    'create_project',
    'Create a new project (a folder in the vault).',
    {
      name: z.string().describe('Project name'),
      description: z.string().optional().describe('Background/rationale (markdown)'),
      due: z.string().optional().describe('Due date YYYY-MM-DD'),
      state: PROJECT_STATE.optional(),
    },
    (args) => run(() => client.createProject(args)),
  );

  server.tool(
    'update_project',
    'Update fields of a project. Only the provided fields change.',
    {
      id: z.string(),
      name: z.string().optional(),
      description: z.string().optional(),
      due: z.string().nullable().optional().describe('YYYY-MM-DD, or null to clear'),
      state: PROJECT_STATE.optional(),
    },
    ({ id, ...changes }) => run(() => client.updateProject(id, changes)),
  );

  server.tool(
    'delete_project',
    'Delete a project. Fails if it still has tasks.',
    { id: z.string() },
    ({ id }) => run(() => client.deleteProject(id)),
  );
}
