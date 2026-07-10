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
  mediaFileIds?: number[]
}

export type MediaAttachmentResponse = {
  id: number
  fileType: string
  contentType: string
  originalFilename: string
  fileSize: number
  url: string
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
  viewCount: number
  likedByCurrentUser: boolean
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
  likePost(postId: number) {
    return http.post<unknown, void>(`/community/posts/${postId}/reactions/like`)
  },
  unlikePost(postId: number) {
    return http.delete<unknown, void>(`/community/posts/${postId}/reactions/like`)
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
  muteMember(userId: number, request: ModerationRequest) {
    return http.post<unknown, void>(`/admin/community/members/${userId}/mute`, request)
  },
  unmuteMember(userId: number, request: ModerationRequest) {
    return http.post<unknown, void>(`/admin/community/members/${userId}/unmute`, request)
  },
}
