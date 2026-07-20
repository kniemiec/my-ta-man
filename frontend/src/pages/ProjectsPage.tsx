import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { projectsApi } from '../api/projects';
import type { Project } from '../api/types';
import { ProjectCard } from '../components/ProjectCard';
import { ProjectForm } from '../components/ProjectForm';

export function ProjectsPage() {
  const [projects, setProjects] = useState<Project[]>([]);
  const [showArchived, setShowArchived] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function reload() {
    try {
      setProjects(await projectsApi.list());
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to load projects');
    }
  }

  useEffect(() => {
    reload();
  }, []);

  async function create(input: { name: string; description?: string; progress?: string }) {
    await projectsApi.create(input);
    reload();
  }

  const visibleProjects = showArchived
    ? projects
    : projects.filter((p) => p.state !== 'archived');

  return (
    <div className="mx-auto max-w-5xl p-6">
      <header className="mb-6 flex items-center justify-between gap-4">
        <h1 className="text-2xl font-bold text-slate-800">Projects</h1>
        <label className="ml-auto flex items-center gap-2 text-sm text-slate-600">
          <input
            type="checkbox"
            checked={showArchived}
            onChange={(e) => setShowArchived(e.target.checked)}
          />
          Show archived
        </label>
        <Link to="/inbox" className="text-sm font-medium text-sky-700 hover:underline">
          Inbox →
        </Link>
      </header>

      {error && <div className="mb-4 rounded bg-rose-100 p-3 text-sm text-rose-800">{error}</div>}

      <div className="mb-6">
        <ProjectForm onCreate={create} />
      </div>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {visibleProjects.map((p) => (
          <ProjectCard key={p.id} project={p} />
        ))}
        {visibleProjects.length === 0 && !error && (
          <p className="text-slate-400">No projects yet. Create one to get started.</p>
        )}
      </div>
    </div>
  );
}
