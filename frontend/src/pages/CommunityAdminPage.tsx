import {
  CheckCircleOutlined,
  CloseCircleOutlined,
  DeleteOutlined,
  SafetyOutlined,
} from '@ant-design/icons'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  Alert,
  Button,
  Card,
  Empty,
  Form,
  Input,
  InputNumber,
  Select,
  Space,
  Tag,
  Typography,
} from 'antd'
import dayjs from 'dayjs'
import { useState } from 'react'
import { Navigate } from 'react-router-dom'
import { getStoredUser } from '../api/auth'
import { communityApi, type CommunityPostResponse, type MediaAttachmentResponse, type ModerationRequest } from '../api/community'

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

function getVideoMedia(post: CommunityPostResponse): MediaAttachmentResponse | undefined {
  return post.media.find((media) => media.fileType === 'VIDEO')
}

function getPreviewMedia(post: CommunityPostResponse): MediaAttachmentResponse | undefined {
  return getVideoMedia(post) ?? post.media[0]
}

function getCoverUrl(post: CommunityPostResponse) {
  const previewMedia = getPreviewMedia(post)
  if (!previewMedia) {
    return null
  }
  return previewMedia.fileType === 'VIDEO' ? previewMedia.coverUrl : previewMedia.url
}

function formatContentType(post: CommunityPostResponse) {
  if (post.contentType === 'VIDEO') {
    return '视频'
  }
  if (post.contentType === 'LIVE') {
    return '直播'
  }
  return '图文'
}

function getShortContent(content: string) {
  return content.length > 120 ? `${content.slice(0, 120)}...` : content
}

