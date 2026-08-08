/**
 * VideoHub Pro Enterprise — Client Store (Real Backend)
 * متجر الحالة العميلي — يتصل بـ REST API حقيقي خلفي
 *
 * كل البيانات تُخزَّن في قاعدة بيانات SQLite عبر Prisma.
 * العميل يُحدّث الحالة عبر polling للـ API.
 */

'use client';

import { create } from 'zustand';
import type {
  DownloadTask, MediaFormat, MediaMetadata, PluginHealth, PlatformPlugin,
  UserSettings, AppStats, StorageStats, FaultReport, AutoTestResult, ShareIntent,
} from '@/lib/videohub/types';

// ============ Plugin Registry (frontend mirror) ============

const FRONTEND_PLUGINS: PlatformPlugin[] = [
  { id: 'youtube', name: 'YouTube', nameAr: 'يوتيوب', icon: '▶', color: '#FF0000', version: '2.4.1', enabled: true,
    canHandle: (url) => /youtube\.com|youtu\.be/i.test(url),
    identify: (url) => ({ platformId: 'youtube', contentId: url.match(/(?:v=|youtu\.be\/|shorts\/)([\w-]{6,})/)?.[1] ?? '' }),
    fetchMetadata: async () => { throw new Error('use API'); },
    resolveDownloadLinks: async () => { throw new Error('use API'); },
  },
  { id: 'facebook', name: 'Facebook', nameAr: 'فيسبوك', icon: 'f', color: '#1877F2', version: '1.9.0', enabled: true,
    canHandle: (url) => /facebook\.com|fb\.watch|fb\.com/i.test(url),
    identify: (url) => ({ platformId: 'facebook', contentId: url.match(/(?:videos\/|watch\/\?v=)(\d+)/)?.[1] ?? '' }),
    fetchMetadata: async () => { throw new Error('use API'); },
    resolveDownloadLinks: async () => { throw new Error('use API'); },
  },
  { id: 'tiktok', name: 'TikTok', nameAr: 'تيك توك', icon: '♪', color: '#FE2C55', version: '3.1.2', enabled: true,
    canHandle: (url) => /tiktok\.com|vm\.tiktok/i.test(url),
    identify: (url) => ({ platformId: 'tiktok', contentId: url.match(/\/video\/(\d+)|vm\.tiktok\.com\/([\w]+)/)?.[1] ?? '' }),
    fetchMetadata: async () => { throw new Error('use API'); },
    resolveDownloadLinks: async () => { throw new Error('use API'); },
  },
  { id: 'x', name: 'X (Twitter)', nameAr: 'إكس', icon: '𝕏', color: '#000000', version: '1.5.0', enabled: true,
    canHandle: (url) => /twitter\.com|x\.com|t\.co/i.test(url),
    identify: (url) => ({ platformId: 'x', contentId: url.match(/\/status\/(\d+)/)?.[1] ?? '' }),
    fetchMetadata: async () => { throw new Error('use API'); },
    resolveDownloadLinks: async () => { throw new Error('use API'); },
  },
  { id: 'instagram', name: 'Instagram', nameAr: 'إنستغرام', icon: '◉', color: '#E4405F', version: '2.0.3', enabled: true,
    canHandle: (url) => /instagram\.com|instagr\.am/i.test(url),
    identify: (url) => ({ platformId: 'instagram', contentId: url.match(/\/(?:p|reel|tv)\/([\w-]+)/)?.[1] ?? '' }),
    fetchMetadata: async () => { throw new Error('use API'); },
    resolveDownloadLinks: async () => { throw new Error('use API'); },
  },
  { id: 'vimeo', name: 'Vimeo', nameAr: 'فيميو', icon: 'V', color: '#1AB7EA', version: '1.2.0', enabled: true,
    canHandle: (url) => /vimeo\.com/i.test(url),
    identify: (url) => ({ platformId: 'vimeo', contentId: url.match(/vimeo\.com\/(\d+)/)?.[1] ?? '' }),
    fetchMetadata: async () => { throw new Error('use API'); },
    resolveDownloadLinks: async () => { throw new Error('use API'); },
  },
  { id: 'dailymotion', name: 'Dailymotion', nameAr: 'ديلي موشن', icon: 'D', color: '#0066DC', version: '1.0.1', enabled: true,
    canHandle: (url) => /dailymotion\.com|dai\.ly/i.test(url),
    identify: (url) => ({ platformId: 'dailymotion', contentId: url.match(/dai\.ly\/([\w]+)|video\/([\w]+)/)?.[1] ?? '' }),
    fetchMetadata: async () => { throw new Error('use API'); },
    resolveDownloadLinks: async () => { throw new Error('use API'); },
  },
  // ============ NEW PLATFORMS ============
  { id: 'reddit', name: 'Reddit', nameAr: 'ريديت', icon: 'R', color: '#FF4500', version: '1.1.0', enabled: true,
    canHandle: (url) => /reddit\.com|r\.reddit/i.test(url),
    identify: (url) => ({ platformId: 'reddit', contentId: url.match(/\/comments\/([\w]+)/)?.[1] ?? '' }),
    fetchMetadata: async () => { throw new Error('use API'); },
    resolveDownloadLinks: async () => { throw new Error('use API'); },
  },
  { id: 'twitch', name: 'Twitch', nameAr: 'تويتش', icon: 'T', color: '#9146FF', version: '1.0.2', enabled: true,
    canHandle: (url) => /twitch\.tv|clips\.twitch/i.test(url),
    identify: (url) => ({ platformId: 'twitch', contentId: url.match(/\/clip\/([\w-]+)/)?.[1] ?? '' }),
    fetchMetadata: async () => { throw new Error('use API'); },
    resolveDownloadLinks: async () => { throw new Error('use API'); },
  },
  { id: 'soundcloud', name: 'SoundCloud', nameAr: 'ساوند كلاود', icon: 'S', color: '#FF5500', version: '1.0.0', enabled: true,
    canHandle: (url) => /soundcloud\.com|snd\.sc/i.test(url),
    identify: (url) => ({ platformId: 'soundcloud', contentId: 'sc' }),
    fetchMetadata: async () => { throw new Error('use API'); },
    resolveDownloadLinks: async () => { throw new Error('use API'); },
  },
  { id: 'pinterest', name: 'Pinterest', nameAr: 'بينتريست', icon: 'P', color: '#BD081C', version: '1.0.0', enabled: true,
    canHandle: (url) => /pinterest\.com|pin\.it/i.test(url),
    identify: (url) => ({ platformId: 'pinterest', contentId: url.match(/\/pin\/([\w-]+)/)?.[1] ?? '' }),
    fetchMetadata: async () => { throw new Error('use API'); },
    resolveDownloadLinks: async () => { throw new Error('use API'); },
  },
  { id: 'linkedin', name: 'LinkedIn', nameAr: 'لينكدإن', icon: 'in', color: '#0A66C2', version: '1.0.0', enabled: true,
    canHandle: (url) => /linkedin\.com|lnkd\.in/i.test(url),
    identify: (url) => ({ platformId: 'linkedin', contentId: 'li' }),
    fetchMetadata: async () => { throw new Error('use API'); },
    resolveDownloadLinks: async () => { throw new Error('use API'); },
  },
  { id: 'tumblr', name: 'Tumblr', nameAr: 'تمبلر', icon: 't', color: '#36465D', version: '1.0.0', enabled: true,
    canHandle: (url) => /tumblr\.com/i.test(url),
    identify: (url) => ({ platformId: 'tumblr', contentId: 'tb' }),
    fetchMetadata: async () => { throw new Error('use API'); },
    resolveDownloadLinks: async () => { throw new Error('use API'); },
  },
  { id: 'streamable', name: 'Streamable', nameAr: 'ستريمابل', icon: 'St', color: '#0F90FA', version: '1.0.0', enabled: true,
    canHandle: (url) => /streamable\.com/i.test(url),
    identify: (url) => ({ platformId: 'streamable', contentId: url.match(/streamable\.com\/([\w]+)/)?.[1] ?? '' }),
    fetchMetadata: async () => { throw new Error('use API'); },
    resolveDownloadLinks: async () => { throw new Error('use API'); },
  },
];

