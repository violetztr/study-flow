import { ArrowLeftOutlined } from '@ant-design/icons'
import { Alert, Button } from 'antd'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { communityApi } from '../api/community'
import { mediaApi } from '../api/media'
import PostComposer, { type CommunityPostFormValues } from '../components/community/PostComposer'

function CreatePostPage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()

  const createMutation = useMutation({
    mutationFn: async (values: CommunityPostFormValues) => {
      const mediaFileIds: number[] = []
      for (const file of values.mediaFiles ?? []) {
        const prepareResponse = await mediaApi.prepareUpload({
          filename: file.name,
          contentType: file.type || 'application/octet-stream',
          fileSize: file.size,
        })
        await mediaApi.uploadToSignedUrl(prepareResponse.uploadUrl, file, prepareResponse.headers)
        const completeResponse = await mediaApi.completeUpload(prepareResponse.mediaFileId)
        mediaFileIds.push(completeResponse.id)
      }

      return communityApi.createPost({
        title: values.title,
        content: values.content,
        topicId: null,
        topicName: values.topicName ?? null,
        mediaFileIds,
      })
    },
    onSuccess: (post) => {
      queryClient.invalidateQueries({ queryKey: ['community-feed'] })
      navigate(`/circle/posts/${post.id}`)
    },
  })

  function handleSubmit(values: CommunityPostFormValues) {
    createMutation.mutate(values)
  }

  return (
    <section className="page-section compose-page">
      <div className="compose-shell">
        <Button
          type="text"
          icon={<ArrowLeftOutlined />}
          className="ghost-link-button"
          onClick={() => navigate('/circle')}
        >
          返回
        </Button>

        <div className="compose-panel">
          {createMutation.isError ? (
            <Alert showIcon type="error" message={createMutation.error.message} />
          ) : null}
          <PostComposer loading={createMutation.isPending} onSubmit={handleSubmit} />
        </div>
      </div>
    </section>
  )
}

export default CreatePostPage
