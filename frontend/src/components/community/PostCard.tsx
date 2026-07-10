import {
  CommentOutlined,
  EyeOutlined,
  HeartFilled,
  HeartOutlined,
  PushpinFilled,
} from '@ant-design/icons'
import { Card, Space, Typography } from 'antd'
import dayjs from 'dayjs'
import { Link } from 'react-router-dom'
import type { CommunityPostResponse, CommunityTopicResponse } from '../../api/community'
import TopicBadge from './TopicBadge'

type PostCardProps = {
  post: CommunityPostResponse
  topics?: CommunityTopicResponse[]
}

function getExcerpt(content: string) {
  return content.length > 180 ? `${content.slice(0, 180)}...` : content
}

function PostCard({ post, topics = [] }: PostCardProps) {
  const topic = topics.find((item) => item.id === post.topicId)

  return (
    <Card className="workspace-card" hoverable>
      <Space direction="vertical" size={12} style={{ width: '100%' }}>
        <Space wrap>
          <TopicBadge name={post.topicName} color={topic?.color} />
          {post.pinned ? <PushpinFilled style={{ color: 'var(--sf-rust)' }} /> : null}
          <Typography.Text type="secondary">
            {post.authorName} · {dayjs(post.createdAt).format('YYYY-MM-DD HH:mm')}
          </Typography.Text>
        </Space>

        <Link to={`/circle/posts/${post.id}`} style={{ color: 'inherit' }}>
          <Typography.Title level={3} style={{ margin: 0 }}>
            {post.title}
          </Typography.Title>
        </Link>
        <Typography.Paragraph style={{ margin: 0, whiteSpace: 'pre-wrap' }}>
          {getExcerpt(post.content)}
        </Typography.Paragraph>

        {post.media.length > 0 ? (
          <div className="post-media-grid">
            {post.media.slice(0, 4).map((media) => (
              media.fileType === 'VIDEO' ? (
                <video
                  key={media.id}
                  className="post-media-video"
                  controls
                  preload="metadata"
                  src={media.url}
                />
              ) : (
                <img
                  key={media.id}
                  alt={media.originalFilename}
                  className="post-media-image"
                  src={media.url}
                />
              )
            ))}
          </div>
        ) : null}

        <Space wrap size={18}>
          <Typography.Text type="secondary">
            <CommentOutlined /> {post.commentCount}
          </Typography.Text>
          <Typography.Text type="secondary">
            {post.likedByCurrentUser ? <HeartFilled /> : <HeartOutlined />} {post.reactionCount}
          </Typography.Text>
          <Typography.Text type="secondary">
            <EyeOutlined /> {post.viewCount}
          </Typography.Text>
          <Link to={`/circle/posts/${post.id}`}>查看详情</Link>
        </Space>
      </Space>
    </Card>
  )
}

export default PostCard
