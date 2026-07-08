import { http } from './http'

export type StatisticsOverview = {
  totalTasks: number
  completedTasks: number
  inProgressTasks: number
  overdueTasks: number
  totalEstimatedMinutes: number
  completedEstimatedMinutes: number
}

export function getStatisticsOverview() {
  return http.get<unknown, StatisticsOverview>('/statistics/overview')
}
