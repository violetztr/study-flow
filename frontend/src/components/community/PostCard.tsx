import {
  CommentOutlined,
  EyeOutlined,
  HeartFilled,
  HeartOutlined,
  PushpinFilled,
} from '@ant-design/icons'
import { Button } from 'antd'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import dayjs from 'dayjs'
import { Link, useNavigate } from 'react-router-dom'
import { getStoredUser, getStoredWallet, saveStoredWallet } from '../../api/auth'
import { communityApi } from '../../api/community'
import type { CommunityPostResponse, MediaAttachmentResponse } from '../../api/community'
import TopicBadge from './TopicBadge'

type PostCardProps = {
  post: CommunityPostResponse
}

type PostMutationContext = {
  previousFeed?: CommunityPostResponse[]
}

function getExcerpt(content: string) {
  return content.length > 90 ? `${content.slice(0, 90)}...` : content
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

function togglePostPig(post: CommunityPostResponse) {
  if (post.piggedByCurrentUser) {
    return post
  }

  return {
    ...post,
    piggedByCurrentUser: true,
    pigCount: post.pigCount + 1,
  }
}

function getPrimaryMedia(post: CommunityPostResponse): MediaAttachmentResponse | undefined {
  return post.media.find((media) => media.fileType === 'VIDEO') ?? post.media[0]
}

function mediaPreviewUrl(media?: MediaAttachmentResponse) {
  if (!media) {
    return null
  }
  return media.fileType === 'VIDEO' ? media.coverUrl : media.url
}

function PostCard({ post }: PostCardProps) {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const user = getStoredUser()
  const primaryMedia = getPrimaryMedia(post)
  const previewUrl = mediaPreviewUrl(primaryMedia)
  const isVideoPost = post.contentType === 'VIDEO' || primaryMedia?.fileType === 'VIDEO'

  const likeMutation = useMutation<void, Error, void, PostMutationContext>({
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

  const pigMutation = useMutation<void, Error, void, PostMutationContext>({
    mutationFn: () => communityApi.pigPost(post.id),
    onMutate: async () => {
      await queryClient.cancelQueries({ queryKey: ['community-feed'] })
      const previousFeed = queryClient.getQueryData<CommunityPostResponse[]>(['community-feed'])

      queryClient.setQueryData<CommunityPostResponse[]>(['community-feed'], (currentFeed) =>
        currentFeed?.map((item) => (item.id === post.id ? togglePostPig(item) : item)),
      )

      return { previousFeed }
    },
    onError: (_error, _variables, context) => {
      if (context?.previousFeed) {
        queryClient.setQueryData(['community-feed'], context.previousFeed)
      }
    },
    onSuccess: () => {
      const wallet = getStoredWallet()
      if (wallet && !post.piggedByCurrentUser) {
        saveStoredWallet({ ...wallet, pigBalance: Math.max(0, wallet.pigBalance - 1) })
      }
    },
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: ['community-feed'] })
      queryClient.invalidateQueries({ queryKey: ['community-post', post.id] })
    },
  })

  function requireLogin() {
    navigate('/login', { state: { from: '/circle' } })
  }

  function handleLike() {
    if (!user) {
      requireLogin()
      return
    }
    likeMutation.mutate()
  }

  function handlePig() {
    if (!user) {
      requireLogin()
      return
    }
    pigMutation.mutate()
  }

  return (
    <article className="post-card discovery-card">
      <Link to={`/circle/posts/${post.id}`} className="discovery-cover">
        {previewUrl ? (
          <img alt={primaryMedia?.originalFilename ?? post.title} src={previewUrl} loading="lazy" />
        ) : isVideoPost ? (
          <div className="text-cover video-cover-fallback">
            <span>▶</span>
            <p>{post.title}</p>
          </div>
        ) : (
          <div className="text-cover">
            <span>{getInitial(post.authorName)}</span>
            <p>{getExcerpt(post.content)}</p>
          </div>
        )}

        {post.pinned ? (
          <span className="pin-badge">
            <PushpinFilled /> 置顶
          </span>
        ) : null}
      </Link>

      <div className="discovery-card-body">
        <Link to={`/circle/posts/${post.id}`} className="post-title-link">
          <h3>{post.title}</h3>
        </Link>

        <div className="discovery-author-row">
          <span>{post.authorName}</span>
          <span>{dayjs(post.createdAt).format('MM-DD')}</span>
        </div>

        <div className="discovery-topic-row">
          <TopicBadge name={post.topicName} />
        </div>

        <footer className="discovery-card-footer">
          <Button
            type="text"
            size="small"
            className={`post-action-button ${post.likedByCurrentUser ? 'liked' : ''}`}
            icon={post.likedByCurrentUser ? <HeartFilled /> : <HeartOutlined />}
            loading={likeMutation.isPending}
            onClick={handleLike}
          >
            {post.reactionCount}
          </Button>
          <span>
            <CommentOutlined /> {post.commentCount}
          </span>
          <Button
            type="text"
            size="small"
            className={`post-action-button pig-action ${post.piggedByCurrentUser ? 'pigged' : ''}`}
            loading={pigMutation.isPending}
            onClick={handlePig}
          >
            🐖 {post.pigCount}
          </Button>
          <span>
            <EyeOutlined /> {post.viewCount}
          </span>
        </footer>
      </div>
    </article>
  )
}

export default PostCard
