import { DeleteOutlined, PlusOutlined } from '@ant-design/icons'
import { Button, Card, Form, Input, InputNumber, Select, Space, message } from 'antd'
import { useMutation } from '@tanstack/react-query'
import {
  type ProjectTechStackCategory,
  type ProjectTechStackRequest,
  saveProjectTechStacks,
} from '../../api/projectHub'

type TechStackFormValues = {
  techStacks: ProjectTechStackRequest[]
}

type TechStackEditorProps = {
  projectId: number | null
}

const categoryOptions: { label: string; value: ProjectTechStackCategory }[] = [
  { label: '前端', value: 'FRONTEND' },
  { label: '后端', value: 'BACKEND' },
  { label: '数据库', value: 'DATABASE' },
  { label: '部署', value: 'DEPLOYMENT' },
  { label: '工具链', value: 'TOOLING' },
  { label: '其他', value: 'OTHER' },
]

function TechStackEditor({ projectId }: TechStackEditorProps) {
  const [form] = Form.useForm<TechStackFormValues>()
  const [messageApi, contextHolder] = message.useMessage()

  const saveMutation = useMutation({
    mutationFn: (values: TechStackFormValues) => {
      if (!projectId) {
        throw new Error('Please select a project first')
      }
      return saveProjectTechStacks(projectId, values.techStacks ?? [])
    },
    onSuccess: () => messageApi.success('技术栈已保存'),
  })

  return (
    <Card className="workspace-card" title="技术栈">
      {contextHolder}
      <Form<TechStackFormValues>
        form={form}
        layout="vertical"
        initialValues={{
          techStacks: [
            { name: 'React', category: 'FRONTEND', sortOrder: 1 },
            { name: 'Spring Boot', category: 'BACKEND', sortOrder: 2 },
            { name: 'Docker', category: 'DEPLOYMENT', sortOrder: 3 },
          ],
        }}
        onFinish={(values) => saveMutation.mutate(values)}
      >
        <Form.List name="techStacks">
          {(fields, { add, remove }) => (
            <div className="stack-editor">
              {fields.map((field) => (
                <div className="stack-row" key={field.key}>
                  <Form.Item
                    {...field}
                    label="名称"
                    name={[field.name, 'name']}
                    rules={[{ required: true, message: '请输入技术名称' }]}
                  >
                    <Input placeholder="React" />
                  </Form.Item>
                  <Form.Item {...field} label="分类" name={[field.name, 'category']}>
                    <Select options={categoryOptions} />
                  </Form.Item>
                  <Form.Item {...field} label="排序" name={[field.name, 'sortOrder']}>
                    <InputNumber min={0} />
                  </Form.Item>
                  <Button danger icon={<DeleteOutlined />} onClick={() => remove(field.name)} />
                </div>
              ))}
              <Space>
                <Button
                  icon={<PlusOutlined />}
                  onClick={() => add({ name: '', category: 'OTHER', sortOrder: fields.length + 1 })}
                >
                  添加技术
                </Button>
                <Button
                  type="primary"
                  htmlType="submit"
                  disabled={!projectId}
                  loading={saveMutation.isPending}
                >
                  保存技术栈
                </Button>
              </Space>
            </div>
          )}
        </Form.List>
      </Form>
    </Card>
  )
}

export default TechStackEditor
