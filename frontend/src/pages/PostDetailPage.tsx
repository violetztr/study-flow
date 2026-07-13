import {
  ArrowLeftOutlined,
  DeleteOutlined,
  EyeOutlined,
  HeartFilled,
  HeartOutlined,
  MessageOutlined,
  ShareAltOutlined,
  SendOutlined,
  StarFilled,
  StarOutlined,
  UserAddOutlined,
  UserDeleteOutlined,
} from '@ant-design/icons'
import { Alert, Button, Form, Input, Popconfirm, Skeleton, Switch } from 'antd'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import dayjs from 'dayjs'
import { useEffect, useRef, useState, type CSSProperties, type SyntheticEvent } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { getStoredUser, saveStoredWallet } from '../api/auth'
import { communityApi } from '../api/community'
import type { CommunityPostResponse } from '../api/community'
import CommentList from '../components/community/CommentList'
import RuruVideoPlayer from '../components/community/RuruVideoPlayer'
import TopicBadge from '../components/community/TopicBadge'

type CommentFormValues = {
  content: string
}

type DanmakuFormValues = {
  content: string
}

type LikeMutationContext = {
  previousPost?: CommunityPostResponse
}

const DANMAKU_FLOAT_SECONDS = 8

const danmakuColorOptions = [
  { label: '白', value: '#ffffff' },
  { label: '粉', value: '#ff6699' },
  { label: '蓝', value: '#66ccff' },
  { label: '黄', value: '#ffd166' },
  { label: '绿', value: '#7ee787' },
]

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

function togglePostFavorite(post: CommunityPostResponse) {
  const nextFavorited = !post.favoritedByCurrentUser
  const delta = nextFavorited ? 1 : -1
  const favoriteCount = post.favoriteCount ?? 0

  return {
    ...post,
    favoritedByCurrentUser: nextFavorited,
    favoriteCount: Math.max(0, favoriteCount + delta),
  }
}

function hasVideo(post: CommunityPostResponse) {
  return post.contentType === 'VIDEO' || post.media.some((media) => media.fileType === 'VIDEO')
}

function shouldReportPlayback(playedSeconds: number, durationSeconds: number) {
  return playedSeconds >= 10 || (durationSeconds > 0 && playedSeconds * 5 >= durationSeconds)
}

function formatPlaybackTime(seconds: number) {
  const safeSeconds = Math.max(0, Math.floor(seconds))
  const minutes = Math.floor(safeSeconds / 60)
  const restSeconds = safeSeconds % 60

  return `${minutes}:${restSeconds.toString().padStart(2, '0')}`
}

function formatMetric(value?: number | null) {
  const safeValue = value ?? 0
  if (safeValue >= 10000) {
    return `${(safeValue / 10000).toFixed(safeValue >= 100000 ? 0 : 1).replace(/\.0$/, '')}万`
  }
  return `${safeValue}`
}

function firstVideo(post?: CommunityPostResponse) {
  return post?.media.find((media) => media.fileType === 'VIDEO')
}

function firstImages(post?: CommunityPostResponse) {
  return post?.media.filter((media) => media.fileType !== 'VIDEO') ?? []
}

function renderAuthorAvatar(name: string, avatarUrl?: string | null) {
  return avatarUrl ? <img alt={name} src={avatarUrl} /> : name.trim().slice(0, 1).toUpperCase() || 'R'
}

function getVideoSourceLabel(video?: ReturnType<typeof firstVideo>) {
  if (!video) {
    return ''
  }

  if (video.playbackUrl && video.playbackType === 'HLS') {
    return '自动'
  }

  if (video.transcodeStatus === 'FAILED') {
    return '转码失败'
  }

  if (
    video.url &&
    (video.transcodeStatus === 'WAITING' ||
      video.transcodeStatus === 'PENDING' ||
      video.transcodeStatus === 'TRANSCODING')
  ) {
    return '高清处理中'
  }

  return '原视频'
}

function canModerateCommunity(user: ReturnType<typeof getStoredUser>) {
  return Boolean(user && (user.username === 'ruru' || user.role === 'ADMIN' || user.role === 'OWNER'))
}

