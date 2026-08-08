'use client';

import { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  X, Play, Music, Scissors, Share2, Cloud, Image as ImageIcon,
  FileVideo, Repeat, Volume2, Download, Tag, Archive, Sparkles,
} from 'lucide-react';
import { cn } from '@/lib/utils';
import { openMediaPlayer } from './MediaPlayer';
import type { DownloadTask } from '@/lib/videohub/types';

interface WorkspaceState {
  isOpen: boolean;
  task: DownloadTask | null;
}

let workspaceState: WorkspaceState = { isOpen: false, task: null };
const listeners = new Set<() => void>();

export function openMediaWorkspace(task: DownloadTask) {
  workspaceState = { isOpen: true, task };
  listeners.forEach((l) => l());
}

export function closeMediaWorkspace() {
  workspaceState = { isOpen: false, task: null };
  listeners.forEach((l) => l());
}

function useWorkspaceState() {
  const [, force] = useState(0);
  useEffect(() => {
    const l = () => force((x) => x + 1);
    listeners.add(l);
    return () => { listeners.delete(l); };
  }, []);
  return workspaceState;
}

const TOOLS = [
  { id: 'play', label: 'تشغيل', icon: Play, color: 'amber', desc: 'مشغل وسائط مدمج' },
  { id: 'extract_audio', label: 'استخراج الصوت', icon: Music, color: 'emerald', desc: 'MP4 → MP3' },
  { id: 'trim', label: 'قص', icon: Scissors, color: 'blue', desc: 'اقتطاع جزء' },
  { id: 'convert', label: 'تحويل الصيغة', icon: Repeat, color: 'purple', desc: 'MP4 ↔ MKV ↔ AVI' },
  { id: 'thumbnail', label: 'صورة مصغّرة', icon: ImageIcon, color: 'orange', desc: 'التقاط إطار' },
  { id: 'remove_audio', label: 'إزالة الصوت', icon: Volume2, color: 'red', desc: 'فيديو صامت' },
  { id: 'compress', label: 'ضغط', icon: Archive, color: 'zinc', desc: 'تقليل الحجم' },
  { id: 'share', label: 'مشاركة', icon: Share2, color: 'amber', desc: 'رابط/QR/Wi-Fi' },
  { id: 'cloud', label: 'رفع للسحابة', icon: Cloud, color: 'blue', desc: 'Google Drive/OneDrive' },
  { id: 'tags', label: 'وسوم', icon: Tag, color: 'emerald', desc: 'تنظيم الملف' },
];

