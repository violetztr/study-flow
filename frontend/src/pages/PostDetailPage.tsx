import { ArrowLeftOutlined, EyeOutlined, HeartFilled, HeartOutlined, SendOutlined } from '@ant-design/icons'
import { Alert, Button, Form, Input, Skeleton } from 'antd'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import dayjs from 'dayjs'
import { useNavigate, useParams } from 'react-router-dom'
import { getStoredUser } from '../api/auth'
import { communityApi } from '../api/community'
import type { CommunityPostResponse } from '../api/community'
import CommentList from '../components/community/CommentList'
import TopicBadge from '../components/community/TopicBadge'

type CommentFormValues = {
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

function PostDetailPage() {
  const { id } = useParams()
  const postId = Number(id)
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [form] = Form.useForm<CommentFormValues>()
  const user = getStoredUser()

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

  const meQuery = useQuery({
    queryKey: ['community-me'],
    queryFn: communityApi.getMe,
    enabled: Boolean(user),
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

  const commentMutation = useMutation({
    mutationFn: (values: CommentFormValues) => communityApi.createComment(postId, values),
    onSuccess: () => {
      form.resetFields()
      queryClient.invalidateQueries({ queryKey: ['community-post', postId] })
      queryClient.invalidateQueries({ queryKey: ['community-comments', postId] })
    },
  })

  const deleteCommentMutation = useMutation({
    mutationFn: communityApi.deleteComment,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['community-post', postId] })
      queryClient.invalidateQueries({ queryKey: ['community-comments', postId] })
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

  return (
    <section className="page-section detail-page">
      <div className="detail-shell">
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
          {commentsQuery.isError ? (
            <Alert showIcon type="error" message={commentsQuery.error.message} />
          ) : null}
          {commentMutation.isError ? (
            <Alert showIcon type="error" message={commentMutation.error.message} />
          ) : null}
        </div>

        {postQuery.isLoading ? <Skeleton active /> : null}

        {post ? (
          <article className="post-detail-card">
            <div className="post-detail-meta">
              <TopicBadge name={post.topicName} />
              <span>{post.authorName}</span>
              <span>{dayjs(post.createdAt).format('YYYY-MM-DD HH:mm')}</span>
            </div>

            <h1>{post.title}</h1>
            <p className="post-detail-content">{post.content}</p>

            {post.media.length > 0 ? (
              <div className="post-detail-media-grid">
                {post.media.map((media) => (
                  <div className="post-detail-media-item" key={media.id}>
                    {media.fileType === 'VIDEO' ? (
                      <video
                        className="post-detail-media-video"
                        controls
                        preload="metadata"
                        src={media.url}
                      />
                    ) : (
                      <img
                        alt={media.originalFilename}
                        className="post-detail-media-image"
                        src={media.url}
                      />
                    )}
                  </div>
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
              <span>
                {post.commentCount} 条评论
              </span>
              <span>
                <EyeOutlined /> {post.viewCount}
              </span>
            </div>
          </article>
        ) : null}

        <section className="comment-panel">
          <div className="comment-panel-head">
            <h2>评论</h2>
            <span>{commentsQuery.data?.length ?? 0} 条</span>
          </div>

          {user ? (
            <Form<CommentFormValues>
              className="comment-composer"
              form={form}
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
                  placeholder="写一句回应..."
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
              <span>登录后可以评论和点赞。</span>
              <Button type="primary" onClick={requireLogin}>
                登录
              </Button>
            </div>
          )}

          <CommentList
            comments={commentsQuery.data ?? []}
            currentUserId={meQuery.data?.userId}
            deletingId={deleteCommentMutation.variables ?? null}
            onDelete={user ? (commentId) => deleteCommentMutation.mutate(commentId) : undefined}
          />
        </section>
      </div>
    </section>
  )
}

export default PostDetailPage
