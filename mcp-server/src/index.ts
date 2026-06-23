import { StreamableHTTPServerTransport } from '@modelcontextprotocol/sdk/server/streamableHttp.js';
import express from 'express';
import { BackendClient } from './backendClient.js';
import { createMcpServer } from './server.js';

const BACKEND_URL = process.env.BACKEND_URL ?? 'http://localhost:7000';
const PORT = Number(process.env.PORT ?? 3001);

const client = new BackendClient(BACKEND_URL);
const app = express();
app.use(express.json());

app.get('/health', (_req, res) => {
  res.json({ status: 'ok', backend: BACKEND_URL });
});

// Stateless Streamable HTTP: a fresh server + transport per request. Fine for a
// single-user local tool and avoids any cross-request session bookkeeping.
app.post('/mcp', async (req, res) => {
  const server = createMcpServer(client);
  const transport = new StreamableHTTPServerTransport({ sessionIdGenerator: undefined });

  res.on('close', () => {
    transport.close();
    server.close();
  });

  try {
    await server.connect(transport);
    await transport.handleRequest(req, res, req.body);
  } catch (err) {
    console.error('MCP request failed:', err);
    if (!res.headersSent) {
      res.status(500).json({
        jsonrpc: '2.0',
        error: { code: -32603, message: 'Internal server error' },
        id: null,
      });
    }
  }
});

// Stateless mode does not support server-initiated streams via GET/DELETE.
const methodNotAllowed = (_req: express.Request, res: express.Response) => {
  res.status(405).json({
    jsonrpc: '2.0',
    error: { code: -32000, message: 'Method not allowed.' },
    id: null,
  });
};
app.get('/mcp', methodNotAllowed);
app.delete('/mcp', methodNotAllowed);

app.listen(PORT, () => {
  console.log(`MyTaMan MCP server listening on :${PORT}/mcp (backend: ${BACKEND_URL})`);
});
