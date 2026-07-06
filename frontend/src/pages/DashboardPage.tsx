import {
  BookOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  FolderOpenOutlined,
  LogoutOutlined,
  ProjectOutlined,
} from '@ant-design/icons'
import { Alert, Button, Card, Skeleton, Space, Statistic, Tag } from 'antd'
import { useQuery } from '@tanstack/react-query'
import { Link, useNavigate } from 'react-router-dom'
import { clearSession, getCurrentUser, getStoredUser } from '../api/auth'
import { http } from '../api/http'

type StatisticsOverview = {
  totalTasks: number
  completedTasks: number
  inProgressTasks: number
  overdueTasks: number
}

function getStatisticsOverview() {
  return http.get<unknown, StatisticsOverview>('/statistics/overview')
}

function DashboardPage() {
  const navigate = useNavigate()
  const storedUser = getStoredUser()

  const userQuery = useQuery({
    queryKey: ['current-user'],
    queryFn: getCurrentUser,
  })

  const statisticsQuery = useQuery({
    queryKey: ['statistics-overview'],
    queryFn: getStatisticsOverview,
  })

  const user = userQuery.data ?? storedUser
  const overview = statisticsQuery.data

  function handleLogout() {
    clearSession()
    navigate('/login', { replace: true })
  }

  return (
    <main className="dashboard-shell">
      <section className="dashboard-header">
        <div>
          <p className="dashboard-kicker">StudyFlow cockpit</p>
          <h1 className="dashboard-title">
            {user?.username ? `${user.username}，继续推进。` : '继续推进。'}
          </h1>
          <p className="dashboard-subtitle">
            这里是你的学习任务总览。当前阶段先接入统计概览，
            下一阶段会把项目列表、任务列表和筛选功能接上来。
          </p>
        </div>
        <div className="dashboard-actions">
          {user?.email ? <Tag color="green">{user.email}</Tag> : null}
          <Button icon={<LogoutOutlined />} onClick={handleLogout}>
            退出登录
          </Button>
        </div>
      </section>

      <section className="dashboard-content">
        {statisticsQuery.isError ? (
          <Alert
            showIcon
            type="warning"
            message="统计接口暂时不可用"
            description={statisticsQuery.error.message}
          />
        ) : null}

        <div className="stat-grid">
          <Card className="stat-card">
            {statisticsQuery.isLoading ? (
              <Skeleton active paragraph={false} />
            ) : (
              <Statistic
                title="任务总数"
                value={overview?.totalTasks ?? 0}
                prefix={<BookOutlined />}
              />
            )}
          </Card>
          <Card className="stat-card">
            {statisticsQuery.isLoading ? (
              <Skeleton active paragraph={false} />
            ) : (
              <Statistic
                title="已完成"
                value={overview?.completedTasks ?? 0}
                prefix={<CheckCircleOutlined />}
              />
            )}
          </Card>
          <Card className="stat-card">
            {statisticsQuery.isLoading ? (
              <Skeleton active paragraph={false} />
            ) : (
              <Statistic
                title="进行中"
                value={overview?.inProgressTasks ?? 0}
                prefix={<ProjectOutlined />}
              />
            )}
          </Card>
          <Card className="stat-card">
            {statisticsQuery.isLoading ? (
              <Skeleton active paragraph={false} />
            ) : (
              <Statistic
                title="已逾期"
                value={overview?.overdueTasks ?? 0}
                prefix={<ClockCircleOutlined />}
              />
            )}
          </Card>
        </div>

        <div className="quick-grid">
          <Card className="quick-card">
            <FolderOpenOutlined style={{ color: '#256f62', fontSize: 28 }} />
            <h2 className="quick-title">项目管理</h2>
            <p className="quick-copy">查看学习项目，把 Java、React、部署拆成独立路线。</p>
            <Link to="/projects">进入项目</Link>
          </Card>
          <Card className="quick-card">
            <CheckCircleOutlined style={{ color: '#256f62', fontSize: 28 }} />
            <h2 className="quick-title">任务清单</h2>
            <p className="quick-copy">用状态、优先级、标签筛选今天最该推进的任务。</p>
            <Link to="/tasks">进入任务</Link>
          </Card>
          <Card className="quick-card">
            <Space direction="vertical" size={10}>
              <Tag color="gold">下一步</Tag>
              <h2 className="quick-title">前后端联调</h2>
              <p className="quick-copy">
                启动后端和前端后，登录页会真实调用 Spring Boot API。
              </p>
            </Space>
          </Card>
        </div>
      </section>
    </main>
  )
}

export default DashboardPage
