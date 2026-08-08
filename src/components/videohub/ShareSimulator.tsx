'use client';

import { useState } from 'react';
import { Share2, Sparkles, ChevronDown, Link as LinkIcon } from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';
import { useVideoHub, findFrontendPluginForUrl } from '@/store/videohub';
import { cn } from '@/lib/utils';

const SAMPLE_URLS = [
  { label: 'يوتيوب — فيديو تعليمي', url: 'https://www.youtube.com/watch?v=dQw4w9WgXcQ' },
  { label: 'تيك توك — مقطع قصير', url: 'https://www.tiktok.com/@user/video/7234567890123' },
  { label: 'فيسبوك — فيديو', url: 'https://www.facebook.com/watch?v=1234567890' },
  { label: 'إكس — مقطع', url: 'https://x.com/user/status/1234567890' },
  { label: 'إنستغرام — Reel', url: 'https://www.instagram.com/reel/Cabcdefg/' },
  { label: 'فيميو — فيديو HD', url: 'https://vimeo.com/123456789' },
  { label: 'ريديت — فيديو', url: 'https://www.reddit.com/r/videos/comments/abc123/title/' },
  { label: 'تويتش — مقطع', url: 'https://www.twitch.tv/clip/abc123' },
  { label: 'ساوند كلاود — مقطع صوتي', url: 'https://soundcloud.com/artist/track' },
  { label: 'بينتريست — صورة', url: 'https://www.pinterest.com/pin/123456789/' },
  { label: 'لينكدإن — فيديو', url: 'https://www.linkedin.com/posts/activity-123456789' },
  { label: 'ستريمابل — مقطع', url: 'https://streamable.com/abcdef' },
];

export function ShareSimulator() {
  const [open, setOpen] = useState(false);
  const [url, setUrl] = useState('');
  const receiveShareIntent = useVideoHub((s) => s.receiveShareIntent);
  const plugins = useVideoHub((s) => s.plugins);

  const detected = url.trim() ? findFrontendPluginForUrl(url.trim()) : undefined;
  const detectedPlugin = detected ? plugins.find((p) => p.id === detected.id) : undefined;

  const handleSend = (link: string) => {
    if (!link.trim()) return;
    receiveShareIntent(link.trim(), 'browser');
    setUrl('');
    setOpen(false);
  };

  return (
    <div className="rounded-2xl border border-amber-500/20 bg-gradient-to-br from-amber-500/5 to-zinc-900/40 p-4">
      <div className="mb-3 flex items-center justify-between">
        <div className="flex items-center gap-2">
          <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-amber-500/15 text-amber-300">
            <Sparkles className="h-4 w-4" />
          </div>
          <div>
            <h3 className="text-sm font-semibold text-zinc-100">محاكاة المشاركة الذكية</h3>
            <p className="text-[11px] text-zinc-500">أرسل رابطًا كأنه قادم من تطبيق خارجي</p>
          </div>
        </div>
        <button
          onClick={() => setOpen((v) => !v)}
          className="flex h-11 items-center gap-1.5 rounded-lg border border-white/10 px-3 text-xs font-medium text-zinc-200 hover:bg-white/5"
        >
          <Share2 className="h-3.5 w-3.5" />
          <span className="hidden sm:inline">إرسال رابط</span>
          <ChevronDown className={cn('h-3 w-3 transition-transform', open && 'rotate-180')} />
        </button>
      </div>

      <AnimatePresence>
        {open && (
          <motion.div
            initial={{ opacity: 0, height: 0 }}
            animate={{ opacity: 1, height: 'auto' }}
            exit={{ opacity: 0, height: 0 }}
            className="overflow-hidden"
          >
            <div className="space-y-3 border-t border-white/5 pt-3">
              {/* URL input */}
              <div>
                <div className="relative">
                  <LinkIcon className="absolute right-3 top-1/2 h-4 w-4 -translate-y-1/2 text-zinc-500" />
                  <input
                    type="text"
                    value={url}
                    onChange={(e) => setUrl(e.target.value)}
                    onKeyDown={(e) => e.key === 'Enter' && handleSend(url)}
                    placeholder="الصق رابط الفيديو هنا..."
                    dir="ltr"
                    className="w-full rounded-lg border border-white/10 bg-zinc-950/60 py-3 pr-10 pl-24 text-left text-sm text-zinc-100 placeholder:text-zinc-600 focus:border-amber-500/40 focus:outline-none"
                  />
                  <button
                    onClick={() => handleSend(url)}
                    disabled={!url.trim()}
                    className="absolute left-1.5 top-1/2 -translate-y-1/2 rounded-md bg-amber-500 px-3 py-1.5 text-xs font-semibold text-zinc-950 transition-colors hover:bg-amber-400 disabled:opacity-40"
                  >
                    إرسال
                  </button>
                </div>

                {url.trim() && (
                  <div className="mt-2 flex items-center gap-2 text-[11px]">
                    {detectedPlugin ? (
                      <>
                        <div
                          className="flex h-5 w-5 items-center justify-center rounded text-[10px] font-bold text-white"
                          style={{ backgroundColor: detectedPlugin.color }}
                        >
                          {detectedPlugin.icon}
                        </div>
                        <span className="text-zinc-300">تم التعرف: {detectedPlugin.nameAr}</span>
                        <span className="text-zinc-600">· v{detectedPlugin.version}</span>
                      </>
                    ) : (
                      <span className="text-red-300">لا توجد وحدة مناسبة لهذا الرابط</span>
                    )}
                  </div>
                )}
              </div>

              {/* Quick samples */}
              <div>
                <div className="mb-1.5 text-[10px] uppercase tracking-wider text-zinc-500">روابط لتجربة سريعة</div>
                <div className="grid grid-cols-1 gap-1.5 sm:grid-cols-2">
                  {SAMPLE_URLS.map((s) => {
                    const sp = findFrontendPluginForUrl(s.url);
                    const spInfo = sp ? plugins.find((p) => p.id === sp.id) : undefined;
                    return (
                      <button
                        key={s.url}
                        onClick={() => handleSend(s.url)}
                        className="group flex h-11 items-center gap-2 rounded-lg border border-white/5 bg-white/[0.02] px-2.5 text-right text-[11px] hover:border-amber-500/30 hover:bg-amber-500/5"
                      >
                        {spInfo && (
                          <div
                            className="flex h-6 w-6 items-center justify-center rounded text-[11px] font-bold text-white"
                            style={{ backgroundColor: spInfo.color }}
                          >
                            {spInfo.icon}
                          </div>
                        )}
                        <span className="flex-1 text-zinc-300 group-hover:text-zinc-100">{s.label}</span>
                      </button>
                    );
                  })}
                </div>
              </div>
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}
