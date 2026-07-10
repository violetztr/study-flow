import { Button, Form, Input, Select } from 'antd'
import type { CommunityPostRequest, CommunityTopicResponse } from '../../api/community'

type PostComposerProps = {
  topics: CommunityTopicResponse[]
  loading?: boolean
  initialValues?: Partial<CommunityPostRequest>
  onSubmit: (values: CommunityPostRequest) => void
}

function PostComposer({ topics, loading, initialValues, onSubmit }: PostComposerProps) {
  return (
    <Form<CommunityPostRequest>
      layout="vertical"
      requiredMark={false}
      initialValues={initialValues}
      onFinish={(values) => onSubmit({ ...values, topicId: values.topicId ?? null })}
    >
      <Form.Item
        label="标题"
        name="title"
        rules={[
          { required: true, message: '请输入标题' },
          { max: 160, message: '标题不能超过 160 个字符' },
        ]}
      >
        <Input size="large" placeholder="分享一个问题、进展或复盘" />
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
        <Input.TextArea rows={10} placeholder="写下你的上下文、尝试过的方法、下一步计划..." />
      </Form.Item>

      <Button type="primary" htmlType="submit" loading={loading}>
        发布动态
      </Button>
    </Form>
  )
}

export default PostComposer
