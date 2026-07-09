import {
  CalendarOutlined,
  CheckCircleOutlined,
  FireOutlined,
  PlusOutlined,
  SaveOutlined,
} from '@ant-design/icons'
import {
  Button,
  Card,
  DatePicker,
  Empty,
  Form,
  Input,
  List,
  Select,
  Space,
  Tag,
  message,
} from 'antd'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import dayjs, { type Dayjs } from 'dayjs'
import { useEffect, useState } from 'react'
import {
  type DailyPlanRequest,
  type DailyPlanResponse,
  type DailyPlanStatus,
  type HabitRequest,
  type JournalRequest,
  createDailyPlan,
  createHabit,
  getJournal,
  listDailyPlans,
  listHabits,
  saveHabitRecord,
  saveJournal,
  updateDailyPlan,
} from '../api/daily'

type PlanFormValues = Omit<DailyPlanRequest, 'planDate'>
type JournalFormValues = Omit<JournalRequest, 'journalDate'>

const planStatusOptions: Array<{ label: string; value: DailyPlanStatus }> = [
  { label: '待做', value: 'TODO' },
  { label: '进行中', value: 'DOING' },
  { label: '完成', value: 'DONE' },
]

const planStatusColor: Record<DailyPlanStatus, string> = {
  TODO: 'default',
  DOING: 'processing',
  DONE: 'success',
}

