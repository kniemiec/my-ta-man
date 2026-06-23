import { defineConfig } from '@playwright/test';

// The e2e runs against the full docker-compose stack. By default Playwright will
// bring the stack up itself (and tear it down) using a scratch vault under e2e/.
// Set E2E_NO_WEBSERVER=1 to test an already-running stack.
const BASE_URL = process.env.E2E_BASE_URL ?? 'http://localhost:8080';

export default defineConfig({
  testDir: './tests',
  timeout: 60_000,
  expect: { timeout: 15_000 },
  fullyParallel: false,
  reporter: 'list',
  use: {
    baseURL: BASE_URL,
    trace: 'on-first-retry',
  },
  webServer: process.env.E2E_NO_WEBSERVER
    ? undefined
    : {
        command: 'docker compose up --build',
        cwd: '..',
        url: BASE_URL,
        timeout: 240_000,
        reuseExistingServer: !process.env.CI,
        env: { VAULT_PATH: './e2e/.tmp-vault' },
      },
});
