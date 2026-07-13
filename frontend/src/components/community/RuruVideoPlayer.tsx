import Hls from 'hls.js'
import { useEffect, useRef, type SyntheticEvent } from 'react'

type RuruVideoPlayerProps = {
  src: string
  poster?: string
  autoPlay?: boolean
  muted?: boolean
  onTimeUpdate?: (event: SyntheticEvent<HTMLVideoElement>) => void
  onPlay?: () => void
  onPause?: () => void
  onEnded?: () => void
}

function isHlsSource(src: string) {
  return src.includes('.m3u8')
}

function RuruVideoPlayer({
  src,
  poster,
  autoPlay = false,
  muted = false,
  onTimeUpdate,
  onPlay,
  onPause,
  onEnded,
}: RuruVideoPlayerProps) {
  const videoRef = useRef<HTMLVideoElement | null>(null)

  useEffect(() => {
    const video = videoRef.current
    if (!video) {
      return undefined
    }

    function tryAutoPlay() {
      if (!autoPlay) {
        return
      }
      video!.muted = muted
      void video!.play().catch(() => {
        // Some browsers block autoplay; native controls remain available.
      })
    }

    video.removeAttribute('src')
    video.load()

    if (!src) {
      return undefined
    }

    if (!isHlsSource(src)) {
      video.src = src
      video.load()
      tryAutoPlay()
      return undefined
    }

    if (video.canPlayType('application/vnd.apple.mpegurl')) {
      video.src = src
      video.load()
      tryAutoPlay()
      return undefined
    }

    if (!Hls.isSupported()) {
      video.src = src
      video.load()
      tryAutoPlay()
      return undefined
    }

    const hls = new Hls({
      enableWorker: true,
      lowLatencyMode: false,
    })
    hls.loadSource(src)
    hls.attachMedia(video)
    hls.on(Hls.Events.MANIFEST_PARSED, tryAutoPlay)

    return () => {
      hls.destroy()
    }
  }, [autoPlay, muted, src])

  return (
    <video
      ref={videoRef}
      autoPlay={autoPlay}
      controls
      muted={muted}
      playsInline
      preload={autoPlay ? 'auto' : 'metadata'}
      poster={poster}
      onTimeUpdate={onTimeUpdate}
      onPlay={onPlay}
      onPause={onPause}
      onEnded={onEnded}
    />
  )
}

export default RuruVideoPlayer
