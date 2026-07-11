import {
  ArrowLeftOutlined,
  HeartOutlined,
  PlusOutlined,
  StarOutlined,
  UserAddOutlined,
  UserDeleteOutlined,
  UserOutlined,
} from '@ant-design/icons'
import { Alert, Avatar, Button, Empty, Skeleton, Tabs, Tag } from 'antd'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { getStoredUser } from '../api/auth'
import { communityApi } from '../api/community'
import type { CommunityPostResponse } from '../api/community'
import PostCard from '../components/community/PostCard'

function splitProfileSkills(skills?: string | null) {
  return (skills ?? '')
    .split(',')
    .map((skill) => skill.trim())
    .filter(Boolean)
}

function hasVideo(post: CommunityPostResponse) {
  return post.contentType === 'VIDEO' || post.media.some((media) => media.fileType === 'VIDEO')
}

function formatMetric(value?: number | null) {
  const safeValue = value ?? 0
  if (safeValue >= 10000) {
    return `${(safeValue / 10000).toFixed(safeValue >= 100000 ? 0 : 1).replace(/\.0$/, '')}万`
  }
  return `${safeValue}`
}

function renderPostGrid(
  posts: CommunityPostResponse[],
  emptyText: string,
  invalidateQueryKeys: Array<readonly unknown[]>,
) {
  if (posts.length === 0) {
    return <Empty className="profile-empty" description={emptyText} />
  }

  return (
    <div className="profile-post-grid discovery-grid">
      {posts.map((post) => (
        <PostCard key={post.id} post={post} invalidateQueryKeys={invalidateQueryKeys} />
      ))}
    </div>
  )
}

function MemberProfilePage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const user = getStoredUser()
  const userId = Number(id)
  const profileQueryKey = ['community-profile', userId] as const

  const profileQuery = useQuery({
    queryKey: profileQueryKey,
    queryFn: () => communityApi.getProfile(userId),
    enabled: Number.isFinite(userId),
  })

  const profile = profileQuery.data
  const member = profile?.member
  const displayName = member?.displayName || member?.username || 'ruru'
  const isSelf = Boolean(user && member && user.id === member.userId)
  const canFollow = Boolean(member && (!user || user.id !== member.userId))
  const videoPosts = profile?.posts.filter(hasVideo) ?? []
  const articlePosts = profile?.posts.filter((post) => !hasVideo(post)) ?? []
  const skillTags = splitProfileSkills(member?.skills)

  const followMutation = useMutation({
    mutationFn: () =>
      member?.followedByCurrentUser
        ? communityApi.unfollowMember(member.userId)
        : communityApi.followMember(member!.userId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: profileQueryKey })
      queryClient.invalidateQueries({ queryKey: ['community-member', member?.userId] })
      queryClient.invalidateQueries({ queryKey: ['community-members'] })
    },
  })

  function handleFollow() {
    if (!member) {
      return
    }
    if (!user) {
      navigate('/login', { state: { from: `/circle/members/${member.userId}` } })
      return
    }
    followMutation.mutate()
  }

  if (!Number.isFinite(userId)) {
    return (
      <section className="page-section member-space-page">
        <Alert showIcon type="error" message="成员地址无效" />
      </section>
    )
  }

  return (
    <section className="page-section member-space-page">
      <button className="profile-back-link" type="button" onClick={() => navigate('/circle')}>
        <ArrowLeftOutlined /> 返回 ruru
      </button>

      {profileQuery.isError ? <Alert showIcon type="error" message={profileQuery.error.message} /> : null}
      {profileQuery.isLoading ? <Skeleton active /> : null}

      {profile && member ? (
        <>
          <header className="member-hero">
            <div className="member-hero-glow" />
            <div className="member-avatar-wrap">
              <Avatar size={88} src={member.avatarUrl} icon={<UserOutlined />} />
            </div>

            <div className="member-hero-main">
              <div className="member-title-row">
                <div>
                  <p className="member-handle">@{member.username}</p>
                  <h1>{displayName}</h1>
                </div>
                <div className="member-actions">
                  {isSelf ? (
                    <Button type="primary" icon={<PlusOutlined />} onClick={() => navigate('/circle/posts/new')}>
                      发布作品
                    </Button>
                  ) : null}
                  {canFollow ? (
                    <Button
                      type={member.followedByCurrentUser ? 'default' : 'primary'}
                      icon={member.followedByCurrentUser ? <UserDeleteOutlined /> : <UserAddOutlined />}
                      loading={followMutation.isPending}
                      onClick={handleFollow}
                    >
                      {user
                        ? member.followedByCurrentUser
                          ? '已关注'
                          : '关注'
                        : '登录后关注'}
                    </Button>
                  ) : null}
                </div>
              </div>

              <p className="member-bio">{member.bio || '这个人还没有写简介。'}</p>

              <div className="member-stat-row">
                <span>
                  <strong>{formatMetric(profile.totalPostCount)}</strong>
                  作品
                </span>
                <span>
                  <strong>{formatMetric(member.followerCount)}</strong>
                  粉丝
                </span>
                <span>
                  <strong>{formatMetric(member.followingCount)}</strong>
                  关注
                </span>
              </div>

              <div className="member-tag-row">
                <Tag>{member.role === 'OWNER' ? '站长' : '成员'}</Tag>
                {skillTags.map((skill) => (
                  <Tag key={skill}>{skill}</Tag>
                ))}
              </div>
            </div>
          </header>

          <Tabs
            className="member-space-tabs"
            items={[
              {
                key: 'works',
                label: `作品 ${profile.totalPostCount}`,
                children: renderPostGrid(profile.posts, '还没有发布作品。', [profileQueryKey]),
              },
              {
                key: 'videos',
                label: `视频 ${profile.videoCount}`,
                children: renderPostGrid(videoPosts, '还没有视频作品。', [profileQueryKey]),
              },
              {
                key: 'articles',
                label: `图文 ${profile.articleCount}`,
                children: renderPostGrid(articlePosts, '还没有图文作品。', [profileQueryKey]),
              },
              ...(profile.currentUserProfile
                ? [
                    {
                      key: 'favorites',
                      label: (
                        <span>
                          <StarOutlined /> 收藏 {profile.favoritePosts.length}
                        </span>
                      ),
                      children: renderPostGrid(profile.favoritePosts, '还没有收藏内容。', [profileQueryKey]),
                    },
                  ]
                : []),
            ]}
          />

          {profile.currentUserProfile ? (
            <div className="profile-self-note">
              <HeartOutlined /> 这里是你的个人空间。后面我们可以继续加投稿管理、播放历史和创作中心。
            </div>
          ) : null}
        </>
      ) : null}

      {!profileQuery.isLoading && !profile ? (
        <Empty description="没有找到这个成员">
          <Button>
            <Link to="/circle">回到首页</Link>
          </Button>
        </Empty>
      ) : null}
    </section>
  )
}

export default MemberProfilePage
