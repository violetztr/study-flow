import {
  FileImageOutlined,
  InboxOutlined,
  LoadingOutlined,
  PaperClipOutlined,
  PictureOutlined,
  VideoCameraOutlined,
} from '@ant-design/icons'
import { Button, Form, Input, Upload, message } from 'antd'
import { useEffect, useState } from 'react'
import type { UploadFile } from 'antd'
import type { CommunityPostRequest } from '../../api/community'

export type PostComposerMode = 'article' | 'video'

export type CommunityPostFormValues = CommunityPostRequest & {
  mode: PostComposerMode
  mediaFiles?: File[]
  videoCoverFile?: File | null
}

type PostComposerProps = {
  loading?: boolean
  initialValues?: Partial<CommunityPostRequest>
  onSubmit: (values: CommunityPostFormValues) => void
}

const IMAGE_SIZE_LIMIT = 10 * 1024 * 1024
const VIDEO_SIZE_LIMIT = 200 * 1024 * 1024

const modeOptions: Array<{
  key: PostComposerMode
  title: string
  description: string
  icon: React.ReactNode
}> = [
  {
    key: 'video',
    title: '视频投稿',
    description: '上传视频、封面和简介，提交后进入审核。',
    icon: <VideoCameraOutlined />,
  },
  {
    key: 'article',
    title: '图文发布',
    description: '发布文字和图片，适合日常动态和小发现。',
    icon: <FileImageOutlined />,
  },
]

function normalizeTopicName(topicName?: string | null) {
  const trimmedTopicName = topicName?.trim()
  return trimmedTopicName ? trimmedTopicName : null
}

function isVideoUpload(file: UploadFile) {
  const contentType = file.type || file.originFileObj?.type || ''
  return contentType.startsWith('video/')
}

