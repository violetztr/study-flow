import {
  ArrowLeftOutlined,
  DeleteOutlined,
  EyeOutlined,
  HeartFilled,
  HeartOutlined,
  SendOutlined,
  UserAddOutlined,
  UserDeleteOutlined,
} from '@ant-design/icons'
import { Alert, Button, Form, Input, Popconfirm, Skeleton, Tag } from 'antd'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import dayjs from 'dayjs'
import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { getStoredUser } from '../api/auth'
import { communityApi } from '../api/community'
import type { CommunityPostResponse } from '../api/community'
import CommentList from '../components/community/CommentList'
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

function togglePostLike(post: CommunityPostResponse) {
  const nextLiked = !post.likedByCurrentUser
  const delta = nextLiked ? 1 : -1

  return {
    ...post,
    likedByCurrentUser: nextLiked,
    reactionCount: Math.max(0, post.reactionCount + delta),
  }
}

function hasVideo(post: CommunityPostResponse) {
  return post.media.some((media) => media.fileType === 'VIDEO')
}

function firstVideo(post?: CommunityPostResponse) {
  return post?.media.find((media) => media.fileType === 'VIDEO')
}

function firstImages(post?: CommunityPostResponse) {
  return post?.media.filter((media) => media.fileType !== 'VIDEO') ?? []
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
    queryKey: ['community-member', postQuery.data?.authorId],
    queryFn: () => communityApi.getMember(postQuery.data!.authorId),
    enabled: Boolean(user && postQuery.data?.authorId),
  })

  const feedQuery = useQuery({
    queryKey: ['community-feed'],
    queryFn: communityApi.listFeed,
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

  const followMutation = useMutation({
    mutationFn: () =>
      authorQuery.data?.followedByCurrentUser
        ? communityApi.unfollowMember(postQuery.data!.authorId)
        : communityApi.followMember(postQuery.data!.authorId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['community-member', postQuery.data?.authorId] })
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
        color: '#ffffff',
      }),
    onSuccess: () => {
      danmakuForm.resetFields()
      queryClient.invalidateQueries({ queryKey: ['community-danmaku', postId] })
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
    },
  })

  const deletePostMutation = useMutation({
    mutationFn: () => (canModerate ? communityApi.adminDeletePost(postId) : communityApi.deletePost(postId)),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['community-feed'] })
      navigate('/circle', { replace: true })
    },
  })

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
  const imageMedia = firstImages(post)
  const relatedVideos = (feedQuery.data ?? [])
    .filter((item) => item.id !== post?.id && hasVideo(item))
    .slice(0, 5)
  const activeDanmaku = (danmakuQuery.data ?? []).filter(
    (item) => Math.abs(item.timeSeconds - currentSecond) <= 5,
  )
  const canFollowAuthor = Boolean(user && post && user.id !== post.authorId)

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
                    <span>{dayjs(post.createdAt).format('YYYY-MM-DD HH:mm')}</span>
                    <span>
                      <EyeOutlined /> {post.viewCount}
                    </span>
                  </div>
                </div>
              </div>

              <div className="ruru-player">
                <video
                  controls
                  preload="metadata"
                  src={video.url}
                  onTimeUpdate={(event) => setCurrentSecond(Math.floor(event.currentTarget.currentTime))}
                />
                <div className="danmaku-layer" aria-hidden="true">
                  {activeDanmaku.map((item, index) => (
                    <span
                      key={item.id}
                      className="danmaku-float"
                      style={{
                        color: item.color,
                        top: `${12 + (index % 7) * 11}%`,
                        animationDelay: `${(index % 4) * 0.5}s`,
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

                <div className="quality-switch">
                  <button type="button" className="active">
                    原画
                  </button>
                  <button type="button" disabled title="后面接转码服务后开放">
                    1080P
                  </button>
                  <button type="button" disabled title="后面接转码服务后开放">
                    720P
                  </button>
                </div>
              </div>

              <div className="watch-actions">
                <Button
                  type="text"
                  className={`post-action-button ${post.likedByCurrentUser ? 'liked' : ''}`}
                  icon={post.likedByCurrentUser ? <HeartFilled /> : <HeartOutlined />}
                  loading={likeMutation.isPending}
                  onClick={() => (user ? likeMutation.mutate() : requireLogin())}
                >
                  {post.likedByCurrentUser ? '已赞' : '点赞'} {post.reactionCount}
                </Button>
                <span>{post.commentCount} 条评论</span>
                <span>{danmakuQuery.data?.length ?? 0} 条弹幕</span>
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
                <div className="author-avatar">{post.authorName.slice(0, 1).toUpperCase()}</div>
                <div className="author-info">
                  <strong>{post.authorName}</strong>
                  <span>
                    {authorQuery.data?.followerCount ?? 0} 粉丝 · {authorQuery.data?.followingCount ?? 0} 关注
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
                      const cover = item.media[0]
                      return (
                        <Link className="related-video" to={`/circle/posts/${item.id}`} key={item.id}>
                          <div className="related-cover">
                            {cover?.fileType === 'VIDEO' ? (
                              <video muted preload="metadata" src={cover.url} />
                            ) : null}
                          </div>
                          <div>
                            <strong>{item.title}</strong>
                            <span>{item.authorName}</span>
                          </div>
                        </Link>
                      )
                    })
                  ) : (
                    <p className="muted-text">还没有更多视频。</p>
                  )}
                </div>
              </section>

              {canModerate ? (
                <section className="side-card">
                  <div className="side-card-title">弹幕管理</div>
                  <div className="danmaku-admin-list">
                    {(danmakuQuery.data ?? []).length > 0 ? (
                      (danmakuQuery.data ?? []).map((item) => (
                        <div className="danmaku-admin-item" key={item.id}>
                          <span>
                            {item.timeSeconds}s · {item.content}
                          </span>
                          <Popconfirm title="删除这条弹幕？" onConfirm={() => deleteDanmakuMutation.mutate(item.id)}>
                            <Button
                              danger
                              type="text"
                              size="small"
                              icon={<DeleteOutlined />}
                              loading={deleteDanmakuMutation.variables === item.id && deleteDanmakuMutation.isPending}
                            />
                          </Popconfirm>
                        </div>
                      ))
                    ) : (
                      <p className="muted-text">还没有弹幕。</p>
                    )}
                  </div>
                </section>
              ) : null}
            </aside>
          </div>
        ) : null}

        {post && !video ? (
          <article className="post-detail-card">
            <div className="post-detail-meta">
              <TopicBadge name={post.topicName} />
              <span>{post.authorName}</span>
              <span>{dayjs(post.createdAt).format('YYYY-MM-DD HH:mm')}</span>
            </div>

            <h1>{post.title}</h1>
            <p className="post-detail-content">{post.content}</p>

            {imageMedia.length > 0 ? (
              <div className="post-detail-media-grid">
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
                className={`post-action-button ${post.likedByCurrentUser ? 'liked' : ''}`}
                icon={post.likedByCurrentUser ? <HeartFilled /> : <HeartOutlined />}
                loading={likeMutation.isPending}
                onClick={() => (user ? likeMutation.mutate() : requireLogin())}
              >
                {post.likedByCurrentUser ? '已喜欢' : '喜欢'} {post.reactionCount}
              </Button>
              <span>{post.commentCount} 条评论</span>
              <span>
                <EyeOutlined /> {post.viewCount}
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

        {post && !video ? (
          <div className="article-author-footer">
            <Tag color="green">{post.authorName}</Tag>
            {canFollowAuthor ? (
              <Button
                size="small"
                icon={authorQuery.data?.followedByCurrentUser ? <UserDeleteOutlined /> : <UserAddOutlined />}
                loading={followMutation.isPending || authorQuery.isLoading}
                onClick={() => followMutation.mutate()}
              >
                {authorQuery.data?.followedByCurrentUser ? '已关注' : '关注作者'}
              </Button>
            ) : null}
          </div>
        ) : null}
      </div>
    </section>
  )
}

export default PostDetailPage
