'use client';

import { cn } from '@/lib/utils';
import { formatBytes, formatDuration, useVideoHub } from '@/store/videohub';
import type { DownloadTask, TaskStatus, PluginHealth } from '@/lib/videohub/types';
import { Pause, Play, RotateCcw, Trash2, FolderOpen, AlertCircle, Clock, ArrowUp, ArrowDown, Minus, Sparkles } from 'lucide-react';
import { useState } from 'react';
import { openMediaWorkspace } from './MediaWorkspace';
import { openMediaPlayer } from './MediaPlayer';

// ============ Status Badge ============

const STATUS_CONFIG: Record<TaskStatus, { label: string; color: string; bg: string; dot: string }> = {
  queued: { label: 'في الانتظار', color: 'text-amber-300', bg: 'bg-amber-500/10 border-amber-500/30', dot: 'bg-amber-400' },
  resolving: { label: 'جاري التحليل', color: 'text-blue-300', bg: 'bg-blue-500/10 border-blue-500/30', dot: 'bg-blue-400' },
  downloading: { label: 'قيد التنزيل', color: 'text-emerald-300', bg: 'bg-emerald-500/10 border-emerald-500/30', dot: 'bg-emerald-400 animate-pulse' },
  paused: { label: 'متوقف', color: 'text-zinc-300', bg: 'bg-zinc-500/10 border-zinc-500/30', dot: 'bg-zinc-400' },
  completed: { label: 'مكتمل', color: 'text-green-300', bg: 'bg-green-500/10 border-green-500/30', dot: 'bg-green-400' },
  failed: { label: 'فشل', color: 'text-red-300', bg: 'bg-red-500/10 border-red-500/30', dot: 'bg-red-400' },
  retrying: { label: 'إعادة المحاولة', color: 'text-orange-300', bg: 'bg-orange-500/10 border-orange-500/30', dot: 'bg-orange-400 animate-pulse' },
};

export function StatusBadge({ status, className }: { status: TaskStatus; className?: string }) {
  const cfg = STATUS_CONFIG[status];
  return (
    <span
      className={cn(
        'inline-flex items-center gap-1.5 rounded-md border px-2 py-0.5 text-[10px] font-medium',
        cfg.bg, cfg.color, className,
      )}
    >
      <span className={cn('h-1.5 w-1.5 rounded-full', cfg.dot)} />
      {cfg.label}
    </span>
  );
}

export function HealthBadge({ health }: { health: PluginHealth }) {
  const pct = Math.round(health.successRate * 100);
  const cfg =
    health.status === 'healthy'
      ? { label: 'سليم', color: 'text-emerald-300', bg: 'bg-emerald-500/10 border-emerald-500/30' }
      : health.status === 'degraded'
      ? { label: 'متدهور', color: 'text-amber-300', bg: 'bg-amber-500/10 border-amber-500/30' }
      : { label: 'معطل', color: 'text-red-300', bg: 'bg-red-500/10 border-red-500/30' };

  return (
    <span className={cn('inline-flex items-center gap-1.5 rounded-md border px-2 py-0.5 text-[10px] font-medium', cfg.bg, cfg.color)}>
      {cfg.label} · {pct}%
    </span>
  );
}

// ============ Progress Bar ============

export function ProgressBar({
  value, className, showGlow = false,
}: {
  value: number; className?: string; showGlow?: boolean;
}) {
  const pct = Math.min(100, Math.max(0, value * 100));
  return (
    <div className={cn('relative h-1.5 w-full overflow-hidden rounded-full bg-white/10', className)}>
      <div
        className={cn(
          'h-full rounded-full bg-gradient-to-r from-amber-500 to-amber-300 transition-all duration-300',
          showGlow && pct > 0 && pct < 100 && 'shadow-[0_0_8px_rgba(245,158,11,0.6)]',
        )}
        style={{ width: `${pct}%` }}
      />
    </div>
  );
}

// ============ Speed / ETA ============

export function SpeedDisplay({ bytesPerSec }: { bytesPerSec: number }) {
  if (bytesPerSec <= 0) return <span className="text-zinc-500">—</span>;
  return <span className="font-mono text-emerald-300">{formatBytes(bytesPerSec)}/s</span>;
}

export function EtaDisplay({ seconds }: { seconds: number }) {
  if (!isFinite(seconds) || seconds <= 0) return <span className="text-zinc-500">—</span>;
  return <span className="font-mono text-zinc-300 flex items-center gap-1"><Clock className="h-2.5 w-2.5" />{formatDuration(seconds)}</span>;
}

