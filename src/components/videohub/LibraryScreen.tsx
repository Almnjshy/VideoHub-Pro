'use client';

import { useState, useMemo } from 'react';
import { useVideoHub, formatBytes } from '@/store/videohub';
import { cn } from '@/lib/utils';
import {
  Library, Search, Film, Music, Image as ImageIcon, Star, Clock,
  Grid, List, SortAsc, Play, MoreVertical, FolderOpen, Tag, X,
} from 'lucide-react';
import type { DownloadTask, MediaType } from '@/lib/videohub/types';

type Category = 'all' | 'video' | 'audio' | 'image' | 'favorites' | 'recent';
type SortBy = 'date' | 'name' | 'size';

export function LibraryScreen() {
  const tasks = useVideoHub((s) => s.tasks);
  const [category, setCategory] = useState<Category>('all');
  const [sortBy, setSortBy] = useState<SortBy>('date');
  const [search, setSearch] = useState('');
  const [viewMode, setViewMode] = useState<'grid' | 'list'>('grid');
  const [favorites, setFavorites] = useState<Set<string>>(new Set());

  // Only show completed tasks in library
  const completed = tasks.filter((t) => t.status === 'completed');

  const filtered = useMemo(() => {
    let result = completed;

    // Category filter
    if (category === 'video') result = result.filter((t) => t.selectedFormat.mediaType === 'video');
    else if (category === 'audio') result = result.filter((t) => t.selectedFormat.mediaType === 'audio');
    else if (category === 'image') result = result.filter((t) => t.selectedFormat.mediaType === 'image');
    else if (category === 'favorites') result = result.filter((t) => favorites.has(t.id));
    else if (category === 'recent') {
      const dayAgo = Date.now() - 24 * 60 * 60 * 1000;
      result = result.filter((t) => (t.completedAt ?? t.createdAt) > dayAgo);
    }

    // Search filter
    if (search.trim()) {
      const q = search.toLowerCase();
      result = result.filter((t) =>
        t.metadata.title.toLowerCase().includes(q) ||
        t.metadata.platformId.toLowerCase().includes(q) ||
        (t.metadata.author?.toLowerCase().includes(q) ?? false),
      );
    }

    // Sort
    result = [...result].sort((a, b) => {
      if (sortBy === 'date') return (b.completedAt ?? b.createdAt) - (a.completedAt ?? a.createdAt);
      if (sortBy === 'name') return a.metadata.title.localeCompare(b.metadata.title);
      if (sortBy === 'size') return b.totalBytes - a.totalBytes;
      return 0;
    });

    return result;
  }, [completed, category, search, sortBy, favorites]);

  const counts = {
    all: completed.length,
    video: completed.filter((t) => t.selectedFormat.mediaType === 'video').length,
    audio: completed.filter((t) => t.selectedFormat.mediaType === 'audio').length,
    image: completed.filter((t) => t.selectedFormat.mediaType === 'image').length,
    favorites: favorites.size,
    recent: completed.filter((t) => {
      const dayAgo = Date.now() - 24 * 60 * 60 * 1000;
      return (t.completedAt ?? t.createdAt) > dayAgo;
    }).length,
  };

  const toggleFavorite = (taskId: string) => {
    setFavorites((prev) => {
      const next = new Set(prev);
      if (next.has(taskId)) next.delete(taskId);
      else next.add(taskId);
      return next;
    });
  };

  const categories: Array<{ id: Category; label: string; icon: React.ReactNode; count: number }> = [
    { id: 'all', label: 'الكل', icon: <Library className="h-4 w-4" />, count: counts.all },
    { id: 'video', label: 'فيديو', icon: <Film className="h-4 w-4" />, count: counts.video },
    { id: 'audio', label: 'صوت', icon: <Music className="h-4 w-4" />, count: counts.audio },
    { id: 'image', label: 'صور', icon: <ImageIcon className="h-4 w-4" />, count: counts.image },
    { id: 'favorites', label: 'المفضلة', icon: <Star className="h-4 w-4" />, count: counts.favorites },
    { id: 'recent', label: 'حديثة', icon: <Clock className="h-4 w-4" />, count: counts.recent },
  ];

  return (
    <div className="space-y-3">
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-bold text-zinc-100">مكتبة الوسائط</h2>
        <div className="flex gap-1">
          <button
            onClick={() => setViewMode((v) => v === 'grid' ? 'list' : 'grid')}
            className="rounded-lg border border-white/10 p-2 text-zinc-400 hover:bg-white/5"
            aria-label="تبديل العرض"
          >
            {viewMode === 'grid' ? <List className="h-4 w-4" /> : <Grid className="h-4 w-4" />}
          </button>
        </div>
      </div>

      {/* Search */}
      <div className="relative">
        <Search className="absolute right-3 top-1/2 h-4 w-4 -translate-y-1/2 text-zinc-500" />
        <input
          type="text"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder="ابحث في مكتبتك..."
          className="w-full rounded-xl border border-white/10 bg-zinc-900/60 py-2.5 pr-10 pl-4 text-sm text-zinc-100 placeholder:text-zinc-600 focus:border-amber-500/40 focus:outline-none"
        />
        {search && (
          <button
            onClick={() => setSearch('')}
            className="absolute left-2 top-1/2 -translate-y-1/2 rounded p-1 text-zinc-500 hover:text-zinc-200"
          >
            <X className="h-3.5 w-3.5" />
          </button>
        )}
      </div>

      {/* Categories — horizontal scroll */}
      <div className="flex gap-1.5 overflow-x-auto pb-1">
        {categories.map((cat) => (
          <button
            key={cat.id}
            onClick={() => setCategory(cat.id)}
            className={cn(
              'flex h-9 flex-shrink-0 items-center gap-1.5 rounded-lg px-3 text-xs font-medium transition-colors',
              category === cat.id
                ? 'bg-amber-500/15 text-amber-200'
                : 'text-zinc-400 hover:bg-white/5 hover:text-zinc-200',
            )}
          >
            {cat.icon}
            {cat.label}
            <span className="font-mono text-[10px] opacity-70">{cat.count}</span>
          </button>
        ))}
      </div>

      {/* Sort */}
      <div className="flex items-center gap-1.5 text-[11px]">
        <SortAsc className="h-3.5 w-3.5 text-zinc-500" />
        <span className="text-zinc-500">ترتيب:</span>
        {([
          { v: 'date', label: 'التاريخ' },
          { v: 'name', label: 'الاسم' },
          { v: 'size', label: 'الحجم' },
        ] as const).map((s) => (
          <button
            key={s.v}
            onClick={() => setSortBy(s.v)}
            className={cn(
              'rounded-md px-2 py-0.5 text-[11px] transition-colors',
              sortBy === s.v ? 'bg-amber-500/15 text-amber-200' : 'text-zinc-400 hover:text-zinc-200',
            )}
          >
            {s.label}
          </button>
        ))}
      </div>

      {/* Items */}
      {filtered.length === 0 ? (
        <div className="rounded-2xl border border-dashed border-white/10 bg-zinc-900/30 p-12 text-center">
          <FolderOpen className="mx-auto h-10 w-10 text-zinc-700" />
          <p className="mt-2 text-sm text-zinc-500">
            {search ? 'لا توجد نتائج مطابقة' : 'لا توجد ملفات بعد'}
          </p>
          <p className="text-[10px] text-zinc-600">
            {search ? 'جرّب كلمات مختلفة' : 'ستظهر ملفاتك المكتملة هنا'}
          </p>
        </div>
      ) : viewMode === 'grid' ? (
        <div className="grid grid-cols-2 gap-2 sm:grid-cols-3">
          {filtered.map((task) => (
            <MediaGridItem
              key={task.id}
              task={task}
              isFavorite={favorites.has(task.id)}
              onToggleFavorite={() => toggleFavorite(task.id)}
            />
          ))}
        </div>
      ) : (
        <div className="space-y-1.5">
          {filtered.map((task) => (
            <MediaListItem
              key={task.id}
              task={task}
              isFavorite={favorites.has(task.id)}
              onToggleFavorite={() => toggleFavorite(task.id)}
            />
          ))}
        </div>
      )}
    </div>
  );
}

