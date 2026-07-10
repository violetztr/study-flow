import axios from 'axios'
import { http } from './http'

export type MediaUploadPrepareRequest = {
  filename: string
  contentType: string
  fileSize: number
}

export type MediaUploadPrepareResponse = {
  mediaFileId: number
  objectKey: string
  uploadUrl: string
  headers: Record<string, string>
  contentType: string
  maxSizeBytes: number
  expiresAt: string
}

export type MediaUploadCompleteResponse = {
  id: number
  fileType: string
  contentType: string
  originalFilename: string
  fileSize: number
  status: string
}

export const mediaApi = {
  prepareUpload(request: MediaUploadPrepareRequest) {
    return http.post<unknown, MediaUploadPrepareResponse>('/media/uploads/presign', request)
  },
  completeUpload(mediaFileId: number) {
    return http.post<unknown, MediaUploadCompleteResponse>(
      `/media/uploads/${mediaFileId}/complete`,
    )
  },
  async uploadToSignedUrl(uploadUrl: string, file: File, headers: Record<string, string>) {
    await axios.put(uploadUrl, file, {
      headers,
      timeout: 120000,
    })
  },
}
