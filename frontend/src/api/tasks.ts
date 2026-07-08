import { http } from './http'

export type TaskStatus = 'PENDING' | 'IN_PROGRESS' | 'DONE'
export type TaskPriority = 'LOW' | 'MEDIUM' | 'HIGH'

export type TaskRequest = {
  projectId: number
  title: string
  description?: string
  status?: TaskStatus
  priority?: TaskPriority
  deadline?: string | null
  estimatedMinutes?: number | null
  tagIds?: number[]
}

export type TaskResponse = {
  id: number
  projectId: number
  title: string
  description?: string
  status: TaskStatus
  priority: TaskPriority
  deadline?: string | null
  estimatedMinutes?: number | null
  completedAt?: string | null
  tagIds: number[]
}

export type TaskQuery = {
  projectId?: number
  status?: TaskStatus
  priority?: TaskPriority
  keyword?: string
}

export function listTasks(params?: TaskQuery) {
  return http.get<unknown, TaskResponse[]>('/tasks', { params })
}

export function createTask(request: TaskRequest) {
  return http.post<unknown, TaskResponse>('/tasks', request)
}

export function updateTask(id: number, request: TaskRequest) {
  return http.put<unknown, TaskResponse>(`/tasks/${id}`, request)
}

export function deleteTask(id: number) {
  return http.delete<unknown, void>(`/tasks/${id}`)
}
