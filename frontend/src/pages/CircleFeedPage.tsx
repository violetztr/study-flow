import { PlusOutlined } from '@ant-design/icons'
import { Alert, Button, Empty, Skeleton } from 'antd'
import { useQuery } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { getStoredUser } from '../api/auth'
import { communityApi } from '../api/community'
import PostCard from '../components/community/PostCard'

function CircleFeedPage() {
  const navigate = useNavigate()
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

  function goPublish() {
    if (user) {
      navigate('/circle/posts/new')
      return
    }
    navigate('/login', { state: { from: '/circle/posts/new' } })
  }

  return (
    <section className="page-section feed-page">
      <div className="feed-shell">
        <div className="feed-toolbar">
          <span>{posts.length} 条动态</span>
          <Button type="primary" icon={<PlusOutlined />} onClick={goPublish}>
            发布
          </Button>
        </div>

        <div className="notice-stack">
          {feedQuery.isError ? <Alert showIcon type="error" message={feedQuery.error.message} /> : null}
        </div>

        {feedQuery.isLoading ? <Skeleton active /> : null}

        {!feedQuery.isLoading && posts.length === 0 ? (
          <Empty description="还没有动态">
            <Button type="primary" onClick={goPublish}>
              发布第一条
            </Button>
          </Empty>
        ) : null}

        <div className="feed-list">
          {posts.map((post) => (
            <PostCard key={post.id} post={post} topics={topicsQuery.data ?? []} />
          ))}
        </div>
      </div>
    </section>
  )
}

export default CircleFeedPage
