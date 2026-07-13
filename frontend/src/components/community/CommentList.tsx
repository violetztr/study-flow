import { DeleteOutlined } from '@ant-design/icons'
import { Button, Popconfirm } from 'antd'
import dayjs from 'dayjs'
import type { CommunityCommentResponse } from '../../api/community'

type CommentListProps = {
  comments: CommunityCommentResponse[]
  currentUserId?: number
  canModerate?: boolean
  deletingId?: number | null
  onDelete?: (commentId: number) => void
}

function getInitial(name: string) {
  return name.trim().slice(0, 1).toUpperCase() || 'R'
}

function renderAvatar(name: string, avatarUrl?: string | null) {
  return avatarUrl ? <img alt={name} src={avatarUrl} loading="lazy" /> : getInitial(name)
}

function CommentList({ comments, currentUserId, canModerate = false, deletingId, onDelete }: CommentListProps) {
  if (comments.length === 0) {
    return <div className="comment-empty">还没有评论，来写第一条吧。</div>
  }

  return (
    <div className="comment-list">
      {comments.map((comment) => {
        const canDelete = Boolean(onDelete && (canModerate || currentUserId === comment.authorId))

        return (
          <article className="comment-item" key={comment.id}>
            <div className="comment-avatar">{renderAvatar(comment.authorName, comment.authorAvatarUrl)}</div>
            <div className="comment-body">
              <div className="comment-meta">
                <strong>{comment.authorName}</strong>
                <span>{dayjs(comment.createdAt).format('MM-DD HH:mm')}</span>
              </div>
              <p>{comment.content}</p>
            </div>

            {canDelete ? (
              <Popconfirm title="删除这条评论？" onConfirm={() => onDelete?.(comment.id)}>
                <Button
                  type="text"
                  danger
                  size="small"
                  icon={<DeleteOutlined />}
                  loading={deletingId === comment.id}
                  className="comment-delete-button"
                />
              </Popconfirm>
            ) : null}
          </article>
        )
      })}
    </div>
  )
}

export default CommentList
