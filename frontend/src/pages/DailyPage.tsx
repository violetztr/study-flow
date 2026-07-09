import {
  CalendarOutlined,
  CheckCircleOutlined,
  FireOutlined,
  SmileOutlined,
} from '@ant-design/icons'
import { Card, Tag } from 'antd'

const dailyFeatures = [
  {
    icon: <CalendarOutlined />,
    title: '今日计划',
    copy: '把今天最重要的学习动作列出来，减少打开电脑后不知道先做什么。',
  },
  {
    icon: <FireOutlined />,
    title: '习惯打卡',
    copy: '以后可以记录刷题、看文档、写项目、复盘这些长期习惯。',
  },
  {
    icon: <SmileOutlined />,
    title: '今日日记',
    copy: '记录今天学了什么、卡在哪里、明天继续从哪里开始。',
  },
]

function DailyPage() {
  return (
    <section className="page-section">
      <section className="dashboard-header">
        <div>
          <p className="dashboard-kicker">Daily module</p>
          <h1 className="dashboard-title">今日计划</h1>
          <p className="dashboard-subtitle">
            日常模块会帮你把学习从“想起来再做”变成“每天都有节奏地推进”。
            当前先建立入口，后面再接入计划、习惯和日记数据。
          </p>
        </div>
        <div className="dashboard-actions">
          <Tag color="green" icon={<CheckCircleOutlined />}>
            模块入口已就绪
          </Tag>
        </div>
      </section>

      <section className="dashboard-content">
        <div className="quick-grid">
          {dailyFeatures.map((feature) => (
            <Card className="quick-card" key={feature.title}>
              <span style={{ color: '#256f62', fontSize: 28 }}>{feature.icon}</span>
              <h2 className="quick-title">{feature.title}</h2>
              <p className="quick-copy">{feature.copy}</p>
            </Card>
          ))}
        </div>
      </section>
    </section>
  )
}

export default DailyPage
