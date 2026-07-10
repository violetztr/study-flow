import { ArrowLeftOutlined } from '@ant-design/icons'
import { Alert, Button, Card, Skeleton, Space } from 'antd'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link, useNavigate } from 'react-router-dom'
import { type CommunityPostRequest, communityApi } from '../api/community'
import PostComposer from '../components/community/PostComposer'

function CreatePostPage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()

  const topicsQuery = useQuery({
    queryKey: ['community-topics'],
    queryFn: communityApi.listTopics,
  })

  const createMutation = useMutation({
    mutationFn: communityApi.createPost,
    onSuccess: (post) => {
      queryClient.invalidateQueries({ queryKey: ['community-feed'] })
      navigate(`/circle/posts/${post.id}`)
    },
  })

  function handleSubmit(values: CommunityPostRequest) {
    createMutation.mutate(values)
  }

  return (
    <section className="page-section">
      <div className="page-heading">
        <div>
          <p className="dashboard-kicker">New post</p>
          <h1>发布 Violet Circle 动态</h1>
          <p>把问题背景、当前进展和下一步想法写清楚，圈子成员更容易给到有效反馈。</p>
        </div>
        <Button icon={<ArrowLeftOutlined />}>
          <Link to="/circle">返回圈子</Link>
        </Button>
      </div>

      <Card className="profile-card">
        <Space direction="vertical" size={18} style={{ width: '100%' }}>
          {topicsQuery.isError ? (
            <Alert showIcon type="error" message={topicsQuery.error.message} />
          ) : null}
          {createMutation.isError ? (
            <Alert showIcon type="error" message={createMutation.error.message} />
          ) : null}
          {topicsQuery.isLoading ? (
            <Skeleton active />
          ) : (
            <PostComposer
              topics={topicsQuery.data ?? []}
              loading={createMutation.isPending}
              onSubmit={handleSubmit}
            />
          )}
        </Space>
      </Card>
    </section>
  )
}

export default CreatePostPage
