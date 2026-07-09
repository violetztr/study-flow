import { ArrowRightOutlined, GithubOutlined, GlobalOutlined } from '@ant-design/icons'
import { Button, Card, Empty, Skeleton, Space, Tag } from 'antd'
import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { listPublicPortfolioProjects } from '../api/portfolio'

function PublicPortfolioPage() {
  const portfolioQuery = useQuery({
    queryKey: ['public-portfolio-projects'],
    queryFn: listPublicPortfolioProjects,
  })

  const projects = portfolioQuery.data ?? []

  return (
    <main className="public-page">
      <section className="public-hero">
        <p className="dashboard-kicker">DevFlow Studio Portfolio</p>
        <h1>把学习沉淀成可以被看见的作品。</h1>
        <p>
          这里展示的是 StudyFlow 里的公开项目：技术栈、架构说明、GitHub 信息和线上地址会被整理成一张面试作品卡。
        </p>
      </section>

      <section className="public-grid">
        {portfolioQuery.isLoading ? <Skeleton active /> : null}
        {!portfolioQuery.isLoading && projects.length === 0 ? (
          <Card className="workspace-card">
            <Empty description="暂时还没有公开作品" />
          </Card>
        ) : null}
        {projects.map((project) => (
          <Card className="public-project-card" key={project.slug}>
            <Space className="public-card-top" align="start" size={12}>
              <Tag color={project.featured ? 'gold' : 'green'}>
                {project.featured ? 'Featured' : 'Project'}
              </Tag>
              <span>{project.status}</span>
            </Space>
            <h2>{project.name}</h2>
            <p>{project.headline || project.publicSummary || project.description}</p>
            <div className="tech-badges">
              {project.techStacks.map((stack) => (
                <Tag key={stack.id} color="green">
                  {stack.name}
                </Tag>
              ))}
            </div>
            <Space wrap>
              <Button type="primary" icon={<ArrowRightOutlined />}>
                <Link to={`/portfolio/${project.slug}`}>查看详情</Link>
              </Button>
              {project.productionUrl ? (
                <Button icon={<GlobalOutlined />} href={project.productionUrl} target="_blank">
                  线上地址
                </Button>
              ) : null}
              {project.githubRepository?.htmlUrl ? (
                <Button
                  icon={<GithubOutlined />}
                  href={project.githubRepository.htmlUrl}
                  target="_blank"
                >
                  GitHub
                </Button>
              ) : null}
            </Space>
          </Card>
        ))}
      </section>
    </main>
  )
}

export default PublicPortfolioPage
