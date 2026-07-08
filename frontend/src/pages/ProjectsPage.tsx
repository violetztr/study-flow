import { EditOutlined, PlusOutlined } from '@ant-design/icons'
import {
  Button,
  Card,
  Form,
  Input,
  Modal,
  Popconfirm,
  Select,
  Space,
  Table,
  Tag,
} from 'antd'
import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import {
  type ProjectRequest,
  type ProjectResponse,
  createProject,
  deleteProject,
  listProjects,
  updateProject,
} from '../api/projects'

type ProjectFormValues = ProjectRequest

function renderProjectStatus(status: ProjectResponse['status']) {
  return status === 'ACTIVE' ? <Tag color="green">启用</Tag> : <Tag>归档</Tag>
}

function ProjectsPage() {
  const queryClient = useQueryClient()
  const [form] = Form.useForm<ProjectFormValues>()
  const projectsQuery = useQuery({
    queryKey: ['projects'],
    queryFn: listProjects,
  })

  const createMutation = useMutation({
    mutationFn: createProject,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['projects'] }),
  })

  const updateMutation = useMutation({
    mutationFn: ({ id, request }: { id: number; request: ProjectRequest }) =>
      updateProject(id, request),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['projects'] }),
  })

  const deleteMutation = useMutation({
    mutationFn: deleteProject,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['projects'] }),
  })

  const [editingProject, setEditingProject] = useState<ProjectResponse | null>(null)
  const [modalOpen, setModalOpen] = useState(false)

  function openCreateModal() {
    setEditingProject(null)
    form.setFieldsValue({ name: '', description: '', status: 'ACTIVE' })
    setModalOpen(true)
  }

  function openEditModal(project: ProjectResponse) {
    setEditingProject(project)
    form.setFieldsValue(project)
    setModalOpen(true)
  }

  async function handleSubmit(values: ProjectFormValues) {
    const request = { ...values, status: values.status ?? 'ACTIVE' }
    if (editingProject) {
      await updateMutation.mutateAsync({ id: editingProject.id, request })
    } else {
      await createMutation.mutateAsync(request)
    }
    setModalOpen(false)
  }

  return (
    <section className="page-section">
      <div className="page-heading">
        <div>
          <p className="dashboard-kicker">Projects</p>
          <h1>项目管理</h1>
          <p>项目是任务的上一级容器，比如 Java、React、部署、面试准备。</p>
        </div>
        <Button type="primary" icon={<PlusOutlined />} onClick={openCreateModal}>
          新增项目
        </Button>
      </div>

      <Card className="table-card">
        <Table<ProjectResponse>
          rowKey="id"
          loading={projectsQuery.isLoading}
          dataSource={projectsQuery.data ?? []}
          columns={[
            {
              title: '项目名称',
              dataIndex: 'name',
              render: (name, project) => <Link to={`/projects/${project.id}`}>{name}</Link>,
            },
            {
              title: '描述',
              dataIndex: 'description',
              render: (description) => description || '暂无描述',
            },
            {
              title: '状态',
              dataIndex: 'status',
              render: renderProjectStatus,
            },
            {
              title: '操作',
              width: 190,
              render: (_, project) => (
                <Space>
                  <Button
                    size="small"
                    icon={<EditOutlined />}
                    onClick={() => openEditModal(project)}
                  >
                    编辑
                  </Button>
                  <Popconfirm
                    title="确认删除这个项目？"
                    description="删除后项目不可恢复。"
                    onConfirm={() => deleteMutation.mutate(project.id)}
                  >
                    <Button danger size="small">
                      删除
                    </Button>
                  </Popconfirm>
                </Space>
              ),
            },
          ]}
        />
      </Card>

      <Modal
        title={editingProject ? '编辑项目' : '新增项目'}
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        footer={null}
        destroyOnHidden
      >
        <Form<ProjectFormValues>
          form={form}
          layout="vertical"
          requiredMark={false}
          onFinish={handleSubmit}
        >
          <Form.Item
            label="项目名称"
            name="name"
            rules={[{ required: true, message: '请输入项目名称' }]}
          >
            <Input placeholder="例如 React 挑战计划" />
          </Form.Item>
          <Form.Item label="项目描述" name="description">
            <Input.TextArea rows={4} placeholder="这个项目要解决什么学习目标？" />
          </Form.Item>
          <Form.Item label="状态" name="status" initialValue="ACTIVE">
            <Select
              options={[
                { label: '启用', value: 'ACTIVE' },
                { label: '归档', value: 'ARCHIVED' },
              ]}
            />
          </Form.Item>
          <Button
            block
            type="primary"
            htmlType="submit"
            loading={createMutation.isPending || updateMutation.isPending}
          >
            保存
          </Button>
        </Form>
      </Modal>
    </section>
  )
}

export default ProjectsPage
