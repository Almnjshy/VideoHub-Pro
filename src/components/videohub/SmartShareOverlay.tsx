'use client';

import { motion, AnimatePresence } from 'framer-motion';
import { Loader2, X, Download, AlertCircle, CheckCircle2, Zap, Film, Music } from 'lucide-react';
import { useVideoHub, findFrontendPluginForUrl } from '@/store/videohub';
import { cn } from '@/lib/utils';
import { useState, useEffect, useRef } from 'react';
import type { MediaFormat } from '@/lib/videohub/types';

/**
 * نظام المشاركة الذكي — Overlay Sheet (Mobile-friendly Bottom Sheet)
 * يظهر كـ Bottom Sheet على الجوال، وكـ Side Panel على التابلت
 */
export function SmartShareOverlay() {
  const intents = useVideoHub((s) => s.activeIntents);
  const resolveShareIntent = useVideoHub((s) => s.resolveShareIntent);
  const dismissShareIntent = useVideoHub((s) => s.dismissShareIntent);
  const startDownload = useVideoHub((s) => s.startDownload);
  const resolvedRef = useRef<Set<string>>(new Set());

  useEffect(() => {
    for (const intent of intents) {
      if (!intent.resolving && !intent.metadata && intent.detectedPluginId && !resolvedRef.current.has(intent.id)) {
        resolvedRef.current.add(intent.id);
        void resolveShareIntent(intent.id);
      }
    }
    const currentIds = new Set(intents.map((i) => i.id));
    for (const id of Array.from(resolvedRef.current)) {
      if (!currentIds.has(id)) resolvedRef.current.delete(id);
    }
  }, [intents, resolveShareIntent]);

  if (intents.length === 0) return null;

  return (
    <AnimatePresence>
      {intents.length > 0 && (
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          className="fixed inset-0 z-50 flex items-end justify-center bg-black/60 backdrop-blur-sm sm:items-end sm:justify-end sm:p-4 lg:items-stretch lg:justify-end"
          onClick={() => {
            // Click outside dismisses the topmost intent
            const top = intents[intents.length - 1];
            dismissShareIntent(top.id);
          }}
        >
          <div className="flex w-full flex-col gap-2 sm:max-w-md lg:h-full lg:max-h-screen">
            {intents.map((intent) => (
              <motion.div
                key={intent.id}
                initial={{ y: '100%', opacity: 0 }}
                animate={{ y: 0, opacity: 1 }}
                exit={{ y: '100%', opacity: 0 }}
                transition={{ type: 'spring', stiffness: 380, damping: 32 }}
                onClick={(e) => e.stopPropagation()}
                className="overflow-hidden rounded-t-3xl border border-amber-500/30 bg-zinc-950/95 shadow-2xl backdrop-blur-xl sm:rounded-2xl lg:my-auto lg:max-h-[90vh]"
              >
                {/* Drag handle (mobile) */}
                <div className="flex justify-center pt-2 sm:hidden">
                  <div className="h-1 w-10 rounded-full bg-white/20" />
                </div>

                {/* Header */}
                <div className="flex items-center justify-between border-b border-white/5 bg-gradient-to-r from-amber-500/10 to-transparent px-4 py-3">
                  <div className="flex items-center gap-2">
                    <div className="flex h-7 w-7 items-center justify-center rounded-md bg-amber-500/20 text-amber-300">
                      <Download className="h-4 w-4" />
                    </div>
                    <div>
                      <span className="text-sm font-semibold text-zinc-100">مشاركة ذكية</span>
                      <div className="text-[10px] text-zinc-500">VideoHub Pro</div>
                    </div>
                  </div>
                  <button
                    onClick={() => dismissShareIntent(intent.id)}
                    className="rounded-md p-2 text-zinc-500 hover:bg-white/5 hover:text-zinc-200"
                    aria-label="إغلاق"
                  >
                    <X className="h-4 w-4" />
                  </button>
                </div>

                {/* Body */}
                <div className="max-h-[70vh] overflow-y-auto p-4">
                  {/* Source URL */}
                  <div className="mb-3">
                    <div className="mb-1 text-[10px] uppercase tracking-wider text-zinc-500">الرابط المستلم</div>
                    <div className="truncate rounded-md bg-white/5 px-2 py-1.5 font-mono text-[11px] text-zinc-300" dir="ltr">
                      {intent.url}
                    </div>
                  </div>

                  {/* Plugin detection */}
                  {intent.detectedPluginId ? (
                    <DetectedPlugin pluginId={intent.detectedPluginId} />
                  ) : !intent.resolving ? (
                    <div className="flex items-center gap-2 rounded-md border border-red-500/30 bg-red-500/5 px-3 py-2 text-xs text-red-300">
                      <AlertCircle className="h-4 w-4" />
                      لا توجد وحدة تدعم هذا الرابط
                    </div>
                  ) : null}

                  {/* Loading state */}
                  {intent.resolving && !intent.metadata && (
                    <div className="flex items-center gap-2 py-6 text-sm text-zinc-400">
                      <Loader2 className="h-4 w-4 animate-spin text-amber-400" />
                      جاري تحليل الرابط واستخراج البيانات...
                    </div>
                  )}

                  {/* Metadata */}
                  {intent.metadata && (
                    <MetadataPreview
                      intentId={intent.id}
                      metadata={intent.metadata}
                      onStart={(format, priority) => startDownload(intent.id, format, priority)}
                    />
                  )}
                </div>
              </motion.div>
            ))}
          </div>
        </motion.div>
      )}
    </AnimatePresence>
  );
}

