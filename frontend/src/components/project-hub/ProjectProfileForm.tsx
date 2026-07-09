import { Button, Card, Form, Input, message } from 'antd'
import { useEffect } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import {
  type ProjectProfileRequest,
  type ProjectProfileResponse,
  saveProjectProfile,
} from '../../api/projectHub'

type ProjectProfileFormProps = {
  projectId: number | null
  profile?: ProjectProfileResponse
}

function ProjectProfileForm({ projectId, profile }: ProjectProfileFormProps) {
  const [form] = Form.useForm<ProjectProfileRequest>()
  const [messageApi, contextHolder] = message.useMessage()
  const queryClient = useQueryClient()

  const saveMutation = useMutation({
    mutationFn: (request: ProjectProfileRequest) => {
      if (!projectId) {
        throw new Error('Please select a project first')
      }
      return saveProjectProfile(projectId, request)
    },
    onSuccess: async () => {
      messageApi.success('项目档案已保存')
      await queryClient.invalidateQueries({ queryKey: ['project-profile', projectId] })
    },
  })

  useEffect(() => {
    form.setFieldsValue({
      headline: profile?.headline ?? '',
      productionUrl: profile?.productionUrl ?? '',
      apiDocUrl: profile?.apiDocUrl ?? '',
      databaseDocUrl: profile?.databaseDocUrl ?? '',
      architectureSummary: profile?.architectureSummary ?? '',
      interviewHighlights: profile?.interviewHighlights ?? '',
      coverImageUrl: profile?.coverImageUrl ?? '',
    })
  }, [form, profile])

  return (
    <Card className="workspace-card" title="项目档案">
      {contextHolder}
      <Form<ProjectProfileRequest>
        form={form}
        layout="vertical"
        requiredMark={false}
        onFinish={(values) => saveMutation.mutate(values)}
      >
        <Form.Item label="一句话定位" name="headline">
          <Input placeholder="例如：个人全栈研发中台" />
        </Form.Item>
        <div className="form-grid">
          <Form.Item label="线上地址" name="productionUrl">
            <Input placeholder="https://www.violet-surf.com" />
          </Form.Item>
          <Form.Item label="接口文档" name="apiDocUrl">
            <Input placeholder="https://www.violet-surf.com/doc.html" />
          </Form.Item>
        </div>
        <Form.Item label="数据库文档" name="databaseDocUrl">
          <Input placeholder="docs/database.md 或线上文档地址" />
        </Form.Item>
        <Form.Item label="架构说明" name="architectureSummary">
          <Input.TextArea rows={4} placeholder="React + Spring Boot + MySQL + Docker..." />
        </Form.Item>
        <Form.Item label="面试亮点" name="interviewHighlights">
          <Input.TextArea rows={4} placeholder="权限、测试、部署、GitHub 同步、公开作品集..." />
        </Form.Item>
        <Form.Item label="封面图地址" name="coverImageUrl">
          <Input placeholder="https://example.com/cover.png" />
        </Form.Item>
        <Button
          block
          type="primary"
          htmlType="submit"
          disabled={!projectId}
          loading={saveMutation.isPending}
        >
          保存项目档案
        </Button>
      </Form>
    </Card>
  )
}

export default ProjectProfileForm
