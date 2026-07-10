import { Alert, Button, Card, Form, Input } from 'antd'
import { useMutation } from '@tanstack/react-query'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import {
  type LoginRequest,
  type LoginResponse,
  login,
  saveSession,
} from '../api/auth'

function LoginPage() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const registered = searchParams.get('registered') === '1'

  const loginMutation = useMutation<LoginResponse, Error, LoginRequest>({
    mutationFn: login,
    onSuccess: (response) => {
      saveSession(response)
      navigate('/circle', { replace: true })
    },
  })

  return (
    <main className="auth-page">
      <section className="auth-visual" aria-label="StudyFlow 项目介绍">
        <div className="auth-story">
          <div>
            <span className="brand-mark">S</span>
            <h1 className="auth-title">把学习拆成每天能完成的进度。</h1>
            <p className="auth-copy">
              StudyFlow 用项目、任务、标签和统计把全栈学习变成可追踪的路线图。
              你现在看到的登录页，是前后端联调的入口。
            </p>
          </div>
          <div className="auth-steps">
            <div className="auth-step">
              <span className="auth-step-number">1</span>
              <span>登录后保存 JWT，后续请求自动带上 Authorization。</span>
            </div>
            <div className="auth-step">
              <span className="auth-step-number">2</span>
              <span>进入 Dashboard，读取当前用户和任务统计。</span>
            </div>
          </div>
        </div>
      </section>

      <section className="auth-form-panel">
        <Card className="auth-card">
          <p className="auth-kicker">Welcome back</p>
          <h2 className="auth-card-title">登录 StudyFlow</h2>
          <p className="auth-card-subtitle">继续管理你的学习项目和任务。</p>

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
