import { ExportOutlined } from '@ant-design/icons'
import { Button, Card, Form, Input, InputNumber, Space, Switch, message } from 'antd'
import { Link } from 'react-router-dom'
import { useState } from 'react'
import { useMutation } from '@tanstack/react-query'
import {
  type PortfolioProjectRequest,
  type PortfolioProjectResponse,
  savePortfolioSettings,
} from '../../api/projectHub'

type PortfolioSettingsCardProps = {
  projectId: number | null
}

function PortfolioSettingsCard({ projectId }: PortfolioSettingsCardProps) {
  const [form] = Form.useForm<PortfolioProjectRequest>()
  const [messageApi, contextHolder] = message.useMessage()
  const [portfolio, setPortfolio] = useState<PortfolioProjectResponse | null>(null)

  const saveMutation = useMutation({
    mutationFn: (request: PortfolioProjectRequest) => {
      if (!projectId) {
        throw new Error('Please select a project first')
      }
      return savePortfolioSettings(projectId, request)
    },
    onSuccess: (data) => {
      setPortfolio(data)
      messageApi.success('作品集设置已保存')
    },
  })

  return (
    <Card className="workspace-card" title="公开作品集">
      {contextHolder}
      <Form<PortfolioProjectRequest>
        form={form}
        layout="vertical"
        requiredMark={false}
        initialValues={{
          publicVisible: true,
          featured: true,
          displayOrder: 1,
        }}
        onFinish={(values) => saveMutation.mutate(values)}
      >
        <Form.Item
          label="公开路径 slug"
          name="slug"
          rules={[{ required: true, message: '请输入公开路径' }]}
        >
          <Input placeholder="devflow-studio" />
        </Form.Item>
        <div className="form-grid">
          <Form.Item label="公开展示" name="publicVisible" valuePropName="checked">
            <Switch />
          </Form.Item>
          <Form.Item label="设为精选" name="featured" valuePropName="checked">
            <Switch />
          </Form.Item>
        </div>
        <Form.Item label="排序" name="displayOrder">
          <InputNumber min={0} />
        </Form.Item>
        <Form.Item label="公开摘要" name="publicSummary">
          <Input.TextArea rows={4} placeholder="给面试官看的项目摘要。" />
        </Form.Item>
        <Space wrap>
          <Button
            type="primary"
            htmlType="submit"
            disabled={!projectId}
            loading={saveMutation.isPending}
          >
            保存发布设置
          </Button>
          {portfolio?.publicVisible ? (
            <Button icon={<ExportOutlined />}>
              <Link to={`/portfolio/${portfolio.slug}`}>预览公开页</Link>
            </Button>
          ) : null}
        </Space>
      </Form>
    </Card>
  )
}

export default PortfolioSettingsCard