function PostDetailPage() {
  const { id } = useParams()
  const postId = Number(id)
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [commentForm] = Form.useForm<CommentFormValues>()
  const [danmakuForm] = Form.useForm<DanmakuFormValues>()
  const [currentSecond, setCurrentSecond] = useState(0)
  const [danmakuColor, setDanmakuColor] = useState(danmakuColorOptions[0].value)
  const [danmakuVisible, setDanmakuVisible] = useState(true)
  const [isVideoPlaying, setIsVideoPlaying] = useState(false)
  const [selectedQualityUrl, setSelectedQualityUrl] = useState<string | null>(null)
  const viewReportedRef = useRef(false)
  const user = getStoredUser()
  const canModerate = canModerateCommunity(user)

  const postQuery = useQuery({
    queryKey: ['community-post', postId],
    queryFn: () => communityApi.getPost(postId),
    enabled: Number.isFinite(postId),
  })

  const commentsQuery = useQuery({
    queryKey: ['community-comments', postId],
    queryFn: () => communityApi.listComments(postId),
    enabled: Number.isFinite(postId),
  })

  const danmakuQuery = useQuery({
    queryKey: ['community-danmaku', postId],
    queryFn: () => communityApi.listDanmaku(postId),
    enabled: Number.isFinite(postId),
  })

  const meQuery = useQuery({
    queryKey: ['community-me'],
    queryFn: communityApi.getMe,
    enabled: Boolean(user),
  })

  const authorQuery = useQuery({
    queryKey: ['community-profile', postQuery.data?.authorId],
    queryFn: () => communityApi.getProfile(postQuery.data!.authorId),
    enabled: Boolean(postQuery.data?.authorId),
    select: (profile) => profile.member,
  })

  const feedQuery = useQuery({
    queryKey: ['community-feed'],
    queryFn: communityApi.listFeed,
  })

  useEffect(() => {
    viewReportedRef.current = false
    setCurrentSecond(0)
  }, [postId])

  const activeVideo = firstVideo(postQuery.data)

  useEffect(() => {
    setSelectedQualityUrl(null)
  }, [activeVideo?.id, activeVideo?.playbackUrl])

  const reportViewMutation = useMutation({
    mutationFn: (values: { playedSeconds: number; durationSeconds: number }) =>
      communityApi.reportPostView(postId, values),
    onSuccess: (response) => {
      queryClient.setQueryData<CommunityPostResponse>(['community-post', postId], (currentPost) =>
        currentPost ? { ...currentPost, viewCount: response.viewCount } : currentPost,
      )
      queryClient.invalidateQueries({ queryKey: ['community-feed'] })
      if (user) {
        queryClient.invalidateQueries({ queryKey: ['community-watch-history-my'] })
      }
    },
    onError: () => {
      viewReportedRef.current = false
    },
  })

  const likeMutation = useMutation<void, Error, void, LikeMutationContext>({
    mutationFn: () =>
      postQuery.data?.likedByCurrentUser
        ? communityApi.unlikePost(postId)
        : communityApi.likePost(postId),
    onMutate: async () => {
      await queryClient.cancelQueries({ queryKey: ['community-post', postId] })
      const previousPost = queryClient.getQueryData<CommunityPostResponse>(['community-post', postId])

      queryClient.setQueryData<CommunityPostResponse>(['community-post', postId], (currentPost) =>
        currentPost ? togglePostLike(currentPost) : currentPost,
      )

      return { previousPost }
    },
    onError: (_error, _variables, context) => {
      if (context?.previousPost) {
        queryClient.setQueryData(['community-post', postId], context.previousPost)
      }
    },
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: ['community-post', postId] })
      queryClient.invalidateQueries({ queryKey: ['community-feed'] })
    },
  })

  const pigMutation = useMutation<Awaited<ReturnType<typeof communityApi.pigPost>>, Error, void, LikeMutationContext>({
    mutationFn: () => communityApi.pigPost(postId),
    onMutate: async () => {
      await queryClient.cancelQueries({ queryKey: ['community-post', postId] })
      const previousPost = queryClient.getQueryData<CommunityPostResponse>(['community-post', postId])

      queryClient.setQueryData<CommunityPostResponse>(['community-post', postId], (currentPost) =>
        currentPost ? togglePostPig(currentPost) : currentPost,
      )

      return { previousPost }
    },
    onError: (_error, _variables, context) => {
      if (context?.previousPost) {
        queryClient.setQueryData(['community-post', postId], context.previousPost)
      }
    },
    onSuccess: (wallet) => {
      saveStoredWallet(wallet)
    },
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: ['community-post', postId] })
      queryClient.invalidateQueries({ queryKey: ['community-feed'] })
    },
  })

  const favoriteMutation = useMutation<void, Error, void, LikeMutationContext>({
    mutationFn: () =>
      postQuery.data?.favoritedByCurrentUser
        ? communityApi.unfavoritePost(postId)
        : communityApi.favoritePost(postId),
    onMutate: async () => {
      await queryClient.cancelQueries({ queryKey: ['community-post', postId] })
      const previousPost = queryClient.getQueryData<CommunityPostResponse>(['community-post', postId])

      queryClient.setQueryData<CommunityPostResponse>(['community-post', postId], (currentPost) =>
        currentPost ? togglePostFavorite(currentPost) : currentPost,
      )

      return { previousPost }
    },
    onError: (_error, _variables, context) => {
      if (context?.previousPost) {
        queryClient.setQueryData(['community-post', postId], context.previousPost)
      }
    },
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: ['community-post', postId] })
      queryClient.invalidateQueries({ queryKey: ['community-feed'] })
      queryClient.invalidateQueries({ queryKey: ['community-favorites-my'] })
    },
  })

  const followMutation = useMutation({
    mutationFn: () =>
      authorQuery.data?.followedByCurrentUser
        ? communityApi.unfollowMember(postQuery.data!.authorId)
        : communityApi.followMember(postQuery.data!.authorId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['community-member', postQuery.data?.authorId] })
      queryClient.invalidateQueries({ queryKey: ['community-profile', postQuery.data?.authorId] })
      queryClient.invalidateQueries({ queryKey: ['community-members'] })
    },
  })

  const commentMutation = useMutation({
    mutationFn: (values: CommentFormValues) => communityApi.createComment(postId, values),
    onSuccess: () => {
      commentForm.resetFields()
      queryClient.invalidateQueries({ queryKey: ['community-post', postId] })
      queryClient.invalidateQueries({ queryKey: ['community-comments', postId] })
    },
  })

  const danmakuMutation = useMutation({
    mutationFn: (values: DanmakuFormValues) =>
      communityApi.createDanmaku(postId, {
        content: values.content,
        timeSeconds: currentSecond,
        color: danmakuColor,
      }),
    onSuccess: () => {
      danmakuForm.resetFields()
      queryClient.invalidateQueries({ queryKey: ['community-danmaku', postId] })
      queryClient.invalidateQueries({ queryKey: ['community-post', postId] })
      queryClient.invalidateQueries({ queryKey: ['community-feed'] })
    },
  })

  const deleteCommentMutation = useMutation({
    mutationFn: (commentId: number) =>
      canModerate ? communityApi.adminDeleteComment(commentId) : communityApi.deleteComment(commentId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['community-post', postId] })
      queryClient.invalidateQueries({ queryKey: ['community-comments', postId] })
    },
  })

  const deleteDanmakuMutation = useMutation({
    mutationFn: communityApi.adminDeleteDanmaku,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['community-danmaku', postId] })
      queryClient.invalidateQueries({ queryKey: ['community-post', postId] })
      queryClient.invalidateQueries({ queryKey: ['community-feed'] })
    },
  })

  const deletePostMutation = useMutation({
    mutationFn: () => (canModerate ? communityApi.adminDeletePost(postId) : communityApi.deletePost(postId)),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['community-feed'] })
      navigate('/circle', { replace: true })
    },
  })

  function handleVideoTimeUpdate(event: SyntheticEvent<HTMLVideoElement>) {
    const videoElement = event.currentTarget
    const playedSeconds = Math.floor(videoElement.currentTime)
    const durationSeconds = Number.isFinite(videoElement.duration)
      ? Math.floor(videoElement.duration)
      : 0

    setCurrentSecond(playedSeconds)

    if (!viewReportedRef.current && shouldReportPlayback(playedSeconds, durationSeconds)) {
      viewReportedRef.current = true
      reportViewMutation.mutate({ playedSeconds, durationSeconds })
    }
  }

  if (!Number.isFinite(postId)) {
    return (
      <section className="page-section">
        <Alert showIcon type="error" message="帖子地址无效" />
      </section>
    )
  }

  function requireLogin() {
    navigate('/login', { state: { from: `/circle/posts/${postId}` } })
  }

  const post = postQuery.data
  const video = firstVideo(post)
  const videoQualities = video?.qualities ?? []
  const selectedVideoSource = selectedQualityUrl ?? video?.playbackUrl ?? video?.url ?? ''
  const imageMedia = firstImages(post)
  const relatedVideos = (feedQuery.data ?? [])
    .filter((item) => item.id !== post?.id && hasVideo(item))
    .slice(0, 5)
  const danmakuItems = danmakuQuery.data ?? []
  const activeDanmaku = danmakuVisible
    ? danmakuItems.filter(
        (item) =>
          currentSecond >= item.timeSeconds &&
          currentSecond < item.timeSeconds + DANMAKU_FLOAT_SECONDS,
      )
    : []
  const canFollowAuthor = Boolean(user && post && user.id !== post.authorId)
  const authorName = authorQuery.data?.displayName || post?.authorName || 'ruru'
  const authorAvatarUrl = authorQuery.data?.avatarUrl || post?.authorAvatarUrl
  const sourceLabel = getVideoSourceLabel(video)

  return (
    <section className={`page-section detail-page ${video ? 'video-watch-page' : ''}`}>
      <div className={`detail-shell ${video ? 'video-detail-shell' : ''}`}>
        <Button
          type="text"
          icon={<ArrowLeftOutlined />}
          className="ghost-link-button"
          onClick={() => navigate('/circle')}
        >
          返回
        </Button>

        <div className="notice-stack">
          {postQuery.isError ? <Alert showIcon type="error" message={postQuery.error.message} /> : null}
          {commentsQuery.isError ? <Alert showIcon type="error" message={commentsQuery.error.message} /> : null}
          {danmakuQuery.isError ? <Alert showIcon type="error" message={danmakuQuery.error.message} /> : null}
          {commentMutation.isError ? <Alert showIcon type="error" message={commentMutation.error.message} /> : null}
          {danmakuMutation.isError ? <Alert showIcon type="error" message={danmakuMutation.error.message} /> : null}
          {followMutation.isError ? <Alert showIcon type="error" message={followMutation.error.message} /> : null}
          {deletePostMutation.isError ? <Alert showIcon type="error" message={deletePostMutation.error.message} /> : null}
          {deleteDanmakuMutation.isError ? (
            <Alert showIcon type="error" message={deleteDanmakuMutation.error.message} />
          ) : null}
        </div>

        {postQuery.isLoading ? <Skeleton active /> : null}

        {post && video ? (
          <div className="watch-layout">
            <main className="watch-main">
              <div className="watch-title-row">
                <div>
                  <h1>{post.title}</h1>
                  <div className="watch-meta">
                    <TopicBadge name={post.topicName} />
                    <span>
                      <EyeOutlined /> {formatMetric(post.viewCount)} 播放
                    </span>
                    <span>
                      <MessageOutlined /> {formatMetric(post.danmakuCount)} 弹幕
                    </span>
                    <span>{dayjs(post.createdAt).format('YYYY-MM-DD HH:mm')}</span>
                  </div>
                </div>
              </div>

              <div className="ruru-player">
                <RuruVideoPlayer
                  src={selectedVideoSource}
                  poster={video.coverUrl ?? undefined}
                  autoPlay
                  muted
                  onTimeUpdate={handleVideoTimeUpdate}
                  onPlay={() => setIsVideoPlaying(true)}
                  onPause={() => setIsVideoPlaying(false)}
                  onEnded={() => setIsVideoPlaying(false)}
                />
                <div className={`danmaku-layer ${isVideoPlaying ? '' : 'is-paused'}`} aria-hidden="true">
                  {activeDanmaku.map((item, index) => (
                    <span
                      key={item.id}
                      className="danmaku-float"
                      style={{
                        color: item.color,
                        top: `${12 + (index % 7) * 11}%`,
                        animationDuration: `${DANMAKU_FLOAT_SECONDS}s`,
                      }}
                    >
                      {item.content}
                    </span>
                  ))}
                </div>
              </div>

              <div className="player-toolbar">
                <Form<DanmakuFormValues>
                  className="danmaku-form"
                  form={danmakuForm}
                  onFinish={(values) => (user ? danmakuMutation.mutate(values) : requireLogin())}
                >
                  <Form.Item name="content" rules={[{ required: true, message: '写一句弹幕吧' }]}>
                    <Input
                      maxLength={200}
                      placeholder={user ? '发一条友善的弹幕' : '登录后发送弹幕'}
                      disabled={!user}
                    />
                  </Form.Item>
                  <Button
                    type="primary"
                    htmlType="submit"
                    icon={<SendOutlined />}
                    loading={danmakuMutation.isPending}
                    onClick={() => {
                      if (!user) {
                        requireLogin()
                      }
                    }}
                  >
                    发送
                  </Button>
                </Form>

                <div className="danmaku-controls">
                  <label>
                    <Switch size="small" checked={danmakuVisible} onChange={setDanmakuVisible} />
                    <span>弹幕</span>
                  </label>
                  <div className="danmaku-color-picker" aria-label="弹幕颜色">
                    {danmakuColorOptions.map((option) => (
                      <button
                        key={option.value}
                        type="button"
                        className={danmakuColor === option.value ? 'active' : ''}
                        style={{ '--danmaku-color': option.value } as CSSProperties}
                        title={`${option.label}色弹幕`}
                        onClick={() => setDanmakuColor(option.value)}
                      >
                        <span />
                      </button>
                    ))}
                  </div>
                </div>

                <div className="quality-switch">
                  {video.playbackUrl && video.playbackType === 'HLS' ? (
                    <>
                      <button
                        type="button"
                        className={selectedQualityUrl === null ? 'active' : ''}
                        onClick={() => setSelectedQualityUrl(null)}
                      >
                        自动
                      </button>
                      {videoQualities.map((quality) => (
                        <button
                          key={quality.qualityLabel}
                          type="button"
                          className={selectedQualityUrl === quality.playlistUrl ? 'active' : ''}
                          onClick={() => setSelectedQualityUrl(quality.playlistUrl)}
                        >
                          {quality.qualityLabel}
                        </button>
                      ))}
                    </>
                  ) : (
                    <button type="button" className="active">
                      {sourceLabel}
                    </button>
                  )}
                </div>
              </div>

              <div className="watch-actions">
                <Button
                  type="text"
                  className={`watch-action-button ${post.likedByCurrentUser ? 'liked' : ''}`}
                  icon={post.likedByCurrentUser ? <HeartFilled /> : <HeartOutlined />}
                  loading={likeMutation.isPending}
                  onClick={() => (user ? likeMutation.mutate() : requireLogin())}
                >
                  {post.likedByCurrentUser ? '已赞' : '点赞'} {formatMetric(post.reactionCount)}
                </Button>
                <Button
                  type="text"
                  className={`watch-action-button pig-action ${post.piggedByCurrentUser ? 'pigged' : ''}`}
                  loading={pigMutation.isPending}
                  onClick={() => (user ? pigMutation.mutate() : requireLogin())}
                >
                  🐖 投猪币 {formatMetric(post.pigCount)}
                </Button>
                <Button
                  type="text"
                  className={`watch-action-button ${post.favoritedByCurrentUser ? 'favorited' : ''}`}
                  icon={post.favoritedByCurrentUser ? <StarFilled /> : <StarOutlined />}
                  loading={favoriteMutation.isPending}
                  onClick={() => (user ? favoriteMutation.mutate() : requireLogin())}
                >
                  {post.favoritedByCurrentUser ? '已收藏' : '收藏'} {formatMetric(post.favoriteCount)}
                </Button>
                <Button type="text" className="watch-action-button" icon={<ShareAltOutlined />} disabled>
                  分享
                </Button>
                <span>{formatMetric(post.commentCount)} 评论</span>
                <span>{formatMetric(post.danmakuCount)} 弹幕</span>
                {canModerate ? (
                  <Popconfirm title="确认删除这个帖子/视频？" onConfirm={() => deletePostMutation.mutate()}>
                    <Button
                      danger
                      type="text"
                      icon={<DeleteOutlined />}
                      loading={deletePostMutation.isPending}
                    >
                      删除
                    </Button>
                  </Popconfirm>
                ) : null}
              </div>

              <p className="watch-description">{post.content}</p>
            </main>

            <aside className="watch-side">
              <section className="author-panel">
                <Link to={`/circle/members/${post.authorId}`} className="author-avatar">
                  {renderAuthorAvatar(authorName, authorAvatarUrl)}
                </Link>
                <div className="author-info">
                  <Link to={`/circle/members/${post.authorId}`}>{authorName}</Link>
                  <span>
                    {formatMetric(authorQuery.data?.followerCount)} 粉丝 ·{' '}
                    {formatMetric(authorQuery.data?.followingCount)} 关注
                  </span>
                </div>
                {canFollowAuthor ? (
                  <Button
                    type={authorQuery.data?.followedByCurrentUser ? 'default' : 'primary'}
                    icon={authorQuery.data?.followedByCurrentUser ? <UserDeleteOutlined /> : <UserAddOutlined />}
                    loading={followMutation.isPending || authorQuery.isLoading}
                    onClick={() => followMutation.mutate()}
                  >
                    {authorQuery.data?.followedByCurrentUser ? '已关注' : '关注'}
                  </Button>
                ) : null}
              </section>

              <section className="side-card">
                <div className="side-card-title">简介</div>
                <p>{post.content || '这个视频还没有简介。'}</p>
              </section>

              <section className="side-card">
                <div className="side-card-title">更多视频</div>
                <div className="related-list">
                  {relatedVideos.length > 0 ? (
                    relatedVideos.map((item) => {
                      const relatedVideo = firstVideo(item)
                      return (
                        <Link className="related-video" to={`/circle/posts/${item.id}`} key={item.id}>
                          <div className="related-cover">
                            {relatedVideo?.coverUrl ? (
                              <img alt={relatedVideo.originalFilename} src={relatedVideo.coverUrl} loading="lazy" />
                            ) : (
                              <span>▶</span>
                            )}
                          </div>
                          <div>
                            <strong>{item.title}</strong>
                            <span>
                              {item.authorName} · {formatMetric(item.viewCount)} 播放
                            </span>
                          </div>
                        </Link>
                      )
                    })
                  ) : (
                    <p className="muted-text">还没有更多视频。</p>
                  )}
                </div>
              </section>

              <section className="side-card danmaku-list-card">
                <div className="side-card-title">
                  <span>弹幕列表</span>
                  <small>{formatMetric(danmakuItems.length)}</small>
                </div>
                <div className="danmaku-admin-list">
                  {danmakuItems.length > 0 ? (
                    danmakuItems.map((item) => (
                      <div className="danmaku-admin-item" key={item.id}>
                        <i style={{ background: item.color }} />
                        <span>
                          <b>{formatPlaybackTime(item.timeSeconds)}</b>
                          {item.content}
                        </span>
                        {canModerate ? (
                          <Popconfirm title="删除这条弹幕？" onConfirm={() => deleteDanmakuMutation.mutate(item.id)}>
                            <Button
                              danger
                              type="text"
                              size="small"
                              icon={<DeleteOutlined />}
                              loading={deleteDanmakuMutation.variables === item.id && deleteDanmakuMutation.isPending}
                            />
                          </Popconfirm>
                        ) : null}
                      </div>
                    ))
                  ) : (
                    <p className="muted-text">还没有弹幕。</p>
                  )}
                </div>
              </section>

            </aside>
          </div>
        ) : null}

        {post && !video ? (
          <article className="post-detail-card article-detail-card">
            <div className="article-detail-head">
              <div className="article-author-block">
                <Link to={`/circle/members/${post.authorId}`} className="author-avatar">
                  {renderAuthorAvatar(authorName, authorAvatarUrl)}
                </Link>
                <div>
                  <Link to={`/circle/members/${post.authorId}`}>
                    <strong>{authorName}</strong>
                  </Link>
                  <span>{dayjs(post.createdAt).format('YYYY-MM-DD HH:mm')}</span>
                </div>
              </div>

              {canFollowAuthor ? (
                <Button
                  size="small"
                  icon={authorQuery.data?.followedByCurrentUser ? <UserDeleteOutlined /> : <UserAddOutlined />}
                  loading={followMutation.isPending || authorQuery.isLoading}
                  onClick={() => followMutation.mutate()}
                >
                  {authorQuery.data?.followedByCurrentUser ? '已关注' : '关注'}
                </Button>
              ) : null}
            </div>

            <div className="article-detail-meta">
              <TopicBadge name={post.topicName} />
              <span>
                <EyeOutlined /> {formatMetric(post.viewCount)} 浏览
              </span>
              <span>
                <MessageOutlined /> {formatMetric(post.commentCount)} 评论
              </span>
            </div>

            <h1>{post.title}</h1>
            <p className="post-detail-content">{post.content}</p>

            {imageMedia.length > 0 ? (
              <div
                className={`post-detail-media-grid article-media-grid ${
                  imageMedia.length === 1 ? 'single-image' : ''
                }`}
              >
                {imageMedia.map((media) => (
                  <img
                    key={media.id}
                    alt={media.originalFilename}
                    className="post-detail-media-image"
                    src={media.url}
                  />
                ))}
              </div>
            ) : null}

            <div className="post-detail-actions">
              <Button
                type="text"
                className={`watch-action-button ${post.likedByCurrentUser ? 'liked' : ''}`}
                icon={post.likedByCurrentUser ? <HeartFilled /> : <HeartOutlined />}
                loading={likeMutation.isPending}
                onClick={() => (user ? likeMutation.mutate() : requireLogin())}
              >
                {post.likedByCurrentUser ? '已喜欢' : '喜欢'} {formatMetric(post.reactionCount)}
              </Button>
              <Button
                type="text"
                className={`watch-action-button pig-action ${post.piggedByCurrentUser ? 'pigged' : ''}`}
                loading={pigMutation.isPending}
                onClick={() => (user ? pigMutation.mutate() : requireLogin())}
              >
                🐖 投猪币 {formatMetric(post.pigCount)}
              </Button>
              <Button
                type="text"
                className={`watch-action-button ${post.favoritedByCurrentUser ? 'favorited' : ''}`}
                icon={post.favoritedByCurrentUser ? <StarFilled /> : <StarOutlined />}
                loading={favoriteMutation.isPending}
                onClick={() => (user ? favoriteMutation.mutate() : requireLogin())}
              >
                {post.favoritedByCurrentUser ? '已收藏' : '收藏'} {formatMetric(post.favoriteCount)}
              </Button>
              <Button type="text" className="watch-action-button" icon={<ShareAltOutlined />} disabled>
                分享
              </Button>
              <span>{formatMetric(post.commentCount)} 评论</span>
              <span>
                <EyeOutlined /> {formatMetric(post.viewCount)}
              </span>
              {canModerate ? (
                <Popconfirm title="确认删除这个帖子？" onConfirm={() => deletePostMutation.mutate()}>
                  <Button danger type="text" icon={<DeleteOutlined />} loading={deletePostMutation.isPending}>
                    删除
                  </Button>
                </Popconfirm>
              ) : null}
            </div>
          </article>
        ) : null}

        {post ? (
          <section className="comment-panel watch-comment-panel">
            <div className="comment-panel-head">
              <h2>评论</h2>
              <span>{commentsQuery.data?.length ?? 0} 条</span>
            </div>

            {user ? (
              <Form<CommentFormValues>
                className="comment-composer"
                form={commentForm}
                layout="vertical"
                requiredMark={false}
                onFinish={(values) => commentMutation.mutate(values)}
              >
                <Form.Item
                  name="content"
                  rules={[
                    { required: true, message: '请输入评论内容' },
                    { max: 2000, message: '评论不能超过 2000 个字符' },
                  ]}
                >
                  <Input.TextArea
                    bordered={false}
                    className="comment-input"
                    autoSize={{ minRows: 3, maxRows: 8 }}
                    placeholder="写一条评论..."
                  />
                </Form.Item>
                <Button
                  type="primary"
                  htmlType="submit"
                  icon={<SendOutlined />}
                  loading={commentMutation.isPending}
                >
                  发送
                </Button>
              </Form>
            ) : (
              <div className="login-nudge">
                <span>登录后可以评论、点赞、关注和发送弹幕。</span>
                <Button type="primary" onClick={requireLogin}>
                  登录
                </Button>
              </div>
            )}

            <CommentList
              comments={commentsQuery.data ?? []}
              currentUserId={meQuery.data?.userId}
              canModerate={canModerate}
              deletingId={deleteCommentMutation.variables ?? null}
              onDelete={user ? (commentId) => deleteCommentMutation.mutate(commentId) : undefined}
            />
          </section>
        ) : null}
      </div>
    </section>
  )
}

export default PostDetailPage
