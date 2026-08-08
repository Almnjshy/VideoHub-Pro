'use client';

import { useVideoHub } from '@/store/videohub';
import { cn } from '@/lib/utils';
import {
  Bell, CheckCircle2, AlertTriangle, Zap, RotateCcw, HardDrive, Plug, X,
} from 'lucide-react';
import { useState } from 'react';

const NOTIF_CONFIG: Record<string, { icon: React.ReactNode; color: string; bg: string }> = {
  task_completed: { icon: <CheckCircle2 className="h-4 w-4" />, color: 'text-emerald-300', bg: 'bg-emerald-500/10' },
  task_failed: { icon: <AlertTriangle className="h-4 w-4" />, color: 'text-red-300', bg: 'bg-red-500/10' },
  task_started: { icon: <Zap className="h-4 w-4" />, color: 'text-amber-300', bg: 'bg-amber-500/10' },
  task_retry: { icon: <RotateCcw className="h-4 w-4" />, color: 'text-orange-300', bg: 'bg-orange-500/10' },
  plugin_broken: { icon: <Plug className="h-4 w-4" />, color: 'text-red-300', bg: 'bg-red-500/10' },
  plugin_degraded: { icon: <Plug className="h-4 w-4" />, color: 'text-amber-300', bg: 'bg-amber-500/10' },
  storage_warning: { icon: <HardDrive className="h-4 w-4" />, color: 'text-amber-300', bg: 'bg-amber-500/10' },
};

export function NotificationsScreen() {
  const notifications = useVideoHub((s) => s.notifications);
  const unread = useVideoHub((s) => s.unreadNotifications);
  const markRead = useVideoHub((s) => s.markNotificationRead);

  return (
    <div className="space-y-3">
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-bold text-zinc-100">الإشعارات</h2>
        {unread > 0 && (
          <span className="rounded-full bg-amber-500/15 px-2 py-0.5 text-[10px] font-medium text-amber-300">
            {unread} غير مقروء
          </span>
        )}
      </div>

      {notifications.length === 0 ? (
        <div className="rounded-2xl border border-dashed border-white/10 bg-zinc-900/30 p-12 text-center">
          <Bell className="mx-auto h-8 w-8 text-zinc-700" />
          <p className="mt-2 text-sm text-zinc-500">لا توجد إشعارات</p>
          <p className="text-[10px] text-zinc-600">ستظهر هنا عند اكتمال أو فشل التنزيلات</p>
        </div>
      ) : (
        <div className="space-y-2">
          {notifications.map((n) => {
            const cfg = NOTIF_CONFIG[n.type] ?? {
              icon: <Bell className="h-4 w-4" />,
              color: 'text-zinc-300',
              bg: 'bg-zinc-500/10',
            };
            return (
              <button
                key={n.id}
                onClick={() => !n.read && markRead(n.id)}
                className={cn(
                  'flex w-full items-start gap-2.5 rounded-2xl border p-3 text-right transition-colors',
                  n.read
                    ? 'border-white/5 bg-zinc-900/30'
                    : 'border-amber-500/20 bg-amber-500/5',
                )}
              >
                <div className={cn('flex h-9 w-9 flex-shrink-0 items-center justify-center rounded-lg', cfg.bg, cfg.color)}>
                  {cfg.icon}
                </div>
                <div className="min-w-0 flex-1">
                  <div className="flex items-start justify-between gap-2">
                    <h4 className="text-xs font-semibold text-zinc-100">{n.title}</h4>
                    {!n.read && (
                      <span className="mt-1 h-2 w-2 flex-shrink-0 rounded-full bg-amber-400" />
                    )}
                  </div>
                  <p className="mt-0.5 line-clamp-2 text-[11px] text-zinc-400">{n.message}</p>
                  <div className="mt-1 flex items-center gap-2 text-[10px] text-zinc-500">
                    <span>{new Date(n.timestamp).toLocaleString('ar')}</span>
                    {n.pluginId && (
                      <>
                        <span>·</span>
                        <span className="font-mono uppercase">{n.pluginId}</span>
                      </>
                    )}
                  </div>
                </div>
              </button>
            );
          })}
        </div>
      )}
    </div>
  );
}
