'use client';

import { useState } from 'react';
import { useVideoHub } from '@/store/videohub';
import { cn } from '@/lib/utils';
import { Compass, Flame, TrendingUp, Sparkles, Play, Download, Eye, Clock } from 'lucide-react';

interface DiscoverItem {
  id: string;
  title: string;
  author: string;
  platformId: string;
  thumbnailGradient: string;
  views: string;
  likes: string;
  duration: string;
  category: 'trending' | 'music' | 'gaming' | 'news' | 'education' | 'tech';
}

// Deterministic data — NO Math.random / Date.now in render
const DISCOVER_ITEMS: DiscoverItem[] = [
  { id: '1', title: 'أفضل لحظات كرة القدم هذا الأسبوع', author: 'Sports HD', platformId: 'youtube', thumbnailGradient: 'from-emerald-500 to-blue-600', views: '2.4M', likes: '125K', duration: '8:24', category: 'trending' },
  { id: '2', title: 'مراجعة أحدث هاتف رائد لعام 2026', author: 'Tech Review', platformId: 'youtube', thumbnailGradient: 'from-purple-500 to-pink-600', views: '1.1M', likes: '88K', duration: '12:05', category: 'tech' },
  { id: '3', title: 'وصفة الكيك الأسهل على الإطلاق', author: 'Cooking Pro', platformId: 'tiktok', thumbnailGradient: 'from-amber-500 to-red-600', views: '5.7M', likes: '420K', duration: '0:45', category: 'trending' },
  { id: '4', title: 'جولة في أجمل مدن أوروبا', author: 'Travel Diaries', platformId: 'instagram', thumbnailGradient: 'from-cyan-500 to-blue-700', views: '890K', likes: '76K', duration: '2:15', category: 'trending' },
  { id: '5', title: 'شرح React 19 من الصفر للاحتراف', author: 'Code Academy', platformId: 'youtube', thumbnailGradient: 'from-blue-500 to-indigo-700', views: '340K', likes: '32K', duration: '45:18', category: 'education' },
  { id: '6', title: 'مقطع موسيقي هادئ للاسترخاء', author: 'Relax Music', platformId: 'soundcloud', thumbnailGradient: 'from-violet-500 to-purple-700', views: '1.8M', likes: '195K', duration: '30:00', category: 'music' },
  { id: '7', title: 'أفضل أهداف هذا الموسم', author: 'Football Zone', platformId: 'facebook', thumbnailGradient: 'from-green-500 to-emerald-700', views: '3.2M', likes: '210K', duration: '5:42', category: 'trending' },
  { id: '8', title: 'مشهد لا يُصدق من الطبيعة', author: 'Nature World', platformId: 'youtube', thumbnailGradient: 'from-teal-500 to-green-700', views: '4.5M', likes: '320K', duration: '10:30', category: 'trending' },
  { id: '9', title: 'بث مباشر: ورشة برمجة تفاعلية', author: 'Dev Stream', platformId: 'twitch', thumbnailGradient: 'from-purple-600 to-violet-800', views: '12K', likes: '1.2K', duration: 'LIVE', category: 'tech' },
  { id: '10', title: 'أخبار التقنية لهذا الأسبوع', author: 'Tech News', platformId: 'x', thumbnailGradient: 'from-zinc-600 to-zinc-800', views: '670K', likes: '45K', duration: '3:20', category: 'news' },
  { id: '11', title: 'أفضل لحظات البث المباشر', author: 'Gaming Hub', platformId: 'twitch', thumbnailGradient: 'from-pink-500 to-rose-700', views: '920K', likes: '78K', duration: '15:42', category: 'gaming' },
  { id: '12', title: 'دروس اللغة الإنجليزية للمبتدئين', author: 'Learn Easy', platformId: 'youtube', thumbnailGradient: 'from-orange-500 to-amber-700', views: '1.3M', likes: '110K', duration: '20:15', category: 'education' },
];

const CATEGORIES: Array<{ id: DiscoverItem['category'] | 'all'; label: string; icon: React.ReactNode }> = [
  { id: 'all', label: 'الكل', icon: <Sparkles className="h-4 w-4" /> },
  { id: 'trending', label: 'رائج', icon: <Flame className="h-4 w-4" /> },
  { id: 'music', label: 'موسيقى', icon: <Compass className="h-4 w-4" /> },
  { id: 'gaming', label: 'ألعاب', icon: <Play className="h-4 w-4" /> },
  { id: 'tech', label: 'تقنية', icon: <TrendingUp className="h-4 w-4" /> },
  { id: 'education', label: 'تعليم', icon: <Compass className="h-4 w-4" /> },
  { id: 'news', label: 'أخبار', icon: <Compass className="h-4 w-4" /> },
];

