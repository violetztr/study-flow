import { Navigate, Route, Routes } from 'react-router-dom'
import AppLayout from './layouts/AppLayout'
import LoginPage from './pages/LoginPage'
import RegisterPage from './pages/RegisterPage'
import CircleFeedPage from './pages/CircleFeedPage'
import CreatePostPage from './pages/CreatePostPage'
import MySubmissionsPage from './pages/MySubmissionsPage'
import WatchHistoryPage from './pages/WatchHistoryPage'
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
      <Route element={<AppLayout />}>
        <Route path="/circle" element={<CircleFeedPage />} />
        <Route path="/circle/posts/:id" element={<PostDetailPage />} />
        <Route path="/circle/members/:id" element={<MemberProfilePage />} />
        <Route element={<ProtectedRoute />}>
          <Route path="/circle/posts/new" element={<CreatePostPage />} />
          <Route path="/circle/submissions" element={<MySubmissionsPage />} />
          <Route path="/circle/history" element={<WatchHistoryPage />} />
          <Route path="/circle/members" element={<MembersPage />} />
          <Route path="/admin/community" element={<CommunityAdminPage />} />
        </Route>
      </Route>
      <Route path="*" element={<Navigate to="/circle" replace />} />
    </Routes>
  )
}

export default App