// ============ Task Card (Mobile-first) ============

export function TaskCard({ task }: { task: DownloadTask }) {
  const platform = task.metadata.platformId;
  const [expanded, setExpanded] = useState(false);

  return (
    <div className="rounded-2xl border border-white/10 bg-zinc-900/60 backdrop-blur transition-colors hover:border-white/20">
      <div className="p-3">
        <div className="flex gap-3">
          {/* Thumbnail */}
          {task.metadata.thumbnailUrl && (
            <img
              src={task.metadata.thumbnailUrl}
              alt={task.metadata.title}
              className="h-16 w-24 flex-shrink-0 rounded-lg object-cover sm:h-20 sm:w-32"
            />
          )}

          {/* Content */}
          <div className="min-w-0 flex-1">
            <div className="flex items-start justify-between gap-2">
              <h4 className="line-clamp-2 text-xs font-semibold text-zinc-100 sm:text-sm">{task.metadata.title}</h4>
              <StatusBadge status={task.status} />
            </div>
            <div className="mt-1 flex flex-wrap items-center gap-x-2 gap-y-0.5 text-[10px] text-zinc-500">
              <span className="uppercase">{platform}</span>
              <span>·</span>
              <span>{task.selectedFormat.label}</span>
              <span>·</span>
              <span>{formatBytes(task.totalBytes)}</span>
              {task.priority !== 1 && (
                <>
                  <span>·</span>
                  <PriorityBadge priority={task.priority} />
                </>
              )}
            </div>

            {/* Progress */}
            <div className="mt-2">
              <ProgressBar value={task.progress} showGlow={task.status === 'downloading'} />
              <div className="mt-1 flex items-center justify-between text-[10px]">
                <span className="font-mono text-zinc-400">
                  {formatBytes(task.downloadedBytes)} / {formatBytes(task.totalBytes)}
                </span>
                <div className="flex items-center gap-2">
                  {task.status === 'downloading' && (
                    <>
                      <SpeedDisplay bytesPerSec={task.speedBytesPerSec} />
                      <EtaDisplay seconds={task.etaSeconds} />
                    </>
                  )}
                  {task.status === 'completed' && task.completedAt && (
                    <span className="text-zinc-500">{new Date(task.completedAt).toLocaleTimeString('ar')}</span>
                  )}
                  {task.status === 'failed' && (
                    <span className="text-red-300 truncate max-w-[120px]" title={task.error}>
                      {task.error?.slice(0, 30) ?? 'خطأ'}
                    </span>
                  )}
                </div>
              </div>
            </div>

            {/* Segment visualization */}
            {(task.status === 'downloading' || task.status === 'paused') && task.segments.length > 0 && (
              <button
                onClick={() => setExpanded((v) => !v)}
                className="mt-2 flex w-full items-center gap-1"
              >
                {task.segments.map((seg) => (
                  <div
                    key={seg.id}
                    className={cn(
                      'h-1 flex-1 rounded-full transition-colors',
                      seg.status === 'done' && 'bg-emerald-500',
                      seg.status === 'active' && 'bg-amber-400 animate-pulse',
                      seg.status === 'pending' && 'bg-white/10',
                      seg.status === 'failed' && 'bg-red-500',
                    )}
                    title={`الجزء ${seg.id + 1}: ${formatBytes(seg.downloadedByte)}/${formatBytes(seg.endByte - seg.startByte)}`}
                  />
                ))}
              </button>
            )}
          </div>
        </div>

        {/* Action buttons — touch-friendly 44px targets */}
        <div className="mt-3 grid grid-cols-4 gap-1.5">
          {(task.status === 'downloading' || task.status === 'queued') && (
            <ActionButton onClick={() => useVideoHub.getState().pauseTask(task.id)} icon={<Pause className="h-4 w-4" />} label="إيقاف" />
          )}
          {task.status === 'paused' && (
            <ActionButton onClick={() => useVideoHub.getState().resumeTask(task.id)} icon={<Play className="h-4 w-4" />} label="استئناف" variant="primary" />
          )}
          {(task.status === 'failed' || task.status === 'paused') && (
            <ActionButton onClick={() => useVideoHub.getState().retryTask(task.id)} icon={<RotateCcw className="h-4 w-4" />} label="إعادة" variant="warning" />
          )}
          {task.status === 'completed' && (
            <>
              <ActionButton
                onClick={() => openMediaPlayer({
                  url: task.outputPath ?? '',
                  title: task.metadata.title,
                  type: task.selectedFormat.mediaType === 'audio' ? 'audio' : 'video',
                  thumbnailUrl: task.metadata.thumbnailUrl,
                })}
                icon={<Play className="h-4 w-4" />}
                label="تشغيل"
                variant="primary"
              />
              <ActionButton
                onClick={() => openMediaWorkspace(task)}
                icon={<Sparkles className="h-4 w-4" />}
                label="مساحة عمل"
                variant="warning"
              />
            </>
          )}
          {/* Priority controls (only for non-completed) */}
          {task.status !== 'completed' && (
            <>
              <ActionButton
                onClick={() => useVideoHub.getState().setTaskPriority(task.id, Math.max(0, task.priority - 1))}
                icon={<ArrowUp className="h-4 w-4" />}
                label="رفع"
                disabled={task.priority === 0}
              />
              <ActionButton
                onClick={() => useVideoHub.getState().setTaskPriority(task.id, Math.min(2, task.priority + 1))}
                icon={<ArrowDown className="h-4 w-4" />}
                label="خفض"
                disabled={task.priority === 2}
              />
            </>
          )}
          <ActionButton
            onClick={() => useVideoHub.getState().deleteTask(task.id)}
            icon={<Trash2 className="h-4 w-4" />}
            label="حذف"
            variant="danger"
          />
        </div>

        {/* Expanded segment details */}
        {expanded && task.segments.length > 0 && (
          <div className="mt-2 grid grid-cols-2 gap-1 rounded-md bg-white/5 p-2 text-[10px] sm:grid-cols-4">
            {task.segments.map((seg) => (
              <div key={seg.id} className="font-mono">
                <div className="text-zinc-400">جزء {seg.id + 1}</div>
                <div className="text-zinc-300">
                  {formatBytes(seg.downloadedByte)}/{formatBytes(seg.endByte - seg.startByte)}
                </div>
              </div>
            ))}
          </div>
        )}

        {/* Error details */}
        {task.status === 'failed' && task.error && (
          <div className="mt-2 flex items-start gap-1.5 rounded-md bg-red-500/5 px-2 py-1.5 text-[10px] text-red-300">
            <AlertCircle className="mt-0.5 h-3 w-3 flex-shrink-0" />
            <span className="flex-1">{task.error}</span>
            {task.errorStage && <span className="font-mono text-red-500/60">{task.errorStage}</span>}
          </div>
        )}
      </div>
    </div>
  );
}

