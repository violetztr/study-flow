import { http } from './http'
import type { UserWalletResponse } from './auth'

export type CommunityTopicResponse = {
  id: number
  name: string
  slug: string
  description?: string | null
  color?: string | null
  postCount: number
}

export type CommunityPostRequest = {
  title: string
  content: string
  topicId?: number | null
  topicName?: string | null
  videoCoverMediaFileId?: number | null
  mediaFileIds?: number[]
}

export type MediaAttachmentResponse = {
  id: number
  fileType: string
  contentType: string
  originalFilename: string
  fileSize: number
  url: string
  coverUrl?: string | null
  playbackUrl?: string | null
  playbackType?: string | null
  transcodeStatus?: string | null
  transcodeError?: string | null
  qualities?: MediaTranscodeVariantResponse[]
}

export type MediaTranscodeVariantResponse = {
  qualityLabel: string
  width?: number | null
  height?: number | null
  bitrateKbps?: number | null
  playlistUrl: string
}

export type CommunityPostResponse = {
  id: number
  circleId: number
  authorId: number
  authorName: string
  topicId?: number | null
  topicName?: string | null
  title: string
  content: string
  contentType: 'ARTICLE' | 'VIDEO' | 'LIVE' | string
  status: string
  reviewedBy?: number | null
  reviewedAt?: string | null
  reviewReason?: string | null
  pinned: boolean
  commentCount: number
  danmakuCount: number
  reactionCount: number
  pigCount: number
  favoriteCount: number
  viewCount: number
  likedByCurrentUser: boolean
  piggedByCurrentUser: boolean
  favoritedByCurrentUser: boolean
  media: MediaAttachmentResponse[]
  lastActivityAt?: string | null
  createdAt: string
  updatedAt?: string | null
}

export type CommunityCommentRequest = {
  content: string
}

export type CommunityCommentResponse = {
  id: number
  postId: number
  authorId: number
  authorName: string
  content: string
  status: string
  reactionCount: number
  createdAt: string
  updatedAt?: string | null
}

export type CommunityDanmakuRequest = {
  content: string
  timeSeconds: number
  color?: string
}

export type CommunityDanmakuResponse = {
  id: number
  postId: number
  authorId: number
  authorName: string
  content: string
  timeSeconds: number
  color: string
  createdAt: string
}

export type CommunityViewReportRequest = {
  playedSeconds: number
  durationSeconds: number
}

export type CommunityViewReportResponse = {
  counted: boolean
  viewCount: number
}

export type CommunityWatchHistoryResponse = {
  post: CommunityPostResponse
  maxProgressSeconds: number
  durationSeconds: number
  firstViewedAt: string
  lastViewedAt: string
}

export type CommunityMemberResponse = {
  userId: number
  username: string
  role: string
  memberStatus: string
  circleId: number
  circleName: string
  circleSlug: string
  displayName?: string | null
  bio?: string | null
  avatarUrl?: string | null
  profileBackgroundUrl?: string | null
  profileBackgroundType?: 'IMAGE' | 'VIDEO' | string | null
  homeBackgroundUrl?: string | null
  homeBackgroundType?: 'IMAGE' | 'VIDEO' | string | null
  skills?: string | null
  githubUrl?: string | null
  websiteUrl?: string | null
  followerCount: number
  followingCount: number
  followedByCurrentUser: boolean
}

export type UserProfileRequest = {
  displayName?: string | null
  bio?: string | null
  avatarUrl?: string | null
  profileBackgroundUrl?: string | null
  profileBackgroundType?: 'IMAGE' | 'VIDEO' | string | null
  homeBackgroundUrl?: string | null
  homeBackgroundType?: 'IMAGE' | 'VIDEO' | string | null
  skills?: string | null
  githubUrl?: string | null
  websiteUrl?: string | null
}

export type CommunityMemberProfileResponse = {
  member: CommunityMemberResponse
  posts: CommunityPostResponse[]
  favoritePosts: CommunityPostResponse[]
  totalPostCount: number
  articleCount: number
  videoCount: number
  liveCount: number
  currentUserProfile: boolean
}

export type ModerationRequest = {
  reason?: string
}

export type BackgroundPlacement = 'HOME' | 'PROFILE'

export type BackgroundMediaType = 'IMAGE' | 'VIDEO'

export type BackgroundPresetResponse = {
  id: number
  placement: BackgroundPlacement
  name: string
  url: string
  mediaType: BackgroundMediaType
  systemProvided: boolean
  sortOrder: number
  createdAt: string
}

export type BackgroundPresetRequest = {
  placement: BackgroundPlacement
  name: string
  url: string
  mediaType: BackgroundMediaType
}

