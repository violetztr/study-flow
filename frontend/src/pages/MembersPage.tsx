import { UserOutlined } from '@ant-design/icons'
import { Alert, Avatar, Card, Empty, Skeleton, Space, Tag, Typography } from 'antd'
import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { communityApi, type CommunityMemberResponse } from '../api/community'

function splitSkills(skills?: string | null) {
  return (skills ?? '')
    .split(',')
    .map((skill) => skill.trim())
    .filter(Boolean)
}

function renderMemberCard(member: CommunityMemberResponse) {
  const displayName = member.displayName || member.username
  const skills = splitSkills(member.skills)

  return (
    <Card key={member.userId} className="workspace-card" hoverable>
      <Space direction="vertical" size={12} style={{ width: '100%' }}>
        <Space align="start">
          <Avatar size={48} src={member.avatarUrl} icon={<UserOutlined />} />
          <div>
            <Link to={`/circle/members/${member.userId}`}>
              <Typography.Title level={4} style={{ margin: 0 }}>
                {displayName}
              </Typography.Title>
            </Link>
            <Typography.Text type="secondary">@{member.username}</Typography.Text>
          </div>
        </Space>
        <Typography.Paragraph className="muted-text">
          {member.bio || '这个成员还没有填写简介。'}
        </Typography.Paragraph>
        <Space wrap>
          <Tag color="green">{member.role}</Tag>
          <Tag color={member.memberStatus === 'ACTIVE' ? 'blue' : 'orange'}>
            {member.memberStatus}
          </Tag>
          {skills.map((skill) => (
            <Tag key={skill}>{skill}</Tag>
          ))}
        </Space>
      </Space>
    </Card>
  )
}

function MembersPage() {
  const membersQuery = useQuery({
    queryKey: ['community-members'],
    queryFn: communityApi.listMembers,
  })

  return (
    <section className="page-section">
      <div className="page-heading">
        <div>
          <p className="dashboard-kicker">Members</p>
          <h1>社区成员</h1>
          <p>这里展示 Ruru 社区里的成员。现在先保留基础资料，后面再慢慢加关注、私信、好友关系这些能力。</p>
        </div>
      </div>

      <div className="dashboard-content">
        {membersQuery.isError ? <Alert showIcon type="error" message={membersQuery.error.message} /> : null}
        {membersQuery.isLoading ? <Skeleton active /> : null}
        {!membersQuery.isLoading && (membersQuery.data ?? []).length === 0 ? (
          <Empty description="还没有社区成员。" />
        ) : null}
        <div className="quick-grid">{(membersQuery.data ?? []).map(renderMemberCard)}</div>
      </div>
    </section>
  )
}

export default MembersPage
