import { http } from './http'

export type GitHubRepositoryRequest = {
  owner: string
  repo: string
}

export type GitHubRepositoryResponse = {
  id: number
  projectId: number
  owner: string
  repo: string
  htmlUrl?: string
  description?: string
  defaultBranch?: string
  primaryLanguage?: string
  stars?: number
  forks?: number
  openIssues?: number
  pushedAt?: string
  lastSyncedAt?: string
  readmePresent?: boolean
  languagesJson?: string
  latestCommitsJson?: string
}

export function saveGitHubRepository(projectId: number, request: GitHubRepositoryRequest) {
  return http.put<unknown, GitHubRepositoryResponse>(`/projects/${projectId}/github`, request)
}

export function syncGitHubRepository(projectId: number) {
  return http.post<unknown, GitHubRepositoryResponse>(`/projects/${projectId}/github/sync`)
}
