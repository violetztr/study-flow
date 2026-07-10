import { Alert, Button, Card, Form, Input } from 'antd'
import { useMutation } from '@tanstack/react-query'
import { Link, useLocation, useNavigate, useSearchParams } from 'react-router-dom'
import {
  type LoginRequest,
  type LoginResponse,
  login,
  saveSession,
} from '../api/auth'

function LoginPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const [searchParams] = useSearchParams()
  const registered = searchParams.get('registered') === '1'

  const loginMutation = useMutation<LoginResponse, Error, LoginRequest>({
    mutationFn: login,
    onSuccess: (response) => {
      saveSession(response)
      const from = typeof location.state?.from === 'string' ? location.state.from : '/circle'
      navigate(from, { replace: true })
    },
  })

  return (
    <main className="auth-page">
      <section className="auth-form-panel">
        <Card className="auth-card">
          <p className="auth-kicker">Welcome back</p>
          <h2 className="auth-card-title">登录 ruru</h2>
          <p className="auth-card-subtitle">回到你和朋友的小圈子。</p>

          {registered ? (
            <Alert
              showIcon
              type="success"
              message="注册成功，现在可以登录。"
              style={{ marginBottom: 18 }}
            />
          ) : null}

          {loginMutation.isError ? (
            <Alert
              showIcon
              type="error"
              message={loginMutation.error.message}
              style={{ marginBottom: 18 }}
            />
          ) : null}

          <Form<LoginRequest>
            layout="vertical"
            requiredMark={false}
            onFinish={(values) => loginMutation.mutate(values)}
          >
            <Form.Item
              label="用户名"
              name="username"
              rules={[{ required: true, message: '请输入用户名' }]}
            >
              <Input size="large" placeholder="例如 alice" />
            </Form.Item>

            <Form.Item
              label="密码"
              name="password"
              rules={[{ required: true, message: '请输入密码' }]}
            >
              <Input.Password size="large" placeholder="请输入密码" />
            </Form.Item>

            <Button
              block
              size="large"
              type="primary"
              htmlType="submit"
              loading={loginMutation.isPending}
            >
              登录
            </Button>
          </Form>

          <p className="auth-switch">
            还没有账号？ <Link to="/register">去注册</Link>
          </p>
        </Card>
      </section>
    </main>
  )
}

export default LoginPage
