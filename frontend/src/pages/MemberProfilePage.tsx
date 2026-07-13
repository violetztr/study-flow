import {
  ArrowLeftOutlined,
  EditOutlined,
  PlusOutlined,
  SaveOutlined,
  StarOutlined,
  UploadOutlined,
  UserAddOutlined,
  UserDeleteOutlined,
  UserOutlined,
} from '@ant-design/icons'
import { Alert, Avatar, Button, Empty, Form, Input, Skeleton, Tabs, Tag, message } from 'antd'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { type ChangeEvent, useEffect, useRef, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { getStoredUser } from '../api/auth'
import { communityApi } from '../api/community'
import type {
  BackgroundPresetResponse,
  CommunityPostResponse,
  UserProfileRequest,
} from '../api/community'
import { mediaApi } from '../api/media'
import PostCard from '../components/community/PostCard'

type ProfileBackgroundType = 'IMAGE' | 'VIDEO'

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

function normalizeBackgroundType(type?: string | null): ProfileBackgroundType {
  return type === 'VIDEO' ? 'VIDEO' : 'IMAGE'
}

function cleanNullableText(value?: string | null) {
  const nextValue = value?.trim()
  return nextValue ? nextValue : null
}

async function uploadProfileImage(file: File, onStatus: (status: string) => void) {
  if (!file.type.startsWith('image/')) {
    throw new Error('这里只支持上传图片')
  }

  onStatus('准备上传')
  const prepareResponse = await mediaApi.prepareUpload({
    filename: file.name,
    contentType: file.type || 'application/octet-stream',
    fileSize: file.size,
  })

  onStatus('正在上传')
  await mediaApi.uploadToSignedUrl(prepareResponse.uploadUrl, file, prepareResponse.headers)

  onStatus('正在确认')
  const completeResponse = await mediaApi.completeUpload(prepareResponse.mediaFileId)
  return completeResponse.url
}

async function uploadBackgroundAsset(
  file: File,
  onStatus: (status: string) => void,
  allowVideo: boolean,
) {
  const isImage = file.type.startsWith('image/')
  const isVideo = file.type.startsWith('video/')
  if (!isImage && !(allowVideo && isVideo)) {
    throw new Error(allowVideo ? '只支持上传图片或视频背景' : '这里只支持上传图片背景')
  }

  onStatus('准备上传')
  const prepareResponse = await mediaApi.prepareUpload({
    filename: file.name,
    contentType: file.type || 'application/octet-stream',
    fileSize: file.size,
  })

  onStatus('正在上传')
  await mediaApi.uploadToSignedUrl(prepareResponse.uploadUrl, file, prepareResponse.headers)

  onStatus('正在确认')
  return mediaApi.completeUpload(prepareResponse.mediaFileId)
}

function backgroundMediaTypeFromFile(file: File): ProfileBackgroundType {
  return file.type.startsWith('video/') ? 'VIDEO' : 'IMAGE'
}

function presetNameFromFile(file: File) {
  return file.name.replace(/\.[^.]+$/, '').trim() || 'background'
}

function renderBackgroundPresetGrid(
  presets: BackgroundPresetResponse[] | undefined,
  selectedUrl: string | undefined,
  onSelect: (url: string, type: ProfileBackgroundType) => void,
) {
  if (!presets?.length) {
    return <Empty className="profile-background-empty" description="还没有可选背景" />
  }

  return (
    <div className="profile-preset-grid">
      {presets.map((preset) => {
        const active = selectedUrl === preset.url
        return (
          <button
            key={preset.id}
            type="button"
            className={active ? 'active' : ''}
            onClick={() => onSelect(preset.url, preset.mediaType)}
          >
            {preset.mediaType === 'VIDEO' ? (
              <video src={preset.url} autoPlay muted loop playsInline />
            ) : (
              <img src={preset.url} alt={preset.name} />
            )}
            <span>{preset.name}</span>
          </button>
        )
      })}
    </div>
  )
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
  const [form] = Form.useForm<UserProfileRequest>()
  const [messageApi, contextHolder] = message.useMessage()
  const avatarInputRef = useRef<HTMLInputElement | null>(null)
  const backgroundInputRef = useRef<HTMLInputElement | null>(null)
  const profileSystemInputRef = useRef<HTMLInputElement | null>(null)
  const [profileEditorOpen, setProfileEditorOpen] = useState(false)
  const [uploadStatus, setUploadStatus] = useState<string | null>(null)
  const [uploadingTarget, setUploadingTarget] = useState<
    'avatar' | 'profile-background' | 'profile-system' | null
  >(null)
  const userId = Number(id)
  const profileQueryKey = ['community-profile', userId] as const

  const profileQuery = useQuery({
    queryKey: profileQueryKey,
    queryFn: () => communityApi.getProfile(userId),
    enabled: Number.isFinite(userId),
  })

  const profilePresetQuery = useQuery({
    queryKey: ['background-presets', 'PROFILE'],
    queryFn: () => communityApi.listBackgroundPresets('PROFILE'),
  })

  const profile = profileQuery.data
  const member = profile?.member
  const displayName = member?.displayName || member?.username || 'ruru'
  const isSelf = Boolean(user && member && user.id === member.userId)
  const canFollow = Boolean(member && (!user || user.id !== member.userId))
  const videoPosts = profile?.posts.filter(hasVideo) ?? []
  const articlePosts = profile?.posts.filter((post) => !hasVideo(post)) ?? []
  const skillTags = splitProfileSkills(member?.skills)
  const selectedBackgroundUrl = Form.useWatch('profileBackgroundUrl', form)
  const selectedBackgroundType = Form.useWatch('profileBackgroundType', form)
  const selectedAvatarUrl = Form.useWatch('avatarUrl', form)
  const profileBackgroundUrl = member?.profileBackgroundUrl || ''
  const profileBackgroundType = normalizeBackgroundType(member?.profileBackgroundType)
  const isRuruAdmin = user?.username === 'ruru'
  const heroStyle =
    profileBackgroundUrl && profileBackgroundType === 'IMAGE'
      ? {
          backgroundImage: `linear-gradient(90deg, rgba(255, 253, 249, 0.88), rgba(255, 253, 249, 0.64)), url("${profileBackgroundUrl}")`,
        }
      : undefined

  useEffect(() => {
    if (!member) {
      return
    }

    form.setFieldsValue({
      displayName: member.displayName ?? '',
      bio: member.bio ?? '',
      avatarUrl: member.avatarUrl ?? '',
      profileBackgroundUrl: member.profileBackgroundUrl ?? '',
      profileBackgroundType: normalizeBackgroundType(member.profileBackgroundType),
      skills: member.skills ?? '',
      githubUrl: member.githubUrl ?? '',
      websiteUrl: member.websiteUrl ?? '',
    })
  }, [form, member])

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

  const updateProfileMutation = useMutation({
    mutationFn: (values: UserProfileRequest) =>
      communityApi.updateProfile({
        displayName: cleanNullableText(values.displayName),
        bio: cleanNullableText(values.bio),
        avatarUrl: cleanNullableText(values.avatarUrl),
        profileBackgroundUrl: cleanNullableText(values.profileBackgroundUrl),
        profileBackgroundType: normalizeBackgroundType(values.profileBackgroundType),
        skills: member?.skills ?? null,
        githubUrl: member?.githubUrl ?? null,
        websiteUrl: member?.websiteUrl ?? null,
      }),
    onSuccess: () => {
      void messageApi.success('个人空间已更新')
      setProfileEditorOpen(false)
      queryClient.invalidateQueries({ queryKey: profileQueryKey })
      queryClient.invalidateQueries({ queryKey: ['community-profile'] })
      queryClient.invalidateQueries({ queryKey: ['community-me'] })
      queryClient.invalidateQueries({ queryKey: ['community-member', member?.userId] })
      queryClient.invalidateQueries({ queryKey: ['community-members'] })
      queryClient.invalidateQueries({ queryKey: ['community-feed'] })
      queryClient.invalidateQueries({ queryKey: ['community-comments'] })
      queryClient.invalidateQueries({ queryKey: ['community-watch-history-my'] })
      queryClient.invalidateQueries({ queryKey: ['community-rankings-hot'] })
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

  async function handleProfileImageChange(
    event: ChangeEvent<HTMLInputElement>,
    target: 'avatar' | 'profile-background',
  ) {
    const file = event.target.files?.[0]
    event.target.value = ''
    if (!file) {
      return
    }

    try {
      setUploadingTarget(target)
      if (target === 'avatar') {
        const url = await uploadProfileImage(file, setUploadStatus)
        form.setFieldValue('avatarUrl', url)
        void messageApi.success('头像已上传，记得保存')
      } else {
        const media = await uploadBackgroundAsset(file, setUploadStatus, false)
        form.setFieldsValue({
          profileBackgroundUrl: media.url,
          profileBackgroundType: 'IMAGE',
        })
        void messageApi.success('背景图已上传，记得保存')
      }
    } catch (error) {
      void messageApi.error(error instanceof Error ? error.message : '上传失败')
    } finally {
      setUploadingTarget(null)
      setUploadStatus(null)
    }
  }

  function handleSelectPreset(url: string, type: ProfileBackgroundType) {
    form.setFieldsValue({
      profileBackgroundUrl: url,
      profileBackgroundType: type,
    })
  }

  async function handleSystemPresetUpload(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0]
    event.target.value = ''
    if (!file) {
      return
    }

    try {
      setUploadingTarget('profile-system')
      const media = await uploadBackgroundAsset(file, setUploadStatus, true)
      const mediaType = backgroundMediaTypeFromFile(file)

      if (mediaType === 'VIDEO') {
        setUploadStatus('正在审核视频')
        await mediaApi.approveMedia(media.id)
      }

      const preset = await communityApi.createBackgroundPreset({
        placement: 'PROFILE',
        name: presetNameFromFile(file),
        url: media.url,
        mediaType,
      })

      queryClient.invalidateQueries({ queryKey: ['background-presets', 'PROFILE'] })
      handleSelectPreset(preset.url, preset.mediaType)
      void messageApi.success('系统背景已添加，所有人都能选择')
    } catch (error) {
      void messageApi.error(error instanceof Error ? error.message : '添加系统背景失败')
    } finally {
      setUploadingTarget(null)
      setUploadStatus(null)
    }
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
      {contextHolder}
      <button className="profile-back-link" type="button" onClick={() => navigate('/circle')}>
        <ArrowLeftOutlined /> 返回 ruru
      </button>

      {profileQuery.isError ? <Alert showIcon type="error" message={profileQuery.error.message} /> : null}
      {profileQuery.isLoading ? <Skeleton active /> : null}

      {profile && member ? (
        <>
          <header className="member-hero" style={heroStyle}>
            {profileBackgroundUrl && profileBackgroundType === 'VIDEO' ? (
              <video
                className="member-hero-video-bg"
                src={profileBackgroundUrl}
                autoPlay
                muted
                loop
                playsInline
              />
            ) : null}
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
                    <>
                      <Button icon={<EditOutlined />} onClick={() => setProfileEditorOpen((value) => !value)}>
                        编辑资料
                      </Button>
                      <Button type="primary" icon={<PlusOutlined />} onClick={() => navigate('/circle/posts/new')}>
                        投稿
                      </Button>
                    </>
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

          {profile.currentUserProfile && profileEditorOpen ? (
            <section className="profile-editor-card">
              <Form
                form={form}
                layout="vertical"
                onFinish={(values) => updateProfileMutation.mutate(values)}
              >
                <Form.Item name="avatarUrl" hidden>
                  <Input />
                </Form.Item>
                <Form.Item name="profileBackgroundUrl" hidden>
                  <Input />
                </Form.Item>
                <Form.Item name="profileBackgroundType" hidden>
                  <Input />
                </Form.Item>

                {updateProfileMutation.isError ? (
                  <Alert showIcon type="error" message={updateProfileMutation.error.message} />
                ) : null}

                <div className="profile-editor-layout">
                  <div className="profile-avatar-editor">
                    <Avatar size={84} src={selectedAvatarUrl || member.avatarUrl} icon={<UserOutlined />} />
                    <input
                      ref={avatarInputRef}
                      type="file"
                      accept="image/*"
                      hidden
                      onChange={(event) => void handleProfileImageChange(event, 'avatar')}
                    />
                    <Button
                      icon={<UploadOutlined />}
                      loading={uploadingTarget === 'avatar'}
                      onClick={() => avatarInputRef.current?.click()}
                    >
                      换头像
                    </Button>
                  </div>

                  <div className="profile-editor-fields">
                    <Form.Item label="昵称" name="displayName">
                      <Input maxLength={80} placeholder="给朋友看的名字" />
                    </Form.Item>
                    <Form.Item label="简介" name="bio">
                      <Input.TextArea maxLength={500} rows={3} placeholder="一句话介绍你自己" />
                    </Form.Item>
                  </div>
                </div>

                <div className="profile-background-editor">
                  <div className="background-setting-block">
                    <div className="profile-editor-heading">
                      <strong>空间背景</strong>
                      <span>{uploadStatus ? `${uploadStatus}...` : '个人空间顶部展示，自己上传只支持图片'}</span>
                    </div>

                    {renderBackgroundPresetGrid(
                      profilePresetQuery.data,
                      selectedBackgroundUrl ?? undefined,
                      handleSelectPreset,
                    )}

                    <input
                      ref={backgroundInputRef}
                      type="file"
                      accept="image/*"
                      hidden
                      onChange={(event) => void handleProfileImageChange(event, 'profile-background')}
                    />
                    <input
                      ref={profileSystemInputRef}
                      type="file"
                      accept="image/*,video/mp4,video/webm"
                      hidden
                      onChange={(event) => void handleSystemPresetUpload(event)}
                    />

                    <div className="background-setting-actions">
                      <Button
                        icon={<UploadOutlined />}
                        loading={uploadingTarget === 'profile-background'}
                        onClick={() => backgroundInputRef.current?.click()}
                      >
                        上传自己的图片
                      </Button>
                      {isRuruAdmin ? (
                        <Button
                          icon={<PlusOutlined />}
                          loading={uploadingTarget === 'profile-system'}
                          onClick={() => profileSystemInputRef.current?.click()}
                        >
                          添加系统背景
                        </Button>
                      ) : null}
                    </div>

                    {selectedBackgroundUrl ? (
                      <div className="profile-background-preview">
                        {normalizeBackgroundType(selectedBackgroundType) === 'VIDEO' ? (
                          <video src={selectedBackgroundUrl} autoPlay muted loop playsInline />
                        ) : (
                          <img src={selectedBackgroundUrl} alt="空间背景预览" />
                        )}
                      </div>
                    ) : null}
                  </div>

                  <div className="profile-editor-actions">
                    <Button onClick={() => setProfileEditorOpen(false)}>取消</Button>
                    <Button
                      type="primary"
                      htmlType="submit"
                      icon={<SaveOutlined />}
                      loading={updateProfileMutation.isPending}
                    >
                      保存
                    </Button>
                  </div>
                </div>
              </Form>
            </section>
          ) : null}

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
