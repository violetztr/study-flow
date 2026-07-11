import { ArrowLeftOutlined } from '@ant-design/icons'
import { Alert, Button, message } from 'antd'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { communityApi } from '../api/community'
import { mediaApi } from '../api/media'
import PostComposer, { type CommunityPostFormValues } from '../components/community/PostComposer'

async function uploadMediaFile(file: File) {
  const prepareResponse = await mediaApi.prepareUpload({
    filename: file.name,
    contentType: file.type || 'application/octet-stream',
    fileSize: file.size,
  })
  await mediaApi.uploadToSignedUrl(prepareResponse.uploadUrl, file, prepareResponse.headers)
  const completeResponse = await mediaApi.completeUpload(prepareResponse.mediaFileId)
  return completeResponse.id
}

function CreatePostPage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [messageApi, contextHolder] = message.useMessage()

  const createMutation = useMutation({
    mutationFn: async (values: CommunityPostFormValues) => {
      const mediaFileIds: number[] = []
      for (const file of values.mediaFiles ?? []) {
        mediaFileIds.push(await uploadMediaFile(file))
      }

      const videoCoverMediaFileId = values.videoCoverFile
        ? await uploadMediaFile(values.videoCoverFile)
        : null

      return communityApi.createPost({
        title: values.title,
        content: values.content,
        topicId: null,
        topicName: values.topicName ?? null,
        videoCoverMediaFileId,
        mediaFileIds,
      })
    },
    onSuccess: (post) => {
      queryClient.invalidateQueries({ queryKey: ['community-feed'] })
      queryClient.invalidateQueries({ queryKey: ['community-submissions-my'] })
      if (post.status === 'PENDING_REVIEW') {
        void messageApi.success('视频已提交审核，通过后会出现在首页')
        navigate('/circle')
        return
      }
      navigate(`/circle/posts/${post.id}`)
    },
  })

  function handleSubmit(values: CommunityPostFormValues) {
    createMutation.mutate(values)
  }

  return (
    <section className="page-section compose-page">
      {contextHolder}
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
