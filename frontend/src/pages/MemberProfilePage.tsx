import { ArrowLeftOutlined, GithubOutlined, GlobalOutlined, UserOutlined } from '@ant-design/icons'
import { Alert, Avatar, Button, Card, Skeleton, Space, Tag, Typography } from 'antd'
import { useQuery } from '@tanstack/react-query'
import { Link, useParams } from 'react-router-dom'
import { communityApi } from '../api/community'

function splitProfileSkills(skills?: string | null) {
  return (skills ?? '')
    .split(',')
    .map((skill) => skill.trim())
    .filter(Boolean)
}

function MemberProfilePage() {
  const { id } = useParams()
  const userId = Number(id)

  const memberQuery = useQuery({
    queryKey: ['community-member', userId],
    queryFn: () => communityApi.getMember(userId),
    enabled: Number.isFinite(userId),
  })

  if (!Number.isFinite(userId)) {
    return (
      <section className="page-section">
        <Alert showIcon type="error" message="成员地址无效" />
      </section>
    )
  }

  const member = memberQuery.data
  const displayName = member?.displayName || member?.username

  return (
    <section className="page-section">
      <div className="page-heading">
        <div>
          <p className="dashboard-kicker">Member profile</p>
          <h1>{displayName || '成员资料'}</h1>
        </div>
        <Button icon={<ArrowLeftOutlined />}>
          <Link to="/circle/members">返回成员列表</Link>
        </Button>
      </div>

      <Card className="profile-card">
        {memberQuery.isError ? <Alert showIcon type="error" message={memberQuery.error.message} /> : null}
        {memberQuery.isLoading ? <Skeleton active /> : null}
        {member ? (
          <Space direction="vertical" size={18} style={{ width: '100%' }}>
            <Space align="start">
              <Avatar size={72} src={member.avatarUrl} icon={<UserOutlined />} />
              <div>
                <Typography.Title level={2} style={{ margin: 0 }}>
                  {displayName}
                </Typography.Title>
                <Typography.Text type="secondary">@{member.username}</Typography.Text>
              </div>
            </Space>
            <Typography.Paragraph style={{ fontSize: 16 }}>
              {member.bio || '这个成员还没有填写简介。'}
            </Typography.Paragraph>
            <Space wrap>
              <Tag color="green">{member.role}</Tag>
              <Tag color={member.memberStatus === 'ACTIVE' ? 'blue' : 'orange'}>
                {member.memberStatus}
              </Tag>
              {splitProfileSkills(member.skills).map((skill) => (
                <Tag key={skill}>{skill}</Tag>
              ))}
            </Space>
            <Space wrap>
              {member.githubUrl ? (
                <Button icon={<GithubOutlined />} href={member.githubUrl} target="_blank">
                  GitHub
                </Button>
              ) : null}
              {member.websiteUrl ? (
                <Button icon={<GlobalOutlined />} href={member.websiteUrl} target="_blank">
                  Website
                </Button>
              ) : null}
            </Space>
          </Space>
        ) : null}
      </Card>
    </section>
  )
}

export default MemberProfilePage
