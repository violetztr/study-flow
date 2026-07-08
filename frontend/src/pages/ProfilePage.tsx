import { UserOutlined } from '@ant-design/icons'
import { Card, Descriptions, Skeleton } from 'antd'
import { useQuery } from '@tanstack/react-query'
import { getCurrentUser, getStoredUser } from '../api/auth'

function ProfilePage() {
  const storedUser = getStoredUser()
  const userQuery = useQuery({
    queryKey: ['current-user'],
    queryFn: getCurrentUser,
  })

  const user = userQuery.data ?? storedUser

  return (
    <section className="page-section">
      <div className="page-heading">
        <div>
          <p className="dashboard-kicker">Profile</p>
          <h1>个人资料</h1>
          <p>这里展示当前登录用户。后续可以扩展头像、昵称、学习偏好等功能。</p>
        </div>
      </div>

      <Card className="profile-card">
        {userQuery.isLoading && !user ? (
          <Skeleton active />
        ) : (
          <Descriptions
            bordered
            column={1}
            title={
              <span>
                <UserOutlined /> 当前用户
              </span>
            }
          >
            <Descriptions.Item label="用户 ID">{user?.id ?? '-'}</Descriptions.Item>
            <Descriptions.Item label="用户名">{user?.username ?? '-'}</Descriptions.Item>
            <Descriptions.Item label="邮箱">{user?.email ?? '-'}</Descriptions.Item>
          </Descriptions>
        )}
      </Card>
    </section>
  )
}

export default ProfilePage
