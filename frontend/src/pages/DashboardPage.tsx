import {
  BookOutlined,
  CalendarOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  FileTextOutlined,
  FolderOpenOutlined,
  ProjectOutlined,
} from '@ant-design/icons'
import { Alert, Card, Skeleton, Statistic, Tag } from 'antd'
import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { getCurrentUser, getStoredUser } from '../api/auth'
import { getStatisticsOverview } from '../api/statistics'

function DashboardPage() {
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

  return (
    <section className="page-section">
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
          <Card className="stat-card">
            {statisticsQuery.isLoading ? (
              <Skeleton active paragraph={false} />
            ) : (
              <Statistic
                title="预计总时长"
                value={overview?.totalEstimatedMinutes ?? 0}
                suffix="分钟"
                prefix={<ClockCircleOutlined />}
              />
            )}
          </Card>
          <Card className="stat-card">
            {statisticsQuery.isLoading ? (
              <Skeleton active paragraph={false} />
            ) : (
              <Statistic
                title="已完成时长"
                value={overview?.completedEstimatedMinutes ?? 0}
                suffix="分钟"
                prefix={<CheckCircleOutlined />}
              />
            )}
          </Card>
        </div>

        <div className="quick-grid">
          <Card className="quick-card">
            <FolderOpenOutlined style={{ color: '#256f62', fontSize: 28 }} />
            <h2 className="quick-title">学习模块</h2>
            <p className="quick-copy">管理学习项目、任务、标签和预计学习时长，把全栈路线拆成能推进的小步。</p>
            <Link to="/projects">进入学习项目</Link>
          </Card>
          <Card className="quick-card">
            <FileTextOutlined style={{ color: '#256f62', fontSize: 28 }} />
            <h2 className="quick-title">笔记模块</h2>
            <p className="quick-copy">沉淀学习笔记、项目复盘和代码理解，后面会升级成 Notion 风格工作台。</p>
            <Link to="/notes">进入笔记工作台</Link>
          </Card>
          <Card className="quick-card">
            <CalendarOutlined style={{ color: '#256f62', fontSize: 28 }} />
            <h2 className="quick-title">日常模块</h2>
            <p className="quick-copy">安排今日计划、习惯打卡和学习日记，让项目推进变成每天可执行的节奏。</p>
            <Link to="/daily">进入今日计划</Link>
          </Card>
        </div>
      </section>
    </section>
  )
}

export default DashboardPage
