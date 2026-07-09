import { http } from './http'

export type ProjectProfileRequest = {
  headline?: string
  productionUrl?: string
  apiDocUrl?: string
  databaseDocUrl?: string
  architectureSummary?: string
  interviewHighlights?: string
  coverImageUrl?: string
}

export type ProjectProfileResponse = ProjectProfileRequest & {
  id: number | null
  projectId: number
}

export type ProjectTechStackCategory =
  | 'FRONTEND'
  | 'BACKEND'
  | 'DATABASE'
  | 'DEPLOYMENT'
  | 'TOOLING'
  | 'OTHER'

export type ProjectTechStackRequest = {
  name: string
  category?: ProjectTechStackCategory
  sortOrder?: number
}

export type ProjectTechStackResponse = {
  id: number
  name: string
  category: ProjectTechStackCategory
  sortOrder: number
}

export type PortfolioProjectRequest = {
  slug: string
  publicVisible?: boolean
  featured?: boolean
  displayOrder?: number
  publicSummary?: string
}

export type PortfolioProjectResponse = PortfolioProjectRequest & {
  id: number
  projectId: number
  publicVisible: boolean
  featured: boolean
  displayOrder: number
}

export function getProjectProfile(projectId: number) {
  return http.get<unknown, ProjectProfileResponse>(`/projects/${projectId}/profile`)
}

export function saveProjectProfile(projectId: number, request: ProjectProfileRequest) {
  return http.put<unknown, ProjectProfileResponse>(`/projects/${projectId}/profile`, request)
}

export function saveProjectTechStacks(
  projectId: number,
  request: ProjectTechStackRequest[],
) {
  return http.put<unknown, ProjectTechStackResponse[]>(
    `/projects/${projectId}/tech-stacks`,
    request,
  )
}

export function savePortfolioSettings(projectId: number, request: PortfolioProjectRequest) {
  return http.put<unknown, PortfolioProjectResponse>(`/projects/${projectId}/portfolio`, request)
}
