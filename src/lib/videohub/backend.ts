/**
 * VideoHub Pro Enterprise — Backend Service Layer
 * طبقة الخدمات الخلفية الحقيقية — تستخدم Prisma للتخزين
 *
 * Production note: Mock content generation has been REMOVED.
 * The resolveUrl() function now returns NOT_AVAILABLE when no real
 * resolver is implemented for the platform.
 *
 * The Web application NEVER generates fake media formats, fake metadata,
 * or fake download URLs.
 */

import { db } from '@/lib/db';
import type {
  MediaMetadata, MediaFormat, PluginHealth, PluginError,
  FaultReport as FaultReportType, AutoTestResult, ErrorStage,
} from '@/lib/videohub/types';

// ============ Real URL Resolution ============

export async function resolveUrl(url: string): Promise<{
  metadata: MediaMetadata;
  plugin: BackendPlugin;
} | { error: string }> {
  // No mock resolvers — all platforms require a real backend extraction service
  // Return NOT_AVAILABLE for all platforms until a real resolver backend is implemented
  return {
    error: 'NOT_AVAILABLE — Real media resolution requires a backend extraction service (yt-dlp, etc.). Mock resolvers have been removed from production.',
  };
}

// ============ Backend Plugin Interface ============

export interface BackendPlugin {
  id: string;
  name: string;
  nameAr: string;
  icon: string;
  color: string;
  version: string;
  canHandle: (url: string) => boolean;
  fetchMetadata: (url: string) => Promise<MediaMetadata>;
}

// ============ Database Operations ============

export async function ensurePluginsSeeded(): Promise<void> {
  const count = await db.plugin.count();
  if (count === 0) {
    // Seed plugin records with NOT_VERIFIED status
    const plugins = [
      { id: 'youtube', name: 'YouTube', nameAr: 'يوتيوب', icon: '▶', color: '#FF0000', version: '3.0.0' },
      { id: 'facebook', name: 'Facebook', nameAr: 'فيسبوك', icon: 'f', color: '#1877F2', version: '1.0.0' },
      { id: 'tiktok', name: 'TikTok', nameAr: 'تيك توك', icon: '♪', color: '#FE2C55', version: '1.0.0' },
      { id: 'x', name: 'X (Twitter)', nameAr: 'إكس', icon: '𝕏', color: '#000000', version: '1.0.0' },
      { id: 'instagram', name: 'Instagram', nameAr: 'إنستغرام', icon: '◉', color: '#E4405F', version: '1.0.0' },
      { id: 'vimeo', name: 'Vimeo', nameAr: 'فيميو', icon: 'V', color: '#1AB7EA', version: '2.0.0' },
      { id: 'dailymotion', name: 'Dailymotion', nameAr: 'ديلي موشن', icon: 'D', color: '#0066DC', version: '2.0.0' },
      { id: 'reddit', name: 'Reddit', nameAr: 'ريديت', icon: 'R', color: '#FF4500', version: '1.0.0' },
      { id: 'twitch', name: 'Twitch', nameAr: 'تويتش', icon: 'T', color: '#9146FF', version: '1.0.0' },
      { id: 'soundcloud', name: 'SoundCloud', nameAr: 'ساوند كلاود', icon: 'S', color: '#FF5500', version: '1.0.0' },
      { id: 'pinterest', name: 'Pinterest', nameAr: 'بينتريست', icon: 'P', color: '#BD081C', version: '1.0.0' },
      { id: 'linkedin', name: 'LinkedIn', nameAr: 'لينكدإن', icon: 'in', color: '#0A66C2', version: '1.0.0' },
      { id: 'tumblr', name: 'Tumblr', nameAr: 'تمبلر', icon: 't', color: '#36465D', version: '1.0.0' },
      { id: 'streamable', name: 'Streamable', nameAr: 'ستريمابل', icon: 'St', color: '#0F90FA', version: '2.0.0' },
    ];
    await db.plugin.createMany({
      data: plugins.map((p) => ({
        ...p,
        enabled: true,
        status: 'NOT_VERIFIED',
      })),
    });
  }
}

