import {
  BookOutlined,
  EditOutlined,
  FileSearchOutlined,
  StarOutlined,
} from '@ant-design/icons'
import { Card, Tag } from 'antd'

const noteFeatures = [
  {
    icon: <BookOutlined />,
    title: '页面树',
    copy: '以后这里会像 Notion 一样，左侧管理学习笔记、项目复盘和知识目录。',
  },
  {
    icon: <EditOutlined />,
    title: '块编辑器',
    copy: '正文会拆成段落、标题、待办、引用、代码块，方便你边学边沉淀。',
  },
  {
    icon: <FileSearchOutlined />,
    title: '搜索和复盘',
    copy: '后面会支持按标题、内容、收藏状态查找笔记，减少“学过但找不到”的痛苦。',
  },
]

function NotesPage() {
  return (
    <section className="page-section">
      <section className="dashboard-header">
        <div>
          <p className="dashboard-kicker">Notes module</p>
          <h1 className="dashboard-title">笔记工作台</h1>
          <p className="dashboard-subtitle">
            这里会逐步做成 StudyFlow 的知识沉淀区。当前先放入口骨架，
            下一阶段会接入数据库、笔记页面树和 Notion 风格块编辑器。
          </p>
        </div>
        <div className="dashboard-actions">
          <Tag color="gold" icon={<StarOutlined />}>
            规划中
          </Tag>
        </div>
      </section>

      <section className="dashboard-content">
        <div className="quick-grid">
          {noteFeatures.map((feature) => (
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

export default NotesPage
