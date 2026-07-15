import { ArrowLeftOutlined, UserOutlined, EyeOutlined, ClockCircleOutlined } from '@ant-design/icons'
import { Button, Skeleton, Tag, Typography } from 'antd'
import { useQuery } from '@tanstack/react-query'
import { useNavigate, useParams } from 'react-router-dom'
import { getStoredUser } from '../api/auth'
import { communityApi } from '../api/community'
import LivePlayer from '../components/community/LivePlayer'

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
            </div>
          </div>
        </header>

        <div className="live-room-layout">
          <main className="live-room-main">
            <div className="live-player-wrapper">
              {isLive ? (
                <LivePlayer
                  src={streamType === 'flv' ? flvSrc : hlsSrc}
                  streamType={streamType}
                  autoPlay
                  muted={false}
                />
              ) : (
                <div className="live-offline-placeholder">
                  <Text type="secondary" style={{ fontSize: 18 }}>
                    {room.status === 'ENDED' ? '直播已结束' : '主播还没开播…'}
                  </Text>
                </div>
              )}
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
                    推流地址: rtmp://服务器IP:1935/live/{room.streamKey}
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

            <div className="side-card">
              <Title level={5}>聊天</Title>
              <Text type="secondary">实时聊天将在下一阶段上线</Text>
            </div>
          </aside>
        </div>
      </div>
    </section>
  )
}

export default LiveRoomPage
