import { ArrowLeftOutlined, HeartFilled, HeartOutlined, SendOutlined } from '@ant-design/icons'
import { Alert, Button, Card, Form, Input, Skeleton, Space, Typography } from 'antd'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import dayjs from 'dayjs'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { getStoredUser } from '../api/auth'
import { communityApi } from '../api/community'
import CommentList from '../components/community/CommentList'
import TopicBadge from '../components/community/TopicBadge'

type CommentFormValues = {
  content: string
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

  const likeMutation = useMutation({
    mutationFn: () =>
      postQuery.data?.likedByCurrentUser
        ? communityApi.unlikePost(postId)
        : communityApi.likePost(postId),
    onSuccess: () => {
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

  return (
    <section className="page-section">
      <div className="page-heading">
        <div>
          <p className="dashboard-kicker">Post detail</p>
          <h1>动态详情</h1>
        </div>
        <Button icon={<ArrowLeftOutlined />}>
          <Link to="/circle">返回社区</Link>
        </Button>
      </div>

      <div className="dashboard-content">
        {postQuery.isError ? <Alert showIcon type="error" message={postQuery.error.message} /> : null}
        {commentsQuery.isError ? (
          <Alert showIcon type="error" message={commentsQuery.error.message} />
        ) : null}
        {commentMutation.isError ? (
          <Alert showIcon type="error" message={commentMutation.error.message} />
        ) : null}

        {postQuery.isLoading ? (
          <Skeleton active />
        ) : postQuery.data ? (
          <Card className="workspace-card">
            <Space direction="vertical" size={16} style={{ width: '100%' }}>
              <Space wrap>
                <TopicBadge name={postQuery.data.topicName} />
                <Typography.Text type="secondary">
                  {postQuery.data.authorName} · {dayjs(postQuery.data.createdAt).format('YYYY-MM-DD HH:mm')}
                </Typography.Text>
              </Space>
              <Typography.Title level={2} style={{ margin: 0 }}>
                {postQuery.data.title}
              </Typography.Title>
              <Typography.Paragraph style={{ whiteSpace: 'pre-wrap', fontSize: 16 }}>
                {postQuery.data.content}
              </Typography.Paragraph>
              {postQuery.data.media.length > 0 ? (
                <div className="post-detail-media-grid">
                  {postQuery.data.media.map((media) => (
                    <img
                      key={media.id}
                      alt={media.originalFilename}
                      className="post-detail-media-image"
                      src={media.url}
                    />
                  ))}
                </div>
              ) : null}
              <Space wrap>
                <Button
                  type={postQuery.data.likedByCurrentUser ? 'primary' : 'default'}
                  icon={postQuery.data.likedByCurrentUser ? <HeartFilled /> : <HeartOutlined />}
                  loading={likeMutation.isPending}
                  onClick={() => (user ? likeMutation.mutate() : requireLogin())}
                >
                  {postQuery.data.likedByCurrentUser ? '已喜欢' : '喜欢'} · {postQuery.data.reactionCount}
                </Button>
                <Typography.Text type="secondary">
                  {postQuery.data.commentCount} 条评论 · {postQuery.data.viewCount} 次浏览
                </Typography.Text>
              </Space>
            </Space>
          </Card>
        ) : null}

        <Card className="workspace-card" title="评论">
          {user ? (
            <Form<CommentFormValues>
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
                <Input.TextArea rows={4} placeholder="写下你的建议、追问或鼓励..." />
              </Form.Item>
              <Button
                type="primary"
                htmlType="submit"
                icon={<SendOutlined />}
                loading={commentMutation.isPending}
              >
                发表评论
              </Button>
            </Form>
          ) : (
            <Alert
              showIcon
              type="info"
              message="登录后可以评论和点赞"
              action={<Button onClick={requireLogin}>去登录</Button>}
              style={{ marginBottom: 18 }}
            />
          )}

          <CommentList
            comments={commentsQuery.data ?? []}
            currentUserId={meQuery.data?.userId}
            deletingId={deleteCommentMutation.variables ?? null}
            onDelete={user ? (commentId) => deleteCommentMutation.mutate(commentId) : undefined}
          />
        </Card>
      </div>
    </section>
  )
}

export default PostDetailPage
