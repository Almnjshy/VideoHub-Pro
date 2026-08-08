'use client';

import { useState } from 'react';
import { useVideoHub } from '@/store/videohub';
import { TaskCard } from './TaskCard';
import { Trash2, Filter } from 'lucide-react';
import { cn } from '@/lib/utils';
import type { TaskStatus } from '@/lib/videohub/types';

type FilterTab = 'all' | 'active' | 'completed' | 'failed';

const TABS: Array<{ id: FilterTab; label: string; statuses: TaskStatus[] }> = [
  { id: 'all', label: 'الكل', statuses: [] },
  { id: 'active', label: 'النشطة', statuses: ['queued', 'downloading', 'paused', 'retrying'] },
  { id: 'completed', label: 'المكتملة', statuses: ['completed'] },
  { id: 'failed', label: 'الفاشلة', statuses: ['failed'] },
];

export function DownloadsScreen() {
  const tasks = useVideoHub((s) => s.tasks);
  const clearCompleted = useVideoHub((s) => s.clearCompleted);
  const [tab, setTab] = useState<FilterTab>('all');

  const filtered = tasks.filter((t) => {
    if (tab === 'all') return true;
    return TABS.find((tb) => tb.id === tab)!.statuses.includes(t.status);
  });

  const counts = {
    all: tasks.length,
    active: tasks.filter((t) => TABS[1].statuses.includes(t.status)).length,
    completed: tasks.filter((t) => t.status === 'completed').length,
    failed: tasks.filter((t) => t.status === 'failed').length,
  };

  return (
    <div className="space-y-3">
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-bold text-zinc-100">التنزيلات</h2>
        {counts.completed > 0 && (
          <button
            onClick={clearCompleted}
            className="flex h-9 items-center gap-1.5 rounded-lg border border-white/10 px-2.5 text-[11px] text-zinc-400 hover:bg-red-500/10 hover:text-red-300"
          >
            <Trash2 className="h-3 w-3" />
            مسح المكتملة
          </button>
        )}
      </div>

      {/* Filter tabs — horizontal scroll on mobile */}
      <div className="flex items-center gap-1 overflow-x-auto pb-1">
        <Filter className="h-3.5 w-3.5 flex-shrink-0 text-zinc-500" />
        {TABS.map((tb) => (
          <button
            key={tb.id}
            onClick={() => setTab(tb.id)}
            className={cn(
              'flex h-9 flex-shrink-0 items-center gap-1.5 rounded-lg px-3 text-xs font-medium transition-colors',
              tab === tb.id
                ? 'bg-amber-500/15 text-amber-200'
                : 'text-zinc-400 hover:bg-white/5 hover:text-zinc-200',
            )}
          >
            {tb.label}
            <span className="font-mono text-[10px] opacity-70">{counts[tb.id]}</span>
          </button>
        ))}
      </div>

      {/* Task list */}
      {filtered.length > 0 ? (
        <div className="space-y-2">
          {filtered.map((t) => (
            <TaskCard key={t.id} task={t} />
          ))}
        </div>
      ) : (
        <div className="rounded-2xl border border-dashed border-white/10 bg-zinc-900/30 p-12 text-center">
          <Filter className="mx-auto h-8 w-8 text-zinc-700" />
          <p className="mt-2 text-sm text-zinc-500">لا توجد مهام في هذا التصنيف</p>
        </div>
      )}
    </div>
  );
}
