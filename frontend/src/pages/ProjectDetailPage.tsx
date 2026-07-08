import { ArrowLeftOutlined } from '@ant-design/icons'
import { Button, Card, Result, Skeleton, Space, Statistic, Table, Tag } from 'antd'
import { useQuery } from '@tanstack/react-query'
import { Link, useParams } from 'react-router-dom'
import { listProjects } from '../api/projects'
import { type TaskResponse, listTasks } from '../api/tasks'

function ProjectDetailPage() {
  const params = useParams()
  const projectId = Number(params.id)

  const projectsQuery = useQuery({
    queryKey: ['projects'],
    queryFn: listProjects,
  })

  const tasksQuery = useQuery({
    queryKey: ['tasks', { projectId }],
    queryFn: () => listTasks({ projectId }),
    enabled: Number.isFinite(projectId),
  })

  const project = projectsQuery.data?.find((item) => item.id === projectId)
  const tasks = tasksQuery.data ?? []
  const completedTasks = tasks.filter((task) => task.status === 'DONE').length

  if (projectsQuery.isLoading) {
    return (
      <section className="page-section">
        <Skeleton active />
      </section>
    )
  }

  if (!project) {
    return (
      <section className="page-section">
        <Result
          status="404"
          title="项目不存在"
          subTitle="这个项目可能已被删除，或者不属于当前用户。"
          extra={
            <Button type="primary">
              <Link to="/projects">返回项目列表</Link>
            </Button>
          }
        />
      </section>
    )
  }

  return (
    <section className="page-section">
      <div className="page-heading">
        <div>
          <p className="dashboard-kicker">Project detail</p>
          <h1>{project.name}</h1>
          <p>{project.description || '这个项目暂时还没有描述。'}</p>
        </div>
        <Button icon={<ArrowLeftOutlined />}>
          <Link to="/projects">返回项目</Link>
        </Button>
      </div>

      <div className="stat-grid compact">
        <Card className="stat-card">
          <Statistic title="任务总数" value={tasks.length} />
        </Card>
        <Card className="stat-card">
          <Statistic title="已完成" value={completedTasks} />
        </Card>
        <Card className="stat-card">
          <Statistic title="项目状态" value={project.status === 'ACTIVE' ? '启用' : '归档'} />
        </Card>
      </div>

      <Card className="table-card">
        <Table<TaskResponse>
          rowKey="id"
          loading={tasksQuery.isLoading}
          dataSource={tasks}
          columns={[
            {
              title: '任务标题',
              dataIndex: 'title',
            },
            {
              title: '状态',
              dataIndex: 'status',
              render: (status) =>
                status === 'DONE' ? (
                  <Tag color="success">已完成</Tag>
                ) : (
                  <Tag color="processing">{status === 'IN_PROGRESS' ? '进行中' : '待开始'}</Tag>
                ),
            },
            {
              title: '优先级',
              dataIndex: 'priority',
              render: (priority) => (
                <Tag color={priority === 'HIGH' ? 'red' : priority === 'MEDIUM' ? 'gold' : 'blue'}>
                  {priority === 'HIGH' ? '高' : priority === 'MEDIUM' ? '中' : '低'}
                </Tag>
              ),
            },
            {
              title: '截止时间',
              dataIndex: 'deadline',
              render: (deadline) => deadline || '未设置',
            },
            {
              title: '操作',
              render: () => (
                <Space>
                  <Link to="/tasks">去任务页编辑</Link>
                </Space>
              ),
            },
          ]}
        />
      </Card>
    </section>
  )
}

export default ProjectDetailPage