function PriorityBadge({ priority }: { priority: number }) {
  const cfg = priority === 0
    ? { label: 'عالية', icon: <ArrowUp className="h-2.5 w-2.5" />, color: 'text-red-300' }
    : priority === 2
    ? { label: 'منخفضة', icon: <ArrowDown className="h-2.5 w-2.5" />, color: 'text-zinc-400' }
    : { label: 'عادية', icon: <Minus className="h-2.5 w-2.5" />, color: 'text-zinc-400' };
  return (
    <span className={`flex items-center gap-0.5 ${cfg.color}`}>
      {cfg.icon}
      {cfg.label}
    </span>
  );
}

function ActionButton({
  onClick, icon, label, variant = 'default', disabled,
}: {
  onClick: () => void;
  icon: React.ReactNode;
  label: string;
  variant?: 'default' | 'primary' | 'warning' | 'danger';
  disabled?: boolean;
}) {
  const variantClass = {
    default: 'border-white/10 text-zinc-300 hover:bg-white/5',
    primary: 'border-emerald-500/30 bg-emerald-500/10 text-emerald-300 hover:bg-emerald-500/20',
    warning: 'border-amber-500/30 bg-amber-500/10 text-amber-300 hover:bg-amber-500/20',
    danger: 'border-white/10 text-zinc-400 hover:bg-red-500/10 hover:text-red-300',
  }[variant];

  return (
    <button
      onClick={onClick}
      disabled={disabled}
      className={cn(
        'flex h-11 flex-col items-center justify-center gap-0.5 rounded-lg border text-[10px] font-medium transition-colors disabled:opacity-30',
        variantClass,
      )}
    >
      {icon}
      {label}
    </button>
  );
}
