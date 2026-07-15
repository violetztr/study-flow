import {
  ClockCircleOutlined,
  FileTextOutlined,
  FireOutlined,
  HeartOutlined,
  PlusOutlined,
  PlaySquareOutlined,
  VideoCameraOutlined,
} from '@ant-design/icons'
import { Alert, Button, Empty, Input, Skeleton } from 'antd'
import { useQuery } from '@tanstack/react-query'
import { useDeferredValue, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { getStoredUser } from '../api/auth'
import { communityApi } from '../api/community'
import type { CommunityPostResponse, LiveRoomResponse } from '../api/community'
import PostCard from '../components/community/PostCard'

type FeedChannel = 'live' | 'article' | 'video' | 'following'

function hasVideo(post: CommunityPostResponse) {
  return post.contentType === 'VIDEO' || post.media.some((media) => media.fileType === 'VIDEO')
}

function getChannelPosts(channel: FeedChannel, posts: CommunityPostResponse[]) {
  if (channel === 'live') {
    return posts.filter((post) => post.contentType === 'LIVE')
  }

  if (channel === 'following') {
    return posts
  }

  if (channel === 'video') {
    return posts.filter(hasVideo)
  }

  return posts.filter((post) => !hasVideo(post))
}

function CircleFeedPage() {
  const navigate = useNavigate()
  const user = getStoredUser()
  const [activeChannel, setActiveChannel] = useState<FeedChannel>('video')
  const [showHot, setShowHot] = useState(false)
  const [searchKeyword, setSearchKeyword] = useState('')
  const deferredKeyword = useDeferredValue(searchKeyword.trim())

  const feedQuery = useQuery({
    queryKey: ['community-feed'],
    queryFn: communityApi.listFeed,
  })

  const followingQuery = useQuery({
    queryKey: ['community-feed-following'],
    queryFn: communityApi.listFollowingFeed,
    enabled: activeChannel === 'following' && deferredKeyword.length === 0,
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

  const liveRoomsQuery = useQuery({
    queryKey: ['live-rooms'],
    queryFn: communityApi.listLiveRooms,
    enabled: activeChannel === 'live',
    refetchInterval: 15_000,
  })

  const activeQuery = deferredKeyword.length > 0
    ? searchQuery
    : showHot
      ? hotQuery
      : activeChannel === 'following'
        ? followingQuery
        : feedQuery
  const posts = activeQuery.data ?? []
  const visiblePosts = getChannelPosts(activeChannel, posts)

  function goSubmit() {
    if (user) {
      navigate('/circle/posts/new')
      return
    }
    navigate('/login', { state: { from: '/circle/posts/new' } })
  }

  function toggleHot() {
    setShowHot((current) => {
      const next = !current
      if (next) {
        setActiveChannel('video')
      }
      return next
    })
    setSearchKeyword('')
  }

  return (
    <section className="page-section feed-page discovery-page">
      <div className="youtube-home-shell">
        <aside className="youtube-sidebar" aria-label="ruru 导航">
          <nav className="youtube-sidebar-nav">
            <button
              type="button"
              className={activeChannel === 'video' && !showHot ? 'active' : ''}
              onClick={() => {
                setActiveChannel('video')
                setShowHot(false)
              }}
            >
              <PlaySquareOutlined /> 推荐
            </button>
            {user ? (
              <button
                type="button"
                className={activeChannel === 'following' ? 'active' : ''}
                onClick={() => {
                  setActiveChannel('following')
                  setShowHot(false)
                }}
              >
                <HeartOutlined /> 关注
              </button>
            ) : null}
            <button
              type="button"
              className={activeChannel === 'live' && !showHot ? 'active' : ''}
              onClick={() => {
                setActiveChannel('live')
                setShowHot(false)
              }}
            >
              <VideoCameraOutlined /> 直播
            </button>
            <button
              type="button"
              className={activeChannel === 'article' && !showHot ? 'active' : ''}
              onClick={() => {
                setActiveChannel('article')
                setShowHot(false)
              }}
            >
              <FileTextOutlined /> 图文
            </button>
            {user ? (
              <button type="button" onClick={() => navigate('/circle/history')}>
                <ClockCircleOutlined /> 历史记录
              </button>
            ) : null}
            <button
              type="button"
              className={showHot && deferredKeyword.length === 0 ? 'active' : ''}
              onClick={toggleHot}
            >
              <FireOutlined /> 热门
            </button>
          </nav>
        </aside>

        <main className="youtube-main">
          <header className="discovery-topbar">
            <div className="discovery-actions">
              <Input.Search
                allowClear
                className="discovery-search"
                placeholder="搜索视频、图文、话题或作者"
                value={searchKeyword}
                onChange={(event) => setSearchKeyword(event.target.value)}
                onSearch={(value) => setSearchKeyword(value)}
              />
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

          {activeChannel === 'live' && !liveRoomsQuery.isLoading ? (
            liveRoomsQuery.data && liveRoomsQuery.data.length > 0 ? (
              <div className="discovery-grid youtube-video-grid">
                {liveRoomsQuery.data.map((room: LiveRoomResponse) => (
                  <div
                    key={room.id}
                    className="live-room-card"
                    onClick={() => navigate(`/circle/live/${room.id}`)}
                    style={{ cursor: 'pointer' }}
                  >
                    <div className="live-room-cover">
                      {room.coverUrl ? (
                        <img src={room.coverUrl} alt={room.title} />
                      ) : (
                        <div className="live-room-cover-placeholder" />
                      )}
                      <span className="live-badge">直播中</span>
                    </div>
                    <div className="live-room-card-body">
                      <div className="live-room-title">{room.title}</div>
                      <div className="live-room-meta">
                        <span>{room.username || '主播'}</span>
                        <span>👁 {room.currentViewers}</span>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <div className="channel-empty-card">
                <strong>当前没有直播</strong>
                {user ? (
                  <Button
                    type="primary"
                    size="small"
                    style={{ marginTop: 8 }}
                    onClick={goSubmit}
                  >
                    去开播
                  </Button>
                ) : null}
              </div>
            )
          ) : null}

          {!activeQuery.isLoading && activeChannel !== 'live' && visiblePosts.length === 0 ? (
            <Empty description={activeChannel === 'video' ? '还没有视频' : '还没有图文'}>
              <Button type="primary" onClick={goSubmit}>
                去投稿
              </Button>
            </Empty>
          ) : null}

          {activeChannel !== 'live' ? (
            <div className="discovery-grid youtube-video-grid">
              {visiblePosts.map((post) => (
                <PostCard key={post.id} post={post} />
              ))}
            </div>
          ) : null}
        </main>
      </div>
    </section>
  )
}

export default CircleFeedPage
