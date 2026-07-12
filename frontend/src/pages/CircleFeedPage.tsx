import { PlusOutlined } from '@ant-design/icons'
import { Alert, Button, Empty, Input, Skeleton } from 'antd'
import { useQuery } from '@tanstack/react-query'
import { useDeferredValue, useState } from 'react'
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
  const [showHot, setShowHot] = useState(false)
  const [searchKeyword, setSearchKeyword] = useState('')
  const deferredKeyword = useDeferredValue(searchKeyword.trim())

  const feedQuery = useQuery({
    queryKey: ['community-feed'],
    queryFn: communityApi.listFeed,
  })

  const hotQuery = useQuery({
    queryKey: ['community-rankings-hot'],
    queryFn: communityApi.listHotRanking,
    enabled: showHot && deferredKeyword.length === 0,
  })

  const searchQuery = useQuery({
    queryKey: ['community-search', deferredKeyword],
    queryFn: () => communityApi.searchPosts(deferredKeyword),
    enabled: deferredKeyword.length > 0,
  })

  const activeQuery = deferredKeyword.length > 0 ? searchQuery : showHot ? hotQuery : feedQuery
  const posts = activeQuery.data ?? []
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
      <video
        className="site-home-bg-video"
        src="/system-backgrounds/site/home-hero.mp4"
        autoPlay
        muted
        loop
        playsInline
        aria-hidden="true"
      />
      <div className="site-home-bg-overlay" />
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
            <Input.Search
              allowClear
              className="discovery-search"
              placeholder="搜索视频、图文、话题或作者"
              value={searchKeyword}
              onChange={(event) => setSearchKeyword(event.target.value)}
              onSearch={(value) => setSearchKeyword(value)}
            />
            <Button
              type={showHot && deferredKeyword.length === 0 ? 'primary' : 'default'}
              onClick={() => {
                setShowHot((current) => !current)
                setSearchKeyword('')
              }}
            >
              {showHot && deferredKeyword.length === 0 ? '热门' : '最新'}
            </Button>
            {user ? <Button onClick={() => navigate('/circle/history')}>历史</Button> : null}
            {user ? <Button onClick={() => navigate('/circle/submissions')}>稿件</Button> : null}
            <Button type="primary" icon={<PlusOutlined />} onClick={goSubmit}>
              投稿
            </Button>
          </div>
        </header>

        <div className="notice-stack">
          {feedQuery.isError ? <Alert showIcon type="error" message={feedQuery.error.message} /> : null}
          {hotQuery.isError ? <Alert showIcon type="error" message={hotQuery.error.message} /> : null}
          {searchQuery.isError ? <Alert showIcon type="error" message={searchQuery.error.message} /> : null}
        </div>

        {activeQuery.isLoading ? <Skeleton active /> : null}

        {!activeQuery.isLoading && activeChannel === 'live' ? (
          <div className="channel-empty-card">
            <strong>直播准备中</strong>
          </div>
        ) : null}

        {!activeQuery.isLoading && activeChannel !== 'live' && visiblePosts.length === 0 ? (
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
