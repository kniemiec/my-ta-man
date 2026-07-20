import { Link } from 'react-router-dom';
import type { Project } from '../api/types';
import { StateTag } from './StateTag';

export function ProjectCard({ project }: { project: Project }) {
  return (
    <Link
      to={`/projects/${project.id}`}
      className={`flex flex-col gap-2 rounded-xl border border-slate-200 bg-white p-4 shadow-sm transition hover:border-slate-300 hover:shadow ${
        project.state === 'archived' ? 'opacity-60' : ''
      }`}
    >
      <div className="flex items-start justify-between gap-2">
        <h3 className="font-semibold text-slate-800">{project.name}</h3>
        <StateTag state={project.state} />
      </div>
      <div className="flex items-center gap-2 text-xs text-slate-500">
        <span>{project.id}</span>
        <span>· {project.taskCount ?? 0} tasks</span>
        {project.due && <span>· due {project.due}</span>}
      </div>
    </Link>
  );
}
