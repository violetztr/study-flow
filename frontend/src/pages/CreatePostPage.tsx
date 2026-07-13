import { ArrowLeftOutlined } from '@ant-design/icons'
import { Alert, Button, Progress, message } from 'antd'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { communityApi } from '../api/community'
import { mediaApi } from '../api/media'
import PostComposer, { type CommunityPostFormValues } from '../components/community/PostComposer'

async function uploadMediaFile(file: File, onStatus?: (status: string) => void) {
  onStatus?.(`准备上传：${file.name}`)
  const prepareResponse = await mediaApi.prepareUpload({
    filename: file.name,
    contentType: file.type || 'application/octet-stream',
    fileSize: file.size,
  })

  onStatus?.(`正在上传：${file.name}`)
  await mediaApi.uploadToSignedUrl(prepareResponse.uploadUrl, file, prepareResponse.headers)

  onStatus?.(`正在确认：${file.name}`)
  const completeResponse = await mediaApi.completeUpload(prepareResponse.mediaFileId)
  return completeResponse.id
}

function CreatePostPage() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const queryClient = useQueryClient()
  const [messageApi, contextHolder] = message.useMessage()
  const [uploadStatus, setUploadStatus] = useState<string | null>(null)
  const requestedCollectionId = Number(searchParams.get('collectionId'))
  const initialCollectionId = Number.isFinite(requestedCollectionId) && requestedCollectionId > 0
    ? requestedCollectionId
    : null

  const collectionsQuery = useQuery({
    queryKey: ['community-collections-my'],
    queryFn: communityApi.listMyCollections,
  })

  const createMutation = useMutation({
    mutationFn: async (values: CommunityPostFormValues) => {
      const mediaFileIds: number[] = []
      const mediaFiles = values.mediaFiles ?? []
      const totalUploadCount = mediaFiles.length + (values.videoCoverFile ? 1 : 0)

      for (const [index, file] of mediaFiles.entries()) {
        setUploadStatus(`上传文件 ${index + 1}/${totalUploadCount}`)
        mediaFileIds.push(await uploadMediaFile(file, setUploadStatus))
      }

      const videoCoverMediaFileId = values.videoCoverFile
        ? await uploadMediaFile(values.videoCoverFile, setUploadStatus)
        : null

      setUploadStatus(values.mode === 'video' ? '正在提交审核' : '正在发布图文')
      return communityApi.createPost({
        title: values.title,
        content: values.content,
        topicId: null,
        topicName: values.topicName ?? null,
        collectionEnabled: values.collectionEnabled ?? false,
        collectionId: values.collectionId ?? null,
        collectionTitle: values.collectionTitle ?? null,
        collectionDescription: values.collectionDescription ?? null,
        videoCoverMediaFileId,
        mediaFileIds,
      })
    },
    onSuccess: (post) => {
      queryClient.invalidateQueries({ queryKey: ['community-feed'] })
      queryClient.invalidateQueries({ queryKey: ['community-submissions-my'] })
      queryClient.invalidateQueries({ queryKey: ['community-collections-my'] })
      if (post.status === 'PENDING_REVIEW') {
        void messageApi.success('视频已提交审核，可以在我的稿件里查看状态')
        navigate('/circle/submissions')
        return
      }
      navigate(`/circle/posts/${post.id}`)
    },
    onSettled: () => {
      setUploadStatus(null)
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
          {uploadStatus ? (
            <div className="composer-upload-status">
              <span>{uploadStatus}</span>
              <Progress percent={createMutation.isPending ? 68 : 100} showInfo={false} size="small" />
            </div>
          ) : null}
          <PostComposer
            collections={collectionsQuery.data ?? []}
            initialCollectionId={initialCollectionId}
            loading={createMutation.isPending}
            onSubmit={handleSubmit}
          />
        </div>
      </div>
    </section>
  )
}

export default CreatePostPage
