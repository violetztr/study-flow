import { PlusOutlined } from '@ant-design/icons'
import { Alert, Button, Empty, Skeleton } from 'antd'
import { useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { getStoredUser } from '../api/auth'
import { communityApi } from '../api/community'
import type { CommunityPostResponse } from '../api/community'
import PostCard from '../components/community/PostCard'

type FeedChannel = 'live' | 'article' | 'video'

const channels: Array<{ key: FeedChannel; label: string }> = [
  { key: 'live', label: '直播' },
  { key: 'article', label: '图文' },
  { key: 'video', label: '视频' },
]

function hasVideo(post: CommunityPostResponse) {
  return post.contentType === 'VIDEO' || post.media.some((media) => media.fileType === 'VIDEO')
}

function getChannelPosts(channel: FeedChannel, posts: CommunityPostResponse[]) {
  if (channel === 'live') {
    return posts.filter((post) => post.contentType === 'LIVE')
  }

  if (channel === 'video') {
    return posts.filter(hasVideo)
  }

  return posts.filter((post) => !hasVideo(post))
}

function getChannelCount(channel: FeedChannel, posts: CommunityPostResponse[]) {
  return getChannelPosts(channel, posts).length
}

function CircleFeedPage() {
  const navigate = useNavigate()
  const user = getStoredUser()
  const [activeChannel, setActiveChannel] = useState<FeedChannel>('article')

  const feedQuery = useQuery({
    queryKey: ['community-feed'],
    queryFn: communityApi.listFeed,
  })

  const posts = feedQuery.data ?? []
  const visiblePosts = getChannelPosts(activeChannel, posts)

  function goSubmit() {
    if (user) {
      navigate('/circle/posts/new')
      return
    }
    navigate('/login', { state: { from: '/circle/posts/new' } })
  }

  return (
    <section className="page-section feed-page discovery-page">
      <div className="discovery-shell">
        <header className="discovery-topbar">
          <button className="discovery-logo" type="button" onClick={() => setActiveChannel('article')}>
            ruru
          </button>

          <nav className="feed-channel-tabs" aria-label="内容频道">
            {channels.map((channel) => (
              <button
                key={channel.key}
                type="button"
                className={activeChannel === channel.key ? 'active' : ''}
                onClick={() => setActiveChannel(channel.key)}
              >
                {channel.label}
                <span>{getChannelCount(channel.key, posts)}</span>
              </button>
            ))}
          </nav>

          <div className="discovery-actions">
            {user ? <Button onClick={() => navigate('/circle/history')}>历史</Button> : null}
            {user ? <Button onClick={() => navigate('/circle/submissions')}>稿件</Button> : null}
            <Button type="primary" icon={<PlusOutlined />} onClick={goSubmit}>
              投稿
            </Button>
          </div>
        </header>

        <div className="notice-stack">
          {feedQuery.isError ? <Alert showIcon type="error" message={feedQuery.error.message} /> : null}
        </div>

        {feedQuery.isLoading ? <Skeleton active /> : null}

        {!feedQuery.isLoading && activeChannel === 'live' ? (
          <div className="channel-empty-card">
            <strong>直播准备中</strong>
          </div>
        ) : null}

        {!feedQuery.isLoading && activeChannel !== 'live' && visiblePosts.length === 0 ? (
          <Empty description={activeChannel === 'video' ? '还没有视频' : '还没有图文'}>
            <Button type="primary" onClick={goSubmit}>
              去投稿
            </Button>
          </Empty>
        ) : null}

        {activeChannel !== 'live' ? (
          <div className="discovery-grid">
            {visiblePosts.map((post) => (
              <PostCard key={post.id} post={post} />
            ))}
          </div>
        ) : null}
      </div>
    </section>
  )
}

export default CircleFeedPage
