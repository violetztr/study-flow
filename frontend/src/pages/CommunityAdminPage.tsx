import { SafetyOutlined } from '@ant-design/icons'
import { Alert, Card, Form, Input, Select, Space, Typography } from 'antd'

function CommunityAdminPage() {
  return (
    <section className="page-section">
      <section className="dashboard-header">
        <div>
          <p className="dashboard-kicker">Moderation</p>
          <h1 className="dashboard-title">Community Admin</h1>
          <p className="dashboard-subtitle">
            这里先作为 Violet Circle 的轻量治理入口：记录处理对象、动作和原因，后续可以接入完整审核列表。
          </p>
        </div>
        <SafetyOutlined style={{ color: 'var(--sf-primary)', fontSize: 44 }} />
      </section>

      <Card className="profile-card" title="基础处理记录">
        <Space direction="vertical" size={16} style={{ width: '100%' }}>
          <Alert
            showIcon
            type="info"
            message="当前页面保留为管理入口"
            description="后端已有隐藏/恢复帖子、隐藏/恢复评论、禁言/解除禁言接口。为了避免误操作，这里暂不直接触发生产动作。"
          />
          <Form layout="vertical" requiredMark={false}>
            <Form.Item label="处理对象">
              <Input placeholder="填写帖子 ID、评论 ID 或用户 ID" />
            </Form.Item>
            <Form.Item label="动作类型">
              <Select
                placeholder="选择计划执行的动作"
                options={[
                  { label: '隐藏帖子', value: 'hide-post' },
                  { label: '恢复帖子', value: 'restore-post' },
                  { label: '隐藏评论', value: 'hide-comment' },
                  { label: '恢复评论', value: 'restore-comment' },
                  { label: '禁言成员', value: 'mute-member' },
                  { label: '解除禁言', value: 'unmute-member' },
                ]}
              />
            </Form.Item>
            <Form.Item label="原因">
              <Input.TextArea rows={4} placeholder="记录上下文，便于之后复盘治理决策。" />
            </Form.Item>
          </Form>
          <Typography.Text type="secondary">
            需要执行真实管理动作时，请先确认管理员权限和目标 ID，避免误伤正常内容。
          </Typography.Text>
        </Space>
      </Card>
    </section>
  )
}

export default CommunityAdminPage
