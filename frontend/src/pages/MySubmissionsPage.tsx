import {
  ArrowLeftOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  CloseCircleOutlined,
  PlusOutlined,
} from '@ant-design/icons'
import { Alert, Button, Empty, Skeleton, Tag } from 'antd'
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

function getStatusMeta(status: string) {
  if (status === 'PENDING_REVIEW') {
    return {
      color: 'gold',
      icon: <ClockCircleOutlined />,
      label: '待审核',
      description: '审核通过后会进入公开列表。',
    }
  }

  if (status === 'REJECTED') {
    return {
      color: 'red',
      icon: <CloseCircleOutlined />,
      label: '已驳回',
      description: '可以根据原因调整后重新投稿。',
    }
  }

  if (status === 'PUBLISHED') {
    return {
      color: 'green',
      icon: <CheckCircleOutlined />,
      label: '已发布',
      description: '作品已经公开展示。',
    }
  }

  return {
    color: 'default',
    icon: <ClockCircleOutlined />,
    label: status,
    description: '稿件状态已更新。',
  }
}

function getContentTypeLabel(post: CommunityPostResponse) {
  if (post.contentType === 'VIDEO') {
    return '视频'
  }
  if (post.contentType === 'LIVE') {
    return '直播'
  }
  return '图文'
}

function MySubmissionsPage() {
  const navigate = useNavigate()
  const submissionsQuery = useQuery({
    queryKey: ['community-submissions-my'],
    queryFn: communityApi.listMySubmissions,
  })

  const submissions = submissionsQuery.data ?? []

  return (
    <section className="page-section submissions-page">
      <div className="submission-shell">
        <button className="profile-back-link" type="button" onClick={() => navigate('/circle')}>
          <ArrowLeftOutlined /> 返回 ruru
        </button>

        <header className="submission-hero">
          <div>
            <p>创作中心</p>
            <h1>我的稿件</h1>
            <span>视频投稿会先进入审核，图文发布后直接公开。</span>
          </div>
          <Button type="primary" icon={<PlusOutlined />} onClick={() => navigate('/circle/posts/new')}>
            继续投稿
          </Button>
        </header>

        {submissionsQuery.isError ? (
          <Alert showIcon type="error" message={submissionsQuery.error.message} />
        ) : null}

        {submissionsQuery.isLoading ? <Skeleton active /> : null}

        {!submissionsQuery.isLoading && submissions.length === 0 ? (
          <Empty description="还没有稿件">
            <Button type="primary" onClick={() => navigate('/circle/posts/new')}>
              去投稿
            </Button>
          </Empty>
        ) : null}

        <div className="submission-list">
          {submissions.map((post) => {
            const previewUrl = getPreviewUrl(post)
            const statusMeta = getStatusMeta(post.status)
            const canOpen = post.status === 'PUBLISHED'

            return (
              <article key={post.id} className="submission-card">
                <div className="submission-cover">
                  {previewUrl ? <img alt={post.title} src={previewUrl} /> : <span>{getContentTypeLabel(post)}</span>}
                </div>

                <div className="submission-main">
                  <div className="submission-title-row">
                    <Tag color={statusMeta.color} icon={statusMeta.icon}>
                      {statusMeta.label}
                    </Tag>
                    <span>{getContentTypeLabel(post)}</span>
                    <span>{dayjs(post.createdAt).format('YYYY-MM-DD HH:mm')}</span>
                  </div>

                  <h2>{post.title}</h2>
                  <p>{post.content}</p>

                  {post.topicName ? <span className="submission-topic">#{post.topicName}</span> : null}

                  {post.status === 'REJECTED' && post.reviewReason ? (
                    <div className="submission-reason">驳回原因：{post.reviewReason}</div>
                  ) : (
                    <div className="submission-hint">{statusMeta.description}</div>
                  )}
                </div>

                <div className="submission-actions">
                  <Button disabled={!canOpen} onClick={() => navigate(`/circle/posts/${post.id}`)}>
                    {canOpen ? '查看作品' : '暂不可查看'}
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

export default MySubmissionsPage