function isImageUpload(file: UploadFile) {
  const contentType = file.type || file.originFileObj?.type || ''
  return contentType.startsWith('image/')
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

function getUploadFiles(fileList: UploadFile[]) {
  return fileList
    .map((file) => file.originFileObj)
    .filter((file): file is NonNullable<UploadFile['originFileObj']> => Boolean(file))
}

function PostComposer({ loading, initialValues, onSubmit }: PostComposerProps) {
  const [messageApi, contextHolder] = message.useMessage()
  const [mode, setMode] = useState<PostComposerMode>('video')
  const [fileList, setFileList] = useState<UploadFile[]>([])
  const [videoFileUid, setVideoFileUid] = useState<string | null>(null)
  const [videoCoverFile, setVideoCoverFile] = useState<File | null>(null)
  const [coverPreviewUrl, setCoverPreviewUrl] = useState<string | null>(null)
  const [coverGenerating, setCoverGenerating] = useState(false)
  const [coverSource, setCoverSource] = useState<'auto' | 'manual' | null>(null)

  const isVideoMode = mode === 'video'
  const currentMode = modeOptions.find((item) => item.key === mode) ?? modeOptions[0]

  useEffect(() => {
    if (!videoCoverFile) {
      setCoverPreviewUrl(null)
      return
    }

    const nextPreviewUrl = URL.createObjectURL(videoCoverFile)
    setCoverPreviewUrl(nextPreviewUrl)

    return () => URL.revokeObjectURL(nextPreviewUrl)
  }, [videoCoverFile])

  function resetMediaState() {
    setFileList([])
    setVideoFileUid(null)
    setVideoCoverFile(null)
    setCoverSource(null)
    setCoverGenerating(false)
  }

  function handleModeChange(nextMode: PostComposerMode) {
    if (nextMode === mode) {
      return
    }
    setMode(nextMode)
    resetMediaState()
  }

  function validateMediaBeforeUpload(file: File) {
    if (mode === 'article') {
      if (!file.type.startsWith('image/')) {
        void messageApi.warning('图文发布只能上传图片')
        return Upload.LIST_IGNORE
      }
      if (file.size > IMAGE_SIZE_LIMIT) {
        void messageApi.warning('单张图片不能超过 10MB')
        return Upload.LIST_IGNORE
      }
      return false
    }

    if (!file.type.startsWith('video/')) {
      void messageApi.warning('视频投稿只能上传视频文件')
      return Upload.LIST_IGNORE
    }
    if (file.size > VIDEO_SIZE_LIMIT) {
      void messageApi.warning('视频不能超过 200MB')
      return Upload.LIST_IGNORE
    }
    if (fileList.some(isVideoUpload)) {
      void messageApi.warning('一次投稿先上传一个视频')
      return Upload.LIST_IGNORE
    }
    return false
  }

  async function handleMediaChange(nextFileList: UploadFile[]) {
    const nextList = isVideoMode
      ? nextFileList.filter(isVideoUpload).slice(-1)
      : nextFileList.filter(isImageUpload).slice(0, 9)

    setFileList(nextList)

    if (!isVideoMode) {
      setVideoFileUid(null)
      setVideoCoverFile(null)
      setCoverSource(null)
      return
    }

    const nextVideoFile = nextList.find(isVideoUpload)
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
      return Upload.LIST_IGNORE
    }
    if (file.size > IMAGE_SIZE_LIMIT) {
      void messageApi.warning('封面不能超过 10MB')
      return Upload.LIST_IGNORE
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
          const mediaFiles = getUploadFiles(fileList)

          if (mode === 'video') {
            const videoFile = mediaFiles.find((file) => file.type.startsWith('video/'))
            if (!videoFile) {
              void messageApi.warning('请先上传视频')
              return
            }
            if (!videoCoverFile) {
              void messageApi.warning('视频需要封面')
              return
            }
          }

          onSubmit({
            ...values,
            mode,
            topicId: null,
            topicName: normalizeTopicName(values.topicName),
            mediaFiles,
            videoCoverFile: mode === 'video' ? videoCoverFile : null,
          })
        }}
      >
        <div className="composer-mode-switch" aria-label="投稿类型">
          {modeOptions.map((option) => (
            <button
              key={option.key}
              type="button"
              className={mode === option.key ? 'composer-mode-card active' : 'composer-mode-card'}
              onClick={() => handleModeChange(option.key)}
            >
              <span className="composer-mode-icon">{option.icon}</span>
              <span>
                <strong>{option.title}</strong>
                <em>{option.description}</em>
              </span>
            </button>
          ))}
        </div>

        <div className="composer-head">
          <p>{currentMode.title}</p>
          <span>{isVideoMode ? '视频会先进入审核，通过后再公开。' : '图文会直接出现在社区里。'}</span>
        </div>

        <Form.Item
          name="title"
          rules={[
            { required: true, message: '请输入标题' },
            { max: 160, message: '标题不能超过 160 个字' },
          ]}
        >
          <Input
            bordered={false}
            className="composer-title-input"
            placeholder={isVideoMode ? '给视频起个标题' : '标题'}
          />
        </Form.Item>

        <Form.Item
          name="content"
          rules={[
            { required: true, message: isVideoMode ? '请输入简介' : '请输入内容' },
            { max: 10000, message: '内容不能超过 10000 个字' },
          ]}
        >
          <Input.TextArea
            bordered={false}
            className="composer-content-input"
            placeholder={isVideoMode ? '写一段简介，让大家知道这个视频讲什么。' : '说点什么'}
            autoSize={{ minRows: isVideoMode ? 4 : 8, maxRows: 18 }}
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
        </div>

        <Upload.Dragger
          accept={isVideoMode ? 'video/mp4,video/webm,video/quicktime' : 'image/jpeg,image/png,image/webp,image/gif'}
          beforeUpload={validateMediaBeforeUpload}
          disabled={loading}
          fileList={fileList}
          maxCount={isVideoMode ? 1 : 9}
          multiple={!isVideoMode}
          onChange={({ fileList: nextFileList }) => {
            void handleMediaChange(nextFileList)
          }}
          className="composer-upload-zone"
        >
          <p className="ant-upload-drag-icon">{isVideoMode ? <InboxOutlined /> : <PaperClipOutlined />}</p>
          <p className="ant-upload-text">{isVideoMode ? '拖入视频，或点击选择视频' : '拖入图片，或点击选择图片'}</p>
          <p className="ant-upload-hint">
            {isVideoMode ? '支持 MP4 / WebM / MOV，单个视频 200MB 内。' : '最多 9 张图片，单张 10MB 内。'}
          </p>
        </Upload.Dragger>

        {fileList.length > 0 ? (
          <p className="composer-media-note">
            <PictureOutlined /> 已选择 {fileList.length} 个文件
          </p>
        ) : null}

        {isVideoMode ? (
          <div className="composer-cover-box">
            <div className="composer-cover-copy">
              <strong>视频封面</strong>
              <span>
                {coverGenerating
                  ? '正在从视频中截取封面'
                  : coverSource === 'manual'
                    ? '已使用手动封面'
                    : coverSource === 'auto'
                      ? '已自动截取，可手动更换'
                      : '上传视频后自动截取，也可以手动上传'}
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
            {loading ? '提交中' : isVideoMode ? '提交审核' : '发布图文'}
          </Button>
        </div>
      </Form>
    </>
  )
}

export default PostComposer
