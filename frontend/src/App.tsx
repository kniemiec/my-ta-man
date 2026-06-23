import { BrowserRouter, Route, Routes } from 'react-router-dom';
import { ProjectBoardPage } from './pages/ProjectBoardPage';
import { ProjectsPage } from './pages/ProjectsPage';

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<ProjectsPage />} />
        <Route path="/projects/:id" element={<ProjectBoardPage />} />
        <Route path="/inbox" element={<ProjectBoardPage inbox />} />
      </Routes>
    </BrowserRouter>
  );
}
