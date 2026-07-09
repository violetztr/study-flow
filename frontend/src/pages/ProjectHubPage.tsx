import { GlobalOutlined, RocketOutlined } from '@ant-design/icons'
import { Alert, Button, Card, Select, Skeleton, Space, Tag } from 'antd'
import { useEffect, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { getProjectProfile } from '../api/projectHub'
import { listProjects } from '../api/projects'
import GitHubRepositoryPanel from '../components/project-hub/GitHubRepositoryPanel'
import PortfolioSettingsCard from '../components/project-hub/PortfolioSettingsCard'
import ProjectProfileForm from '../components/project-hub/ProjectProfileForm'
import TechStackEditor from '../components/project-hub/TechStackEditor'

function ProjectHubPage() {
  const [selectedProjectId, setSelectedProjectId] = useState<number | null>(null)

  const projectsQuery = useQuery({
    queryKey: ['projects'],
    queryFn: listProjects,
  })

  const projects = projectsQuery.data ?? []

  useEffect(() => {
    if (!selectedProjectId && projects.length > 0) {
      setSelectedProjectId(projects[0].id)
    }
  }, [projects, selectedProjectId])

  const profileQuery = useQuery({
    queryKey: ['project-profile', selectedProjectId],
    queryFn: () => getProjectProfile(selectedProjectId as number),
    enabled: selectedProjectId !== null,
  })

  const selectedProject = projects.find((project) => project.id === selectedProjectId)

  if (projectsQuery.isLoading) {
    return (
      <section className="page-section">
        <Skeleton active />
      </section>
    )
  }

  return (
    <section className="page-section">
      <section className="dashboard-header project-hub-hero">
        <div>
          <p className="dashboard-kicker">DevFlow Studio</p>
          <h1 className="dashboard-title">全栈项目中台</h1>
          <p className="dashboard-subtitle">
            把一个项目从“能运行”整理成“能展示”：线上地址、技术栈、GitHub 数据、公开作品集，全都放在这里维护。
          </p>
        </div>
        <Space wrap>
          <Button icon={<GlobalOutlined />}>
            <Link to="/portfolio">查看公开作品集</Link>
          </Button>
          <Button type="primary" icon={<RocketOutlined />}>
            <Link to="/projects">管理项目</Link>
          </Button>
        </Space>
      </section>

      {projects.length === 0 ? (
        <Alert
          showIcon
          type="info"
          message="还没有项目"
          description="先创建一个学习项目，再回来把它包装成可以展示的全栈作品。"
          action={
            <Button type="primary">
              <Link to="/projects">去创建项目</Link>
            </Button>
          }
        />
      ) : (
        <section className="project-hub-shell">
          <Card className="workspace-card project-selector-card">
            <Space direction="vertical" size={12} style={{ width: '100%' }}>
              <span className="dashboard-kicker">Current project</span>
              <Select
                value={selectedProjectId ?? undefined}
                options={projects.map((project) => ({
                  label: project.name,
                  value: project.id,
                }))}
                onChange={setSelectedProjectId}
                style={{ width: '100%' }}
              />
              {selectedProject ? (
                <div>
                  <h2 className="project-selector-title">{selectedProject.name}</h2>
                  <p className="muted-text">
                    {selectedProject.description || '这个项目还没有描述。'}
                  </p>
                  <Tag color={selectedProject.status === 'ACTIVE' ? 'green' : 'default'}>
                    {selectedProject.status}
                  </Tag>
                </div>
              ) : null}
            </Space>
          </Card>

          <div className="project-hub-grid">
            <ProjectProfileForm projectId={selectedProjectId} profile={profileQuery.data} />
            <TechStackEditor projectId={selectedProjectId} />
            <GitHubRepositoryPanel projectId={selectedProjectId} />
            <PortfolioSettingsCard projectId={selectedProjectId} />
          </div>
        </section>
      )}
    </section>
  )
}

export default ProjectHubPage