export const communityApi = {
  listBackgroundPresets(placement: BackgroundPlacement) {
    return http.get<unknown, BackgroundPresetResponse[]>('/background-presets', {
      params: { placement },
    })
  },
  createBackgroundPreset(request: BackgroundPresetRequest) {
    return http.post<unknown, BackgroundPresetResponse>('/admin/background-presets', request)
  },
  listFeed() {
    return http.get<unknown, CommunityPostResponse[]>('/community/feed')
  },
  searchPosts(keyword: string) {
    return http.get<unknown, CommunityPostResponse[]>('/community/search', {
      params: { keyword },
    })
  },
  listHotRanking() {
    return http.get<unknown, CommunityPostResponse[]>('/community/rankings/hot')
  },
  listTopics() {
    return http.get<unknown, CommunityTopicResponse[]>('/community/topics')
  },
  createPost(request: CommunityPostRequest) {
    return http.post<unknown, CommunityPostResponse>('/community/posts', request)
  },
  listMySubmissions() {
    return http.get<unknown, CommunityPostResponse[]>('/community/submissions/my')
  },
  getPost(id: number) {
    return http.get<unknown, CommunityPostResponse>(`/community/posts/${id}`)
  },
  updatePost(id: number, request: CommunityPostRequest) {
    return http.put<unknown, CommunityPostResponse>(`/community/posts/${id}`, request)
  },
  deletePost(id: number) {
    return http.delete<unknown, void>(`/community/posts/${id}`)
  },
  listComments(postId: number) {
    return http.get<unknown, CommunityCommentResponse[]>(`/community/posts/${postId}/comments`)
  },
  createComment(postId: number, request: CommunityCommentRequest) {
    return http.post<unknown, CommunityCommentResponse>(
      `/community/posts/${postId}/comments`,
      request,
    )
  },
  deleteComment(commentId: number) {
    return http.delete<unknown, void>(`/community/comments/${commentId}`)
  },
  listDanmaku(postId: number) {
    return http.get<unknown, CommunityDanmakuResponse[]>(`/community/posts/${postId}/danmaku`)
  },
  createDanmaku(postId: number, request: CommunityDanmakuRequest) {
    return http.post<unknown, CommunityDanmakuResponse>(`/community/posts/${postId}/danmaku`, request)
  },
  reportPostView(postId: number, request: CommunityViewReportRequest) {
    return http.post<unknown, CommunityViewReportResponse>(`/community/posts/${postId}/views`, request)
  },
  listMyWatchHistory() {
    return http.get<unknown, CommunityWatchHistoryResponse[]>('/community/views/history/my')
  },
  likePost(postId: number) {
    return http.post<unknown, void>(`/community/posts/${postId}/reactions/like`)
  },
  unlikePost(postId: number) {
    return http.delete<unknown, void>(`/community/posts/${postId}/reactions/like`)
  },
  pigPost(postId: number) {
    return http.post<unknown, UserWalletResponse>(`/community/posts/${postId}/reactions/pig`)
  },
  favoritePost(postId: number) {
    return http.post<unknown, void>(`/community/posts/${postId}/favorites`)
  },
  unfavoritePost(postId: number) {
    return http.delete<unknown, void>(`/community/posts/${postId}/favorites`)
  },
  listMyFavorites() {
    return http.get<unknown, CommunityPostResponse[]>('/community/favorites/my')
  },
  getMe() {
    return http.get<unknown, CommunityMemberResponse>('/community/members/me')
  },
  updateProfile(request: UserProfileRequest) {
    return http.put<unknown, CommunityMemberResponse>('/community/members/me/profile', request)
  },
  listMembers() {
    return http.get<unknown, CommunityMemberResponse[]>('/community/members')
  },
  getMember(userId: number) {
    return http.get<unknown, CommunityMemberResponse>(`/community/members/${userId}`)
  },
  getProfile(userId: number) {
    return http.get<unknown, CommunityMemberProfileResponse>(`/community/profiles/${userId}`)
  },
  followMember(userId: number) {
    return http.post<unknown, CommunityMemberResponse>(`/community/members/${userId}/follow`)
  },
  unfollowMember(userId: number) {
    return http.delete<unknown, CommunityMemberResponse>(`/community/members/${userId}/follow`)
  },
  hidePost(postId: number, request: ModerationRequest) {
    return http.post<unknown, void>(`/admin/community/posts/${postId}/hide`, request)
  },
  listPendingSubmissions() {
    return http.get<unknown, CommunityPostResponse[]>('/admin/community/submissions/pending')
  },
  approveSubmission(postId: number) {
    return http.post<unknown, void>(`/admin/community/posts/${postId}/approve`)
  },
  rejectSubmission(postId: number, request: ModerationRequest) {
    return http.post<unknown, void>(`/admin/community/posts/${postId}/reject`, request)
  },
  restorePost(postId: number, request: ModerationRequest) {
    return http.post<unknown, void>(`/admin/community/posts/${postId}/restore`, request)
  },
  hideComment(commentId: number, request: ModerationRequest) {
    return http.post<unknown, void>(`/admin/community/comments/${commentId}/hide`, request)
  },
  restoreComment(commentId: number, request: ModerationRequest) {
    return http.post<unknown, void>(`/admin/community/comments/${commentId}/restore`, request)
  },
  adminDeletePost(postId: number) {
    return http.delete<unknown, void>(`/admin/community/posts/${postId}`)
  },
  adminDeleteComment(commentId: number) {
    return http.delete<unknown, void>(`/admin/community/comments/${commentId}`)
  },
  adminDeleteDanmaku(danmakuId: number) {
    return http.delete<unknown, void>(`/admin/community/danmaku/${danmakuId}`)
  },
  muteMember(userId: number, request: ModerationRequest) {
    return http.post<unknown, void>(`/admin/community/members/${userId}/mute`, request)
  },
  unmuteMember(userId: number, request: ModerationRequest) {
    return http.post<unknown, void>(`/admin/community/members/${userId}/unmute`, request)
  },
}