export function DiscoverScreen() {
  const plugins = useVideoHub((s) => s.plugins);
  const receiveShareIntent = useVideoHub((s) => s.receiveShareIntent);
  const [category, setCategory] = useState<DiscoverItem['category'] | 'all'>('all');

  const filtered = category === 'all'
    ? DISCOVER_ITEMS
    : DISCOVER_ITEMS.filter((i) => i.category === category);

  const handleDownload = (item: DiscoverItem) => {
    // Generate a mock URL for the platform
    const url = `https://${item.platformId}.com/discover/${item.id}`;
    receiveShareIntent(url, 'discover');
  };

  return (
    <div className="space-y-3">
      <div>
        <h2 className="text-lg font-bold text-zinc-100">اكتشف</h2>
        <p className="text-xs text-zinc-500">محتوى رائج عبر كل المنصات</p>
      </div>

      {/* Categories */}
      <div className="flex gap-1.5 overflow-x-auto pb-1">
        {CATEGORIES.map((cat) => (
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
          </button>
        ))}
      </div>

      {/* Featured item */}
      {filtered[0] && (
        <div className="relative overflow-hidden rounded-2xl border border-amber-500/20">
          <div className={cn('aspect-video bg-gradient-to-br', filtered[0].thumbnailGradient)}>
            <div className="flex h-full flex-col justify-end bg-gradient-to-t from-black/70 via-black/20 to-transparent p-4">
              <div className="mb-1 flex items-center gap-2">
                <span className="rounded bg-amber-500 px-1.5 py-0.5 text-[9px] font-bold text-zinc-950">مميز</span>
                <span className="text-[10px] text-white/80 uppercase">{filtered[0].platformId}</span>
              </div>
              <h3 className="text-sm font-bold text-white line-clamp-2">{filtered[0].title}</h3>
              <div className="mt-1 flex items-center gap-2 text-[10px] text-white/70">
                <span>{filtered[0].author}</span>
                <span>·</span>
                <span className="flex items-center gap-0.5"><Eye className="h-2.5 w-2.5" />{filtered[0].views}</span>
                <span>·</span>
                <span className="flex items-center gap-0.5"><Clock className="h-2.5 w-2.5" />{filtered[0].duration}</span>
              </div>
              <button
                onClick={() => handleDownload(filtered[0])}
                className="mt-2 flex h-9 w-fit items-center gap-1.5 rounded-lg bg-amber-500 px-3 text-[11px] font-semibold text-zinc-950 hover:bg-amber-400"
              >
                <Download className="h-3.5 w-3.5" />
                تنزيل الآن
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Grid */}
      <div className="grid grid-cols-2 gap-2">
        {filtered.slice(1).map((item) => {
          const plugin = plugins.find((p) => p.id === item.platformId);
          return (
            <button
              key={item.id}
              onClick={() => handleDownload(item)}
              className="group overflow-hidden rounded-xl border border-white/10 bg-zinc-900/60 text-right transition-all hover:border-white/20 active:scale-[0.98]"
            >
              <div className={cn('relative aspect-video bg-gradient-to-br', item.thumbnailGradient)}>
                {/* Platform badge */}
                <div
                  className="absolute right-1.5 top-1.5 flex h-5 w-5 items-center justify-center rounded text-[10px] font-bold text-white"
                  style={{ backgroundColor: plugin?.color ?? '#71717A' }}
                >
                  {plugin?.icon ?? '?'}
                </div>
                {/* Duration */}
                {item.duration !== 'LIVE' && (
                  <div className="absolute bottom-1.5 right-1.5 rounded bg-black/70 px-1 py-0.5 font-mono text-[9px] text-white">
                    {item.duration}
                  </div>
                )}
                {item.duration === 'LIVE' && (
                  <div className="absolute bottom-1.5 right-1.5 flex items-center gap-0.5 rounded bg-red-600 px-1 py-0.5 text-[9px] font-bold text-white">
                    <span className="h-1 w-1 rounded-full bg-white animate-pulse" />
                    مباشر
                  </div>
                )}
                {/* Hover overlay */}
                <div className="absolute inset-0 flex items-center justify-center bg-black/0 opacity-0 transition-all group-hover:bg-black/40 group-hover:opacity-100">
                  <div className="flex h-9 w-9 items-center justify-center rounded-full bg-amber-500/90 text-zinc-950">
                    <Download className="h-4 w-4" />
                  </div>
                </div>
              </div>
              <div className="p-2">
                <h4 className="line-clamp-2 text-[11px] font-semibold text-zinc-100">{item.title}</h4>
                <div className="mt-1 flex items-center gap-1.5 text-[9px] text-zinc-500">
                  <span className="truncate">{item.author}</span>
                  <span>·</span>
                  <span className="flex items-center gap-0.5"><Eye className="h-2.5 w-2.5" />{item.views}</span>
                </div>
              </div>
            </button>
          );
        })}
      </div>
    </div>
  );
}
