import { http } from './http'

export type NoteBlockType = 'paragraph' | 'heading' | 'todo' | 'quote' | 'code'

export type NoteBlockRequest = {
  type: NoteBlockType
  content?: string
  checked?: boolean
  sortOrder?: number
}

export type NoteBlockResponse = {
  id: number
  type: NoteBlockType
  content?: string
  checked: boolean
  sortOrder: number
}

export type NoteRequest = {
  parentId?: number | null
  title: string
  icon?: string
  favorite?: boolean
  sortOrder?: number
}

export type NoteResponse = {
  id: number
  parentId?: number | null
  title: string
  icon?: string
  favorite: boolean
  archived: boolean
  sortOrder: number
  blocks: NoteBlockResponse[]
}

export function listNotes() {
  return http.get<unknown, NoteResponse[]>('/notes')
}

export function createNote(request: NoteRequest) {
  return http.post<unknown, NoteResponse>('/notes', request)
}

export function getNote(id: number) {
  return http.get<unknown, NoteResponse>(`/notes/${id}`)
}

export function updateNote(id: number, request: NoteRequest) {
  return http.put<unknown, NoteResponse>(`/notes/${id}`, request)
}

export function deleteNote(id: number) {
  return http.delete<unknown, void>(`/notes/${id}`)
}

export function saveNoteBlocks(id: number, blocks: NoteBlockRequest[]) {
  return http.put<unknown, NoteResponse>(`/notes/${id}/blocks`, blocks)
}
