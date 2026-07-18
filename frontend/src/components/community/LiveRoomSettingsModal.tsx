import { Modal, Input, Button, Upload, message } from 'antd'
import { UploadOutlined } from '@ant-design/icons'
import { useState, useCallback } from 'react'
import { mediaApi } from '../../api/media'
import { communityApi, type LiveRoomResponse } from '../../api/community'

type LiveRoomSettingsModalProps = {
  open: boolean
  /** Existing room data for edit mode; omit for create mode */
  room?: LiveRoomResponse | null
  onClose: () => void
  /** Called after create success (only in create mode) */
  onCreated?: (room: LiveRoomResponse) => void
  /** Called after edit success (only in edit mode) */
  onUpdated?: () => void
}

async function uploadCover(file: File) {
  const prepareRes = await mediaApi.prepareUpload({
    filename: file.name,
    contentType: file.type,
    fileSize: file.size,
  })
  await mediaApi.uploadToSignedUrl(prepareRes.uploadUrl, file, prepareRes.headers)
  const completeRes = await mediaApi.completeUpload(prepareRes.mediaFileId)
  return completeRes.url
}

export default function LiveRoomSettingsModal({
  open,
  room,
  onClose,
  onCreated,
  onUpdated,
}: LiveRoomSettingsModalProps) {
  const isEdit = !!room
  const [title, setTitle] = useState(room?.title ?? '直播中')
  const [coverUrl, setCoverUrl] = useState<string | null>(room?.coverUrl ?? null)
  const [uploading, setUploading] = useState(false)
  const [saving, setSaving] = useState(false)

  const reset = useCallback(() => {
    setTitle(room?.title ?? '直播中')
    setCoverUrl(room?.coverUrl ?? null)
  }, [room])

  const handleUpload = useCallback(
    async (file: File) => {
      setUploading(true)
      try {
        const url = await uploadCover(file)
        setCoverUrl(url)
      } catch {
        void message.error('封面上传失败')
      } finally {
        setUploading(false)
      }
      return false
    },
    [],
  )

  const handleSave = useCallback(async () => {
    const trimmed = title.trim()
    if (!trimmed) {
      void message.warning('请输入直播标题')
      return
    }
    setSaving(true)
    try {
      if (isEdit) {
        await communityApi.updateLiveRoom(room!.id, { title: trimmed, coverUrl })
        onUpdated?.()
        void message.success('已更新')
      } else {
        const created = await communityApi.createLiveRoom({ title: trimmed, coverUrl })
        onCreated?.(created)
        void message.success('直播间已创建')
      }
      onClose()
    } catch {
      void message.error('操作失败，请稍后重试')
    } finally {
      setSaving(false)
    }
  }, [title, coverUrl, isEdit, room, onCreated, onUpdated, onClose])

  const handleOpenChange = useCallback(
    (visible: boolean) => {
      if (!visible) {
        reset()
        onClose()
      }
    },
    [reset, onClose],
  )

  return (
    <Modal
      title={isEdit ? '编辑直播间' : '发起直播'}
      open={open}
      onCancel={() => handleOpenChange(false)}
      onOk={handleSave}
      okText={isEdit ? '保存' : '开始直播'}
      cancelText="取消"
      confirmLoading={saving}
      destroyOnClose
      width={420}
    >
      <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
        <div>
          <label style={{ display: 'block', marginBottom: 6, fontWeight: 500, fontSize: 13 }}>
            直播标题
          </label>
          <Input
            placeholder="给直播取个名字吧"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            maxLength={200}
            showCount
          />
        </div>

        <div>
          <label style={{ display: 'block', marginBottom: 6, fontWeight: 500, fontSize: 13 }}>
            封面图片（可选）
          </label>
          {coverUrl ? (
            <div style={{ position: 'relative', marginBottom: 8 }}>
              <img
                src={coverUrl}
                alt="封面预览"
                style={{
                  width: '100%',
                  maxHeight: 180,
                  objectFit: 'cover',
                  borderRadius: 8,
                }}
              />
              <Button
                size="small"
                danger
                style={{ position: 'absolute', top: 6, right: 6, opacity: 0.9 }}
                onClick={() => setCoverUrl(null)}
              >
                移除
              </Button>
            </div>
          ) : null}
          <Upload
            accept="image/*"
            showUploadList={false}
            beforeUpload={handleUpload}
          >
            <Button icon={<UploadOutlined />} loading={uploading} block>
              {coverUrl ? '更换封面' : '上传封面'}
            </Button>
          </Upload>
        </div>
      </div>
    </Modal>
  )
}
