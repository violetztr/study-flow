import { InboxOutlined } from '@ant-design/icons'
import { Button, Form, Input, Select, Upload } from 'antd'
import { useState } from 'react'
import type { UploadFile } from 'antd'
import type { CommunityPostRequest, CommunityTopicResponse } from '../../api/community'

export type CommunityPostFormValues = CommunityPostRequest & {
  imageFiles?: File[]
}

type PostComposerProps = {
  topics: CommunityTopicResponse[]
  loading?: boolean
  initialValues?: Partial<CommunityPostRequest>
  onSubmit: (values: CommunityPostFormValues) => void
}

function PostComposer({ topics, loading, initialValues, onSubmit }: PostComposerProps) {
  const [fileList, setFileList] = useState<UploadFile[]>([])

  return (
    <Form<CommunityPostRequest>
      layout="vertical"
      requiredMark={false}
      initialValues={initialValues}
      onFinish={(values) => {
        const imageFiles = fileList
          .map((file) => file.originFileObj)
          .filter((file): file is NonNullable<UploadFile['originFileObj']> => Boolean(file))
        onSubmit({
          ...values,
          topicId: values.topicId ?? null,
          imageFiles,
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

      <Form.Item label="图片">
        <Upload.Dragger
          accept="image/jpeg,image/png,image/webp,image/gif"
          beforeUpload={() => false}
          disabled={loading}
          fileList={fileList}
          listType="picture"
          maxCount={9}
          multiple
          onChange={({ fileList: nextFileList }) => setFileList(nextFileList.slice(0, 9))}
        >
          <p className="ant-upload-drag-icon">
            <InboxOutlined />
          </p>
          <p className="ant-upload-text">选择图片，发布时会直接上传到 R2 对象存储</p>
          <p className="ant-upload-hint">支持 JPG、PNG、WebP、GIF，最多 9 张，单张不超过 10MB。</p>
        </Upload.Dragger>
      </Form.Item>

      <Button type="primary" htmlType="submit" loading={loading}>
        {loading ? '上传并发布中' : '发布动态'}
      </Button>
    </Form>
  )
}

export default PostComposer
