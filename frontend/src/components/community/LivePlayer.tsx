import { useEffect, useRef, type SyntheticEvent } from 'react'

type LivePlayerProps = {
  src: string
  streamType: 'flv' | 'hls'
  autoPlay?: boolean
  muted?: boolean
  onVideoRef?: (el: HTMLVideoElement | null) => void
}

/**
 * Ruru live-stream player. Favors HTTP-FLV (~2-3s latency) with HLS fallback.
 *
 * Uses mpegts.js for FLV (lighter than flv.js, same API) and hls.js for HLS.
 */
function LivePlayer({ src, streamType, autoPlay = true, muted = false, onVideoRef }: LivePlayerProps) {
  const videoRef = useRef<HTMLVideoElement>(null)

  useEffect(() => {
    onVideoRef?.(videoRef.current)
  }, [onVideoRef])

  useEffect(() => {
    const videoElement = videoRef.current
    if (!videoElement) {
      return
    }

    let destroyPlayer: (() => void) | null = null
    let cancelled = false

    async function init() {
      if (streamType === 'flv') {
        try {
          const mpegts = await import('mpegts.js')
          if (cancelled || !videoElement) {
            return
          }
          const player = mpegts.default.createPlayer({
            type: 'flv',
            isLive: true,
            url: src,
          })
          player.attachMediaElement(videoElement)
          player.load()
          if (autoPlay) {
            videoElement.play().catch(() => {
              /* autoplay may be blocked */
            })
          }
          destroyPlayer = () => {
            player.destroy()
          }
        } catch {
          /* mpegts.js not available — fallback handled upstream */
        }
        return
      }

      if (streamType === 'hls') {
        try {
          const Hls = (await import('hls.js')).default
          if (cancelled || !videoElement) {
            return
          }
          if (Hls.isSupported()) {
            const hls = new Hls()
            hls.loadSource(src)
            hls.attachMedia(videoElement)
            if (autoPlay) {
              videoElement.play().catch(() => {
                /* autoplay blocked */
              })
            }
            destroyPlayer = () => {
              hls.destroy()
            }
          } else if (videoElement.canPlayType('application/vnd.apple.mpegurl')) {
            videoElement.src = src
            if (autoPlay) {
              videoElement.play().catch(() => {
                /* autoplay blocked */
              })
            }
          }
        } catch {
          /* hls.js not available */
        }
      }
    }

    init()

    return () => {
      cancelled = true
      destroyPlayer?.()
    }
  }, [src, streamType, autoPlay])

  function handleError(_event: SyntheticEvent<HTMLVideoElement>) {
    // Error handling
  }

  return (
    <video
      ref={videoRef}
      controls
      muted={muted}
      playsInline
      style={{ width: '100%', background: '#000' }}
      onError={handleError}
    />
  )
}

export default LivePlayer