export function MediaWorkspace() {
  const state = useWorkspaceState();
  const [processing, setProcessing] = useState<string | null>(null);

  if (!state.isOpen || !state.task) return null;

  const task = state.task;

  const handleTool = (toolId: string) => {
    if (toolId === 'play') {
      // Open media player
      openMediaPlayer({
        url: task.outputPath ?? '',
        title: task.metadata.title,
        type: task.selectedFormat.mediaType === 'audio' ? 'audio' : 'video',
        thumbnailUrl: task.metadata.thumbnailUrl,
      });
      closeMediaWorkspace();
      return;
    }

    // Simulate processing
    setProcessing(toolId);
    setTimeout(() => {
      setProcessing(null);
    }, 1500);
  };

  return (
    <AnimatePresence>
      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        exit={{ opacity: 0 }}
        className="fixed inset-0 z-[70] flex items-end justify-center bg-black/70 backdrop-blur-sm sm:items-center"
        onClick={closeMediaWorkspace}
      >
        <motion.div
          initial={{ y: '100%', opacity: 0 }}
          animate={{ y: 0, opacity: 1 }}
          exit={{ y: '100%', opacity: 0 }}
          transition={{ type: 'spring', stiffness: 380, damping: 32 }}
          onClick={(e) => e.stopPropagation()}
          className="max-h-[85vh] w-full max-w-lg overflow-hidden rounded-t-3xl border border-white/10 bg-zinc-950/95 shadow-2xl sm:rounded-2xl"
        >
          {/* Drag handle */}
          <div className="flex justify-center pt-2 sm:hidden">
            <div className="h-1 w-10 rounded-full bg-white/20" />
          </div>

          {/* Header */}
          <div className="flex items-center justify-between border-b border-white/5 px-4 py-3">
            <div className="flex items-center gap-2">
              <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-amber-500/15 text-amber-300">
                <Sparkles className="h-4 w-4" />
              </div>
              <div>
                <div className="text-sm font-semibold text-zinc-100">مساحة عمل الوسائط</div>
                <div className="text-[10px] text-zinc-500">إجراءات على الملف</div>
              </div>
            </div>
            <button
              onClick={closeMediaWorkspace}
              className="rounded-lg p-2 text-zinc-500 hover:bg-white/5 hover:text-zinc-200"
            >
              <X className="h-5 w-5" />
            </button>
          </div>

          {/* File info */}
          <div className="flex gap-3 border-b border-white/5 p-4">
            {task.metadata.thumbnailUrl && (
              <img
                src={task.metadata.thumbnailUrl}
                alt={task.metadata.title}
                className="h-16 w-24 flex-shrink-0 rounded-md object-cover"
              />
            )}
            <div className="min-w-0 flex-1">
              <h4 className="line-clamp-2 text-sm font-semibold text-zinc-100">{task.metadata.title}</h4>
              <div className="mt-1 flex flex-wrap items-center gap-x-2 gap-y-0.5 text-[10px] text-zinc-500">
                <span className="uppercase">{task.metadata.platformId}</span>
                <span>·</span>
                <span>{task.selectedFormat.label}</span>
                <span>·</span>
                <span>{(task.totalBytes / (1024 * 1024)).toFixed(1)} MB</span>
              </div>
            </div>
          </div>

          {/* Tools grid */}
          <div className="max-h-[50vh] overflow-y-auto p-4">
            <div className="grid grid-cols-3 gap-2 sm:grid-cols-4">
              {TOOLS.map((tool) => {
                const Icon = tool.icon;
                const isProcessing = processing === tool.id;
                return (
                  <button
                    key={tool.id}
                    onClick={() => handleTool(tool.id)}
                    disabled={processing !== null}
                    className={cn(
                      'flex flex-col items-center gap-1.5 rounded-xl border border-white/10 bg-zinc-900/60 p-3 transition-all hover:border-white/20 hover:bg-zinc-900 active:scale-95 disabled:opacity-50',
                      isProcessing && 'border-amber-500/50 bg-amber-500/10',
                    )}
                  >
                    <span className={cn(
                      'flex h-10 w-10 items-center justify-center rounded-lg',
                      tool.color === 'amber' && 'bg-amber-500/10 text-amber-300',
                      tool.color === 'emerald' && 'bg-emerald-500/10 text-emerald-300',
                      tool.color === 'blue' && 'bg-blue-500/10 text-blue-300',
                      tool.color === 'purple' && 'bg-purple-500/10 text-purple-300',
                      tool.color === 'orange' && 'bg-orange-500/10 text-orange-300',
                      tool.color === 'red' && 'bg-red-500/10 text-red-300',
                      tool.color === 'zinc' && 'bg-zinc-500/10 text-zinc-300',
                    )}>
                      {isProcessing ? (
                        <div className="h-4 w-4 animate-spin rounded-full border-2 border-current border-t-transparent" />
                      ) : (
                        <Icon className="h-5 w-5" />
                      )}
                    </span>
                    <div className="text-center">
                      <div className="text-[11px] font-medium text-zinc-200">{tool.label}</div>
                      <div className="text-[9px] text-zinc-500">{tool.desc}</div>
                    </div>
                  </button>
                );
              })}
            </div>

            {/* AI section */}
            <div className="mt-4 rounded-xl border border-purple-500/20 bg-purple-500/5 p-3">
              <div className="mb-2 flex items-center gap-1.5">
                <Sparkles className="h-3.5 w-3.5 text-purple-300" />
                <span className="text-xs font-semibold text-purple-200">أدوات الذكاء الاصطناعي</span>
                <span className="mr-auto rounded bg-purple-500/20 px-1.5 py-0.5 text-[9px] text-purple-300">قريباً</span>
              </div>
              <div className="grid grid-cols-2 gap-1.5">
                {['تلخيص الفيديو', 'استخراج النص', 'ترجمة الترجمة', 'توليد وصف'].map((ai) => (
                  <button
                    key={ai}
                    disabled
                    className="rounded-lg border border-white/5 bg-white/[0.02] p-2 text-right text-[10px] text-zinc-500"
                  >
                    {ai}
                  </button>
                ))}
              </div>
            </div>
          </div>
        </motion.div>
      </motion.div>
    </AnimatePresence>
  );
}
