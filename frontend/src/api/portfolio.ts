import type { GitHubRepositoryResponse } from './github'
import { http } from './http'
import type { ProjectStatus } from './projects'
import type { ProjectTechStackResponse } from './projectHub'

export type PublicPortfolioProjectResponse = {
  projectId: number
  name: string
  description?: string
  status: ProjectStatus
  slug: string
  featured: boolean
  displayOrder: number
  publicSummary?: string
  headline?: string
  productionUrl?: string
  apiDocUrl?: string
  databaseDocUrl?: string
  architectureSummary?: string
  interviewHighlights?: string
  coverImageUrl?: string
  githubRepository?: GitHubRepositoryResponse
  techStacks: ProjectTechStackResponse[]
}

export function listPublicPortfolioProjects() {
  return http.get<unknown, PublicPortfolioProjectResponse[]>('/portfolio/projects')
}

export function getPublicPortfolioProject(slug: string) {
  return http.get<unknown, PublicPortfolioProjectResponse>(`/portfolio/projects/${slug}`)
}
