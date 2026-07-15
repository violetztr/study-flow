import { ArrowLeftOutlined, UserOutlined, EyeOutlined, ClockCircleOutlined } from '@ant-design/icons'
import { Button, Skeleton, Tag, Typography, Switch } from 'antd'
import { useQuery } from '@tanstack/react-query'
import { useNavigate, useParams } from 'react-router-dom'
import { useEffect, useRef, useState, useCallback } from 'react'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { getStoredUser } from '../api/auth'
import { communityApi, type LiveMessageResponse } from '../api/community'
import LivePlayer from '../components/community/LivePlayer'
import LiveChat from '../components/community/LiveChat'
import LiveDanmaku from '../components/community/LiveDanmaku'

const { Text, Title } = Typography

function LiveRoomPage() {
  const { roomId } = useParams<{ roomId: string }>()
  const navigate = useNavigate()
  const user = getStoredUser()

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

    const token = localStorage.getItem('token')
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
  const flvSrc = room.flvUrl || ''
  const hlsSrc = room.hlsUrl || ''
  const streamType: 'flv' | 'hls' = flvSrc ? 'flv' : 'hls'
  const stompProxy = stompClient ? { send: sendMessage, subscribe } : null

  return (
    <section className="page-section live-room-page">
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

              {room.streamKey && user?.id === room.userId ? (
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
