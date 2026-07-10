import { InboxOutlined } from '@ant-design/icons'
import { Button, Form, Input, Select, Upload } from 'antd'
import { useState } from 'react'
import type { UploadFile } from 'antd'
import type { CommunityPostRequest, CommunityTopicResponse } from '../../api/community'

export type CommunityPostFormValues = CommunityPostRequest & {
  mediaFiles?: File[]
}

type PostComposerProps = {
  topics: CommunityTopicResponse[]
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

function PostComposer({ topics, loading, initialValues, onSubmit }: PostComposerProps) {
  const [fileList, setFileList] = useState<UploadFile[]>([])

  return (
    <Form<CommunityPostRequest>
      layout="vertical"
      requiredMark={false}
      initialValues={initialValues}
      onFinish={(values) => {
        const mediaFiles = fileList
          .map((file) => file.originFileObj)
          .filter((file): file is NonNullable<UploadFile['originFileObj']> => Boolean(file))
        onSubmit({
          ...values,
          topicId: values.topicId ?? null,
          mediaFiles,
        })
      }}
    >
      <Form.Item
        label="标题"
        name="title"
        rules={[
          { required: true, message: '请输入标题' },
          { max: 160, message: '标题不能超过 160 个字符' },
        ]}
      >
        <Input size="large" placeholder="分享一件事、一个问题或一个小发现" />
      </Form.Item>

      <Form.Item label="话题" name="topicId">
        <Select
          allowClear
          placeholder="选择一个话题"
          options={topics.map((topic) => ({
            label: topic.name,
            value: topic.id,
          }))}
        />
      </Form.Item>

      <Form.Item
        label="内容"
        name="content"
        rules={[
          { required: true, message: '请输入内容' },
          { max: 10000, message: '内容不能超过 10000 个字符' },
        ]}
      >
        <Input.TextArea rows={10} placeholder="写下上下文、想法、问题或你想分享的东西..." />
      </Form.Item>

      <Form.Item label="媒体">
        <Upload.Dragger
          accept="image/jpeg,image/png,image/webp,image/gif,video/mp4,video/webm"
          beforeUpload={() => false}
          disabled={loading}
          fileList={fileList}
          listType="picture"
          maxCount={9}
          multiple
          onChange={({ fileList: nextFileList }) => setFileList(limitMediaFiles(nextFileList))}
        >
          <p className="ant-upload-drag-icon">
            <InboxOutlined />
          </p>
          <p className="ant-upload-text">选择图片或视频</p>
          <p className="ant-upload-hint">图片最多 10MB，视频最多 50MB；视频发布后需要 ruru 审核。</p>
        </Upload.Dragger>
      </Form.Item>

      <Button type="primary" htmlType="submit" loading={loading}>
        {loading ? '上传并发布中' : '发布动态'}
      </Button>
    </Form>
  )
}

export default PostComposer
