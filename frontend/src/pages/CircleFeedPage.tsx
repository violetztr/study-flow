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
          <p className="dashboard-kicker">Ruru Community</p>
          <h1 className="dashboard-title">Ruru 社区</h1>
          <p className="dashboard-subtitle">
            一个先从朋友小圈子开始的社区。先把发帖、评论、点赞、成员和管理这些基础能力做好，后面再一点点长出更强的模块。
          </p>
        </div>
        <Space wrap>
          <Button icon={<TeamOutlined />}>
            <Link to="/circle/members">成员</Link>
          </Button>
          <Button type="primary" icon={<PlusOutlined />}>
            <Link to="/circle/posts/new">发布动态</Link>
          </Button>
        </Space>
      </section>

      <div className="dashboard-content">
        {feedQuery.isError ? <Alert showIcon type="error" message={feedQuery.error.message} /> : null}
        {feedQuery.isLoading ? <Skeleton active /> : null}

        {!feedQuery.isLoading && posts.length === 0 ? (
          <Empty description="社区里还没有动态，来发第一条吧。">
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
