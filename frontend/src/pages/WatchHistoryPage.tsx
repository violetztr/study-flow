import { ArrowLeftOutlined, ClockCircleOutlined, PlayCircleOutlined } from '@ant-design/icons'
import { Alert, Button, Empty, Progress, Skeleton } from 'antd'
import { useQuery } from '@tanstack/react-query'
import dayjs from 'dayjs'
import { useNavigate } from 'react-router-dom'
import { communityApi } from '../api/community'
import type { CommunityPostResponse, MediaAttachmentResponse } from '../api/community'

function getPrimaryMedia(post: CommunityPostResponse): MediaAttachmentResponse | undefined {
  return post.media.find((media) => media.fileType === 'VIDEO') ?? post.media[0]
}

function getPreviewUrl(post: CommunityPostResponse) {
  const primaryMedia = getPrimaryMedia(post)
  if (!primaryMedia) {
    return null
  }
  return primaryMedia.fileType === 'VIDEO' ? primaryMedia.coverUrl : primaryMedia.url
}

function formatDuration(seconds: number) {
  const safeSeconds = Math.max(0, Math.floor(seconds))
  const minutes = Math.floor(safeSeconds / 60)
  const restSeconds = safeSeconds % 60

  return `${minutes}:${restSeconds.toString().padStart(2, '0')}`
}

function getProgressPercent(maxProgressSeconds: number, durationSeconds: number) {
  if (durationSeconds <= 0) {
    return 0
  }

  return Math.min(100, Math.round((maxProgressSeconds / durationSeconds) * 100))
}

function WatchHistoryPage() {
  const navigate = useNavigate()
  const historyQuery = useQuery({
    queryKey: ['community-watch-history-my'],
    queryFn: communityApi.listMyWatchHistory,
  })

  const historyItems = historyQuery.data ?? []

  return (
    <section className="page-section history-page">
      <div className="history-shell">
        <button className="profile-back-link" type="button" onClick={() => navigate('/circle')}>
          <ArrowLeftOutlined /> 返回 ruru
        </button>

        <header className="history-hero">
          <div>
            <p>观看记录</p>
            <h1>历史</h1>
            <span>只记录真正播放过的视频，方便你之后继续看。</span>
          </div>
        </header>

        {historyQuery.isError ? <Alert showIcon type="error" message={historyQuery.error.message} /> : null}

        {historyQuery.isLoading ? <Skeleton active /> : null}

        {!historyQuery.isLoading && historyItems.length === 0 ? (
          <Empty description="还没有观看历史">
            <Button type="primary" onClick={() => navigate('/circle')}>
              去看视频
            </Button>
          </Empty>
        ) : null}

        <div className="history-list">
          {historyItems.map((item) => {
            const previewUrl = getPreviewUrl(item.post)
            const percent = getProgressPercent(item.maxProgressSeconds, item.durationSeconds)

            return (
              <article key={item.post.id} className="history-card">
                <button
                  className="history-cover"
                  type="button"
                  onClick={() => navigate(`/circle/posts/${item.post.id}`)}
                >
                  {previewUrl ? <img alt={item.post.title} src={previewUrl} loading="lazy" /> : <span>视频</span>}
                  <i>
                    <PlayCircleOutlined />
                  </i>
                </button>

                <div className="history-main">
                  <div className="history-meta">
                    <span>{item.post.authorName}</span>
                    <span>
                      <ClockCircleOutlined /> {dayjs(item.lastViewedAt).format('MM-DD HH:mm')}
                    </span>
                  </div>
                  <h2>{item.post.title}</h2>
                  <p>{item.post.content}</p>
                  <div className="history-progress">
                    <Progress percent={percent} showInfo={false} strokeColor="#2f6657" />
                    <span>
                      看到 {formatDuration(item.maxProgressSeconds)}
                      {item.durationSeconds > 0 ? ` / ${formatDuration(item.durationSeconds)}` : ''}
                    </span>
                  </div>
                </div>

                <div className="history-actions">
                  <Button type="primary" onClick={() => navigate(`/circle/posts/${item.post.id}`)}>
                    继续看
                  </Button>
                </div>
              </article>
            )
          })}
        </div>
      </div>
    </section>
  )
}

export default WatchHistoryPage
