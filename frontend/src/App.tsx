import { Navigate, Route, Routes } from 'react-router-dom'
import AppLayout from './layouts/AppLayout'
import LoginPage from './pages/LoginPage'
import RegisterPage from './pages/RegisterPage'
import DashboardPage from './pages/DashboardPage'
import ProjectsPage from './pages/ProjectsPage'
import ProjectDetailPage from './pages/ProjectDetailPage'
import ProjectHubPage from './pages/ProjectHubPage'
import PublicPortfolioPage from './pages/PublicPortfolioPage'
import PublicProjectDetailPage from './pages/PublicProjectDetailPage'
import TasksPage from './pages/TasksPage'
import NotesPage from './pages/NotesPage'
import DailyPage from './pages/DailyPage'
import ProfilePage from './pages/ProfilePage'
import CircleFeedPage from './pages/CircleFeedPage'
import CreatePostPage from './pages/CreatePostPage'
import PostDetailPage from './pages/PostDetailPage'
import MembersPage from './pages/MembersPage'
import MemberProfilePage from './pages/MemberProfilePage'
import CommunityAdminPage from './pages/CommunityAdminPage'
import ProtectedRoute from './routes/ProtectedRoute'

function App() {
  return (
    <Routes>
      <Route path="/" element={<Navigate to="/circle" replace />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route path="/portfolio" element={<PublicPortfolioPage />} />
      <Route path="/portfolio/:slug" element={<PublicProjectDetailPage />} />
      <Route element={<ProtectedRoute />}>
        <Route element={<AppLayout />}>
          <Route path="/circle" element={<CircleFeedPage />} />
          <Route path="/circle/posts/new" element={<CreatePostPage />} />
          <Route path="/circle/posts/:id" element={<PostDetailPage />} />
          <Route path="/circle/members" element={<MembersPage />} />
          <Route path="/circle/members/:id" element={<MemberProfilePage />} />
          <Route path="/admin/community" element={<CommunityAdminPage />} />
          <Route path="/dashboard" element={<DashboardPage />} />
          <Route path="/project-hub" element={<ProjectHubPage />} />
          <Route path="/projects" element={<ProjectsPage />} />
          <Route path="/projects/:id" element={<ProjectDetailPage />} />
          <Route path="/tasks" element={<TasksPage />} />
          <Route path="/notes" element={<NotesPage />} />
          <Route path="/daily" element={<DailyPage />} />
          <Route path="/settings/profile" element={<ProfilePage />} />
        </Route>
      </Route>
      <Route path="*" element={<Navigate to="/circle" replace />} />
    </Routes>
  )
}

export default App
