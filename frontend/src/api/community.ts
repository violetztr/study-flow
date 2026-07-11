import { http } from './http'

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
  status: string
  pinned: boolean
  commentCount: number
  reactionCount: number
  pigCount: number
  viewCount: number
  likedByCurrentUser: boolean
  piggedByCurrentUser: boolean
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
  skills?: string | null
  githubUrl?: string | null
  websiteUrl?: string | null
  followerCount: number
  followingCount: number
  followedByCurrentUser: boolean
}

export type ModerationRequest = {
  reason?: string
}

export const communityApi = {
  listFeed() {
    return http.get<unknown, CommunityPostResponse[]>('/community/feed')
  },
  listTopics() {
    return http.get<unknown, CommunityTopicResponse[]>('/community/topics')
  },
  createPost(request: CommunityPostRequest) {
    return http.post<unknown, CommunityPostResponse>('/community/posts', request)
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
  likePost(postId: number) {
    return http.post<unknown, void>(`/community/posts/${postId}/reactions/like`)
  },
  unlikePost(postId: number) {
    return http.delete<unknown, void>(`/community/posts/${postId}/reactions/like`)
  },
  pigPost(postId: number) {
    return http.post<unknown, void>(`/community/posts/${postId}/reactions/pig`)
  },
  getMe() {
    return http.get<unknown, CommunityMemberResponse>('/community/members/me')
  },
  listMembers() {
    return http.get<unknown, CommunityMemberResponse[]>('/community/members')
  },
  getMember(userId: number) {
    return http.get<unknown, CommunityMemberResponse>(`/community/members/${userId}`)
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
