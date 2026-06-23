import { BackendError } from '../backendClient.js';

export interface ToolResult {
  // Index signature required to match the SDK's CallToolResult shape.
  [x: string]: unknown;
  content: { type: 'text'; text: string }[];
  isError?: boolean;
}

/** Wrap a successful payload as a JSON text tool result. */
export function ok(data: unknown): ToolResult {
  const text = data === undefined ? 'OK' : JSON.stringify(data, null, 2);
  return { content: [{ type: 'text', text }] };
}

/** Run a backend call and translate failures into a clean tool error result. */
export async function run(fn: () => Promise<unknown>): Promise<ToolResult> {
  try {
    return ok(await fn());
  } catch (e) {
    const message =
      e instanceof BackendError ? `Backend error (${e.status}): ${e.message}` : String(e);
    return { content: [{ type: 'text', text: message }], isError: true };
  }
}
