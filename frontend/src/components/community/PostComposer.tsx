import { PaperClipOutlined, PictureOutlined } from '@ant-design/icons'
import { Button, Form, Input, Upload } from 'antd'
import { useState } from 'react'
import type { UploadFile } from 'antd'
import type { CommunityPostRequest } from '../../api/community'

export type CommunityPostFormValues = CommunityPostRequest & {
  mediaFiles?: File[]
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

function PostComposer({ loading, initialValues, onSubmit }: PostComposerProps) {
  const [fileList, setFileList] = useState<UploadFile[]>([])

  return (
    <Form<CommunityPostRequest>
      className="composer-form"
      layout="vertical"
      requiredMark={false}
      initialValues={initialValues}
      onFinish={(values) => {
        const mediaFiles = fileList
          .map((file) => file.originFileObj)
          .filter((file): file is NonNullable<UploadFile['originFileObj']> => Boolean(file))
        onSubmit({
          ...values,
          topicId: null,
          topicName: normalizeTopicName(values.topicName),
          mediaFiles,
        })
      }}
    >
      <div className="composer-head">
        <p>发布动态</p>
        <span>写给朋友看，不用太正式。</span>
      </div>

      <Form.Item
        name="title"
        rules={[
          { required: true, message: '请输入标题' },
          { max: 160, message: '标题不能超过 160 个字符' },
        ]}
      >
        <Input bordered={false} className="composer-title-input" placeholder="这一条想说什么？" />
      </Form.Item>

      <Form.Item
        name="content"
        rules={[
          { required: true, message: '请输入内容' },
          { max: 10000, message: '内容不能超过 10000 个字符' },
        ]}
      >
        <Input.TextArea
          bordered={false}
          className="composer-content-input"
          placeholder="写下想法、问题、记录，或者今天的小发现..."
          autoSize={{ minRows: 8, maxRows: 18 }}
        />
      </Form.Item>

      <div className="composer-tools">
        <Form.Item
          name="topicName"
          className="composer-topic-item manual-topic-item"
          rules={[{ max: 80, message: '话题不能超过 80 个字符' }]}
        >
          <Input allowClear className="composer-topic-input" placeholder="输入话题，比如 apex、日常、求助" />
        </Form.Item>

        <Upload
          accept="image/jpeg,image/png,image/webp,image/gif,video/mp4,video/webm"
          beforeUpload={() => false}
          disabled={loading}
          fileList={fileList}
          maxCount={9}
          multiple
          onChange={({ fileList: nextFileList }) => setFileList(limitMediaFiles(nextFileList))}
        >
          <Button icon={<PaperClipOutlined />} className="composer-media-button">
            添加图片/视频
          </Button>
        </Upload>
      </div>

      {fileList.length > 0 ? (
        <p className="composer-media-note">
          <PictureOutlined /> 已选择 {fileList.length} 个文件。图片最大 10MB，视频最大 50MB，视频发布后需要 ruru 审核。
        </p>
      ) : (
        <p className="composer-media-note muted">
          可附加图片或 1 个视频。视频审核通过前，其他人暂时看不到。
        </p>
      )}

      <div className="composer-actions">
        <Button type="primary" htmlType="submit" loading={loading} className="composer-submit">
          {loading ? '发布中' : '发布'}
        </Button>
      </div>
    </Form>
  )
}

export default PostComposer
