import { ArrowLeftOutlined, UserOutlined, EyeOutlined, ClockCircleOutlined, CameraOutlined, UploadOutlined } from '@ant-design/icons'
import { Button, Skeleton, Tag, Typography, Switch, message, Upload } from 'antd'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useNavigate, useParams } from 'react-router-dom'
import { useEffect, useRef, useState, useCallback } from 'react'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { getStoredUser } from '../api/auth'
import { AUTH_TOKEN_KEY } from '../api/http'
import { communityApi, type LiveMessageResponse } from '../api/community'
import { mediaApi } from '../api/media'
import LivePlayer from '../components/community/LivePlayer'
import LiveChat from '../components/community/LiveChat'
import LiveDanmaku from '../components/community/LiveDanmaku'

const { Text, Title } = Typography

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

function LiveRoomPage() {
  const { roomId } = useParams<{ roomId: string }>()
  const navigate = useNavigate()
  const user = getStoredUser()
  const queryClient = useQueryClient()
  const [messageApi, contextHolder] = message.useMessage()
  const videoRef = useRef<HTMLVideoElement | null>(null)
  const [coverUploading, setCoverUploading] = useState(false)

  const roomQuery = useQuery({
    queryKey: ['live-room', roomId],
    queryFn: () => communityApi.getLiveRoom(Number(roomId)),
    enabled: !!roomId,
    refetchInterval: 10_000,
  })

  const [stompClient, setStompClient] = useState<Client | null>(null)
  const [connected, setConnected] = useState(false)
  const [danmakuEnabled, setDanmakuEnabled] = useState(true)
  const clientRef = useRef<Client | null>(null)

  useEffect(() => {
    if (!roomId) return

    const token = localStorage.getItem(AUTH_TOKEN_KEY)
    const wsUrl = `${window.location.protocol}//${window.location.host}/ws`

    const client = new Client({
      webSocketFactory: () => new SockJS(wsUrl),
      connectHeaders: token ? { Authorization: `Bearer ${token}` } : {},
      debug: () => {},
      onConnect: () => {
        setConnected(true)
      },
      onDisconnect: () => {
        setConnected(false)
      },
      onStompError: () => {
        setConnected(false)
      },
      reconnectDelay: 5000,
    })

    client.activate()
    clientRef.current = client
    setStompClient(client)

    return () => {
      client.deactivate()
    }
  }, [roomId])

  // Heartbeat: send every 15s while the room is live
  useEffect(() => {
    const room = roomQuery.data
    if (!room || room.status !== 'LIVE' || !user) return

    const interval = setInterval(() => {
      communityApi.heartbeatLiveRoom(Number(roomId)).catch(() => {
        // silently ignore heartbeat errors
      })
    }, 15_000)

    // Send first heartbeat immediately
    communityApi.heartbeatLiveRoom(Number(roomId)).catch(() => {})

    return () => clearInterval(interval)
  }, [roomId, roomQuery.data, user])

  const updateCoverMutation = useMutation({
    mutationFn: (coverUrl: string) =>
      communityApi.updateLiveRoomCover(Number(roomId), coverUrl),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['live-room', roomId] })
      void messageApi.success('封面已更新')
    },
    onError: () => {
      void messageApi.error('封面更新失败')
    },
  })

  const handleUploadCover = useCallback(
    async (file: File) => {
      setCoverUploading(true)
      try {
        const url = await uploadCover(file)
        await updateCoverMutation.mutateAsync(url)
      } catch {
        void messageApi.error('封面上传失败')
      } finally {
        setCoverUploading(false)
      }
    },
    [updateCoverMutation, messageApi]
  )

  const handleScreenshotCover = useCallback(async () => {
    const video = videoRef.current
    if (!video) {
      void messageApi.warning('视频未加载')
      return
    }
    try {
      const canvas = document.createElement('canvas')
      canvas.width = video.videoWidth || 1280
      canvas.height = video.videoHeight || 720
      const ctx = canvas.getContext('2d')
      if (!ctx) return
      ctx.drawImage(video, 0, 0, canvas.width, canvas.height)

      const blob = await new Promise<Blob | null>((resolve) =>
        canvas.toBlob(resolve, 'image/jpeg', 0.85)
      )
      if (!blob) {
        void messageApi.error('截图失败')
        return
      }

      setCoverUploading(true)
      try {
        const url = await uploadCover(new File([blob], `live-cover-${roomId}.jpg`, { type: 'image/jpeg' }))
        await updateCoverMutation.mutateAsync(url)
      } catch {
        void messageApi.error('封面上传失败')
      } finally {
        setCoverUploading(false)
      }
    } catch {
      void messageApi.error('截图失败')
    }
  }, [roomId, updateCoverMutation, messageApi])

  const sendMessage = useCallback(
    (destination: string, body: Record<string, unknown>) => {
      if (!clientRef.current || !clientRef.current.connected) return
      clientRef.current.publish({ destination, body: JSON.stringify(body) })
    },
    []
  )

  const subscribe = useCallback(
    (destination: string, callback: (message: LiveMessageResponse) => void) => {
      if (!clientRef.current) return () => {}
      const sub = clientRef.current.subscribe(destination, (msg) => {
        try {
          const body = JSON.parse(msg.body) as LiveMessageResponse
          callback(body)
        } catch {
          // ignore parse errors
        }
      })
      return () => sub.unsubscribe()
    },
    []
  )

  if (roomQuery.isLoading) {
    return (
      <section className="page-section">
        <Skeleton active />
      </section>
    )
  }

  const room = roomQuery.data
  if (!room) {
    return (
      <section className="page-section">
        <div className="channel-empty-card">
          <strong>直播间不存在</strong>
          <Button type="link" onClick={() => navigate('/circle')}>
            返回首页
          </Button>
        </div>
      </section>
    )
  }

  const isLive = room.status === 'LIVE'
  const isHost = user?.id === room.userId
  const flvSrc = room.flvUrl || ''
  const hlsSrc = room.hlsUrl || ''
  const streamType: 'flv' | 'hls' = flvSrc ? 'flv' : 'hls'
  const stompProxy = stompClient ? { send: sendMessage, subscribe } : null

  return (
    <section className="page-section live-room-page">
      {contextHolder}
      <div className="live-room-shell">
        <header className="live-room-topbar">
          <Button
            type="text"
            icon={<ArrowLeftOutlined />}
            onClick={() => navigate('/circle')}
          >
            返回
          </Button>
          <div className="live-room-title-section">
            <Title level={5} style={{ margin: 0 }}>
              {room.title}
            </Title>
            <div className="live-room-meta">
              <Text type="secondary">
                <UserOutlined /> {room.username || '主播'}
              </Text>
              {isLive ? (
                <Tag color="red">直播中</Tag>
              ) : room.status === 'ENDED' ? (
                <Tag>已结束</Tag>
              ) : (
                <Tag color="orange">等待中</Tag>
              )}
              <Text type="secondary">
                <EyeOutlined /> {room.currentViewers} 人在看
              </Text>
              {room.startedAt && isLive ? (
                <Text type="secondary">
                  <ClockCircleOutlined /> 开播于{' '}
                  {new Date(room.startedAt).toLocaleTimeString()}
                </Text>
              ) : null}
              {isLive ? (
                <span className="danmaku-toggle">
                  <Text type="secondary" style={{ fontSize: 12 }}>弹幕</Text>
                  <Switch size="small" checked={danmakuEnabled} onChange={setDanmakuEnabled} />
                </span>
              ) : null}
            </div>
          </div>
        </header>

        <div className="live-room-layout">
          <main className="live-room-main">
            <div className="live-player-wrapper">
              {isLive ? (
                <>
                  <LivePlayer
                    src={streamType === 'flv' ? flvSrc : hlsSrc}
                    streamType={streamType}
                    autoPlay
                    muted={false}
                    onVideoRef={(el) => { videoRef.current = el }}
                  />
                  <LiveDanmaku
                    roomId={Number(roomId)}
                    stompClient={stompProxy}
                    connected={connected}
                    enabled={danmakuEnabled}
                  />
                </>
              ) : (
                <div className="live-offline-placeholder">
                  <Text type="secondary" style={{ fontSize: 18 }}>
                    {room.status === 'ENDED' ? '直播已结束' : '主播还没开播…'}
                  </Text>
                </div>
              )}
            </div>
            <div className="live-chat-area">
              <LiveChat
                roomId={Number(roomId)}
                stompClient={stompProxy}
                connected={connected}
              />
            </div>
          </main>

          <aside className="live-room-sidebar">
            <div className="side-card">
              <Title level={5}>主播信息</Title>
              <div className="host-info">
                <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                  {room.userAvatarUrl ? (
                    <img
                      src={room.userAvatarUrl}
                      alt={room.username || '主播'}
                      style={{
                        width: 40,
                        height: 40,
                        borderRadius: '50%',
                        objectFit: 'cover',
                      }}
                    />
                  ) : (
                    <div
                      style={{
                        width: 40,
                        height: 40,
                        borderRadius: '50%',
                        background: '#f0f0f0',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                      }}
                    >
                      <UserOutlined />
                    </div>
                  )}
                  <div>
                    <Text strong>{room.username || '主播'}</Text>
                  </div>
                </div>
                {room.topicName ? <Tag>{room.topicName}</Tag> : null}
              </div>

              {room.streamKey && isHost ? (
                <div style={{ marginTop: 12 }}>
                  <Text type="secondary" style={{ fontSize: 12 }}>
                    推流密钥: {room.streamKey}
                  </Text>
                  <br />
                  <Text type="secondary" style={{ fontSize: 12 }}>
                    推流地址: rtmp://{window.location.hostname}:1935/live/{room.streamKey}
                  </Text>
                </div>
              ) : null}
            </div>

            {isHost ? (
              <div className="side-card">
                <Title level={5}>封面设置</Title>
                <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
                  {room.coverUrl ? (
                    <img
                      src={room.coverUrl}
                      alt="直播封面"
                      style={{ width: '100%', borderRadius: 8, objectFit: 'cover', maxHeight: 120 }}
                    />
                  ) : (
                    <Text type="secondary" style={{ fontSize: 12 }}>
                      暂未设置封面，直播卡片将显示为空白
                    </Text>
                  )}
                  <Upload
                    accept="image/*"
                    showUploadList={false}
                    beforeUpload={(file) => {
                      handleUploadCover(file as File)
                      return false
                    }}
                  >
                    <Button
                      icon={<UploadOutlined />}
                      size="small"
                      loading={coverUploading}
                      style={{ width: '100%' }}
                    >
                      上传封面图片
                    </Button>
                  </Upload>
                  {isLive ? (
                    <Button
                      icon={<CameraOutlined />}
                      size="small"
                      loading={coverUploading}
                      onClick={handleScreenshotCover}
                    >
                      截取当前画面作为封面
                    </Button>
                  ) : null}
                </div>
              </div>
            ) : null}

            <div className="side-card">
              <Title level={5}>数据</Title>
              <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
                <Text>
                  <EyeOutlined /> 当前观看: {room.currentViewers}
                </Text>
                <Text type="secondary">
                  峰值: {room.peakViewers} · 累计: {room.totalViews}
                </Text>
              </div>
            </div>
          </aside>
        </div>
      </div>
    </section>
  )
}

export default LiveRoomPage
