import {
  EditOutlined,
  PlusOutlined,
  TagsOutlined,
} from '@ant-design/icons'
import {
  Button,
  Card,
  DatePicker,
  Form,
  Input,
  Modal,
  Popconfirm,
  Select,
  Space,
  Table,
  Tag,
} from 'antd'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import dayjs, { type Dayjs } from 'dayjs'
import { useState } from 'react'
import { listProjects } from '../api/projects'
import {
  type TaskPriority,
  type TaskQuery,
  type TaskRequest,
  type TaskResponse,
  type TaskStatus,
  createTask,
  deleteTask,
  listTasks,
  updateTask,
} from '../api/tasks'
import { type TagRequest, createTag, listTags } from '../api/tags'

type TaskFormValues = Omit<TaskRequest, 'deadline'> & {
  deadline?: Dayjs | null
}

const statusOptions: Array<{ label: string; value: TaskStatus }> = [
  { label: '待开始', value: 'PENDING' },
  { label: '进行中', value: 'IN_PROGRESS' },
  { label: '已完成', value: 'DONE' },
]

const priorityOptions: Array<{ label: string; value: TaskPriority }> = [
  { label: '低', value: 'LOW' },
  { label: '中', value: 'MEDIUM' },
  { label: '高', value: 'HIGH' },
]

const statusColor: Record<TaskStatus, string> = {
  PENDING: 'default',
  IN_PROGRESS: 'processing',
  DONE: 'success',
}

const priorityColor: Record<TaskPriority, string> = {
  LOW: 'blue',
  MEDIUM: 'gold',
  HIGH: 'red',
}

function normalizeTaskRequest(values: TaskFormValues): TaskRequest {
  return {
    ...values,
    status: values.status ?? 'PENDING',
    priority: values.priority ?? 'MEDIUM',
    deadline: values.deadline ? values.deadline.format('YYYY-MM-DDTHH:mm:ss') : null,
    tagIds: values.tagIds ?? [],
  }
}

