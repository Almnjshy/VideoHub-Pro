'use client';

import { useEffect, useState, useSyncExternalStore } from 'react';
import { useVideoHub, startEngine, stopEngine } from '@/store/videohub';
import { HomeScreen } from '@/components/videohub/HomeScreen';
import { DownloadsScreen } from '@/components/videohub/DownloadsScreen';
import { PluginsScreen } from '@/components/videohub/PluginsScreen';
import { SettingsScreen } from '@/components/videohub/SettingsScreen';
import { StatsScreen } from '@/components/videohub/StatsScreen';
import { NotificationsScreen } from '@/components/videohub/NotificationsScreen';
import { LibraryScreen } from '@/components/videohub/LibraryScreen';
import { DiscoverScreen } from '@/components/videohub/DiscoverScreen';
import { SearchScreen } from '@/components/videohub/SearchScreen';
import { MediaPlayer } from '@/components/videohub/MediaPlayer';
import { MediaWorkspace } from '@/components/videohub/MediaWorkspace';
import { ClipboardMonitor } from '@/components/videohub/ClipboardMonitor';
import { SmartShareOverlay } from '@/components/videohub/SmartShareOverlay';
import {
  Home, Download, Plug, Settings as SettingsIcon, BarChart3, Zap, Bell,
  Library, Compass, Search as SearchIcon,
} from 'lucide-react';
import { cn } from '@/lib/utils';

type Tab = 'home' | 'downloads' | 'library' | 'discover' | 'search' | 'plugins' | 'stats' | 'notifications' | 'settings';

const TABS: Array<{ id: Tab; label: string; icon: React.ReactNode }> = [
  { id: 'home', label: 'الرئيسية', icon: <Home className="h-5 w-5" /> },
  { id: 'downloads', label: 'التنزيلات', icon: <Download className="h-5 w-5" /> },
  { id: 'library', label: 'الوسائط', icon: <Library className="h-5 w-5" /> },
  { id: 'discover', label: 'اكتشف', icon: <Compass className="h-5 w-5" /> },
  { id: 'search', label: 'بحث', icon: <SearchIcon className="h-5 w-5" /> },
  { id: 'plugins', label: 'الوحدات', icon: <Plug className="h-5 w-5" /> },
  { id: 'stats', label: 'الإحصائيات', icon: <BarChart3 className="h-5 w-5" /> },
  { id: 'notifications', label: 'الإشعارات', icon: <Bell className="h-5 w-5" /> },
  { id: 'settings', label: 'الإعدادات', icon: <SettingsIcon className="h-5 w-5" /> },
];

// Visible tabs in bottom nav (most used 5)
const BOTTOM_TABS: Tab[] = ['home', 'downloads', 'library', 'discover', 'search'];

// Clock subscription — uses useSyncExternalStore to avoid hydration mismatch
// Server renders empty string, client subscribes and updates.
const clockStore = {
  listeners: new Set<() => void>(),
  value: '',
  init() {
    if (typeof window === 'undefined') return;
    const update = () => {
      const next = new Date().toLocaleTimeString('ar', { hour: '2-digit', minute: '2-digit' });
      if (next !== this.value) {
        this.value = next;
        this.listeners.forEach((l) => l());
      }
    };
    update();
    setInterval(update, 30000);
  },
  subscribe(l: () => void) {
    this.listeners.add(l);
    if (this.listeners.size === 1) this.init();
    return () => { this.listeners.delete(l); };
  },
  getSnapshot() { return this.value; },
};

