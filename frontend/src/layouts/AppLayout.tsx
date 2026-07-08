import {
  BarChartOutlined,
  CheckSquareOutlined,
  FolderOpenOutlined,
  LogoutOutlined,
  UserOutlined,
} from '@ant-design/icons'
import { Button, Layout, Menu, Space, Typography } from 'antd'
import { Link, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { clearSession, getStoredUser } from '../api/auth'

const menuItems = [
  { key: '/dashboard', icon: <BarChartOutlined />, label: <Link to="/dashboard">仪表盘</Link> },
  { key: '/projects', icon: <FolderOpenOutlined />, label: <Link to="/projects">项目</Link> },
  { key: '/tasks', icon: <CheckSquareOutlined />, label: <Link to="/tasks">任务</Link> },
  {
    key: '/settings/profile',
    icon: <UserOutlined />,
    label: <Link to="/settings/profile">个人资料</Link>,
  },
]

function AppLayout() {
  const navigate = useNavigate()
  const location = useLocation()
  const user = getStoredUser()

  function handleLogout() {
    clearSession()
    navigate('/login', { replace: true })
  }

  const selectedKey =
    menuItems.find((item) => location.pathname.startsWith(item.key))?.key ??
    '/dashboard'

  return (
    <Layout className="app-layout">
      <Layout.Sider className="app-sider" width={248} breakpoint="lg" collapsedWidth={0}>
        <Link className="app-logo" to="/dashboard">
          <span className="brand-mark">S</span>
          <span>
            <strong>StudyFlow</strong>
            <small>全栈学习驾驶舱</small>
          </span>
        </Link>
        <Menu
          className="app-menu"
          mode="inline"
          selectedKeys={[selectedKey]}
          items={menuItems}
        />
      </Layout.Sider>

      <Layout>
        <header className="app-header">
          <div>
            <Typography.Text className="dashboard-kicker">Keep shipping</Typography.Text>
            <Typography.Title level={4} style={{ margin: 0 }}>
              把今天的学习推进一点点
            </Typography.Title>
          </div>
          <Space>
            {user ? <span className="app-user">{user.username}</span> : null}
            <Button icon={<LogoutOutlined />} onClick={handleLogout}>
              退出
            </Button>
          </Space>
        </header>

        <Layout.Content className="app-content">
          <Outlet />
        </Layout.Content>
      </Layout>
    </Layout>
  )
}

export default AppLayout