function CommunityAdminPage() {
  const [form] = Form.useForm<AdminFormValues>()
  const [successText, setSuccessText] = useState<string | null>(null)
  const [rejectingPostId, setRejectingPostId] = useState<number | null>(null)
  const [rejectReason, setRejectReason] = useState('')
  const queryClient = useQueryClient()
  const user = getStoredUser()
  const canModerateCommunity = isCommunityAdmin(user?.role)

  const pendingSubmissionsQuery = useQuery({
    queryKey: ['admin-pending-submissions'],
    queryFn: communityApi.listPendingSubmissions,
    enabled: canModerateCommunity,
  })

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

  const approveSubmissionMutation = useMutation({
    mutationFn: communityApi.approveSubmission,
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['admin-pending-submissions'] }),
        queryClient.invalidateQueries({ queryKey: ['community-feed'] }),
        queryClient.invalidateQueries({ queryKey: ['community-submissions-my'] }),
      ])
    },
  })

  const rejectSubmissionMutation = useMutation({
    mutationFn: ({ postId, reason }: { postId: number; reason: string }) =>
      communityApi.rejectSubmission(postId, { reason }),
    onSuccess: async () => {
      setRejectingPostId(null)
      setRejectReason('')
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['admin-pending-submissions'] }),
        queryClient.invalidateQueries({ queryKey: ['community-submissions-my'] }),
      ])
    },
  })

  const deleteSubmissionMutation = useMutation({
    mutationFn: communityApi.adminDeletePost,
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['admin-pending-submissions'] }),
        queryClient.invalidateQueries({ queryKey: ['community-feed'] }),
        queryClient.invalidateQueries({ queryKey: ['community-submissions-my'] }),
      ])
    },
  })

  function handleApproveSubmission(postId: number) {
    approveSubmissionMutation.mutate(postId)
  }

  function handleStartReject(postId: number) {
    setRejectingPostId(postId)
    setRejectReason('')
  }

  function handleConfirmReject(postId: number) {
    const trimmedReason = rejectReason.trim()
    if (!trimmedReason) {
      return
    }
    rejectSubmissionMutation.mutate({ postId, reason: trimmedReason })
  }

  function handleDeleteSubmission(postId: number) {
    const confirmed = window.confirm('确认删除这个待审稿件吗？删除后作者和首页都不会再看到。')
    if (!confirmed) {
      return
    }
    deleteSubmissionMutation.mutate(postId)
  }

  if (!canModerateCommunity) {
    return <Navigate to="/circle" replace />
  }

  const pendingSubmissions = pendingSubmissionsQuery.data ?? []

  return (
    <section className="page-section admin-review-page">
      <div className="admin-review-shell">
        <header className="admin-review-hero">
          <SafetyOutlined />
          <div>
            <p>RURU ADMIN</p>
            <h1>稿件审核</h1>
            <span>视频先审后公开。通过后进首页，驳回后作者能看到原因。</span>
          </div>
        </header>

        <Card className="profile-card admin-review-card" title={`待审稿件 ${pendingSubmissions.length}`}>
          <Space direction="vertical" size={16} style={{ width: '100%' }}>
            <Alert
              showIcon
              type="info"
              message="审核时先看封面、标题、简介，再播放视频预览。"
              description="如果内容没问题就通过；如果标题、封面或内容不合适，写清楚原因后驳回；明显违规可以直接删除。"
            />

            {pendingSubmissionsQuery.isError ? (
              <Alert showIcon type="error" message={pendingSubmissionsQuery.error.message} />
            ) : null}
            {approveSubmissionMutation.isError ? (
              <Alert showIcon type="error" message={approveSubmissionMutation.error.message} />
            ) : null}
            {rejectSubmissionMutation.isError ? (
              <Alert showIcon type="error" message={rejectSubmissionMutation.error.message} />
            ) : null}
            {deleteSubmissionMutation.isError ? (
              <Alert showIcon type="error" message={deleteSubmissionMutation.error.message} />
            ) : null}

            {pendingSubmissionsQuery.isLoading ? (
              <div className="admin-review-loading">正在读取待审稿件...</div>
            ) : null}

            {!pendingSubmissionsQuery.isLoading && pendingSubmissions.length === 0 ? (
              <Empty description="暂无待审稿件" />
            ) : null}

            <div className="admin-review-list">
              {pendingSubmissions.map((submission) => {
                const videoMedia = getVideoMedia(submission)
                const coverUrl = getCoverUrl(submission)
                const isApproving =
                  approveSubmissionMutation.isPending &&
                  approveSubmissionMutation.variables === submission.id
                const isRejecting =
                  rejectSubmissionMutation.isPending &&
                  rejectSubmissionMutation.variables?.postId === submission.id
                const isDeleting =
                  deleteSubmissionMutation.isPending &&
                  deleteSubmissionMutation.variables === submission.id

                return (
                  <article key={submission.id} className="admin-submission-card">
                    <div className="admin-submission-preview">
                      {videoMedia?.url ? (
                        <video controls preload="metadata" poster={coverUrl ?? undefined} src={videoMedia.url} />
                      ) : coverUrl ? (
                        <img alt={submission.title} src={coverUrl} />
                      ) : (
                        <span>暂无预览</span>
                      )}
                    </div>

                    <div className="admin-submission-main">
                      <div className="admin-submission-meta">
                        <Tag color="gold">待审核</Tag>
                        <Tag>{formatContentType(submission)}</Tag>
                        <span>#{submission.id}</span>
                        <span>{dayjs(submission.createdAt).format('YYYY-MM-DD HH:mm')}</span>
                      </div>

                      <h2>{submission.title}</h2>
                      <p>{getShortContent(submission.content)}</p>

                      <div className="admin-submission-author">
                        <span>作者：{submission.authorName}</span>
                        {submission.topicName ? <span>话题：#{submission.topicName}</span> : null}
                      </div>

                      {rejectingPostId === submission.id ? (
                        <div className="admin-reject-box">
                          <Input.TextArea
                            rows={3}
                            value={rejectReason}
                            placeholder="写清楚驳回原因，比如：封面不清晰、标题不准确、内容不适合公开。"
                            onChange={(event) => setRejectReason(event.target.value)}
                          />
                          <div>
                            <Button
                              danger
                              type="primary"
                              icon={<CloseCircleOutlined />}
                              loading={isRejecting}
                              disabled={!rejectReason.trim()}
                              onClick={() => handleConfirmReject(submission.id)}
                            >
                              确认驳回
                            </Button>
                            <Button
                              onClick={() => {
                                setRejectingPostId(null)
                                setRejectReason('')
                              }}
                            >
                              取消
                            </Button>
                          </div>
                        </div>
                      ) : null}

                      <div className="admin-submission-actions">
                        <Button
                          type="primary"
                          icon={<CheckCircleOutlined />}
                          loading={isApproving}
                          onClick={() => handleApproveSubmission(submission.id)}
                        >
                          通过
                        </Button>
                        <Button
                          danger
                          icon={<CloseCircleOutlined />}
                          disabled={rejectingPostId === submission.id}
                          onClick={() => handleStartReject(submission.id)}
                        >
                          驳回
                        </Button>
                        <Button
                          danger
                          type="text"
                          icon={<DeleteOutlined />}
                          loading={isDeleting}
                          onClick={() => handleDeleteSubmission(submission.id)}
                        >
                          删除
                        </Button>
                      </div>
                    </div>
                  </article>
                )
              })}
            </div>
          </Space>
        </Card>

        <Card className="profile-card admin-action-card" title="执行管理动作">
          <Space direction="vertical" size={16} style={{ width: '100%' }}>
            <Alert
              showIcon
              type="warning"
              message="这里是管理员兜底工具。"
              description="当你已经知道帖子、评论或成员 ID 时，可以直接隐藏、恢复或禁言。常规视频审核优先使用上面的待审卡片。"
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
      </div>
    </section>
  )
}

export default CommunityAdminPage