export default function HomeApp() {
  const [tab, setTab] = useState<Tab>('home');
  const [deviceMode, setDeviceMode] = useState<'phone' | 'tablet'>('phone');
  const clock = useSyncExternalStore(clockStore.subscribe.bind(clockStore), clockStore.getSnapshot.bind(clockStore), () => '');
  const tasks = useVideoHub((s) => s.tasks);
  const plugins = useVideoHub((s) => s.plugins);
  const unreadNotifications = useVideoHub((s) => s.unreadNotifications);
  const activeCount = tasks.filter((t) => t.status === 'downloading' || t.status === 'queued').length;
  const brokenCount = Object.values(useVideoHub((s) => s.pluginHealth)).filter((h) => h.status !== 'healthy').length;

  useEffect(() => {
    startEngine();
    return () => stopEngine();
  }, []);

  return (
    <div className="min-h-screen bg-zinc-950 flex flex-col items-center justify-start py-3 lg:py-6">
      {/* Device Mode Switcher (Desktop only) */}
      <div className="hidden lg:flex items-center gap-2 mb-4">
        <span className="text-[11px] text-zinc-500">وضع المعاينة:</span>
        <div className="flex gap-1 rounded-full border border-white/10 bg-zinc-900/60 p-1">
          <button
            onClick={() => setDeviceMode('phone')}
            className={cn(
              'rounded-full px-3 py-1 text-[11px] font-medium transition-colors',
              deviceMode === 'phone' ? 'bg-amber-500/15 text-amber-200' : 'text-zinc-400 hover:text-zinc-200',
            )}
          >
            📱 جوال (390px)
          </button>
          <button
            onClick={() => setDeviceMode('tablet')}
            className={cn(
              'rounded-full px-3 py-1 text-[11px] font-medium transition-colors',
              deviceMode === 'tablet' ? 'bg-amber-500/15 text-amber-200' : 'text-zinc-400 hover:text-zinc-200',
            )}
          >
            📱 تابلت (768px)
          </button>
        </div>
      </div>

      {/* App Container — NOT fullscreen, normal scrolling page */}
      <div
        className={cn(
          'relative bg-zinc-950 shadow-2xl transition-all w-full',
          // Desktop: framed device mockup with fixed height + internal scroll
          'lg:h-auto lg:rounded-[2rem] lg:border-[10px] lg:border-zinc-800',
          deviceMode === 'phone' ? 'lg:w-[390px] lg:h-[844px]' : 'lg:w-[768px] lg:h-[1024px]',
          // Mobile: natural height, no fixed screen height, page scrolls normally
          'min-h-[100vh] lg:min-h-0',
        )}
      >
        {/* Notch (desktop frame only) */}
        <div className="hidden lg:flex absolute top-0 left-1/2 -translate-x-1/2 h-6 w-32 bg-zinc-800 rounded-b-2xl items-center justify-center gap-2 z-50">
          <div className="h-1.5 w-1.5 rounded-full bg-zinc-700" />
          <div className="h-1 w-8 rounded-full bg-zinc-700" />
        </div>

        {/* Status Bar (desktop frame only) */}
        <div className="hidden lg:flex absolute top-1.5 right-4 left-4 justify-between items-center text-[10px] text-zinc-500 z-40">
          <span suppressHydrationWarning>{clock || '--:--'}</span>
          <span className="flex items-center gap-1">
            <span>●●●</span>
            <span>5G</span>
            <span className="ml-1">100%</span>
          </span>
        </div>

        {/* Top App Bar — sticky at top of scrolling page */}
        <header className="sticky top-0 z-30 bg-zinc-950/80 backdrop-blur-xl border-b border-white/5 lg:mt-8">
          <div className="flex h-14 items-center justify-between px-4">
            {/* Logo */}
            <button
              onClick={() => setTab('home')}
              className="flex items-center gap-2"
            >
              <div className="relative flex h-9 w-9 items-center justify-center rounded-xl bg-gradient-to-br from-amber-400 to-amber-600 text-zinc-950">
                <Zap className="h-5 w-5" fill="currentColor" />
                <span className="absolute -bottom-0.5 -right-0.5 h-2.5 w-2.5 rounded-full bg-emerald-400 ring-2 ring-zinc-950" />
              </div>
              <div className="text-left">
                <div className="text-sm font-bold leading-none">VideoHub Pro</div>
                <div className="text-[9px] text-zinc-500">Enterprise · v3.0</div>
              </div>
            </button>

            {/* Right actions */}
            <div className="flex items-center gap-1">
              <button
                onClick={() => setTab('notifications')}
                className="relative rounded-lg p-2 text-zinc-400 hover:bg-white/5 hover:text-zinc-200 min-w-[44px] min-h-[44px] flex items-center justify-center"
                aria-label="الإشعارات"
              >
                <Bell className="h-5 w-5" />
                {unreadNotifications > 0 && (
                  <span className="absolute right-1 top-1 flex h-4 min-w-4 items-center justify-center rounded-full bg-amber-500 px-1 text-[9px] font-bold text-zinc-950">
                    {unreadNotifications > 99 ? '99+' : unreadNotifications}
                  </span>
                )}
              </button>
              {/* Secondary tabs (stats/plugins/settings) */}
              <button
                onClick={() => setTab('settings')}
                className="rounded-lg p-2 text-zinc-400 hover:bg-white/5 hover:text-zinc-200 min-w-[44px] min-h-[44px] flex items-center justify-center"
                aria-label="الإعدادات"
              >
                <SettingsIcon className="h-5 w-5" />
              </button>
            </div>
          </div>
        </header>

        {/* Main Content — natural flow, no fixed height on mobile */}
        <main className={cn(
          'px-3 py-4 pb-24',
          // Desktop frame: internal scroll with fixed height
          deviceMode === 'phone' ? 'lg:h-[calc(844px-14rem)] lg:overflow-y-auto' : 'lg:h-[calc(1024px-14rem)] lg:overflow-y-auto',
        )}>
          {tab === 'home' && <HomeScreen onNavigate={setTab} />}
          {tab === 'downloads' && <DownloadsScreen />}
          {tab === 'library' && <LibraryScreen />}
          {tab === 'discover' && <DiscoverScreen />}
          {tab === 'search' && <SearchScreen />}
          {tab === 'plugins' && <PluginsScreen />}
          {tab === 'stats' && <StatsScreen />}
          {tab === 'notifications' && <NotificationsScreen />}
          {tab === 'settings' && <SettingsScreen />}
        </main>

        {/* Bottom Navigation — sticky on mobile (natural flow), absolute on desktop frame */}
        <nav className="sticky bottom-0 left-0 right-0 z-30 border-t border-white/5 bg-zinc-950/95 backdrop-blur-xl lg:absolute">
          <div className="grid grid-cols-5 gap-0 px-1 py-1.5">
            {TABS.filter((t) => BOTTOM_TABS.includes(t.id)).map((tb) => {
              const isActive = tab === tb.id;
              const badge =
                tb.id === 'downloads' ? activeCount :
                tb.id === 'notifications' ? unreadNotifications :
                0;

              return (
                <button
                  key={tb.id}
                  onClick={() => setTab(tb.id)}
                  className={cn(
                    'relative flex flex-col items-center justify-center gap-0.5 py-1.5 min-h-[52px] transition-colors',
                    isActive ? 'text-amber-300' : 'text-zinc-500 hover:text-zinc-300',
                  )}
                >
                  <div className="relative">
                    <div className={cn(
                      'flex h-7 w-12 items-center justify-center rounded-full transition-colors',
                      isActive && 'bg-amber-500/15',
                    )}>
                      {tb.icon}
                    </div>
                    {badge > 0 && (
                      <span className="absolute -right-1 -top-1 flex h-3.5 min-w-3.5 items-center justify-center rounded-full px-1 text-[8px] font-bold bg-amber-500 text-zinc-950">
                        {badge > 99 ? '99+' : badge}
                      </span>
                    )}
                  </div>
                  <span className="text-[9px] font-medium leading-none">{tb.label}</span>
                </button>
              );
            })}
          </div>

          {/* Android home indicator */}
          <div className="flex justify-center pb-1 pt-0.5">
            <div className="h-1 w-28 rounded-full bg-white/30" />
          </div>
        </nav>
      </div>

      {/* Smart Share Overlay */}
      <SmartShareOverlay />

      {/* Clipboard Monitor (silent) */}
      <ClipboardMonitor />

      {/* Media Player Modal */}
      <MediaPlayer />

      {/* Media Workspace Modal */}
      <MediaWorkspace />
    </div>
  );
}
