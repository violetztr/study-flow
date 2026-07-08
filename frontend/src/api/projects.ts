import { http } from './http'

export type ProjectStatus = 'ACTIVE' | 'ARCHIVED'

export type ProjectRequest = {
  name: string
  description?: string
  status?: ProjectStatus
}

export type ProjectResponse = {
  id: number
  name: string
  description?: string
  status: ProjectStatus
}

export function listProjects() {
  return http.get<unknown, ProjectResponse[]>('/projects')
}

export function createProject(request: ProjectRequest) {
  return http.post<unknown, ProjectResponse>('/projects', request)
}

export function updateProject(id: number, request: ProjectRequest) {
  return http.put<unknown, ProjectResponse>(`/projects/${id}`, request)
}

export function deleteProject(id: number) {
  return http.delete<unknown, void>(`/projects/${id}`)
}
