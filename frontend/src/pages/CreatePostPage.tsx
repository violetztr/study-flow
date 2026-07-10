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
          <h1>发布社区动态</h1>
          <p>写清楚你想说的事、想问的问题，或者今天想分享的小发现。基础社区先从真实交流开始。</p>
        </div>
        <Button icon={<ArrowLeftOutlined />}>
          <Link to="/circle">返回社区</Link>
        </Button>
      </div>

      <Card className="profile-card">
        <Space direction="vertical" size={18} style={{ width: '100%' }}>
          {topicsQuery.isError ? <Alert showIcon type="error" message={topicsQuery.error.message} /> : null}
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