function DailyPage() {
  const queryClient = useQueryClient()
  const [planForm] = Form.useForm<PlanFormValues>()
  const [journalForm] = Form.useForm<JournalFormValues>()
  const [habitForm] = Form.useForm<HabitRequest>()
  const [selectedDate, setSelectedDate] = useState<Dayjs>(dayjs())
  const selectedDateText = selectedDate.format('YYYY-MM-DD')

  const plansQuery = useQuery({
    queryKey: ['daily-plans', selectedDateText],
    queryFn: () => listDailyPlans(selectedDateText),
  })

  const journalQuery = useQuery({
    queryKey: ['daily-journal', selectedDateText],
    queryFn: () => getJournal(selectedDateText),
  })

  const habitsQuery = useQuery({
    queryKey: ['daily-habits'],
    queryFn: listHabits,
  })

  useEffect(() => {
    journalForm.setFieldsValue({
      mood: journalQuery.data?.mood ?? 'FOCUSED',
      content: journalQuery.data?.content ?? '',
    })
  }, [journalForm, journalQuery.data])

  const createPlanMutation = useMutation({
    mutationFn: createDailyPlan,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['daily-plans', selectedDateText] })
      planForm.resetFields()
      message.success('计划已创建')
    },
  })

  const updatePlanMutation = useMutation({
    mutationFn: ({ id, request }: { id: number; request: DailyPlanRequest }) =>
      updateDailyPlan(id, request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['daily-plans', selectedDateText] })
      message.success('计划状态已更新')
    },
  })

  const saveJournalMutation = useMutation({
    mutationFn: saveJournal,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['daily-journal', selectedDateText] })
      message.success('日记已保存')
    },
  })

  const createHabitMutation = useMutation({
    mutationFn: createHabit,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['daily-habits'] })
      habitForm.resetFields()
      message.success('习惯已创建')
    },
  })

  const saveHabitRecordMutation = useMutation({
    mutationFn: ({ habitId, completed }: { habitId: number; completed: boolean }) =>
      saveHabitRecord(habitId, {
        recordDate: selectedDateText,
        completed,
      }),
    onSuccess: () => {
      message.success('打卡已保存')
    },
  })

  function createPlan(values: PlanFormValues) {
    createPlanMutation.mutate({
      ...values,
      planDate: selectedDateText,
      status: values.status ?? 'TODO',
    })
  }

  function changePlanStatus(plan: DailyPlanResponse, status: DailyPlanStatus) {
    updatePlanMutation.mutate({
      id: plan.id,
      request: {
        planDate: plan.planDate,
        title: plan.title,
        description: plan.description,
        status,
      },
    })
  }

  function submitJournal(values: JournalFormValues) {
    saveJournalMutation.mutate({
      journalDate: selectedDateText,
      mood: values.mood,
      content: values.content,
    })
  }

  return (
    <section className="page-section">
      <div className="page-heading">
        <div>
          <p className="dashboard-kicker">Daily module</p>
          <h1>今日计划</h1>
          <p>把每天要做的事情、学习复盘和长期习惯放在同一个日常驾驶舱里。</p>
        </div>
        <DatePicker
          value={selectedDate}
          onChange={(value) => setSelectedDate(value ?? dayjs())}
          allowClear={false}
        />
      </div>

      <div className="daily-grid">
        <Card className="workspace-card">
          <div className="workspace-card-title">
            <CalendarOutlined />
            <span>{selectedDateText} 的计划</span>
          </div>

          <Form<PlanFormValues>
            form={planForm}
            layout="vertical"
            requiredMark={false}
            onFinish={createPlan}
            initialValues={{ status: 'TODO' }}
          >
            <Form.Item
              name="title"
              label="计划标题"
              rules={[{ required: true, message: '请输入计划标题' }]}
            >
              <Input placeholder="例如 完成笔记前端页面" />
            </Form.Item>
            <Form.Item name="description" label="计划描述">
              <Input.TextArea rows={2} placeholder="补充验收标准或具体动作。" />
            </Form.Item>
            <Form.Item name="status" label="状态">
              <Select options={planStatusOptions} />
            </Form.Item>
            <Button
              block
              type="primary"
              htmlType="submit"
              icon={<PlusOutlined />}
              loading={createPlanMutation.isPending}
            >
              新增计划
            </Button>
          </Form>

          <List
            className="daily-list"
            loading={plansQuery.isLoading}
            dataSource={plansQuery.data ?? []}
            locale={{ emptyText: <Empty description="这一天还没有计划。" /> }}
            renderItem={(plan) => (
              <List.Item
                actions={[
                  <Select
                    key="status"
                    size="small"
                    value={plan.status}
                    options={planStatusOptions}
                    style={{ width: 110 }}
                    onChange={(status) => changePlanStatus(plan, status)}
                  />,
                ]}
              >
                <List.Item.Meta
                  title={
                    <Space>
                      <span>{plan.title}</span>
                      <Tag color={planStatusColor[plan.status]}>
                        {planStatusOptions.find((item) => item.value === plan.status)?.label}
                      </Tag>
                    </Space>
                  }
                  description={plan.description || '暂无描述'}
                />
              </List.Item>
            )}
          />
        </Card>

        <Card className="workspace-card">
          <div className="workspace-card-title">
            <CheckCircleOutlined />
            <span>今日日记</span>
          </div>
          <Form<JournalFormValues>
            form={journalForm}
            layout="vertical"
            requiredMark={false}
            onFinish={submitJournal}
          >
            <Form.Item name="mood" label="状态">
              <Select
                options={[
                  { label: '专注', value: 'FOCUSED' },
                  { label: '开心', value: 'HAPPY' },
                  { label: '有点卡', value: 'STUCK' },
                  { label: '疲惫', value: 'TIRED' },
                ]}
              />
            </Form.Item>
            <Form.Item name="content" label="复盘内容">
              <Input.TextArea
                rows={9}
                placeholder="今天学了什么？卡在哪里？明天从哪里继续？"
              />
            </Form.Item>
            <Button
              block
              type="primary"
              htmlType="submit"
              icon={<SaveOutlined />}
              loading={saveJournalMutation.isPending}
            >
              保存日记
            </Button>
          </Form>
        </Card>

        <Card className="workspace-card">
          <div className="workspace-card-title">
            <FireOutlined />
            <span>习惯打卡</span>
          </div>
          <Form<HabitRequest>
            form={habitForm}
            layout="vertical"
            requiredMark={false}
            onFinish={(values) => createHabitMutation.mutate(values)}
          >
            <Form.Item
              name="name"
              label="习惯名称"
              rules={[{ required: true, message: '请输入习惯名称' }]}
            >
              <Input placeholder="例如 每天写项目" />
            </Form.Item>
            <Form.Item name="description" label="习惯描述">
              <Input placeholder="例如 保持全栈项目推进" />
            </Form.Item>
            <Button
              block
              icon={<PlusOutlined />}
              loading={createHabitMutation.isPending}
              onClick={() => habitForm.submit()}
            >
              新增习惯
            </Button>
          </Form>

          <List
            className="daily-list"
            loading={habitsQuery.isLoading}
            dataSource={habitsQuery.data ?? []}
            locale={{ emptyText: <Empty description="还没有习惯。" /> }}
            renderItem={(habit) => (
              <List.Item
                actions={[
                  <Button
                    key="check"
                    type="primary"
                    size="small"
                    loading={saveHabitRecordMutation.isPending}
                    onClick={() =>
                      saveHabitRecordMutation.mutate({ habitId: habit.id, completed: true })
                    }
                  >
                    今日完成
                  </Button>,
                ]}
              >
                <List.Item.Meta
                  title={habit.name}
                  description={habit.description || '坚持是一种复利'}
                />
              </List.Item>
            )}
          />
        </Card>
      </div>
    </section>
  )
}

export default DailyPage