function TasksPage() {
  const queryClient = useQueryClient()
  const [taskForm] = Form.useForm<TaskFormValues>()
  const [tagForm] = Form.useForm<TagRequest>()
  const [filters, setFilters] = useState<TaskQuery>({})
  const [editingTask, setEditingTask] = useState<TaskResponse | null>(null)
  const [taskModalOpen, setTaskModalOpen] = useState(false)
  const [tagModalOpen, setTagModalOpen] = useState(false)

  const projectsQuery = useQuery({
    queryKey: ['projects'],
    queryFn: listProjects,
  })

  const tagsQuery = useQuery({
    queryKey: ['tags'],
    queryFn: listTags,
  })

  const tasksQuery = useQuery({
    queryKey: ['tasks', filters],
    queryFn: () => listTasks(filters),
  })

  const createTaskMutation = useMutation({
    mutationFn: createTask,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['tasks'] })
      queryClient.invalidateQueries({ queryKey: ['statistics-overview'] })
    },
  })

  const updateTaskMutation = useMutation({
    mutationFn: ({ id, request }: { id: number; request: TaskRequest }) =>
      updateTask(id, request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['tasks'] })
      queryClient.invalidateQueries({ queryKey: ['statistics-overview'] })
    },
  })

  const deleteTaskMutation = useMutation({
    mutationFn: deleteTask,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['tasks'] })
      queryClient.invalidateQueries({ queryKey: ['statistics-overview'] })
    },
  })

  const createTagMutation = useMutation({
    mutationFn: createTag,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['tags'] })
      setTagModalOpen(false)
      tagForm.resetFields()
    },
  })

  function openCreateTaskModal() {
    setEditingTask(null)
    taskForm.setFieldsValue({
      projectId: projectsQuery.data?.[0]?.id,
      title: '',
      description: '',
      status: 'PENDING',
      priority: 'MEDIUM',
      deadline: null,
      tagIds: [],
    })
    setTaskModalOpen(true)
  }

  function openEditTaskModal(task: TaskResponse) {
    setEditingTask(task)
    taskForm.setFieldsValue({
      ...task,
      deadline: task.deadline ? dayjs(task.deadline) : null,
    })
    setTaskModalOpen(true)
  }

  async function handleTaskSubmit(values: TaskFormValues) {
    const request = normalizeTaskRequest(values)
    if (editingTask) {
      await updateTaskMutation.mutateAsync({ id: editingTask.id, request })
    } else {
      await createTaskMutation.mutateAsync(request)
    }
    setTaskModalOpen(false)
  }

  function findProjectName(projectId: number) {
    return projectsQuery.data?.find((project) => project.id === projectId)?.name ?? `项目 ${projectId}`
  }

  function renderTags(tagIds: number[]) {
    const tags = tagsQuery.data ?? []
    if (!tagIds.length) {
      return <span className="muted-text">无标签</span>
    }

    return tagIds.map((tagId) => {
      const tag = tags.find((item) => item.id === tagId)
      return (
        <Tag key={tagId} color={tag?.color || 'blue'}>
          {tag?.name ?? tagId}
        </Tag>
      )
    })
  }

  return (
    <section className="page-section">
      <div className="page-heading">
        <div>
          <p className="dashboard-kicker">Tasks</p>
          <h1>任务管理</h1>
          <p>用状态、优先级、项目和关键词筛选任务，把学习路线拆成可执行动作。</p>
        </div>
        <Space wrap>
          <Button icon={<TagsOutlined />} onClick={() => setTagModalOpen(true)}>
            新增标签
          </Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={openCreateTaskModal}>
            新增任务
          </Button>
        </Space>
      </div>

      <Card className="filter-card">
        <Space wrap>
          <Select
            allowClear
            placeholder="按项目筛选"
            style={{ width: 180 }}
            options={(projectsQuery.data ?? []).map((project) => ({
              label: project.name,
              value: project.id,
            }))}
            onChange={(projectId) => setFilters((current) => ({ ...current, projectId }))}
          />
          <Select
            allowClear
            placeholder="按状态筛选"
            style={{ width: 150 }}
            options={statusOptions}
            onChange={(status) => setFilters((current) => ({ ...current, status }))}
          />
          <Select
            allowClear
            placeholder="按优先级筛选"
            style={{ width: 150 }}
            options={priorityOptions}
            onChange={(priority) => setFilters((current) => ({ ...current, priority }))}
          />
          <Input.Search
            allowClear
            placeholder="搜索任务标题"
            style={{ width: 240 }}
            onSearch={(keyword) =>
              setFilters((current) => ({ ...current, keyword: keyword || undefined }))
            }
          />
          <Button onClick={() => setFilters({})}>清空筛选</Button>
        </Space>
      </Card>

      <Card className="table-card">
        <Table<TaskResponse>
          rowKey="id"
          loading={tasksQuery.isLoading}
          dataSource={tasksQuery.data ?? []}
          columns={[
            {
              title: '任务',
              dataIndex: 'title',
              render: (title, task) => (
                <div>
                  <strong>{title}</strong>
                  <p className="table-description">{task.description || '暂无描述'}</p>
                </div>
              ),
            },
            {
              title: '项目',
              dataIndex: 'projectId',
              render: findProjectName,
            },
            {
              title: '状态',
              dataIndex: 'status',
              render: (status: TaskStatus) => (
                <Tag color={statusColor[status]}>
                  {statusOptions.find((item) => item.value === status)?.label}
                </Tag>
              ),
            },
            {
              title: '优先级',
              dataIndex: 'priority',
              render: (priority: TaskPriority) => (
                <Tag color={priorityColor[priority]}>
                  {priorityOptions.find((item) => item.value === priority)?.label}
                </Tag>
              ),
            },
            {
              title: '截止时间',
              dataIndex: 'deadline',
              render: (deadline) => deadline || '未设置',
            },
            {
              title: '标签',
              dataIndex: 'tagIds',
              render: renderTags,
            },
            {
              title: '操作',
              width: 190,
              render: (_, task) => (
                <Space>
                  <Button
                    size="small"
                    icon={<EditOutlined />}
                    onClick={() => openEditTaskModal(task)}
                  >
                    编辑
                  </Button>
                  <Popconfirm
                    title="确认删除这个任务？"
                    onConfirm={() => deleteTaskMutation.mutate(task.id)}
                  >
                    <Button danger size="small">
                      删除
                    </Button>
                  </Popconfirm>
                </Space>
              ),
            },
          ]}
          scroll={{ x: 980 }}
        />
      </Card>

      <Modal
        title={editingTask ? '编辑任务' : '新增任务'}
        open={taskModalOpen}
        onCancel={() => setTaskModalOpen(false)}
        footer={null}
        destroyOnHidden
        width={680}
      >
        <Form<TaskFormValues>
          form={taskForm}
          layout="vertical"
          requiredMark={false}
          onFinish={handleTaskSubmit}
        >
          <div className="form-grid">
            <Form.Item
              label="所属项目"
              name="projectId"
              rules={[{ required: true, message: '请选择项目' }]}
            >
              <Select
                placeholder="选择项目"
                options={(projectsQuery.data ?? []).map((project) => ({
                  label: project.name,
                  value: project.id,
                }))}
              />
            </Form.Item>
            <Form.Item label="状态" name="status" initialValue="PENDING">
              <Select options={statusOptions} />
            </Form.Item>
          </div>

          <Form.Item
            label="任务标题"
            name="title"
            rules={[{ required: true, message: '请输入任务标题' }]}
          >
            <Input placeholder="例如 完成项目管理页面" />
          </Form.Item>

          <Form.Item label="任务描述" name="description">
            <Input.TextArea rows={4} placeholder="补充任务背景、验收标准或学习笔记。" />
          </Form.Item>

          <div className="form-grid">
            <Form.Item label="优先级" name="priority" initialValue="MEDIUM">
              <Select options={priorityOptions} />
            </Form.Item>
            <Form.Item label="截止时间" name="deadline">
              <DatePicker showTime style={{ width: '100%' }} />
            </Form.Item>
          </div>

          <Form.Item label="标签" name="tagIds">
            <Select
              mode="multiple"
              placeholder="选择标签"
              options={(tagsQuery.data ?? []).map((tag) => ({
                label: tag.name,
                value: tag.id,
              }))}
            />
          </Form.Item>

          <Button
            block
            type="primary"
            htmlType="submit"
            loading={createTaskMutation.isPending || updateTaskMutation.isPending}
          >
            保存任务
          </Button>
        </Form>
      </Modal>

      <Modal
        title="新增标签"
        open={tagModalOpen}
        onCancel={() => setTagModalOpen(false)}
        footer={null}
        destroyOnHidden
      >
        <Form<TagRequest>
          form={tagForm}
          layout="vertical"
          requiredMark={false}
          onFinish={(values) => createTagMutation.mutate(values)}
          initialValues={{ color: '#1677ff' }}
        >
          <Form.Item
            label="标签名称"
            name="name"
            rules={[{ required: true, message: '请输入标签名称' }]}
          >
            <Input placeholder="例如 后端、React、部署" />
          </Form.Item>
          <Form.Item label="标签颜色" name="color">
            <Input placeholder="#1677ff" />
          </Form.Item>
          <Button block type="primary" htmlType="submit" loading={createTagMutation.isPending}>
            保存标签
          </Button>
        </Form>
      </Modal>
    </section>
  )
}

export default TasksPage
