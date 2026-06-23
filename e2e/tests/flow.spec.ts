import { expect, test } from '@playwright/test';
import { readFileSync, readdirSync } from 'node:fs';
import { join } from 'node:path';

// The scratch vault that docker-compose mounts into the backend (see playwright.config.ts).
const VAULT_DIR = join(process.cwd(), '.tmp-vault');

function findTaskFile(projectSlug: string, taskSlug: string): string {
  const dir = join(VAULT_DIR, projectSlug);
  const file = readdirSync(dir).find((f) => f.startsWith(taskSlug) && f.endsWith('.md'));
  if (!file) throw new Error(`Task file not found in ${dir}`);
  return readFileSync(join(dir, file), 'utf-8');
}

test('create project, add task, drag to in-progress; state persists in API and on disk', async ({
  page,
  request,
}) => {
  const projectName = `E2E Project ${Date.now()}`;

  // Create a project from the projects page.
  await page.goto('/');
  await page.getByRole('button', { name: '+ New project' }).click();
  await page.getByLabel('Project name').fill(projectName);
  await page.getByRole('button', { name: 'Create' }).click();

  // Open its board.
  await page.getByText(projectName).click();
  await expect(page.getByRole('heading', { name: projectName })).toBeVisible();

  // Add a task.
  await page.getByLabel('New task name').fill('E2E Task');
  await page.getByRole('button', { name: 'Add task' }).click();

  const card = page.locator('[data-testid^="task-card-"]', { hasText: 'E2E Task' });
  await expect(card).toBeVisible();
  const taskId = (await card.getAttribute('data-testid'))!.replace('task-card-', '');

  // Drag the card into the "In Progress" column (dnd-kit pointer sequence).
  const target = page.getByTestId('column-in-progress');
  const cb = (await card.boundingBox())!;
  const tb = (await target.boundingBox())!;
  await page.mouse.move(cb.x + cb.width / 2, cb.y + cb.height / 2);
  await page.mouse.down();
  await page.mouse.move(cb.x + cb.width / 2 + 30, cb.y + cb.height / 2, { steps: 5 });
  await page.mouse.move(tb.x + tb.width / 2, tb.y + tb.height / 2, { steps: 10 });
  await page.mouse.up();

  // Assert via the API that the state changed.
  await expect
    .poll(async () => {
      const res = await request.get(`/api/tasks/${taskId}`);
      return (await res.json()).state;
    })
    .toBe('in-progress');

  // Assert the markdown file on the mounted vault reflects it (filesystem-as-truth).
  const projectSlug = projectName.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/(^-|-$)/g, '');
  await expect
    .poll(() => findTaskFile(projectSlug, 'e2e-task'))
    .toContain('state: in-progress');
});
