import { LoginOutlined, LogoutOutlined, PlusOutlined, UserAddOutlined } from '@ant-design/icons'
import { Button, Layout, Space } from 'antd'
import { Link, Outlet, useNavigate } from 'react-router-dom'
import { clearSession, getStoredUser } from '../api/auth'

function AppLayout() {
  const navigate = useNavigate()
  const user = getStoredUser()

  function handleLogout() {
    clearSession()
    navigate('/circle', { replace: true })
  }

  return (
    <Layout className="app-layout">
      <header className="app-header compact-header">
        <Link className="compact-brand" to="/circle">
          <span className="brand-mark small">R</span>
          <span>
            <strong>ruru</strong>
          </span>
        </Link>

        <Space wrap size={10} className="compact-actions">
          {user ? (
            <>
              <span className="app-user">{user.username}</span>
              <Button type="primary" icon={<PlusOutlined />} onClick={() => navigate('/circle/posts/new')}>
                发布
              </Button>
              {user.username === 'ruru' ? (
                <Button onClick={() => navigate('/admin/community')}>
                  审核
                </Button>
              ) : null}
              <Button icon={<LogoutOutlined />} onClick={handleLogout}>
                退出
              </Button>
            </>
          ) : (
            <>
              <Button icon={<LoginOutlined />} onClick={() => navigate('/login', { state: { from: '/circle' } })}>
                登录
              </Button>
              <Button type="primary" icon={<UserAddOutlined />} onClick={() => navigate('/register')}>
                注册
              </Button>
            </>
          )}
        </Space>
      </header>

      <Layout.Content className="app-content">
        <Outlet />
      </Layout.Content>
    </Layout>
  )
}

export default AppLayout