function MediaGridItem({
  task, isFavorite, onToggleFavorite,
}: {
  task: DownloadTask;
  isFavorite: boolean;
  onToggleFavorite: () => void;
}) {
  const typeIcon = {
    video: <Film className="h-3.5 w-3.5" />,
    audio: <Music className="h-3.5 w-3.5" />,
    image: <ImageIcon className="h-3.5 w-3.5" />,
    file: <FolderOpen className="h-3.5 w-3.5" />,
  }[task.selectedFormat.mediaType];

  return (
    <div className="group relative overflow-hidden rounded-xl border border-white/10 bg-zinc-900/60">
      {/* Thumbnail */}
      <div className="relative aspect-video bg-zinc-800">
        {task.metadata.thumbnailUrl && (
          <img src={task.metadata.thumbnailUrl} alt={task.metadata.title} className="h-full w-full object-cover" />
        )}
        {/* Type badge */}
        <div className="absolute right-1.5 top-1.5 flex h-6 w-6 items-center justify-center rounded-md bg-black/60 text-white backdrop-blur">
          {typeIcon}
        </div>
        {/* Favorite */}
        <button
          onClick={onToggleFavorite}
          className="absolute left-1.5 top-1.5 flex h-6 w-6 items-center justify-center rounded-md bg-black/60 backdrop-blur"
        >
          <Star className={cn('h-3.5 w-3.5', isFavorite ? 'fill-amber-400 text-amber-400' : 'text-white')} />
        </button>
        {/* Play overlay */}
        <button className="absolute inset-0 flex items-center justify-center bg-black/0 opacity-0 transition-all hover:bg-black/40 hover:opacity-100">
          <div className="flex h-12 w-12 items-center justify-center rounded-full bg-amber-500/90 text-zinc-950">
            <Play className="h-5 w-5" fill="currentColor" />
          </div>
        </button>
        {/* Duration */}
        {task.metadata.durationSeconds && (
          <div className="absolute bottom-1.5 right-1.5 rounded bg-black/70 px-1.5 py-0.5 font-mono text-[9px] text-white">
            {formatDurationShort(task.metadata.durationSeconds)}
          </div>
        )}
      </div>
      {/* Info */}
      <div className="p-2">
        <h4 className="line-clamp-1 text-[11px] font-semibold text-zinc-100">{task.metadata.title}</h4>
        <div className="mt-0.5 flex items-center justify-between text-[9px] text-zinc-500">
          <span className="uppercase">{task.metadata.platformId}</span>
          <span>{formatBytes(task.totalBytes)}</span>
        </div>
      </div>
    </div>
  );
}