export function listFrontendPlugins(): PlatformPlugin[] {
  return FRONTEND_PLUGINS;
}

export function findFrontendPluginForUrl(url: string): PlatformPlugin | undefined {
  return FRONTEND_PLUGINS.find((p) => p.enabled && p.canHandle(url));
}

// ============ Notification types ============

export interface AppNotification {
  id: string;
  type: string;
  title: string;
  message: string;
  taskId?: string | null;
  pluginId?: string | null;
  read: boolean;
  timestamp: number;
}

// ============ Store Interface ============

interface VideoHubState {
  // Data (from backend)
  tasks: DownloadTask[];
  plugins: PlatformPlugin[];
  pluginHealth: Record<string, PluginHealth>;
  faultReports: FaultReport[];
  autoTestResults: AutoTestResult[];
  notifications: AppNotification[];
  unreadNotifications: number;
  settings: UserSettings;
  stats: AppStats;
  storage: StorageStats;

  // Local share intents (transient)
  activeIntents: ShareIntent[];

  // Loading states
  loading: {
    tasks: boolean;
    plugins: boolean;
    stats: boolean;
    settings: boolean;
  };

  // ============ Actions ============
  receiveShareIntent: (url: string, source?: string) => string;
  resolveShareIntent: (intentId: string) => Promise<void>;
  dismissShareIntent: (intentId: string) => void;

