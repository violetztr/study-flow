import { DeleteOutlined } from '@ant-design/icons'
import { Button, List, Popconfirm, Space, Typography } from 'antd'
import dayjs from 'dayjs'
import type { CommunityCommentResponse } from '../../api/community'

type CommentListProps = {
  comments: CommunityCommentResponse[]
  currentUserId?: number
  deletingId?: number | null
  onDelete?: (commentId: number) => void
}

function CommentList({ comments, currentUserId, deletingId, onDelete }: CommentListProps) {
  return (
    <List
      dataSource={comments}
      locale={{ emptyText: '还没有评论，来写第一条吧。' }}
      renderItem={(comment) => {
        const canDelete = Boolean(onDelete && currentUserId === comment.authorId)

        return (
          <List.Item
            actions={
              canDelete
                ? [
                    <Popconfirm
                      key="delete"
                      title="删除这条评论？"
                      onConfirm={() => onDelete?.(comment.id)}
                    >
                      <Button
                        danger
                        size="small"
                        icon={<DeleteOutlined />}
                        loading={deletingId === comment.id}
                      >
                        删除
                      </Button>
                    </Popconfirm>,
                  ]
                : undefined
            }
          >
            <List.Item.Meta
              title={
                <Space wrap>
                  <Typography.Text strong>{comment.authorName}</Typography.Text>
                  <Typography.Text type="secondary">
                    {dayjs(comment.createdAt).format('YYYY-MM-DD HH:mm')}
                  </Typography.Text>
                </Space>
              }
              description={
                <Typography.Paragraph style={{ whiteSpace: 'pre-wrap', marginBottom: 0 }}>
                  {comment.content}
                </Typography.Paragraph>
              }
            />
          </List.Item>
        )
      }}
    />
  )
}

export default CommentList
