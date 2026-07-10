import { SafetyOutlined } from '@ant-design/icons'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Alert, Button, Card, Form, Input, InputNumber, Select, Space, Typography } from 'antd'
import { useState } from 'react'
import { Navigate } from 'react-router-dom'
import { getStoredUser } from '../api/auth'
import { communityApi, type ModerationRequest } from '../api/community'

type AdminAction =
  | 'hide-post'
  | 'restore-post'
  | 'hide-comment'
  | 'restore-comment'
  | 'mute-member'
  | 'unmute-member'

type AdminFormValues = {
  targetId: number
  action: AdminAction
  reason?: string
}

const actionLabels: Record<AdminAction, string> = {
  'hide-post': '隐藏帖子',
  'restore-post': '恢复帖子',
  'hide-comment': '隐藏评论',
  'restore-comment': '恢复评论',
  'mute-member': '禁言成员',
  'unmute-member': '解除禁言',
}

function isCommunityAdmin(role?: string) {
  return role === 'ADMIN' || role === 'OWNER'
}

function buildModerationRequest(reason?: string): ModerationRequest {
  const trimmedReason = reason?.trim()
  return trimmedReason ? { reason: trimmedReason } : {}
}

function runModerationAction(values: AdminFormValues) {
  const request = buildModerationRequest(values.reason)

  switch (values.action) {
    case 'hide-post':
      return communityApi.hidePost(values.targetId, request)
    case 'restore-post':
      return communityApi.restorePost(values.targetId, request)
    case 'hide-comment':
      return communityApi.hideComment(values.targetId, request)
    case 'restore-comment':
      return communityApi.restoreComment(values.targetId, request)
    case 'mute-member':
      return communityApi.muteMember(values.targetId, request)
    case 'unmute-member':
      return communityApi.unmuteMember(values.targetId, request)
  }
}

function CommunityAdminPage() {
  const [form] = Form.useForm<AdminFormValues>()
  const [successText, setSuccessText] = useState<string | null>(null)
  const queryClient = useQueryClient()
  const user = getStoredUser()

  const moderationMutation = useMutation({
    mutationFn: runModerationAction,
    onSuccess: async (_, values) => {
      setSuccessText(`${actionLabels[values.action]}成功，目标 ID：${values.targetId}`)
      form.resetFields()
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['community-feed'] }),
        queryClient.invalidateQueries({ queryKey: ['community-members'] }),
      ])
    },
    onError: () => {
      setSuccessText(null)
    },
  })

  if (!isCommunityAdmin(user?.role)) {
    return <Navigate to="/circle" replace />
  }

  return (
    <section className="page-section">
      <section className="dashboard-header">
        <div>
          <p className="dashboard-kicker">Moderation</p>
          <h1 className="dashboard-title">社区管理</h1>
          <p className="dashboard-subtitle">
            这里是 Ruru 社区的基础治理入口。输入帖子、评论或成员 ID 后，系统会直接调用后端管理接口并记录原因。
          </p>
        </div>
        <SafetyOutlined style={{ color: 'var(--sf-primary)', fontSize: 44 }} />
      </section>

      <Card className="profile-card" title="执行管理动作">
        <Space direction="vertical" size={16} style={{ width: '100%' }}>
          <Alert
            showIcon
            type="warning"
            message="这些操作会真实生效"
            description="隐藏帖子/评论会让普通成员看不到内容；禁言成员会让对方只能阅读，不能发帖、评论、点赞或改资料。请先确认目标 ID。"
          />

          {successText ? <Alert showIcon type="success" message={successText} /> : null}
          {moderationMutation.isError ? (
            <Alert showIcon type="error" message={moderationMutation.error.message} />
          ) : null}

          <Form<AdminFormValues>
            form={form}
            layout="vertical"
            requiredMark={false}
            onFinish={(values) => moderationMutation.mutate(values)}
          >
            <Form.Item
              label="目标 ID"
              name="targetId"
              rules={[{ required: true, message: '请输入帖子、评论或成员 ID' }]}
            >
              <InputNumber min={1} precision={0} style={{ width: '100%' }} placeholder="例如 12" />
            </Form.Item>

            <Form.Item
              label="动作类型"
              name="action"
              rules={[{ required: true, message: '请选择要执行的动作' }]}
            >
              <Select
                placeholder="选择一个管理动作"
                options={[
                  { label: actionLabels['hide-post'], value: 'hide-post' },
                  { label: actionLabels['restore-post'], value: 'restore-post' },
                  { label: actionLabels['hide-comment'], value: 'hide-comment' },
                  { label: actionLabels['restore-comment'], value: 'restore-comment' },
                  { label: actionLabels['mute-member'], value: 'mute-member' },
                  { label: actionLabels['unmute-member'], value: 'unmute-member' },
                ]}
              />
            </Form.Item>

            <Form.Item label="原因" name="reason">
              <Input.TextArea rows={4} placeholder="建议写清楚上下文，方便以后复盘治理决定。" />
            </Form.Item>

            <Button type="primary" htmlType="submit" loading={moderationMutation.isPending}>
              确认执行
            </Button>
          </Form>

          <Typography.Text type="secondary">
            后端会阻止普通成员操作，也会阻止管理员禁言自己、禁言 owner 或禁言同级管理员。
          </Typography.Text>
        </Space>
      </Card>
    </section>
  )
}

export default CommunityAdminPage
