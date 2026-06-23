import { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import { BackendClient } from './backendClient.js';
import { registerProjectTools } from './tools/projectTools.js';
import { registerTaskTools } from './tools/taskTools.js';

/** Build an McpServer with all MyTaMan tools registered against the given backend. */
export function createMcpServer(client: BackendClient): McpServer {
  const server = new McpServer({
    name: 'mytaman',
    version: '1.0.0',
  });

  registerProjectTools(server, client);
  registerTaskTools(server, client);

  return server;
}