function MediaListItem({
  task, isFavorite, onToggleFavorite,
}: {
  task: DownloadTask;
  isFavorite: boolean;
  onToggleFavorite: () => void;
}) {
  const typeIcon = {
    video: <Film className="h-4 w-4" />,
    audio: <Music className="h-4 w-4" />,
    image: <ImageIcon className="h-4 w-4" />,
    file: <FolderOpen className="h-4 w-4" />,
  }[task.selectedFormat.mediaType];

  return (
    <div className="flex items-center gap-2.5 rounded-xl border border-white/10 bg-zinc-900/60 p-2">
      {/* Thumbnail */}
      <div className="relative h-12 w-16 flex-shrink-0 overflow-hidden rounded-md bg-zinc-800">
        {task.metadata.thumbnailUrl && (
          <img src={task.metadata.thumbnailUrl} alt={task.metadata.title} className="h-full w-full object-cover" />
        )}
        <div className="absolute inset-0 flex items-center justify-center bg-black/30">
          <div className="text-white">{typeIcon}</div>
        </div>
      </div>
      {/* Info */}
      <div className="min-w-0 flex-1">
        <h4 className="line-clamp-1 text-xs font-semibold text-zinc-100">{task.metadata.title}</h4>
        <div className="mt-0.5 flex items-center gap-2 text-[10px] text-zinc-500">
          <span className="uppercase">{task.metadata.platformId}</span>
          <span>·</span>
          <span>{formatBytes(task.totalBytes)}</span>
          {task.metadata.durationSeconds && (
            <>
              <span>·</span>
              <span className="font-mono">{formatDurationShort(task.metadata.durationSeconds)}</span>
            </>
          )}
        </div>
      </div>
      {/* Actions */}
      <button
        onClick={onToggleFavorite}
        className="rounded-md p-2 text-zinc-400 hover:bg-white/5"
      >
        <Star className={cn('h-4 w-4', isFavorite ? 'fill-amber-400 text-amber-400' : 'text-zinc-400')} />
      </button>
      <button className="rounded-md p-2 text-zinc-400 hover:bg-white/5">
        <MoreVertical className="h-4 w-4" />
      </button>
    </div>
  );
}

function formatDurationShort(seconds: number): string {
  const m = Math.floor(seconds / 60);
  const s = Math.floor(seconds % 60);
  if (m >= 60) {
    const h = Math.floor(m / 60);
    return `${h}:${String(m % 60).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
  }
  return `${m}:${String(s).padStart(2, '0')}`;
}
