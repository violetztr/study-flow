import type { CSSProperties } from 'react'

type TopicBadgeProps = {
  name?: string | null
  color?: string | null
}

function TopicBadge({ name, color }: TopicBadgeProps) {
  if (!name) {
    return null
  }

  return (
    <span className="topic-pill" style={{ '--topic-color': color || '#2f6f60' } as CSSProperties}>
      {name}
    </span>
  )
}

export default TopicBadge
