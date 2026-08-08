'use client';

import { useState } from 'react';
import { useVideoHub } from '@/store/videohub';
import { cn } from '@/lib/utils';
import { Search, X, Download, TrendingUp, Clock } from 'lucide-react';

interface SearchResult {
  id: string;
  title: string;
  author: string;
  platformId: string;
  thumbnailGradient: string;
  views: string;
  duration: string;
  uploadedAt: string;
}

// Deterministic search results — based on hash of query
function generateResults(query: string): SearchResult[] {
  if (!query.trim()) return [];

  let h = 0;
  for (let i = 0; i < query.length; i++) {
    h = ((h << 5) - h) + query.charCodeAt(i);
    h |= 0;
  }
  const seed = Math.abs(h);

  const platforms = ['youtube', 'tiktok', 'facebook', 'instagram', 'x', 'vimeo'];
  const gradients = [
    'from-emerald-500 to-blue-600',
    'from-purple-500 to-pink-600',
    'from-amber-500 to-red-600',
    'from-cyan-500 to-blue-700',
    'from-violet-500 to-purple-700',
    'from-orange-500 to-rose-700',
  ];

  const results: SearchResult[] = [];
  for (let i = 0; i < 8; i++) {
    const s = (seed + i * 31) % 1000;
    results.push({
      id: `${i}`,
      title: `${query} — نتيجة ${i + 1}`,
      author: ['قناة المعرفة', 'Tech Plus', 'World Explorer', 'Creative Studio', 'Daily Vlog', 'Pro Academy'][(seed + i) % 6],
      platformId: platforms[(seed + i) % platforms.length],
      thumbnailGradient: gradients[(seed + i) % gradients.length],
      views: `${1 + (s % 9)}.${s % 9}M`,
      duration: `${(s % 30) + 1}:${String(s % 60).padStart(2, '0')}`,
      uploadedAt: ['منذ يوم', 'منذ 3 أيام', 'منذ أسبوع', 'منذ شهر', 'منذ ساعة'][(seed + i) % 5],
    });
  }
  return results;
}

const TRENDING_SEARCHES = [
  'موسيقى هادئة',
  'وصفات سريعة',
  'أهداف كرة القدم',
  'دروس برمجة',
  'وثائقي طبيعة',
  'أفلام قصيرة',
  'مراجعات تقنية',
  'نصائح تطوير الذات',
];

const RECENT_SEARCHES_KEY = 'videohub_recent_searches';

