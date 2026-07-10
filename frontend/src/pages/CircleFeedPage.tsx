import { PlusOutlined } from '@ant-design/icons'
import { Alert, Button, Empty, Skeleton, Space, Typography } from 'antd'
import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { getStoredUser } from '../api/auth'
import { communityApi } from '../api/community'
import PostCard from '../components/community/PostCard'

function CircleFeedPage() {
  const user = getStoredUser()

  const feedQuery = useQuery({
    queryKey: ['community-feed'],
    queryFn: communityApi.listFeed,
  })

  const topicsQuery = useQuery({
    queryKey: ['community-topics'],
    queryFn: communityApi.listTopics,
  })

  const posts = feedQuery.data ?? []
  const publishTarget = user ? '/circle/posts/new' : '/login'
  const publishState = user ? undefined : { from: '/circle/posts/new' }

  return (
    <section className="page-section">
      <section className="community-toolbar">
        <div>
          <Typography.Title level={2} className="community-title">
            最新动态
          </Typography.Title>
          <Typography.Text type="secondary">
            不登录也能浏览社区；发布、点赞和评论需要注册登录。
          </Typography.Text>
        </div>
        <Space wrap>
          <Button type="primary" icon={<PlusOutlined />}>
            <Link to={publishTarget} state={publishState}>
              发布动态
            </Link>
          </Button>
        </Space>
      </section>

      <div className="dashboard-content">
        {feedQuery.isError ? <Alert showIcon type="error" message={feedQuery.error.message} /> : null}
        {feedQuery.isLoading ? <Skeleton active /> : null}

        {!feedQuery.isLoading && posts.length === 0 ? (
          <Empty description="社区里还没有动态，来发第一条吧。">
            <Button type="primary">
              <Link to={publishTarget} state={publishState}>
                发布第一条动态
              </Link>
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
