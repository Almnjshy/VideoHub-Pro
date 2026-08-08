'use client';

import { useEffect, useRef, useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Clipboard, X, Download, Zap } from 'lucide-react';
import { useVideoHub, findFrontendPluginForUrl } from '@/store/videohub';
import { cn } from '@/lib/utils';

/**
 * مراقب الحافظة — يكتشف الروابط المنسوخة تلقائياً
 * يعرض overlay صغير يطلب من المستخدم التنزيل
 */
export function ClipboardMonitor() {
  const [detectedUrl, setDetectedUrl] = useState<string | null>(null);
  const [dismissed, setDismissed] = useState<Set<string>>(new Set());
  const lastCheckRef = useRef<string>('');
  const plugins = useVideoHub((s) => s.plugins);
  const receiveShareIntent = useVideoHub((s) => s.receiveShareIntent);

  useEffect(() => {
    if (typeof window === 'undefined') return;
    if (!navigator.clipboard?.readText) return;

    let active = true;

    const checkClipboard = async () => {
      if (!active) return;
      try {
        const text = await navigator.clipboard.readText();
        if (!text || text === lastCheckRef.current) return;
        lastCheckRef.current = text;

        // Check if text looks like a URL
        const urlMatch = text.match(/https?:\/\/[^\s]+/i);
        if (!urlMatch) return;

        const url = urlMatch[0];

        // Check if dismissed
        if (dismissed.has(url)) return;

        // Check if any plugin can handle it
        const plugin = findFrontendPluginForUrl(url);
        if (!plugin) return;

        setDetectedUrl(url);
      } catch {
        // Permission denied or other error — silent
      }
    };

    // Check on mount and on focus
    checkClipboard();
    const onFocus = () => checkClipboard();
    window.addEventListener('focus', onFocus);

    // Poll every 2 seconds
    const interval = setInterval(checkClipboard, 2000);

    return () => {
      active = false;
      window.removeEventListener('focus', onFocus);
      clearInterval(interval);
    };
  }, [dismissed, plugins]);

  const handleDownload = () => {
    if (!detectedUrl) return;
    receiveShareIntent(detectedUrl, 'clipboard');
    setDismissed((prev) => new Set(prev).add(detectedUrl));
    setDetectedUrl(null);
  };

  const handleDismiss = () => {
    if (!detectedUrl) return;
    setDismissed((prev) => new Set(prev).add(detectedUrl));
    setDetectedUrl(null);
  };

  if (!detectedUrl) return null;
  const plugin = findFrontendPluginForUrl(detectedUrl);
  if (!plugin) return null;

  return (
    <AnimatePresence>
      <motion.div
        initial={{ opacity: 0, y: -20 }}
        animate={{ opacity: 1, y: 0 }}
        exit={{ opacity: 0, y: -20 }}
        className="fixed left-1/2 top-4 z-[60] w-[calc(100%-2rem)] max-w-md -translate-x-1/2 lg:top-20"
      >
        <div className="overflow-hidden rounded-2xl border border-amber-500/40 bg-zinc-950/95 shadow-2xl backdrop-blur-xl">
          <div className="flex items-center gap-3 p-3">
            {/* Icon */}
            <div className="flex h-10 w-10 flex-shrink-0 items-center justify-center rounded-xl bg-amber-500/15 text-amber-300">
              <Clipboard className="h-5 w-5" />
            </div>

            {/* Content */}
            <div className="min-w-0 flex-1">
              <div className="flex items-center gap-1.5">
                <span className="text-xs font-semibold text-zinc-100">تم اكتشاف رابط</span>
                <span
                  className="flex h-4 w-4 items-center justify-center rounded text-[9px] font-bold text-white"
                  style={{ backgroundColor: plugin.color }}
                >
                  {plugin.icon}
                </span>
                <span className="text-[10px] text-zinc-400">{plugin.nameAr}</span>
              </div>
              <div className="mt-0.5 truncate font-mono text-[10px] text-zinc-500" dir="ltr">
                {detectedUrl}
              </div>
            </div>

            {/* Actions */}
            <button
              onClick={handleDownload}
              className="flex h-9 items-center gap-1.5 rounded-lg bg-amber-500 px-3 text-[11px] font-semibold text-zinc-950 hover:bg-amber-400"
            >
              <Download className="h-3.5 w-3.5" />
              تنزيل
            </button>
            <button
              onClick={handleDismiss}
              className="rounded-lg p-2 text-zinc-500 hover:bg-white/5 hover:text-zinc-200"
              aria-label="إغلاق"
            >
              <X className="h-4 w-4" />
            </button>
          </div>
          <div className="flex items-center gap-1 bg-amber-500/5 px-3 py-1.5 text-[10px] text-amber-300/80">
            <Zap className="h-2.5 w-2.5" />
            تم اكتشاف الرابط في الحافظة — اضغط تنزيل للمتابعة
          </div>
        </div>
      </motion.div>
    </AnimatePresence>
  );
}
