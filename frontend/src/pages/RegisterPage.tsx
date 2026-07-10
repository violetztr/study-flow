import { Alert, Button, Card, Form, Input } from 'antd'
import { useMutation } from '@tanstack/react-query'
import { Link, useNavigate } from 'react-router-dom'
import {
  type RegisterRequest,
  type UserResponse,
  register,
} from '../api/auth'

function RegisterPage() {
  const navigate = useNavigate()

  const registerMutation = useMutation<UserResponse, Error, RegisterRequest>({
    mutationFn: register,
    onSuccess: () => {
      navigate('/login?registered=1', { replace: true })
    },
  })

  return (
    <main className="auth-page">
      <section className="auth-form-panel">
        <Card className="auth-card">
          <p className="auth-kicker">Create account</p>
          <h2 className="auth-card-title">创建账号</h2>
          <p className="auth-card-subtitle">加入 ruru 小圈子。</p>

          {registerMutation.isError ? (
            <Alert
              showIcon
              type="error"
              message={registerMutation.error.message}
              style={{ marginBottom: 18 }}
            />
          ) : null}

          <Form<RegisterRequest>
            layout="vertical"
            requiredMark={false}
            onFinish={(values) => registerMutation.mutate(values)}
          >
            <Form.Item
              label="用户名"
              name="username"
              rules={[
                { required: true, message: '请输入用户名' },
                { max: 50, message: '用户名不能超过 50 个字符' },
              ]}
            >
              <Input size="large" placeholder="例如 alice" />
            </Form.Item>

            <Form.Item
              label="邮箱"
              name="email"
              rules={[
                { required: true, message: '请输入邮箱' },
                { type: 'email', message: '邮箱格式不正确' },
              ]}
            >
              <Input size="large" placeholder="alice@example.com" />
            </Form.Item>

            <Form.Item
              label="密码"
              name="password"
              rules={[
                { required: true, message: '请输入密码' },
                { min: 6, max: 50, message: '密码长度需要在 6 到 50 个字符之间' },
              ]}
            >
              <Input.Password size="large" placeholder="至少 6 个字符" />
            </Form.Item>

            <Button
              block
              size="large"
              type="primary"
              htmlType="submit"
              loading={registerMutation.isPending}
            >
              注册
            </Button>
          </Form>

          <p className="auth-switch">
            已经有账号？ <Link to="/login">去登录</Link>
          </p>
        </Card>
      </section>
    </main>
  )
}

export default RegisterPage
