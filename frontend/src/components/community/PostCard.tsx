import {
  CommentOutlined,
  EyeOutlined,
  HeartFilled,
  HeartOutlined,
  PushpinFilled,
} from '@ant-design/icons'
import { Button, Typography } from 'antd'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import dayjs from 'dayjs'
import { Link, useNavigate } from 'react-router-dom'
import { getStoredUser } from '../../api/auth'
import { communityApi } from '../../api/community'
import type { CommunityPostResponse, CommunityTopicResponse } from '../../api/community'
import TopicBadge from './TopicBadge'

type PostCardProps = {
  post: CommunityPostResponse
  topics?: CommunityTopicResponse[]
}

type LikeMutationContext = {
  previousFeed?: CommunityPostResponse[]
}

function getExcerpt(content: string) {
  return content.length > 220 ? `${content.slice(0, 220)}...` : content
}

function getInitial(name: string) {
  return name.trim().slice(0, 1).toUpperCase() || 'R'
}

function togglePostLike(post: CommunityPostResponse) {
  const nextLiked = !post.likedByCurrentUser
  const delta = nextLiked ? 1 : -1

  return {
    ...post,
    likedByCurrentUser: nextLiked,
    reactionCount: Math.max(0, post.reactionCount + delta),
  }
}

function PostCard({ post, topics = [] }: PostCardProps) {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const user = getStoredUser()
  const topic = topics.find((item) => item.id === post.topicId)

  const likeMutation = useMutation<void, Error, void, LikeMutationContext>({
    mutationFn: () =>
      post.likedByCurrentUser
        ? communityApi.unlikePost(post.id)
        : communityApi.likePost(post.id),
    onMutate: async () => {
      await queryClient.cancelQueries({ queryKey: ['community-feed'] })
      const previousFeed = queryClient.getQueryData<CommunityPostResponse[]>(['community-feed'])

      queryClient.setQueryData<CommunityPostResponse[]>(['community-feed'], (currentFeed) =>
        currentFeed?.map((item) => (item.id === post.id ? togglePostLike(item) : item)),
      )

      return { previousFeed }
    },
    onError: (_error, _variables, context) => {
      if (context?.previousFeed) {
        queryClient.setQueryData(['community-feed'], context.previousFeed)
      }
    },
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: ['community-feed'] })
      queryClient.invalidateQueries({ queryKey: ['community-post', post.id] })
    },
  })

  function handleLike() {
    if (!user) {
      navigate('/login', { state: { from: '/circle' } })
      return
    }
    likeMutation.mutate()
  }

  function goDetail() {
    navigate(`/circle/posts/${post.id}`)
  }

  return (
    <article className="post-card">
      <header className="post-card-head">
        <div className="post-author-avatar">{getInitial(post.authorName)}</div>
        <div className="post-meta">
          <div className="post-meta-line">
            <strong>{post.authorName}</strong>
            <span>{dayjs(post.createdAt).format('MM-DD HH:mm')}</span>
            {post.pinned ? (
              <span className="post-pin">
                <PushpinFilled /> 置顶
              </span>
            ) : null}
          </div>
          <TopicBadge name={post.topicName} color={topic?.color} />
        </div>
      </header>

      <Link to={`/circle/posts/${post.id}`} className="post-title-link">
        <Typography.Title level={3} className="post-card-title">
          {post.title}
        </Typography.Title>
      </Link>

      <Typography.Paragraph className="post-card-content">
        {getExcerpt(post.content)}
      </Typography.Paragraph>

      {post.media.length > 0 ? (
        <div className="post-media-grid">
          {post.media.slice(0, 4).map((media, index) => (
            <div className="post-media-item" key={media.id}>
              {media.fileType === 'VIDEO' ? (
                <video
                  className="post-media-video"
                  controls
                  preload="metadata"
                  src={media.url}
                />
              ) : (
                <img
                  alt={media.originalFilename}
                  className="post-media-image"
                  src={media.url}
                />
              )}
              {index === 3 && post.media.length > 4 ? (
                <span className="post-media-more">+{post.media.length - 4}</span>
              ) : null}
            </div>
          ))}
        </div>
      ) : null}

      <footer className="post-card-footer">
        <Button
          type="text"
          className={`post-action-button ${post.likedByCurrentUser ? 'liked' : ''}`}
          icon={post.likedByCurrentUser ? <HeartFilled /> : <HeartOutlined />}
          loading={likeMutation.isPending}
          onClick={handleLike}
        >
          {post.reactionCount}
        </Button>
        <Button
          type="text"
          className="post-action-button"
          icon={<CommentOutlined />}
          onClick={goDetail}
        >
          {post.commentCount}
        </Button>
        <span className="post-view-count">
          <EyeOutlined /> {post.viewCount}
        </span>
        <Button type="text" className="post-read-button" onClick={goDetail}>
          查看
        </Button>
      </footer>
    </article>
  )
}

export default PostCard
