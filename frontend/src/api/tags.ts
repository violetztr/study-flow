import { http } from './http'

export type TagRequest = {
  name: string
  color?: string
}

export type TagResponse = {
  id: number
  name: string
  color: string
}

export function listTags() {
  return http.get<unknown, TagResponse[]>('/tags')
}

export function createTag(request: TagRequest) {
  return http.post<unknown, TagResponse>('/tags', request)
}
