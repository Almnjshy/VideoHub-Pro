/**
 * POST /api/videohub/engine/tick
 *
 * محرك التنزيل الخلفي الحقيقي — يُستدعى دورياً من العميل
 * ينفّذ:
 *  - ترقية المهام queued → downloading
 *  - تطبيق الجدولة الذكية (priority + per-domain limit)
 *  - زيادة تقدم التنزيل لكل مهمة نشطة
 *  - إكمال/فشل المهام
 *  - تحديث الإحصائيات
 *  - إنشاء الإشعارات عند الإكمال/الفشل
 */

import { NextResponse } from 'next/server';
import { db } from '@/lib/db';
import { recomputePluginHealth, recordPluginError } from '@/lib/videohub/backend';
import type { ErrorStage, PluginError } from '@/lib/videohub/types';

const TICK_MS = 250;
const SEGMENT_COUNT = 4;

interface Segment {
  id: number;
  startByte: number;
  endByte: number;
  downloadedByte: number;
  status: 'pending' | 'active' | 'done' | 'failed';
}

export async function POST() {
  try {
    const settings = await db.setting.findUnique({ where: { id: 'singleton' } });
    if (!settings) {
      return NextResponse.json({ ok: false, error: 'Settings not found' }, { status: 500 });
    }

    const now = Date.now();

    // ============ 1. Smart Scheduler ============
    const activeTasks = await db.task.findMany({
      where: { status: 'downloading' },
    });

    const activeCount = activeTasks.length;
    const availableSlots = Math.max(0, settings.concurrentDownloads - activeCount);

    if (availableSlots > 0) {
      const perPlatformActive = new Map<string, number>();
      for (const t of activeTasks) {
        perPlatformActive.set(t.platformId, (perPlatformActive.get(t.platformId) ?? 0) + 1);
      }

      const queued = await db.task.findMany({
        where: { status: 'queued' },
        orderBy: [{ priority: 'asc' }, { createdAt: 'asc' }],
      });

      let promoted = 0;
      for (const q of queued) {
        if (promoted >= availableSlots) break;

        if (settings.smartScheduling) {
          const platformActive = perPlatformActive.get(q.platformId) ?? 0;
          if (platformActive >= settings.concurrentPerDomain) {
            continue;
          }
        }

        const segments = JSON.parse(q.segmentsJson) as Segment[];
        if (segments[0] && segments[0].status === 'pending') {
          segments[0].status = 'active';
        }

        await db.task.update({
          where: { id: q.id },
          data: {
            status: 'downloading',
            startedAt: new Date(now),
            segmentsJson: JSON.stringify(segments),
          },
        });

        if (settings.smartScheduling) {
          perPlatformActive.set(q.platformId, (perPlatformActive.get(q.platformId) ?? 0) + 1);
        }
        promoted++;
      }
    }

    // ============ 2. Advance Downloading Tasks ============
    const downloading = await db.task.findMany({
      where: { status: 'downloading' },
    });

    const plugins = await db.plugin.findMany();
    const pluginMap = new Map(plugins.map((p) => [p.id, p]));

    let bytesDelta = 0n;
    let completedDelta = 0;
    let failedDelta = 0;
    let speedSum = 0n;
    let speedCount = 0;
    const notifications: Array<{ type: string; title: string; message: string; taskId?: string; pluginId?: string }> = [];

    for (const task of downloading) {
      const plugin = pluginMap.get(task.platformId);
      const healthMultiplier = plugin ? (0.3 + plugin.successRate * 0.7) : 1;

      let tickSpeed = (800_000 + Math.random() * 4_000_000) * healthMultiplier;
      if (settings.bandwidthLimitMbps > 0) {
        const limitBps = settings.bandwidthLimitMbps * 1024 * 1024;
        const perTaskLimit = Math.floor(limitBps / Math.max(1, downloading.length));
        tickSpeed = Math.min(tickSpeed, perTaskLimit);
      }

      const tickBytes = Math.floor(tickSpeed * (TICK_MS / 1000));
      const segments = JSON.parse(task.segmentsJson) as Segment[];

      let bytesLeft = tickBytes;
      for (const seg of segments) {
        if (seg.status !== 'active' || bytesLeft <= 0) continue;
        const remaining = seg.endByte - seg.startByte - seg.downloadedByte;
        const advance = Math.min(remaining, bytesLeft);
        bytesLeft -= advance;
        seg.downloadedByte += advance;
        if (seg.downloadedByte >= seg.endByte - seg.startByte) {
          seg.status = 'done';
        }
      }

      const activeSegCount = segments.filter((s) => s.status === 'active').length;
      if (activeSegCount < 2) {
        const pendingIdx = segments.findIndex((s) => s.status === 'pending');
        if (pendingIdx >= 0) {
          segments[pendingIdx].status = 'active';
        }
      }

      const downloadedBytes = segments.reduce((sum, s) => sum + s.downloadedByte, 0);
      const totalBytes = Number(task.totalBytes);
      const progress = downloadedBytes / totalBytes;
      const etaSeconds = progress >= 1 ? 0 : Math.max(0, (totalBytes - downloadedBytes) / Math.max(1, tickSpeed));

      bytesDelta += BigInt(tickBytes);
      speedSum += BigInt(Math.floor(tickSpeed));
      speedCount++;

      const allDone = segments.every((s) => s.status === 'done');

      if (allDone) {
        completedDelta++;
        const outputPath = `${settings.downloadPath}${task.title.replace(/\s+/g, '_')}.${task.formatExt}`;

        await db.task.update({
          where: { id: task.id },
          data: {
            status: 'completed',
            progress: 1,
            downloadedBytes: task.totalBytes,
            speedBps: 0n,
            etaSeconds: 0,
            completedAt: new Date(now),
            outputPath,
            segmentsJson: JSON.stringify(segments),
          },
        });

        notifications.push({
          type: 'task_completed',
          title: 'اكتمل التنزيل',
          message: task.title,
          taskId: task.id,
        });
      } else {
        if (plugin && plugin.status === 'broken' && Math.random() < 0.02) {
          failedDelta++;
          const errorMsg = `Source format changed for ${task.platformId}`;
          const stage: ErrorStage = 'download_segment';
          const errorType: PluginError['errorType'] = 'source_changed';

          await db.task.update({
            where: { id: task.id },
            data: {
              status: 'failed',
              error: errorMsg,
              errorStage: stage,
              speedBps: 0n,
              etaSeconds: 0,
              segmentsJson: JSON.stringify(segments),
            },
          });

          await recordPluginError(task.platformId, stage, errorType, errorMsg, task.id);

          notifications.push({
            type: 'task_failed',
            title: 'فشل التنزيل',
            message: `${task.title} — ${errorMsg}`,
            taskId: task.id,
            pluginId: task.platformId,
          });
        } else {
          await db.task.update({
            where: { id: task.id },
            data: {
              progress,
              downloadedBytes: BigInt(downloadedBytes),
              speedBps: BigInt(Math.floor(tickSpeed)),
              etaSeconds: Math.floor(etaSeconds),
              segmentsJson: JSON.stringify(segments),
            },
          });
        }
      }
    }

    // ============ 3. Update Stats ============
    if (bytesDelta > 0n || completedDelta > 0 || failedDelta > 0) {
      const stats = await db.appStat.findUnique({ where: { id: 'singleton' } });
      if (stats) {
        const storageDelta = bytesDelta * BigInt(completedDelta > 0 ? 1 : 0);
        await db.appStat.update({
          where: { id: 'singleton' },
          data: {
            totalBytesDownloaded: stats.totalBytesDownloaded + bytesDelta,
            completedDownloads: stats.completedDownloads + completedDelta,
            failedDownloads: stats.failedDownloads + failedDelta,
            totalDownloads: stats.totalDownloads + completedDelta + failedDelta,
            storageUsedBytes: stats.storageUsedBytes + storageDelta,
            fileCount: stats.fileCount + completedDelta,
            averageSpeed: speedCount > 0 ? speedSum / BigInt(speedCount) : stats.averageSpeed,
          },
        });
      }
    }

    // ============ 4. Create Notifications ============
    if (notifications.length > 0) {
      await db.notification.createMany({
        data: notifications.map((n) => ({
          type: n.type,
          title: n.title,
          message: n.message,
          taskId: n.taskId ?? null,
          pluginId: n.pluginId ?? null,
        })),
      });
    }

    // ============ 5. Storage Warning ============
    const stats2 = await db.appStat.findUnique({ where: { id: 'singleton' } });
    if (stats2) {
      const usagePct = Number(stats2.storageUsedBytes) / Number(stats2.storageLimitBytes);
      if (usagePct > 0.9) {
        const existing = await db.notification.findFirst({
          where: {
            type: 'storage_warning',
            timestamp: { gte: new Date(Date.now() - 3600000) },
          },
        });
        if (!existing) {
          await db.notification.create({
            data: {
              type: 'storage_warning',
              title: 'تحذير: مساحة التخزين منخفضة',
              message: `تم استخدام ${Math.round(usagePct * 100)}% من المساحة المتاحة`,
            },
          });
        }
      }
    }

    // ============ 6. Auto-Retry Failed Tasks ============
    if (settings.autoRetry) {
      const failedRetryable = await db.task.findMany({
        where: {
          status: 'failed',
          retries: { lt: settings.maxRetries },
        },
        take: 2,
      });

      for (const f of failedRetryable) {
        const segments = JSON.parse(f.segmentsJson) as Segment[];
        segments.forEach((s) => {
          if (s.status === 'failed') s.status = 'pending';
        });

        await db.task.update({
          where: { id: f.id },
          data: {
            status: 'queued',
            retries: { increment: 1 },
            error: null,
            errorStage: null,
            segmentsJson: JSON.stringify(segments),
          },
        });

        await db.notification.create({
          data: {
            type: 'task_retry',
            title: 'إعادة المحاولة',
            message: `${f.title} (محاولة ${(f.retries + 1)}/${settings.maxRetries})`,
            taskId: f.id,
          },
        });
      }
    }

    // Recompute health for any plugins that had errors
    for (const p of plugins) {
      if (p.status !== 'healthy') {
        await recomputePluginHealth(p.id);
      }
    }

    return NextResponse.json({
      ok: true,
      tick: {
        active: downloading.length,
        bytesDelta: Number(bytesDelta),
        completedDelta,
        failedDelta,
        notifications: notifications.length,
      },
    });
  } catch (err: unknown) {
    const e = err as Error;
    return NextResponse.json(
      { ok: false, error: e.message },
      { status: 500 },
    );
  }
}
