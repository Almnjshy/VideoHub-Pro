/**
 * VideoHub Pro Enterprise — Core Type System
 * أنواع البيانات الأساسية للتطبيق
 */

// ============= Download Task Types =============

export type TaskStatus =
  | 'queued'        // في قائمة الانتظار
  | 'resolving'     // جاري تحليل الرابط
  | 'downloading'   // قيد التنزيل
  | 'paused'        // متوقف مؤقتاً
  | 'completed'     // مكتمل
  | 'failed'        // فشل
  | 'retrying';     // إعادة المحاولة

export type MediaQuality = 'audio' | '144p' | '240p' | '360p' | '480p' | '720p' | '1080p' | '4k';

export type MediaType = 'video' | 'audio' | 'image' | 'file';

export interface MediaFormat {
  id: string;
  quality: MediaQuality;
  ext: string;
  sizeBytes: number;
  mediaType: MediaType;
  hasAudio: boolean;
  bitrate?: number;
  fps?: number;
  label: string;
}

export interface MediaMetadata {
  title: string;
  author?: string;
  thumbnailUrl?: string;
  durationSeconds?: number;
  description?: string;
  sourceUrl: string;
  platformId: string;
  formats: MediaFormat[];
  resolvedAt: number;
}

export interface DownloadTask {
  id: string;
  metadata: MediaMetadata;
  selectedFormat: MediaFormat;
  status: TaskStatus;
  progress: number;
  downloadedBytes: number;
  totalBytes: number;
  speedBytesPerSec: number;
  etaSeconds: number;
  retries: number;
  maxRetries: number;
  error?: string;
  errorStage?: ErrorStage;
  createdAt: number;
  startedAt?: number;
  completedAt?: number;
  outputPath?: string;
  priority: number;
  segments: DownloadSegment[];
}

export interface DownloadSegment {
  id: number;
  startByte: number;
  endByte: number;
  downloadedByte: number;
  status: 'pending' | 'active' | 'done' | 'failed';
}

export type ErrorStage =
  | 'identify'
  | 'fetch_metadata'
  | 'resolve_links'
  | 'download_segment'
  | 'merge'
  | 'finalize';

// ============= Plugin System Types =============

export interface PlatformPlugin {
  id: string;
  name: string;
  nameAr: string;
  icon: string;
  color: string;
  version: string;
  enabled: boolean;
  canHandle: (url: string) => boolean;
  identify: (url: string) => { platformId: string; contentId: string };
  fetchMetadata: (url: string) => Promise<MediaMetadata>;
  resolveDownloadLinks: (url: string, options: ResolveOptions) => Promise<MediaFormat[]>;
  onError?: (error: PluginError) => void;
}

export interface ResolveOptions {
  preferQuality?: MediaQuality;
  preferMediaType?: MediaType;
}

export interface PluginHealth {
  pluginId: string;
  totalAttempts: number;
  successfulAttempts: number;
  failedAttempts: number;
  successRate: number;
  lastError?: PluginError;
  lastSuccessAt?: number;
  status: 'healthy' | 'degraded' | 'broken';
  autoTestEnabled: boolean;
  lastAutoTestAt?: number;
  nextAutoTestAt?: number;
}

export interface PluginError {
  pluginId: string;
  stage: ErrorStage;
  errorType: 'network' | 'parsing' | 'auth' | 'source_changed' | 'rate_limit' | 'unknown';
  message: string;
  timestamp: number;
  contentId?: string;
}

// ============= Fault Detection & Telemetry =============

export interface FaultReport {
  id: string;
  pluginId: string;
  stage: ErrorStage;
  errorType: PluginError['errorType'];
  message: string;
  timestamp: number;
  taskId?: string;
  resolved: boolean;
}

export interface AutoTestResult {
  pluginId: string;
  testName: string;
  passed: boolean;
  durationMs: number;
  timestamp: number;
  error?: string;
}

// ============= Settings & Stats =============

export interface UserSettings {
  defaultQuality: MediaQuality;
  defaultMediaType: MediaType;
  concurrentDownloads: number;
  concurrentPerDomain?: number;
  downloadPath: string;
  autoRetry: boolean;
  maxRetries: number;
  notificationsEnabled: boolean;
  smartScheduling?: boolean;
  bandwidthLimitMbps?: number;
  theme: 'dark' | 'light' | 'amoled';
  language: 'ar' | 'en';
  storageLimitGb: number;
}

export interface StorageStats {
  usedBytes: number;
  totalBytes: number;
  fileCount: number;
  byType: Record<MediaType, number>;
}

export interface AppStats {
  totalDownloads: number;
  completedDownloads: number;
  failedDownloads: number;
  totalBytesDownloaded: number;
  averageSpeed: number;
  uptime: number;
  topPlatforms: Array<{ pluginId: string; count: number }>;
}

// ============= Smart Share Intent =============

export interface ShareIntent {
  id: string;
  url: string;
  source?: string;
  receivedAt: number;
  detectedPluginId?: string;
  metadata?: MediaMetadata;
  resolving: boolean;
}
