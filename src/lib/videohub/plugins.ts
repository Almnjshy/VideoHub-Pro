/**
 * VideoHub Pro Enterprise — Plugin Registry
 * نظام الوحدات المستقلة لكل منصة
 *
 * Production note: Mock content generation has been REMOVED.
 * The Web application delegates media resolution to the backend API
 * (/api/videohub/resolve) which calls real resolvers.
 *
 * If no backend resolver is available, the API returns NOT_AVAILABLE.
 * The Web application NEVER generates fake media formats or fake download URLs.
 */

import type { PlatformPlugin, MediaMetadata, MediaFormat, ResolveOptions } from './types';

// ============ Plugin Definitions ============
// Each plugin handles URL detection only.
// Media resolution is delegated to the backend API.

const PLUGINS: PlatformPlugin[] = [
  { id: 'youtube', name: 'YouTube', nameAr: 'يوتيوب', icon: '▶', color: '#FF0000', version: '3.0.0', enabled: true,
    canHandle: (url) => /youtube\.com|youtu\.be/i.test(url),
    identify: (url) => ({ platformId: 'youtube', contentId: url.match(/(?:v=|youtu\.be\/|shorts\/)([\w-]{6,})/)?.[1] ?? 'unknown' }),
    fetchMetadata: async () => { throw new Error('Use API: /api/videohub/resolve'); },
    resolveDownloadLinks: async () => { throw new Error('Use API: /api/videohub/resolve'); },
  },
  { id: 'facebook', name: 'Facebook', nameAr: 'فيسبوك', icon: 'f', color: '#1877F2', version: '1.0.0', enabled: true,
    canHandle: (url) => /facebook\.com|fb\.watch|fb\.com/i.test(url),
    identify: (url) => ({ platformId: 'facebook', contentId: url.match(/(?:videos\/|watch\/\?v=)(\d+)/)?.[1] ?? 'unknown' }),
    fetchMetadata: async () => { throw new Error('Use API: /api/videohub/resolve'); },
    resolveDownloadLinks: async () => { throw new Error('Use API: /api/videohub/resolve'); },
  },
  { id: 'tiktok', name: 'TikTok', nameAr: 'تيك توك', icon: '♪', color: '#FE2C55', version: '1.0.0', enabled: true,
    canHandle: (url) => /tiktok\.com|vm\.tiktok/i.test(url),
    identify: (url) => ({ platformId: 'tiktok', contentId: url.match(/\/video\/(\d+)|vm\.tiktok\.com\/([\w]+)/)?.[1] ?? 'unknown' }),
    fetchMetadata: async () => { throw new Error('Use API: /api/videohub/resolve'); },
    resolveDownloadLinks: async () => { throw new Error('Use API: /api/videohub/resolve'); },
  },
  { id: 'x', name: 'X (Twitter)', nameAr: 'إكس', icon: '𝕏', color: '#000000', version: '1.0.0', enabled: true,
    canHandle: (url) => /twitter\.com|x\.com|t\.co/i.test(url),
    identify: (url) => ({ platformId: 'x', contentId: url.match(/\/status\/(\d+)/)?.[1] ?? 'unknown' }),
    fetchMetadata: async () => { throw new Error('Use API: /api/videohub/resolve'); },
    resolveDownloadLinks: async () => { throw new Error('Use API: /api/videohub/resolve'); },
  },
  { id: 'instagram', name: 'Instagram', nameAr: 'إنستغرام', icon: '◉', color: '#E4405F', version: '1.0.0', enabled: true,
    canHandle: (url) => /instagram\.com|instagr\.am/i.test(url),
    identify: (url) => ({ platformId: 'instagram', contentId: url.match(/\/(?:p|reel|tv)\/([\w-]+)/)?.[1] ?? 'unknown' }),
    fetchMetadata: async () => { throw new Error('Use API: /api/videohub/resolve'); },
    resolveDownloadLinks: async () => { throw new Error('Use API: /api/videohub/resolve'); },
  },
  { id: 'vimeo', name: 'Vimeo', nameAr: 'فيميو', icon: 'V', color: '#1AB7EA', version: '2.0.0', enabled: true,
    canHandle: (url) => /vimeo\.com/i.test(url),
    identify: (url) => ({ platformId: 'vimeo', contentId: url.match(/vimeo\.com\/(\d+)/)?.[1] ?? 'unknown' }),
    fetchMetadata: async () => { throw new Error('Use API: /api/videohub/resolve'); },
    resolveDownloadLinks: async () => { throw new Error('Use API: /api/videohub/resolve'); },
  },
  { id: 'dailymotion', name: 'Dailymotion', nameAr: 'ديلي موشن', icon: 'D', color: '#0066DC', version: '2.0.0', enabled: true,
    canHandle: (url) => /dailymotion\.com|dai\.ly/i.test(url),
    identify: (url) => ({ platformId: 'dailymotion', contentId: url.match(/dai\.ly\/([\w]+)|video\/([\w]+)/)?.[1] ?? 'unknown' }),
    fetchMetadata: async () => { throw new Error('Use API: /api/videohub/resolve'); },
    resolveDownloadLinks: async () => { throw new Error('Use API: /api/videohub/resolve'); },
  },
  { id: 'reddit', name: 'Reddit', nameAr: 'ريديت', icon: 'R', color: '#FF4500', version: '1.0.0', enabled: true,
    canHandle: (url) => /reddit\.com|r\.reddit/i.test(url),
    identify: (url) => ({ platformId: 'reddit', contentId: url.match(/\/comments\/([\w]+)/)?.[1] ?? 'unknown' }),
    fetchMetadata: async () => { throw new Error('Use API: /api/videohub/resolve'); },
    resolveDownloadLinks: async () => { throw new Error('Use API: /api/videohub/resolve'); },
  },
  { id: 'twitch', name: 'Twitch', nameAr: 'تويتش', icon: 'T', color: '#9146FF', version: '1.0.0', enabled: true,
    canHandle: (url) => /twitch\.tv|clips\.twitch/i.test(url),
    identify: (url) => ({ platformId: 'twitch', contentId: url.match(/\/clip\/([\w-]+)/)?.[1] ?? 'unknown' }),
    fetchMetadata: async () => { throw new Error('Use API: /api/videohub/resolve'); },
    resolveDownloadLinks: async () => { throw new Error('Use API: /api/videohub/resolve'); },
  },
  { id: 'soundcloud', name: 'SoundCloud', nameAr: 'ساوند كلاود', icon: 'S', color: '#FF5500', version: '1.0.0', enabled: true,
    canHandle: (url) => /soundcloud\.com|snd\.sc/i.test(url),
    identify: () => ({ platformId: 'soundcloud', contentId: 'sc' }),
    fetchMetadata: async () => { throw new Error('Use API: /api/videohub/resolve'); },
    resolveDownloadLinks: async () => { throw new Error('Use API: /api/videohub/resolve'); },
  },
  { id: 'pinterest', name: 'Pinterest', nameAr: 'بينتريست', icon: 'P', color: '#BD081C', version: '1.0.0', enabled: true,
    canHandle: (url) => /pinterest\.com|pin\.it/i.test(url),
    identify: (url) => ({ platformId: 'pinterest', contentId: url.match(/\/pin\/([\w-]+)/)?.[1] ?? 'unknown' }),
    fetchMetadata: async () => { throw new Error('Use API: /api/videohub/resolve'); },
    resolveDownloadLinks: async () => { throw new Error('Use API: /api/videohub/resolve'); },
  },
  { id: 'linkedin', name: 'LinkedIn', nameAr: 'لينكدإن', icon: 'in', color: '#0A66C2', version: '1.0.0', enabled: true,
    canHandle: (url) => /linkedin\.com|lnkd\.in/i.test(url),
    identify: () => ({ platformId: 'linkedin', contentId: 'li' }),
    fetchMetadata: async () => { throw new Error('Use API: /api/videohub/resolve'); },
    resolveDownloadLinks: async () => { throw new Error('Use API: /api/videohub/resolve'); },
  },
  { id: 'tumblr', name: 'Tumblr', nameAr: 'تمبلر', icon: 't', color: '#36465D', version: '1.0.0', enabled: true,
    canHandle: (url) => /tumblr\.com/i.test(url),
    identify: () => ({ platformId: 'tumblr', contentId: 'tb' }),
    fetchMetadata: async () => { throw new Error('Use API: /api/videohub/resolve'); },
    resolveDownloadLinks: async () => { throw new Error('Use API: /api/videohub/resolve'); },
  },
  { id: 'streamable', name: 'Streamable', nameAr: 'ستريمابل', icon: 'St', color: '#0F90FA', version: '2.0.0', enabled: true,
    canHandle: (url) => /streamable\.com/i.test(url),
    identify: (url) => ({ platformId: 'streamable', contentId: url.match(/streamable\.com\/([\w]+)/)?.[1] ?? 'unknown' }),
    fetchMetadata: async () => { throw new Error('Use API: /api/videohub/resolve'); },
    resolveDownloadLinks: async () => { throw new Error('Use API: /api/videohub/resolve'); },
  },
];

export function listPlugins(): PlatformPlugin[] {
  return PLUGINS;
}

export function getPlugin(id: string): PlatformPlugin | undefined {
  return PLUGINS.find((p) => p.id === id);
}

export function findPluginForUrl(url: string): PlatformPlugin | undefined {
  return PLUGINS.find((p) => p.enabled && p.canHandle(url));
}
