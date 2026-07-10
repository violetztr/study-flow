import {
  LogoutOutlined,
  SafetyOutlined,
  TeamOutlined,
  UserOutlined,
} from '@ant-design/icons'
import { Button, Layout, Menu, Space, Typography } from 'antd'
import type { MenuProps } from 'antd'
import { Link, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { clearSession, getStoredUser } from '../api/auth'

const selectableMenuKeys = ['/circle/members', '/admin/community', '/circle']

function AppLayout() {
  const navigate = useNavigate()
  const location = useLocation()
  const user = getStoredUser()
  const isAdmin = user?.role === 'ADMIN' || user?.role === 'OWNER'

  const menuItems: MenuProps['items'] = [
    {
      key: 'community',
      type: 'group',
      label: '社区',
      children: [
        { key: '/circle', icon: <TeamOutlined />, label: <Link to="/circle">社区动态</Link> },
        { key: '/circle/members', icon: <UserOutlined />, label: <Link to="/circle/members">成员</Link> },
        ...(isAdmin
          ? [
              {
                key: '/admin/community',
                icon: <SafetyOutlined />,
                label: <Link to="/admin/community">社区管理</Link>,
              },
            ]
          : []),
      ],
    },
  ]

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
          <span className="brand-mark">R</span>
          <span>
            <strong>Ruru 社区</strong>
            <small>朋友的小圈子</small>
          </span>
        </Link>
        <Menu className="app-menu" mode="inline" selectedKeys={[selectedKey]} items={menuItems} />
      </Layout.Sider>

      <Layout>
        <header className="app-header">
          <div>
            <Typography.Text className="dashboard-kicker">Ruru Community</Typography.Text>
            <Typography.Title level={4} style={{ margin: 0 }}>
              先把最基础的小圈子跑通
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
