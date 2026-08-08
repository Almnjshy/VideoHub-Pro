'use client';

import { useState, useEffect, useRef } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  X, Play, Pause, Volume2, VolumeX, Maximize, Minimize,
  SkipBack, SkipForward, Settings, Share2, Download,
  PictureInPicture, Subtitles, Gauge,
} from 'lucide-react';
import { cn } from '@/lib/utils';

interface MediaPlayerState {
  isOpen: boolean;
  url: string;
  title: string;
  type: 'video' | 'audio';
  thumbnailUrl?: string;
}

// Global state for media player
let playerState: MediaPlayerState = { isOpen: false, url: '', title: '', type: 'video' };
const listeners = new Set<() => void>();

export function openMediaPlayer(opts: Omit<MediaPlayerState, 'isOpen'>) {
  playerState = { ...opts, isOpen: true };
  listeners.forEach((l) => l());
}

export function closeMediaPlayer() {
  playerState = { ...playerState, isOpen: false };
  listeners.forEach((l) => l());
}

function usePlayerState() {
  const [, force] = useState(0);
  useEffect(() => {
    const l = () => force((x) => x + 1);
    listeners.add(l);
    return () => { listeners.delete(l); };
  }, []);
  return playerState;
}

export function MediaPlayer() {
  const state = usePlayerState();
  const videoRef = useRef<HTMLVideoElement>(null);
  const audioRef = useRef<HTMLAudioElement>(null);
  const [playing, setPlaying] = useState(false);
  const [currentTime, setCurrentTime] = useState(0);
  const [duration, setDuration] = useState(0);
  const [volume, setVolume] = useState(1);
  const [muted, setMuted] = useState(false);
  const [playbackRate, setPlaybackRate] = useState(1);
  const [showSettings, setShowSettings] = useState(false);
  const [pip, setPip] = useState(false);

  // Sync playback rate
  useEffect(() => {
    const el = state.type === 'video' ? videoRef.current : audioRef.current;
    if (el) el.playbackRate = playbackRate;
  }, [playbackRate, state.type]);

  // Sync volume
  useEffect(() => {
    const el = state.type === 'video' ? videoRef.current : audioRef.current;
    if (el) {
      el.volume = volume;
      el.muted = muted;
    }
  }, [volume, muted, state.type]);

  const togglePlay = () => {
    const el = state.type === 'video' ? videoRef.current : audioRef.current;
    if (!el) return;
    if (playing) {
      el.pause();
      setPlaying(false);
    } else {
      void el.play();
      setPlaying(true);
    }
  };

  const handleTimeUpdate = () => {
    const el = state.type === 'video' ? videoRef.current : audioRef.current;
    if (el) {
      setCurrentTime(el.currentTime);
      setDuration(el.duration || 0);
    }
  };

  const seek = (time: number) => {
    const el = state.type === 'video' ? videoRef.current : audioRef.current;
    if (el) {
      el.currentTime = time;
      setCurrentTime(time);
    }
  };

  const seekBy = (delta: number) => {
    seek(Math.max(0, Math.min(duration, currentTime + delta)));
  };

  const togglePip = async () => {
    const video = videoRef.current;
    if (!video) return;
    try {
      if (pip) {
        await document.exitPictureInPicture();
        setPip(false);
      } else {
        await video.requestPictureInPicture();
        setPip(true);
      }
    } catch {
      // ignore
    }
  };

  if (!state.isOpen) return null;

  const progress = duration > 0 ? currentTime / duration : 0;

  return (
    <AnimatePresence>
      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        exit={{ opacity: 0 }}
        className="fixed inset-0 z-[80] flex items-center justify-center bg-black/90 backdrop-blur-md"
        onClick={closeMediaPlayer}
      >
        <motion.div
          initial={{ scale: 0.9, opacity: 0 }}
          animate={{ scale: 1, opacity: 1 }}
          exit={{ scale: 0.9, opacity: 0 }}
          onClick={(e) => e.stopPropagation()}
          className="relative flex w-full max-w-4xl flex-col"
        >
          {/* Close button */}
          <button
            onClick={closeMediaPlayer}
            className="absolute -top-12 right-0 rounded-lg p-2 text-white/80 hover:bg-white/10 hover:text-white"
            aria-label="إغلاق"
          >
            <X className="h-6 w-6" />
          </button>

          {/* Title */}
          <div className="mb-2 px-2">
            <h3 className="line-clamp-1 text-sm font-semibold text-white">{state.title}</h3>
          </div>

          {/* Media element */}
          {state.type === 'video' ? (
            <video
              ref={videoRef}
              src={state.url}
              poster={state.thumbnailUrl}
              onTimeUpdate={handleTimeUpdate}
              onLoadedMetadata={handleTimeUpdate}
              onPlay={() => setPlaying(true)}
              onPause={() => setPlaying(false)}
              className="aspect-video w-full rounded-xl bg-black"
              playsInline
              controls={false}
            />
          ) : (
            <div className="relative aspect-video w-full overflow-hidden rounded-xl bg-gradient-to-br from-violet-500 to-purple-800">
              {state.thumbnailUrl && (
                <img src={state.thumbnailUrl} alt={state.title} className="h-full w-full object-cover opacity-60" />
              )}
              <div className="absolute inset-0 flex items-center justify-center">
                <div className="text-center">
                  <div className="mx-auto mb-3 h-20 w-20 rounded-full bg-white/10 backdrop-blur" />
                  <div className="text-sm font-semibold text-white">{state.title}</div>
                </div>
              </div>
              <audio
                ref={audioRef}
                src={state.url}
                onTimeUpdate={handleTimeUpdate}
                onLoadedMetadata={handleTimeUpdate}
                onPlay={() => setPlaying(true)}
                onPause={() => setPlaying(false)}
              />
            </div>
          )}

          {/* Controls */}
          <div className="mt-3 space-y-2">
            {/* Progress bar */}
            <div className="flex items-center gap-2">
              <span className="font-mono text-[10px] text-white/70">{formatTime(currentTime)}</span>
              <div
                className="relative h-1.5 flex-1 cursor-pointer rounded-full bg-white/20"
                onClick={(e) => {
                  const rect = e.currentTarget.getBoundingClientRect();
                  const x = (e.clientX - rect.left) / rect.width;
                  seek(x * duration);
                }}
              >
                <div
                  className="absolute inset-y-0 right-0 rounded-full bg-amber-400"
                  style={{ width: `${progress * 100}%` }}
                />
                <div
                  className="absolute top-1/2 h-3 w-3 -translate-y-1/2 rounded-full bg-amber-400 shadow-lg"
                  style={{ right: `calc(${progress * 100}% - 6px)` }}
                />
              </div>
              <span className="font-mono text-[10px] text-white/70">{formatTime(duration)}</span>
            </div>

            {/* Buttons */}
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-1">
                <button
                  onClick={() => seekBy(-10)}
                  className="rounded-lg p-2.5 text-white/80 hover:bg-white/10"
                  aria-label="رجوع 10 ثواني"
                >
                  <SkipBack className="h-5 w-5" />
                </button>
                <button
                  onClick={togglePlay}
                  className="flex h-12 w-12 items-center justify-center rounded-full bg-amber-500 text-zinc-950 hover:bg-amber-400"
                  aria-label={playing ? 'إيقاف' : 'تشغيل'}
                >
                  {playing ? <Pause className="h-6 w-6" fill="currentColor" /> : <Play className="h-6 w-6" fill="currentColor" />}
                </button>
                <button
                  onClick={() => seekBy(10)}
                  className="rounded-lg p-2.5 text-white/80 hover:bg-white/10"
                  aria-label="تقديم 10 ثواني"
                >
                  <SkipForward className="h-5 w-5" />
                </button>
              </div>

              <div className="flex items-center gap-1">
                {/* Volume */}
                <button
                  onClick={() => setMuted((m) => !m)}
                  className="rounded-lg p-2 text-white/80 hover:bg-white/10"
                  aria-label="كتم"
                >
                  {muted || volume === 0 ? <VolumeX className="h-5 w-5" /> : <Volume2 className="h-5 w-5" />}
                </button>
                <input
                  type="range"
                  min={0}
                  max={1}
                  step={0.05}
                  value={muted ? 0 : volume}
                  onChange={(e) => { setVolume(Number(e.target.value)); setMuted(false); }}
                  className="w-16 accent-amber-500"
                />

                {/* Settings */}
                <button
                  onClick={() => setShowSettings((s) => !s)}
                  className={cn('rounded-lg p-2 hover:bg-white/10', showSettings ? 'text-amber-400' : 'text-white/80')}
                  aria-label="إعدادات"
                >
                  <Settings className="h-5 w-5" />
                </button>

                {/* PiP (video only) */}
                {state.type === 'video' && 'pictureInPictureEnabled' in document && (
                  <button
                    onClick={togglePip}
                    className={cn('rounded-lg p-2 hover:bg-white/10', pip ? 'text-amber-400' : 'text-white/80')}
                    aria-label="نافذة عائمة"
                  >
                    <PictureInPicture className="h-5 w-5" />
                  </button>
                )}

                {/* Subtitles (placeholder) */}
                <button
                  className="rounded-lg p-2 text-white/80 hover:bg-white/10"
                  aria-label="ترجمة"
                >
                  <Subtitles className="h-5 w-5" />
                </button>

                {/* Share */}
                <button
                  className="rounded-lg p-2 text-white/80 hover:bg-white/10"
                  aria-label="مشاركة"
                >
                  <Share2 className="h-5 w-5" />
                </button>
              </div>
            </div>

            {/* Settings panel */}
            {showSettings && (
              <div className="rounded-xl border border-white/10 bg-zinc-900/80 p-3 backdrop-blur">
                <div className="mb-2 flex items-center gap-1.5 text-[11px] font-semibold text-zinc-300">
                  <Gauge className="h-3.5 w-3.5" />
                  سرعة التشغيل
                </div>
                <div className="grid grid-cols-5 gap-1">
                  {[0.5, 0.75, 1, 1.25, 1.5, 1.75, 2].map((rate) => (
                    <button
                      key={rate}
                      onClick={() => { setPlaybackRate(rate); setShowSettings(false); }}
                      className={cn(
                        'h-9 rounded-lg border text-[11px] font-medium transition-colors',
                        playbackRate === rate
                          ? 'border-amber-500/50 bg-amber-500/15 text-amber-200'
                          : 'border-white/10 bg-white/5 text-zinc-300 hover:bg-white/10',
                      )}
                    >
                      {rate}x
                    </button>
                  ))}
                </div>
              </div>
            )}
          </div>
        </motion.div>
      </motion.div>
    </AnimatePresence>
  );
}

function formatTime(seconds: number): string {
  if (!isFinite(seconds) || seconds <= 0) return '0:00';
  const h = Math.floor(seconds / 3600);
  const m = Math.floor((seconds % 3600) / 60);
  const s = Math.floor(seconds % 60);
  if (h > 0) return `${h}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
  return `${m}:${String(s).padStart(2, '0')}`;
}