  startDownload: (intentId: string, format: MediaFormat, priority?: number) => Promise<string | null>;
  pauseTask: (taskId: string) => Promise<void>;
  resumeTask: (taskId: string) => Promise<void>;
  retryTask: (taskId: string) => Promise<void>;
  deleteTask: (taskId: string) => Promise<void>;
  setTaskPriority: (taskId: string, priority: number) => Promise<void>;
  clearCompleted: () => Promise<void>;

  togglePlugin: (pluginId: string) => Promise<void>;
  runPluginAutoTest: (pluginId: string) => Promise<void>;

  updateSettings: (patch: Partial<UserSettings>) => Promise<void>;
  markNotificationRead: (notificationId: string) => Promise<void>;

  // Polling
  pollTasks: () => Promise<void>;
  pollPlugins: () => Promise<void>;
  pollStats: () => Promise<void>;
  pollNotifications: () => Promise<void>;
  pollSettings: () => Promise<void>;
  engineTick: () => Promise<void>;
}

// ============ Initial State ============

const initialSettings: UserSettings = {
  defaultQuality: '1080p',
  defaultMediaType: 'video',
  concurrentDownloads: 3,
  downloadPath: '/storage/VideoHub/',
  autoRetry: true,
  maxRetries: 3,
  notificationsEnabled: true,
  theme: 'dark',
  language: 'ar',
  storageLimitGb: 32,
};

const initialStats: AppStats = {
  totalDownloads: 0,
  completedDownloads: 0,
  failedDownloads: 0,
  totalBytesDownloaded: 0,
  averageSpeed: 0,
  uptime: 0,
  topPlatforms: [],
};

const initialStorage: StorageStats = {
  usedBytes: 0,
  totalBytes: 32 * 1024 * 1024 * 1024,
  fileCount: 0,
  byType: { video: 0, audio: 0, image: 0, file: 0 },
};

// ============ Store Implementation ============