export async function ensureStatsSeeded(): Promise<void> {
  const count = await db.appStat.count();
  if (count === 0) {
    await db.appStat.create({
      data: { id: 'singleton' },
    });
  }
}

export async function ensureSettingsSeeded(): Promise<void> {
  const count = await db.setting.count();
  if (count === 0) {
    await db.setting.create({
      data: { id: 'singleton' },
    });
  }
}

export async function ensureSeeded(): Promise<void> {
  await ensurePluginsSeeded();
  await ensureStatsSeeded();
  await ensureSettingsSeeded();
}

// ============ Plugin Health ============

export async function recomputePluginHealth(pluginId: string): Promise<void> {
  const plugin = await db.plugin.findUnique({ where: { id: pluginId } });
  if (!plugin) return;

  const successRate = plugin.totalAttempts > 0
    ? plugin.successfulAttempts / plugin.totalAttempts
    : 1.0;

  const status = successRate > 0.95 ? 'healthy' : successRate > 0.80 ? 'degraded' : 'broken';

  await db.plugin.update({
    where: { id: pluginId },
    data: { successRate, status },
  });
}

// ============ Fault Reports ============

export async function recordPluginError(
  pluginId: string,
  stage: ErrorStage,
  errorType: PluginError['errorType'],
  message: string,
  taskId?: string,
): Promise<void> {
  await db.faultReport.create({
    data: { pluginId, stage, errorType, message, taskId },
  });

  await db.plugin.update({
    where: { id: pluginId },
    data: {
      failedAttempts: { increment: 1 },
      lastErrorJson: JSON.stringify({ pluginId, stage, errorType, message, timestamp: Date.now() }),
    },
  });

  await recomputePluginHealth(pluginId);
}

// ============ Auto Test ============

export async function runPluginTest(pluginId: string): Promise<AutoTestResult> {
  const startedAt = Date.now();
  const result: AutoTestResult = {
    pluginId,
    testName: 'full-cycle',
    passed: false,
    durationMs: 0,
    timestamp: startedAt,
  };

  // All platforms are NOT_VERIFIED — no real resolver to test
  result.passed = false;
  result.error = 'NOT_VERIFIED — No real resolver available for testing';
  result.durationMs = Date.now() - startedAt;

  await db.autoTestResult.create({
    data: {
      pluginId,
      testName: result.testName,
      passed: false,
      durationMs: result.durationMs,
      error: result.error,
    },
  });

  return result;
}

// ============ Plugin List with Health ============

export interface PluginWithHealth extends BackendPlugin {
  enabled: boolean;
  health: PluginHealth;
}

export async function getPluginsWithHealth(): Promise<PluginWithHealth[]> {
  await ensurePluginsSeeded();
  const dbPlugins = await db.plugin.findMany();
  return dbPlugins.map((dbp) => ({
    id: dbp.id,
    name: dbp.name,
    nameAr: dbp.nameAr,
    icon: dbp.icon,
    color: dbp.color,
    version: dbp.version,
    enabled: dbp.enabled,
    canHandle: () => false, // Not used in backend
    fetchMetadata: async () => { throw new Error('NOT_AVAILABLE'); },
    health: {
      pluginId: dbp.id,
      totalAttempts: dbp.totalAttempts,
      successfulAttempts: dbp.successfulAttempts,
      failedAttempts: dbp.failedAttempts,
      successRate: dbp.successRate,
      lastError: dbp.lastErrorJson ? JSON.parse(dbp.lastErrorJson) : undefined,
      lastSuccessAt: dbp.lastSuccessAt?.getTime(),
      status: dbp.status as PluginHealth['status'],
      autoTestEnabled: true,
      lastAutoTestAt: dbp.lastAutoTestAt?.getTime(),
      nextAutoTestAt: dbp.nextAutoTestAt?.getTime(),
    },
  }));
}
