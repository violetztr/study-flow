import { ArrowLeftOutlined, GithubOutlined, GlobalOutlined } from '@ant-design/icons'
import { Button, Card, Descriptions, Result, Skeleton, Space, Tag } from 'antd'
import { useQuery } from '@tanstack/react-query'
import { Link, useParams } from 'react-router-dom'
import { getPublicPortfolioProject } from '../api/portfolio'

function PublicProjectDetailPage() {
  const params = useParams()
  const slug = params.slug ?? ''

  const projectQuery = useQuery({
    queryKey: ['public-portfolio-project', slug],
    queryFn: () => getPublicPortfolioProject(slug),
    enabled: slug.length > 0,
  })

  const project = projectQuery.data

  if (projectQuery.isLoading) {
    return (
      <main className="public-page">
        <Skeleton active />
      </main>
    )
  }

  if (!project) {
    return (
      <main className="public-page">
        <Result
          status="404"
          title="项目不存在"
          subTitle="这个公开作品可能还没有发布，或者路径已经变更。"
          extra={
            <Button type="primary">
              <Link to="/portfolio">返回作品集</Link>
            </Button>
          }
        />
      </main>
    )
  }

  return (
    <main className="public-page">
      <section className="public-detail-hero">
        <Button icon={<ArrowLeftOutlined />}>
          <Link to="/portfolio">返回作品集</Link>
        </Button>
        <p className="dashboard-kicker">Case Study</p>
        <h1>{project.name}</h1>
        <p>{project.headline || project.publicSummary || project.description}</p>
        <Space wrap>
          {project.productionUrl ? (
            <Button type="primary" icon={<GlobalOutlined />} href={project.productionUrl} target="_blank">
              打开线上项目
            </Button>
          ) : null}
          {project.githubRepository?.htmlUrl ? (
            <Button icon={<GithubOutlined />} href={project.githubRepository.htmlUrl} target="_blank">
              查看 GitHub
            </Button>
          ) : null}
        </Space>
      </section>

      <section className="public-detail-grid">
        <Card className="workspace-card" title="项目说明">
          <p className="public-copy">{project.publicSummary || '暂未填写公开摘要。'}</p>
          <Descriptions column={1}>
            <Descriptions.Item label="架构">
              {project.architectureSummary || '暂未填写'}
            </Descriptions.Item>
            <Descriptions.Item label="面试亮点">
              {project.interviewHighlights || '暂未填写'}
            </Descriptions.Item>
            <Descriptions.Item label="接口文档">
              {project.apiDocUrl ? (
                <a href={project.apiDocUrl} target="_blank" rel="noreferrer">
                  {project.apiDocUrl}
                </a>
              ) : (
                '暂未填写'
              )}
            </Descriptions.Item>
            <Descriptions.Item label="数据库文档">
              {project.databaseDocUrl ? (
                <a href={project.databaseDocUrl} target="_blank" rel="noreferrer">
                  {project.databaseDocUrl}
                </a>
              ) : (
                '暂未填写'
              )}
            </Descriptions.Item>
          </Descriptions>
        </Card>

        <Card className="workspace-card" title="技术栈">
          <div className="tech-badges large">
            {project.techStacks.map((stack) => (
              <Tag key={stack.id} color="green">
                {stack.category} / {stack.name}
              </Tag>
            ))}
          </div>
        </Card>

        <Card className="workspace-card" title="GitHub 数据">
          <Descriptions column={1}>
            <Descriptions.Item label="主语言">
              {project.githubRepository?.primaryLanguage || '未同步'}
            </Descriptions.Item>
            <Descriptions.Item label="Stars">
              {project.githubRepository?.stars ?? 0}
            </Descriptions.Item>
            <Descriptions.Item label="Forks">
              {project.githubRepository?.forks ?? 0}
            </Descriptions.Item>
            <Descriptions.Item label="README">
              {project.githubRepository?.readmePresent ? '已检测到' : '未检测'}
            </Descriptions.Item>
          </Descriptions>
        </Card>
      </section>
    </main>
  )
}

export default PublicProjectDetailPage
