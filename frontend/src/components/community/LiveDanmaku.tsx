import { useEffect, useRef, useState } from 'react'
import type { LiveMessageResponse } from '../../api/community'

type LiveDanmakuProps = {
  roomId: number
  stompClient: StompClient | null
  connected: boolean
  enabled: boolean
}

export type StompClient = {
  subscribe: (destination: string, callback: (message: LiveMessageResponse) => void) => () => void
}

function LiveDanmaku({ roomId, stompClient, connected, enabled }: LiveDanmakuProps) {
  const [activeDanmaku, setActiveDanmaku] = useState<Array<LiveMessageResponse & { key: number }>>([])
  const keyRef = useRef(0)

  useEffect(() => {
    if (!stompClient || !connected) return

    const unsub = stompClient.subscribe(`/topic/live/${roomId}`, (msg) => {
      if (msg.type !== 'DANMAKU' || !enabled) return
      const key = keyRef.current++
      setActiveDanmaku((prev) => [...prev, { ...msg, key }])
      setTimeout(() => {
        setActiveDanmaku((prev) => prev.filter((d) => d.key !== key))
      }, 8000)
    })

    return () => {
      unsub()
    }
  }, [roomId, stompClient, connected, enabled])

  if (!enabled) return null

  return (
    <div className="live-danmaku-layer" aria-hidden>
      {activeDanmaku.map((danmaku) => (
        <div
          key={danmaku.key}
          className="live-danmaku-item"
          style={{
            top: (danmaku.key % 300) + 40,
            color: danmaku.color || '#ffffff',
            animationDuration: '8s',
          }}
        >
          <span className="live-danmaku-author">{danmaku.username || '匿名'}: </span>
          {danmaku.content}
        </div>
      ))}
    </div>
  )
}

export default LiveDanmaku
