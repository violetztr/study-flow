import { LoginOutlined, LogoutOutlined, PlusOutlined, UserAddOutlined, UserOutlined } from '@ant-design/icons'
import { Button, Layout, Space } from 'antd'
import { useEffect, useState } from 'react'
import { Link, Outlet, useNavigate } from 'react-router-dom'
import {
  AUTH_WALLET_CHANGED_EVENT,
  clearSession,
  getStoredUser,
  getStoredWallet,
  type UserWalletResponse,
} from '../api/auth'

function AppLayout() {
  const navigate = useNavigate()
  const user = getStoredUser()
  const [wallet, setWallet] = useState<UserWalletResponse | null>(() => getStoredWallet())

  useEffect(() => {
    function syncWallet() {
      setWallet(getStoredWallet())
    }

    window.addEventListener(AUTH_WALLET_CHANGED_EVENT, syncWallet)
    window.addEventListener('storage', syncWallet)
    return () => {
      window.removeEventListener(AUTH_WALLET_CHANGED_EVENT, syncWallet)
      window.removeEventListener('storage', syncWallet)
    }
  }, [])

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
              <span className="pig-wallet" title="每日登录获得 1 个猪猪币">
                🐖 {wallet?.pigBalance ?? 0}
              </span>
              <Button icon={<UserOutlined />} onClick={() => navigate(`/circle/members/${user.id}`)}>
                我的主页
              </Button>
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
