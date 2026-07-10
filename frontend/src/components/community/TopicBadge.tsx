import { Tag } from 'antd'

type TopicBadgeProps = {
  name?: string | null
  color?: string | null
}

function TopicBadge({ name, color }: TopicBadgeProps) {
  if (!name) {
    return <Tag>未分类</Tag>
  }

  return (
    <Tag color={color || 'green'} style={{ borderRadius: 999, paddingInline: 10 }}>
      {name}
    </Tag>
  )
}

export default TopicBadge
