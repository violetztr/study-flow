import { Button, Card, Result } from 'antd'
import { Navigate, Route, Routes, useNavigate } from 'react-router-dom'
import LoginPage from './pages/LoginPage'
import RegisterPage from './pages/RegisterPage'
import DashboardPage from './pages/DashboardPage'
import ProtectedRoute from './routes/ProtectedRoute'

type PlaceholderPageProps = {
  title: string
  description: string
}

function PlaceholderPage({ title, description }: PlaceholderPageProps) {
  const navigate = useNavigate()
  return (
    <main className="placeholder-page">
      <Card className="placeholder-card">
        <Result
          status="info"
          title={title}
          subTitle={description}
          extra={
            <Button type="primary" onClick={() => navigate('/dashboard')}>
              返回仪表盘
            </Button>
          }
        />
      </Card>
    </main>
  )
}

function App() {
  return (
    <Routes>
      <Route path="/" element={<Navigate to="/dashboard" replace />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route element={<ProtectedRoute />}>
        <Route path="/dashboard" element={<DashboardPage />} />
        <Route
          path="/projects"
          element={
            <PlaceholderPage
              title="项目管理即将接入"
              description="下一阶段会在这里实现项目列表、新增、编辑和删除。"
            />
          }
        />
        <Route
          path="/projects/:id"
          element={
            <PlaceholderPage
              title="项目详情即将接入"
              description="这里会展示项目基础信息和该项目下的任务列表。"
            />
          }
        />
        <Route
          path="/tasks"
          element={
            <PlaceholderPage
              title="任务管理即将接入"
              description="这里会实现任务筛选、新增、编辑、删除和标签绑定。"
            />
          }
        />
        <Route
          path="/settings/profile"
          element={
            <PlaceholderPage
              title="个人资料即将接入"
              description="这里会展示当前用户信息，后续可以扩展头像和偏好设置。"
            />
          }
        />
      </Route>
      <Route path="*" element={<Navigate to="/dashboard" replace />} />
    </Routes>
  )
}

export default App
