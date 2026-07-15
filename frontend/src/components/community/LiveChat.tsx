import { useEffect, useRef, useState, useCallback } from 'react'
import { Input, Button, Space, Typography } from 'antd'
import { SendOutlined } from '@ant-design/icons'
import type { LiveMessageResponse } from '../../api/community'

const { Text } = Typography

type LiveChatProps = {
  roomId: number
  stompClient: StompClient | null
  connected: boolean
}

export type StompClient = {
  send: (destination: string, body: Record<string, unknown>) => void
  subscribe: (destination: string, callback: (message: LiveMessageResponse) => void) => () => void
}

function LiveChat({ roomId, stompClient, connected }: LiveChatProps) {
  const [messages, setMessages] = useState<LiveMessageResponse[]>([])
  const [input, setInput] = useState('')
  const chatEndRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!stompClient || !connected) return

    const unsub = stompClient.subscribe(`/topic/live/${roomId}`, (msg) => {
      setMessages((prev) => [...prev.slice(-200), msg])
    })

    return () => {
      unsub()
    }
  }, [roomId, stompClient, connected])

  useEffect(() => {
    chatEndRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages])

  const handleSend = useCallback(() => {
    const trimmed = input.trim()
    if (!trimmed || !stompClient || !connected) return

    stompClient.send(`/app/live/${roomId}/chat`, {
      content: trimmed,
      color: '#ffffff',
      type: 'CHAT',
    })
    setInput('')
  }, [input, stompClient, connected, roomId])

  return (
    <div className="live-chat">
      <div className="live-chat-messages">
        {messages.length === 0 ? (
          <Text type="secondary" style={{ padding: 16, display: 'block', textAlign: 'center' }}>
            暂无消息，来发一条吧～
          </Text>
        ) : (
          messages.map((msg, idx) => (
            <div className="live-chat-item" key={msg.id || idx}>
              <Text strong style={{ color: msg.color, fontSize: 12 }}>
                {msg.username || '匿名'}
              </Text>
              <Text style={{ fontSize: 13, marginLeft: 4 }}>{msg.content}</Text>
            </div>
          ))
        )}
        <div ref={chatEndRef} />
      </div>
      <div className="live-chat-input">
        <Space.Compact style={{ width: '100%' }}>
          <Input
            placeholder={connected ? '发送聊天消息…' : '连接中…'}
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onPressEnter={handleSend}
            disabled={!connected}
            maxLength={500}
          />
          <Button
            icon={<SendOutlined />}
            onClick={handleSend}
            disabled={!connected || !input.trim()}
          />
        </Space.Compact>
      </div>
    </div>
  )
}

export default LiveChat
