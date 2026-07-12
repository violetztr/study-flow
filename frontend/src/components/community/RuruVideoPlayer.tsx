import Hls from 'hls.js'
import { useEffect, useRef, type SyntheticEvent } from 'react'

type RuruVideoPlayerProps = {
  src: string
  poster?: string
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

    video.removeAttribute('src')
    video.load()

    if (!isHlsSource(src)) {
      video.src = src
      video.load()
      return undefined
    }

    if (video.canPlayType('application/vnd.apple.mpegurl')) {
      video.src = src
      video.load()
      return undefined
    }

    if (!Hls.isSupported()) {
      video.src = src
      video.load()
      return undefined
    }

    const hls = new Hls({
      enableWorker: true,
      lowLatencyMode: false,
    })
    hls.loadSource(src)
    hls.attachMedia(video)

    return () => {
      hls.destroy()
    }
  }, [src])

  return (
    <video
      ref={videoRef}
      controls
      preload="metadata"
      poster={poster}
      onTimeUpdate={onTimeUpdate}
      onPlay={onPlay}
      onPause={onPause}
      onEnded={onEnded}
    />
  )
}

export default RuruVideoPlayer