export const useVideoHub = create<VideoHubState>((set, get) => ({
  tasks: [],
  plugins: FRONTEND_PLUGINS,
  pluginHealth: {},
  faultReports: [],
  autoTestResults: [],
  notifications: [],
  unreadNotifications: 0,
  settings: initialSettings,
  stats: initialStats,
  storage: initialStorage,
  activeIntents: [],
  loading: { tasks: false, plugins: false, stats: false, settings: false },

  // ============ Smart Share ============

  receiveShareIntent: (url, source) => {
    const intentId = crypto.randomUUID();
    const intent: ShareIntent = {
      id: intentId,
      url,
      source,
      receivedAt: Date.now(),
      resolving: false,
    };
    const plugin = findFrontendPluginForUrl(url);
    if (plugin) intent.detectedPluginId = plugin.id;
    set((s) => ({ activeIntents: [...s.activeIntents, intent] }));
    return intentId;
  },

  resolveShareIntent: async (intentId) => {
    const intent = get().activeIntents.find((i) => i.id === intentId);
    if (!intent || intent.resolving || intent.metadata) return;

    set((s) => ({
      activeIntents: s.activeIntents.map((i) =>
        i.id === intentId ? { ...i, resolving: true } : i,
      ),
    }));

    try {
      const res = await fetch('/api/videohub/resolve', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ url: intent.url }),
      });
      const data = await res.json();

      if (!data.ok) throw new Error(data.error ?? 'Failed to resolve');

      set((s) => ({
        activeIntents: s.activeIntents.map((i) =>
          i.id === intentId
            ? { ...i, metadata: data.metadata as MediaMetadata, resolving: false }
            : i,
        ),
      }));
    } catch (err: unknown) {
      const e = err as Error;
      set((s) => ({
        activeIntents: s.activeIntents.map((i) =>
          i.id === intentId
            ? { ...i, resolving: false, metadata: { 
                title: 'فشل التحليل', 
                sourceUrl: i.url, 
                platformId: i.detectedPluginId ?? 'unknown', 
                formats: [], 
                resolvedAt: Date.now(),
                description: e.message,
              } as MediaMetadata }
            : i,
        ),
      }));
    }
  },

  dismissShareIntent: (intentId) => {
    set((s) => ({ activeIntents: s.activeIntents.filter((i) => i.id !== intentId) }));
  },

  // ============ Task Management ============

  startDownload: async (intentId, format, priority = 1) => {
    const intent = get().activeIntents.find((i) => i.id === intentId);
    if (!intent || !intent.metadata) return null;

    try {
      const res = await fetch('/api/videohub/tasks', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          metadata: intent.metadata,
          format,
          priority,
        }),
      });
      const data = await res.json();
      if (!data.ok) throw new Error(data.error);

      set((s) => ({ activeIntents: s.activeIntents.filter((i) => i.id !== intentId) }));
      // Immediately poll tasks to reflect new task
      void get().pollTasks();
      return data.taskId as string;
    } catch (err: unknown) {
      console.error('startDownload failed:', err);
      return null;
    }
  },

  pauseTask: async (taskId) => {
    await fetch(`/api/videohub/tasks/${taskId}`, {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ action: 'pause' }),
    });
    void get().pollTasks();
  },

  resumeTask: async (taskId) => {
    await fetch(`/api/videohub/tasks/${taskId}`, {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ action: 'resume' }),
    });
    void get().pollTasks();
  },

  retryTask: async (taskId) => {
    await fetch(`/api/videohub/tasks/${taskId}`, {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ action: 'retry' }),
    });
    void get().pollTasks();
  },

  deleteTask: async (taskId) => {
    await fetch(`/api/videohub/tasks/${taskId}`, { method: 'DELETE' });
    void get().pollTasks();
    void get().pollStats();
  },

  setTaskPriority: async (taskId, priority) => {
    await fetch(`/api/videohub/tasks/${taskId}`, {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ action: 'set_priority', priority }),
    });
    void get().pollTasks();
  },

  clearCompleted: async () => {
    const tasks = get().tasks;
    const completed = tasks.filter((t) => t.status === 'completed');
    await Promise.all(completed.map((t) =>
      fetch(`/api/videohub/tasks/${t.id}`, { method: 'DELETE' }),
    ));
    void get().pollTasks();
  },

  // ============ Plugin Management ============

  togglePlugin: async (pluginId) => {
    const plugin = get().plugins.find((p) => p.id === pluginId);
    if (!plugin) return;
    await fetch(`/api/videohub/plugins/${pluginId}`, {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ enabled: !plugin.enabled }),
    });
    void get().pollPlugins();
  },

  runPluginAutoTest: async (pluginId) => {
    await fetch(`/api/videohub/plugins/${pluginId}/test`, { method: 'POST' });
    void get().pollPlugins();
  },

  // ============ Settings ============

  updateSettings: async (patch) => {
    await fetch('/api/videohub/settings', {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(patch),
    });
    void get().pollSettings();
  },

  markNotificationRead: async (notificationId) => {
    await fetch(`/api/videohub/notifications/${notificationId}/read`, { method: 'PATCH' });
    void get().pollNotifications();
  },

  // ============ Polling ============

  pollTasks: async () => {
    try {
      const res = await fetch('/api/videohub/tasks');
      const data = await res.json();
      if (data.ok) {
        set({ tasks: data.tasks as DownloadTask[] });
      }
    } catch (err) {
      console.error('pollTasks error:', err);
    }
  },

  pollPlugins: async () => {
    try {
      const res = await fetch('/api/videohub/plugins');
      const data = await res.json();
      if (data.ok) {
        const plugins = data.plugins as Array<PlatformPlugin & { health: PluginHealth; enabled: boolean }>;
        const health: Record<string, PluginHealth> = {};
        for (const p of plugins) {
          health[p.id] = p.health;
        }
        // Merge with frontend plugins (which have canHandle functions)
        const merged = FRONTEND_PLUGINS.map((fp) => {
          const bp = plugins.find((p) => p.id === fp.id);
          return bp ? { ...fp, enabled: bp.enabled, version: bp.version } : fp;
        });
        set({ plugins: merged, pluginHealth: health });
      }
    } catch (err) {
      console.error('pollPlugins error:', err);
    }
  },

  pollStats: async () => {
    try {
      const res = await fetch('/api/videohub/stats');
      const data = await res.json();
      if (data.ok) {
        set({
          stats: data.stats as AppStats,
          storage: data.storage as StorageStats,
        });
      }
    } catch (err) {
      console.error('pollStats error:', err);
    }
  },

  pollNotifications: async () => {
    try {
      const res = await fetch('/api/videohub/notifications');
      const data = await res.json();
      if (data.ok) {
        const newNotifications = data.notifications as AppNotification[];
        const prev = get().notifications;
        const prevIds = new Set(prev.map((n) => n.id));
        const fresh = newNotifications.filter((n) => !prevIds.has(n.id) && !n.read);

        set({
          notifications: newNotifications,
          unreadNotifications: data.unreadCount as number,
        });

        // Trigger OS notifications for fresh unread
        if (fresh.length > 0) {
          void triggerOSNotifications(fresh);
        }
      }
    } catch (err) {
      console.error('pollNotifications error:', err);
    }
  },

  pollSettings: async () => {
    try {
      const res = await fetch('/api/videohub/settings');
      const data = await res.json();
      if (data.ok && data.settings) {
        set({ settings: data.settings as UserSettings });
      }
    } catch (err) {
      console.error('pollSettings error:', err);
    }
  },

  engineTick: async () => {
    try {
      await fetch('/api/videohub/engine/tick', { method: 'POST' });
    } catch (err) {
      // Silent fail — engine tick is non-critical for UI
    }
  },
}));

