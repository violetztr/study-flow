import { PlusOutlined, TeamOutlined } from '@ant-design/icons'
import { Alert, Button, Empty, Skeleton, Space } from 'antd'
import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { communityApi } from '../api/community'
import PostCard from '../components/community/PostCard'

function CircleFeedPage() {
  const feedQuery = useQuery({
    queryKey: ['community-feed'],
    queryFn: communityApi.listFeed,
  })

  const topicsQuery = useQuery({
    queryKey: ['community-topics'],
    queryFn: communityApi.listTopics,
  })

  const posts = feedQuery.data ?? []

  return (
    <section className="page-section">
      <section className="dashboard-header">
        <div>
          <p className="dashboard-kicker">Community</p>
          <h1 className="dashboard-title">Violet Circle</h1>
          <p className="dashboard-subtitle">
            在这里分享学习进展、提问、复盘和项目卡点，让每一次推进都能被看见。
          </p>
        </div>
        <Space wrap>
          <Button icon={<TeamOutlined />}>
            <Link to="/circle/members">圈子成员</Link>
          </Button>
          <Button type="primary" icon={<PlusOutlined />}>
            <Link to="/circle/posts/new">发布动态</Link>
          </Button>
        </Space>
      </section>

      <div className="dashboard-content">
        {feedQuery.isError ? (
          <Alert showIcon type="error" message={feedQuery.error.message} />
        ) : null}

        {feedQuery.isLoading ? <Skeleton active /> : null}

        {!feedQuery.isLoading && posts.length === 0 ? (
          <Empty description="圈子里还没有动态，发第一条吧。">
            <Button type="primary">
              <Link to="/circle/posts/new">发布第一条动态</Link>
            </Button>
          </Empty>
        ) : null}

        {posts.map((post) => (
          <PostCard key={post.id} post={post} topics={topicsQuery.data ?? []} />
        ))}
      </div>
    </section>
  )
}

export default CircleFeedPage
