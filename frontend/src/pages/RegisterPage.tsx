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
      <section className="auth-visual" aria-label="StudyFlow 注册介绍">
        <div className="auth-story">
          <div>
            <span className="brand-mark">S</span>
            <h1 className="auth-title">先建账号，再建你的全栈训练场。</h1>
            <p className="auth-copy">
              注册接口会创建用户，密码会在后端用 BCrypt 加密保存。前端只负责收集表单，
              不直接处理密码加密。
            </p>
          </div>
          <div className="auth-steps">
            <div className="auth-step">
              <span className="auth-step-number">1</span>
              <span>提交用户名、邮箱和密码到 Spring Boot 后端。</span>
            </div>
            <div className="auth-step">
              <span className="auth-step-number">2</span>
              <span>注册成功后回到登录页，再换取 JWT。</span>
            </div>
          </div>
        </div>
      </section>

      <section className="auth-form-panel">
        <Card className="auth-card">
          <p className="auth-kicker">Create account</p>
          <h2 className="auth-card-title">创建账号</h2>
          <p className="auth-card-subtitle">用一个账号记录你的学习项目。</p>

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
