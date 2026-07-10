import { PlusOutlined } from '@ant-design/icons'
import { Alert, Button, Empty, Skeleton } from 'antd'
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
      <section className="community-toolbar minimal">
        <Button type="primary" icon={<PlusOutlined />}>
          <Link to={publishTarget} state={publishState}>
            发布
          </Link>
        </Button>
      </section>

      <div className="dashboard-content">
        {feedQuery.isError ? <Alert showIcon type="error" message={feedQuery.error.message} /> : null}
        {feedQuery.isLoading ? <Skeleton active /> : null}

        {!feedQuery.isLoading && posts.length === 0 ? (
          <Empty description="还没有动态">
            <Button type="primary">
              <Link to={publishTarget} state={publishState}>
                发布
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
