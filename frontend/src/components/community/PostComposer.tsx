import { LoadingOutlined, PaperClipOutlined, PictureOutlined } from '@ant-design/icons'
import { Button, Form, Input, Upload, message } from 'antd'
import { useEffect, useState } from 'react'
import type { UploadFile } from 'antd'
import type { CommunityPostRequest } from '../../api/community'

export type CommunityPostFormValues = CommunityPostRequest & {
  mediaFiles?: File[]
  videoCoverFile?: File | null
}

type PostComposerProps = {
  loading?: boolean
  initialValues?: Partial<CommunityPostRequest>
  onSubmit: (values: CommunityPostFormValues) => void
}

function limitMediaFiles(nextFileList: UploadFile[]) {
  let videoCount = 0

  return nextFileList
    .filter((file) => {
      const contentType = file.type || file.originFileObj?.type || ''
      if (!contentType.startsWith('video/')) {
        return true
      }
      videoCount += 1
      return videoCount <= 1
    })
    .slice(0, 9)
}

function normalizeTopicName(topicName?: string | null) {
  const trimmedTopicName = topicName?.trim()
  return trimmedTopicName ? trimmedTopicName : null
}

function isVideoUpload(file: UploadFile) {
  const contentType = file.type || file.originFileObj?.type || ''
  return contentType.startsWith('video/')
}

function basename(filename: string) {
  const dotIndex = filename.lastIndexOf('.')
  return dotIndex > 0 ? filename.slice(0, dotIndex) : filename
}

function waitForVideoEvent(
  video: HTMLVideoElement,
  eventName: keyof HTMLMediaElementEventMap,
  timeoutMs = 5000,
) {
  return new Promise<void>((resolve, reject) => {
    let timeoutId = 0
    let cleanup = () => {}
    const onEvent = () => {
      cleanup()
      resolve()
    }
    const onError = () => {
      cleanup()
      reject(new Error('视频封面生成失败'))
    }
    cleanup = () => {
      window.clearTimeout(timeoutId)
      video.removeEventListener(eventName, onEvent)
      video.removeEventListener('error', onError)
    }

    video.addEventListener(eventName, onEvent, { once: true })
    video.addEventListener('error', onError, { once: true })
    timeoutId = window.setTimeout(() => {
      cleanup()
      resolve()
    }, timeoutMs)
  })
}

function canvasToJpegFile(canvas: HTMLCanvasElement, filename: string) {
  return new Promise<File>((resolve, reject) => {
    canvas.toBlob(
      (blob) => {
        if (!blob) {
          reject(new Error('视频封面生成失败'))
          return
        }
        resolve(new File([blob], `${basename(filename)}-cover.jpg`, { type: 'image/jpeg' }))
      },
      'image/jpeg',
      0.86,
    )
  })
}

async function captureVideoCover(videoFile: File) {
  const objectUrl = URL.createObjectURL(videoFile)
  const video = document.createElement('video')

  try {
    video.muted = true
    video.preload = 'metadata'
    video.playsInline = true
    video.src = objectUrl
    video.load()

    await waitForVideoEvent(video, 'loadedmetadata')

    if (Number.isFinite(video.duration) && video.duration > 0.3) {
      video.currentTime = Math.min(1, video.duration * 0.08)
      await waitForVideoEvent(video, 'seeked')
    } else if (video.readyState < 2) {
      await waitForVideoEvent(video, 'loadeddata')
    }

    const width = video.videoWidth || 1280
    const height = video.videoHeight || 720
    const canvas = document.createElement('canvas')
    canvas.width = width
    canvas.height = height
    canvas.getContext('2d')?.drawImage(video, 0, 0, width, height)

    return canvasToJpegFile(canvas, videoFile.name)
  } finally {
    URL.revokeObjectURL(objectUrl)
  }
}

