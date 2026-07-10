import {
  BarChartOutlined,
  CalendarOutlined,
  CheckSquareOutlined,
  FileTextOutlined,
  FolderOpenOutlined,
  GlobalOutlined,
  LogoutOutlined,
  RocketOutlined,
  SafetyOutlined,
  TeamOutlined,
  UserOutlined,
} from '@ant-design/icons'
import { Button, Layout, Menu, Space, Typography } from 'antd'
import type { MenuProps } from 'antd'
import { Link, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { clearSession, getStoredUser } from '../api/auth'

const selectableMenuKeys = [
  '/circle/members',
  '/circle',
  '/admin/community',
  '/dashboard',
  '/project-hub',
  '/projects',
  '/tasks',
  '/notes',
  '/daily',
  '/settings/profile',
]

const menuItems: MenuProps['items'] = [
  {
    key: 'circle',
    type: 'group',
    label: 'Violet Circle',
    children: [
      { key: '/circle', icon: <TeamOutlined />, label: <Link to="/circle">圈子动态</Link> },
      { key: '/circle/members', icon: <UserOutlined />, label: <Link to="/circle/members">圈子成员</Link> },
      { key: '/admin/community', icon: <SafetyOutlined />, label: <Link to="/admin/community">圈子管理</Link> },
    ],
  },
  { key: '/dashboard', icon: <BarChartOutlined />, label: <Link to="/dashboard">驾驶舱</Link> },
  {
    key: 'devflow',
    type: 'group',
    label: '研发中台',
    children: [
      { key: '/project-hub', icon: <RocketOutlined />, label: <Link to="/project-hub">项目中台</Link> },
      { key: '/portfolio-public', icon: <GlobalOutlined />, label: <Link to="/portfolio">公开作品集</Link> },
    ],
  },
  {
    key: 'study',
    type: 'group',
    label: '学习',
    children: [
      { key: '/projects', icon: <FolderOpenOutlined />, label: <Link to="/projects">学习项目</Link> },
      { key: '/tasks', icon: <CheckSquareOutlined />, label: <Link to="/tasks">学习任务</Link> },
    ],
  },
  {
    key: 'notes',
    type: 'group',
    label: '笔记',
    children: [
      { key: '/notes', icon: <FileTextOutlined />, label: <Link to="/notes">笔记工作台</Link> },
    ],
  },
  {
    key: 'daily',
    type: 'group',
    label: '日常',
    children: [
      { key: '/daily', icon: <CalendarOutlined />, label: <Link to="/daily">今日计划</Link> },
    ],
  },
  {
    key: 'settings',
    type: 'group',
    label: '设置',
    children: [
      {
        key: '/settings/profile',
        icon: <UserOutlined />,
        label: <Link to="/settings/profile">个人资料</Link>,
      },
    ],
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
    selectableMenuKeys
      .filter((key) => location.pathname === key || location.pathname.startsWith(`${key}/`))
      .sort((first, second) => second.length - first.length)[0] ?? '/circle'

  return (
    <Layout className="app-layout">
      <Layout.Sider className="app-sider" width={248} breakpoint="lg" collapsedWidth={0}>
        <Link className="app-logo" to="/circle">
          <span className="brand-mark">D</span>
          <span>
            <strong>DevFlow Studio</strong>
            <small>个人全栈研发中台</small>
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