export function SearchScreen() {
  const plugins = useVideoHub((s) => s.plugins);
  const receiveShareIntent = useVideoHub((s) => s.receiveShareIntent);
  const [query, setQuery] = useState('');
  const [searched, setSearched] = useState('');
  const [recentSearches, setRecentSearches] = useState<string[]>([]);
  const [selectedPlatforms, setSelectedPlatforms] = useState<Set<string>>(new Set());

  // Load recent searches on mount
  useState(() => {
    if (typeof window === 'undefined') return;
    try {
      const stored = localStorage.getItem(RECENT_SEARCHES_KEY);
      if (stored) setRecentSearches(JSON.parse(stored));
    } catch {
      // ignore
    }
  });

  const results = searched ? generateResults(searched) : [];

  const handleSearch = (q: string) => {
    if (!q.trim()) return;
    setSearched(q);
    setQuery(q);
    // Save to recent
    const updated = [q, ...recentSearches.filter((s) => s !== q)].slice(0, 8);
    setRecentSearches(updated);
    try {
      localStorage.setItem(RECENT_SEARCHES_KEY, JSON.stringify(updated));
    } catch {
      // ignore
    }
  };

  const handleDownload = (result: SearchResult) => {
    const url = `https://${result.platformId}.com/search/${result.id}`;
    receiveShareIntent(url, 'search');
  };

  const togglePlatform = (id: string) => {
    setSelectedPlatforms((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  };

  const filteredResults = selectedPlatforms.size > 0
    ? results.filter((r) => selectedPlatforms.has(r.platformId))
    : results;

  return (
    <div className="space-y-3">
      <h2 className="text-lg font-bold text-zinc-100">البحث</h2>

      {/* Search bar */}
      <div className="relative">
        <Search className="absolute right-3 top-1/2 h-4 w-4 -translate-y-1/2 text-zinc-500" />
        <input
          type="text"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && handleSearch(query)}
          placeholder="ابحث عن فيديوهات، موسيقى، صور..."
          className="w-full rounded-xl border border-white/10 bg-zinc-900/60 py-3 pr-10 pl-10 text-sm text-zinc-100 placeholder:text-zinc-600 focus:border-amber-500/40 focus:outline-none"
        />
        {query && (
          <button
            onClick={() => { setQuery(''); setSearched(''); }}
            className="absolute left-2 top-1/2 -translate-y-1/2 rounded p-1 text-zinc-500 hover:text-zinc-200"
          >
            <X className="h-4 w-4" />
          </button>
        )}
      </div>

      {/* Platform filter */}
      {searched && (
        <div className="flex gap-1.5 overflow-x-auto pb-1">
          {plugins.filter((p) => ['youtube', 'tiktok', 'facebook', 'instagram', 'x', 'vimeo'].includes(p.id)).map((p) => (
            <button
              key={p.id}
              onClick={() => togglePlatform(p.id)}
              className={cn(
                'flex h-8 flex-shrink-0 items-center gap-1 rounded-full border px-2.5 text-[11px] font-medium transition-colors',
                selectedPlatforms.has(p.id)
                  ? 'border-amber-500/50 bg-amber-500/15 text-amber-200'
                  : 'border-white/10 bg-white/5 text-zinc-300',
              )}
            >
              <span
                className="flex h-4 w-4 items-center justify-center rounded text-[9px] font-bold text-white"
                style={{ backgroundColor: p.color }}
              >
                {p.icon}
              </span>
              {p.nameAr}
            </button>
          ))}
        </div>
      )}

      {/* No search yet — show trending + recent */}
      {!searched && (
        <>
          {recentSearches.length > 0 && (
            <section>
              <h3 className="mb-2 flex items-center gap-1.5 text-xs font-semibold text-zinc-300">
                <Clock className="h-3.5 w-3.5" />
                عمليات البحث الأخيرة
              </h3>
              <div className="flex flex-wrap gap-1.5">
                {recentSearches.map((s) => (
                  <button
                    key={s}
                    onClick={() => handleSearch(s)}
                    className="rounded-full border border-white/10 bg-white/5 px-3 py-1.5 text-[11px] text-zinc-300 hover:bg-white/10"
                  >
                    {s}
                  </button>
                ))}
              </div>
            </section>
          )}

          <section>
            <h3 className="mb-2 flex items-center gap-1.5 text-xs font-semibold text-zinc-300">
              <TrendingUp className="h-3.5 w-3.5" />
              عمليات بحث رائجة
            </h3>
            <div className="grid grid-cols-2 gap-1.5">
              {TRENDING_SEARCHES.map((s, idx) => (
                <button
                  key={s}
                  onClick={() => handleSearch(s)}
                  className="flex items-center gap-2 rounded-xl border border-white/10 bg-zinc-900/60 p-2.5 text-right hover:bg-white/5"
                >
                  <span className="font-mono text-[10px] text-zinc-600">{idx + 1}</span>
                  <span className="flex-1 text-[11px] text-zinc-200">{s}</span>
                </button>
              ))}
            </div>
          </section>
        </>
      )}

      {/* Results */}
      {searched && (
        <section>
          <div className="mb-2 flex items-center justify-between">
            <h3 className="text-xs font-semibold text-zinc-300">
              نتائج البحث عن "{searched}"
            </h3>
            <span className="text-[10px] text-zinc-500">{filteredResults.length} نتيجة</span>
          </div>

          {filteredResults.length === 0 ? (
            <div className="rounded-2xl border border-dashed border-white/10 bg-zinc-900/30 p-8 text-center">
              <Search className="mx-auto h-8 w-8 text-zinc-700" />
              <p className="mt-2 text-sm text-zinc-500">لا توجد نتائج</p>
            </div>
          ) : (
            <div className="space-y-2">
              {filteredResults.map((result) => {
                const plugin = plugins.find((p) => p.id === result.platformId);
                return (
                  <button
                    key={result.id}
                    onClick={() => handleDownload(result)}
                    className="flex w-full items-center gap-2.5 rounded-xl border border-white/10 bg-zinc-900/60 p-2 text-right hover:border-white/20 active:scale-[0.99]"
                  >
                    <div className={cn('relative h-14 w-20 flex-shrink-0 overflow-hidden rounded-md bg-gradient-to-br', result.thumbnailGradient)}>
                      <div className="absolute bottom-1 right-1 rounded bg-black/70 px-1 py-0.5 font-mono text-[9px] text-white">
                        {result.duration}
                      </div>
                    </div>
                    <div className="min-w-0 flex-1">
                      <h4 className="line-clamp-2 text-xs font-semibold text-zinc-100">{result.title}</h4>
                      <div className="mt-0.5 flex items-center gap-1.5 text-[10px] text-zinc-500">
                        <span
                          className="flex h-3.5 w-3.5 items-center justify-center rounded text-[8px] font-bold text-white"
                          style={{ backgroundColor: plugin?.color ?? '#71717A' }}
                        >
                          {plugin?.icon}
                        </span>
                        <span className="truncate">{result.author}</span>
                        <span>·</span>
                        <span>{result.views}</span>
                        <span>·</span>
                        <span>{result.uploadedAt}</span>
                      </div>
                    </div>
                    <div className="flex h-9 w-9 flex-shrink-0 items-center justify-center rounded-lg bg-amber-500/10 text-amber-300">
                      <Download className="h-4 w-4" />
                    </div>
                  </button>
                );
              })}
            </div>
          )}
        </section>
      )}
    </div>
  );
}