function PostComposer({ loading, initialValues, onSubmit }: PostComposerProps) {
  const [messageApi, contextHolder] = message.useMessage()
  const [fileList, setFileList] = useState<UploadFile[]>([])
  const [videoFileUid, setVideoFileUid] = useState<string | null>(null)
  const [videoCoverFile, setVideoCoverFile] = useState<File | null>(null)
  const [coverPreviewUrl, setCoverPreviewUrl] = useState<string | null>(null)
  const [coverGenerating, setCoverGenerating] = useState(false)
  const [coverSource, setCoverSource] = useState<'auto' | 'manual' | null>(null)

  const hasVideo = fileList.some(isVideoUpload)

  useEffect(() => {
    if (!videoCoverFile) {
      setCoverPreviewUrl(null)
      return
    }

    const nextPreviewUrl = URL.createObjectURL(videoCoverFile)
    setCoverPreviewUrl(nextPreviewUrl)

    return () => URL.revokeObjectURL(nextPreviewUrl)
  }, [videoCoverFile])

  async function handleMediaChange(nextFileList: UploadFile[]) {
    const limitedFileList = limitMediaFiles(nextFileList)
    setFileList(limitedFileList)

    const nextVideoFile = limitedFileList.find(isVideoUpload)
    const nextVideo = nextVideoFile?.originFileObj
    if (!nextVideoFile || !nextVideo) {
      setVideoFileUid(null)
      setVideoCoverFile(null)
      setCoverSource(null)
      return
    }

    if (nextVideoFile.uid === videoFileUid && videoCoverFile) {
      return
    }

    setVideoFileUid(nextVideoFile.uid)
    setCoverGenerating(true)
    try {
      const coverFile = await captureVideoCover(nextVideo)
      setVideoCoverFile(coverFile)
      setCoverSource('auto')
    } catch {
      setVideoCoverFile(null)
      setCoverSource(null)
      void messageApi.warning('自动截封面失败，可以手动上传一张封面')
    } finally {
      setCoverGenerating(false)
    }
  }

  function handleManualCover(file: File) {
    if (!file.type.startsWith('image/')) {
      void messageApi.warning('封面只能上传图片')
      return false
    }

    setVideoCoverFile(file)
    setCoverSource('manual')
    return false
  }

  return (
    <>
      {contextHolder}
      <Form<CommunityPostRequest>
        className="composer-form"
        layout="vertical"
        requiredMark={false}
        initialValues={initialValues}
        onFinish={(values) => {
          const mediaFiles = fileList
            .map((file) => file.originFileObj)
            .filter((file): file is NonNullable<UploadFile['originFileObj']> => Boolean(file))

          const containsVideo = mediaFiles.some((file) => file.type.startsWith('video/'))
          if (containsVideo && !videoCoverFile) {
            void messageApi.warning('视频需要封面')
            return
          }

          onSubmit({
            ...values,
            topicId: null,
            topicName: normalizeTopicName(values.topicName),
            mediaFiles,
            videoCoverFile: containsVideo ? videoCoverFile : null,
          })
        }}
      >
        <div className="composer-head">
          <p>发布</p>
        </div>

        <Form.Item
          name="title"
          rules={[
            { required: true, message: '请输入标题' },
            { max: 160, message: '标题不能超过 160 个字' },
          ]}
        >
          <Input bordered={false} className="composer-title-input" placeholder="标题" />
        </Form.Item>

        <Form.Item
          name="content"
          rules={[
            { required: true, message: '请输入内容' },
            { max: 10000, message: '内容不能超过 10000 个字' },
          ]}
        >
          <Input.TextArea
            bordered={false}
            className="composer-content-input"
            placeholder="说点什么"
            autoSize={{ minRows: 8, maxRows: 18 }}
          />
        </Form.Item>

        <div className="composer-tools">
          <Form.Item
            name="topicName"
            className="composer-topic-item manual-topic-item"
            rules={[{ max: 10, message: '话题最多 10 个字' }]}
          >
            <Input allowClear className="composer-topic-input" placeholder="输入话题" />
          </Form.Item>

          <Upload
            accept="image/jpeg,image/png,image/webp,image/gif,video/mp4,video/webm"
            beforeUpload={() => false}
            disabled={loading}
            fileList={fileList}
            maxCount={9}
            multiple
            onChange={({ fileList: nextFileList }) => {
              void handleMediaChange(nextFileList)
            }}
          >
            <Button icon={<PaperClipOutlined />} className="composer-media-button">
              图片 / 视频
            </Button>
          </Upload>
        </div>

        {fileList.length > 0 ? (
          <p className="composer-media-note">
            <PictureOutlined /> 已选 {fileList.length} 个文件。图片 10MB 内，视频 200MB 内。
          </p>
        ) : null}

        {hasVideo ? (
          <div className="composer-cover-box">
            <div className="composer-cover-copy">
              <strong>视频封面</strong>
              <span>
                {coverGenerating
                  ? '正在从视频中截取封面'
                  : coverSource === 'manual'
                    ? '已使用手动封面'
                    : '已自动截取，可手动更换'}
              </span>
            </div>
            <div className="composer-cover-preview">
              {coverPreviewUrl ? (
                <img alt="视频封面预览" src={coverPreviewUrl} />
              ) : (
                <span>{coverGenerating ? <LoadingOutlined /> : '等待封面'}</span>
              )}
            </div>
            <Upload
              accept="image/jpeg,image/png,image/webp,image/gif"
              beforeUpload={handleManualCover}
              disabled={loading}
              maxCount={1}
              showUploadList={false}
            >
              <Button className="composer-media-button">换封面</Button>
            </Upload>
          </div>
        ) : null}

        <div className="composer-actions">
          <Button
            type="primary"
            htmlType="submit"
            loading={loading || coverGenerating}
            className="composer-submit"
          >
            {loading ? '发布中' : '发布'}
          </Button>
        </div>
      </Form>
    </>
  )
}

export default PostComposer