function DetectedPlugin({ pluginId }: { pluginId: string }) {
  const plugin = useVideoHub((s) => s.plugins.find((p) => p.id === pluginId));
  if (!plugin) return null;
  return (
    <div className="mb-3 flex items-center gap-2 rounded-md border border-white/10 bg-white/5 px-3 py-2">
      <div
        className="flex h-8 w-8 items-center justify-center rounded-md text-sm font-bold text-white"
        style={{ backgroundColor: plugin.color }}
      >
        {plugin.icon}
      </div>
      <div className="min-w-0 flex-1">
        <div className="text-xs font-semibold text-zinc-200">{plugin.nameAr}</div>
        <div className="text-[10px] text-zinc-500">الوحدة v{plugin.version} · {plugin.name}</div>
      </div>
      <span className="flex items-center gap-1 rounded-md bg-emerald-500/10 px-2 py-0.5 text-[10px] text-emerald-300">
        <CheckCircle2 className="h-2.5 w-2.5" />
        متعرف عليها
      </span>
    </div>
  );
}

function MetadataPreview({
  metadata, onStart,
}: {
  intentId: string;
  metadata: NonNullable<ReturnType<typeof useVideoHub.getState>['activeIntents'][number]['metadata']>;
  onStart: (format: MediaFormat, priority: number) => void;
}) {
  const settings = useVideoHub((s) => s.settings);

  const defaultFormatId =
    metadata.formats.find((f) => f.quality === settings.defaultQuality && f.mediaType === settings.defaultMediaType)?.id ??
    metadata.formats.find((f) => f.quality === settings.defaultQuality)?.id ??
    metadata.formats.find((f) => f.mediaType === settings.defaultMediaType)?.id ??
    metadata.formats[0]?.id ??
    '';

  const [selectedFormatId, setSelectedFormatId] = useState<string>(defaultFormatId);
  const [priority, setPriority] = useState<number>(1);

  // Separate formats by media type for grouped display
  const videoFormats = metadata.formats.filter((f) => f.mediaType === 'video');
  const audioFormats = metadata.formats.filter((f) => f.mediaType === 'audio');

  const selectedFormat = metadata.formats.find((f) => f.id === selectedFormatId);

  if (metadata.formats.length === 0) {
    return (
      <div className="flex items-center gap-2 rounded-md border border-red-500/30 bg-red-500/5 px-3 py-3 text-xs text-red-300">
        <AlertCircle className="h-4 w-4 flex-shrink-0" />
        <div>
          <div className="font-medium">فشل استخراج البيانات</div>
          <div className="text-[10px] opacity-80">{metadata.description}</div>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-3">
      {/* Media info */}
      <div className="flex gap-3">
        {metadata.thumbnailUrl && (
          <img
            src={metadata.thumbnailUrl}
            alt={metadata.title}
            className="h-20 w-32 flex-shrink-0 rounded-md object-cover"
          />
        )}
        <div className="min-w-0 flex-1">
          <h4 className="line-clamp-2 text-sm font-semibold text-zinc-100">{metadata.title}</h4>
          {metadata.author && (
            <div className="mt-0.5 text-[11px] text-zinc-400">{metadata.author}</div>
          )}
          {metadata.durationSeconds && (
            <div className="mt-1 inline-flex items-center gap-1 rounded bg-white/5 px-1.5 py-0.5 font-mono text-[10px] text-zinc-400">
              {formatDurationShort(metadata.durationSeconds)}
            </div>
          )}
        </div>
      </div>

      {/* Format selection — separated into Video and Audio sections */}
      <div className="space-y-4">
        {/* Video section */}
        {videoFormats.length > 0 && (
          <div>
            <div className="mb-2 flex items-center gap-2">
              <div className="flex h-6 w-6 items-center justify-center rounded-md bg-amber-500/15 text-amber-300">
                <Film className="h-3.5 w-3.5" />
              </div>
              <div className="flex-1">
                <div className="text-xs font-semibold text-zinc-100">تنزيل كفيديو</div>
                <div className="text-[9px] text-zinc-500">{videoFormats.length} جودات متاحة</div>
              </div>
            </div>
            <div className="grid grid-cols-2 gap-1.5">
              {videoFormats.map((fmt) => (
                <button
                  key={fmt.id}
                  onClick={() => setSelectedFormatId(fmt.id)}
                  className={cn(
                    'flex items-center justify-between rounded-md border px-2.5 py-2 text-[11px] transition-colors',
                    selectedFormatId === fmt.id
                      ? 'border-amber-500/50 bg-amber-500/15 text-amber-200'
                      : 'border-white/10 bg-white/5 text-zinc-300 hover:bg-white/10',
                  )}
                >
                  <span className="font-medium">{fmt.label}</span>
                  <span className="font-mono text-[10px] opacity-70">
                    {formatBytesShort(fmt.sizeBytes)}
                  </span>
                </button>
              ))}
            </div>
          </div>
        )}

        {/* Audio section */}
        {audioFormats.length > 0 && (
          <div>
            <div className="mb-2 flex items-center gap-2">
              <div className="flex h-6 w-6 items-center justify-center rounded-md bg-emerald-500/15 text-emerald-300">
                <Music className="h-3.5 w-3.5" />
              </div>
              <div className="flex-1">
                <div className="text-xs font-semibold text-zinc-100">تنزيل كصوت</div>
                <div className="text-[9px] text-zinc-500">{audioFormats.length} صيغ متاحة</div>
              </div>
            </div>
            <div className="grid grid-cols-2 gap-1.5">
              {audioFormats.map((fmt) => (
                <button
                  key={fmt.id}
                  onClick={() => setSelectedFormatId(fmt.id)}
                  className={cn(
                    'flex items-center justify-between rounded-md border px-2.5 py-2 text-[11px] transition-colors',
                    selectedFormatId === fmt.id
                      ? 'border-emerald-500/50 bg-emerald-500/15 text-emerald-200'
                      : 'border-white/10 bg-white/5 text-zinc-300 hover:bg-white/10',
                  )}
                >
                  <span className="font-medium">{fmt.label}</span>
                  <span className="font-mono text-[10px] opacity-70">
                    {formatBytesShort(fmt.sizeBytes)}
                  </span>
                </button>
              ))}
            </div>
          </div>
        )}
      </div>

      {/* Priority selection */}
      <div>
        <div className="mb-1.5 text-[10px] uppercase tracking-wider text-zinc-500">الأولوية</div>
        <div className="grid grid-cols-3 gap-1.5">
          {[
            { v: 0, label: 'عالية', icon: <Zap className="h-3 w-3" /> },
            { v: 1, label: 'عادية', icon: null },
            { v: 2, label: 'منخفضة', icon: null },
          ].map((p) => (
            <button
              key={p.v}
              onClick={() => setPriority(p.v)}
              className={cn(
                'flex items-center justify-center gap-1 rounded-md border px-2 py-1.5 text-[11px] font-medium transition-colors',
                priority === p.v
                  ? 'border-amber-500/50 bg-amber-500/15 text-amber-200'
                  : 'border-white/10 bg-white/5 text-zinc-300 hover:bg-white/10',
              )}
            >
              {p.icon}
              {p.label}
            </button>
          ))}
        </div>
      </div>

      {/* Action */}
      <button
        onClick={() => selectedFormat && onStart(selectedFormat, priority)}
        disabled={!selectedFormat}
        className="flex w-full items-center justify-center gap-2 rounded-xl bg-gradient-to-r from-amber-500 to-amber-400 px-4 py-3 text-sm font-semibold text-zinc-950 transition-all hover:shadow-[0_0_20px_rgba(245,158,11,0.4)] active:scale-[0.98] disabled:opacity-50"
      >
        <Download className="h-4 w-4" />
        بدء التنزيل
      </button>
    </div>
  );
}

function formatDurationShort(seconds: number): string {
  const m = Math.floor(seconds / 60);
  const s = Math.floor(seconds % 60);
  return `${m}:${String(s).padStart(2, '0')}`;
}

function formatBytesShort(bytes: number): string {
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(0)}KB`;
  if (bytes < 1024 * 1024 * 1024) return `${(bytes / (1024 * 1024)).toFixed(1)}MB`;
  return `${(bytes / (1024 * 1024 * 1024)).toFixed(2)}GB`;
}