// ============ OS Notifications (Web Notifications API) ============

async function triggerOSNotifications(notifications: AppNotification[]): Promise<void> {
  if (typeof window === 'undefined') return;
  if (!('Notification' in window)) return;

  // Request permission if needed
  if (Notification.permission === 'default') {
    const perm = await Notification.requestPermission();
    if (perm !== 'granted') return;
  }
  if (Notification.permission !== 'granted') return;

  for (const n of notifications) {
    try {
      const notif = new Notification(n.title, {
        body: n.message,
        tag: n.id,
        icon: '/logo.svg',
        badge: '/logo.svg',
      });
      // Auto-close after 5 seconds
      setTimeout(() => notif.close(), 5000);
    } catch (err) {
      console.error('OS notification failed:', err);
    }
  }
}

// ============ Engine Loop (browser-only polling) ============

let engineInterval: ReturnType<typeof setInterval> | null = null;

export function startEngine(): void {
  if (engineInterval) return;
  if (typeof window === 'undefined') return;

  // Initial fetch
  const state = useVideoHub.getState();
  void state.pollTasks();
  void state.pollPlugins();
  void state.pollStats();
  void state.pollSettings();
  void state.pollNotifications();

  // Engine tick every 1 second (server-side advances task progress)
  // Note: even though TICK_MS is 250ms on server, we call tick every 1s to reduce HTTP load
  engineInterval = setInterval(() => {
    const s = useVideoHub.getState();
    void s.engineTick();
    void s.pollTasks();
    void s.pollStats();
    void s.pollNotifications();
  }, 1000);

  // Slower polling for plugins/settings (every 10s)
  setInterval(() => {
    const s = useVideoHub.getState();
    void s.pollPlugins();
    void s.pollSettings();
  }, 10000);
}

export function stopEngine(): void {
  if (engineInterval) {
    clearInterval(engineInterval);
    engineInterval = null;
  }
}

// ============ Utility ============

export function formatBytes(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  if (bytes < 1024 * 1024 * 1024) return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  return `${(bytes / (1024 * 1024 * 1024)).toFixed(2)} GB`;
}

export function formatDuration(seconds: number): string {
  if (!isFinite(seconds) || seconds <= 0) return '—';
  const h = Math.floor(seconds / 3600);
  const m = Math.floor((seconds % 3600) / 60);
  const s = Math.floor(seconds % 60);
  if (h > 0) return `${h}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
  return `${m}:${String(s).padStart(2, '0')}`;
}

// ============ Selectors ============

export function selectActiveTasks(state: VideoHubState): DownloadTask[] {
  return state.tasks.filter((t) =>
    t.status === 'downloading' || t.status === 'queued' || t.status === 'retrying',
  );
}

export function selectCompletedTasks(state: VideoHubState): DownloadTask[] {
  return state.tasks.filter((t) => t.status === 'completed');
}

export function selectFailedTasks(state: VideoHubState): DownloadTask[] {
  return state.tasks.filter((t) => t.status === 'failed');
}
