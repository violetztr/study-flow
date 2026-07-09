import { http } from './http'

export type DailyPlanStatus = 'TODO' | 'DOING' | 'DONE'

export type DailyPlanRequest = {
  planDate: string
  title: string
  description?: string
  status?: DailyPlanStatus
}

export type DailyPlanResponse = {
  id: number
  planDate: string
  title: string
  description?: string
  status: DailyPlanStatus
}

export type JournalRequest = {
  journalDate: string
  mood?: string
  content?: string
}

export type JournalResponse = {
  id: number
  journalDate: string
  mood?: string
  content?: string
}

export type HabitRequest = {
  name: string
  description?: string
}

export type HabitResponse = {
  id: number
  name: string
  description?: string
  active: boolean
}

export type HabitRecordRequest = {
  recordDate: string
  completed: boolean
}

export type HabitRecordResponse = {
  id: number
  habitId: number
  recordDate: string
  completed: boolean
}

export function listDailyPlans(date: string) {
  return http.get<unknown, DailyPlanResponse[]>('/daily/plans', { params: { date } })
}

export function createDailyPlan(request: DailyPlanRequest) {
  return http.post<unknown, DailyPlanResponse>('/daily/plans', request)
}

export function updateDailyPlan(id: number, request: DailyPlanRequest) {
  return http.put<unknown, DailyPlanResponse>(`/daily/plans/${id}`, request)
}

export function getJournal(date: string) {
  return http.get<unknown, JournalResponse | null>('/daily/journal', { params: { date } })
}

export function saveJournal(request: JournalRequest) {
  return http.put<unknown, JournalResponse>('/daily/journal', request)
}

export function listHabits() {
  return http.get<unknown, HabitResponse[]>('/daily/habits')
}

export function createHabit(request: HabitRequest) {
  return http.post<unknown, HabitResponse>('/daily/habits', request)
}

export function saveHabitRecord(habitId: number, request: HabitRecordRequest) {
  return http.put<unknown, HabitRecordResponse>(`/daily/habits/${habitId}/records`, request)
}
