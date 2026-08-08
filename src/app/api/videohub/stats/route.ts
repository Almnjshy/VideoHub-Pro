/**
 * GET /api/videohub/stats — get app statistics
 */

import { NextResponse } from 'next/server';
import { db } from '@/lib/db';
import { ensureSeeded } from '@/lib/videohub/backend';

export async function GET() {
  await ensureSeeded();
  const stats = await db.appStat.findUnique({ where: { id: 'singleton' } });

  if (!stats) {
    return NextResponse.json({ error: 'Stats not initialized' }, { status: 500 });
  }

  // Get top platforms from tasks
  const tasks = await db.task.findMany({ select: { platformId: true } });
  const platformCounts = new Map<string, number>();
  for (const t of tasks) {
    platformCounts.set(t.platformId, (platformCounts.get(t.platformId) ?? 0) + 1);
  }
  const topPlatforms = Array.from(platformCounts.entries())
    .map(([pluginId, count]) => ({ pluginId, count }))
    .sort((a, b) => b.count - a.count)
    .slice(0, 5);

  return NextResponse.json({
    ok: true,
    stats: {
      totalDownloads: stats.totalDownloads,
      completedDownloads: stats.completedDownloads,
      failedDownloads: stats.failedDownloads,
      totalBytesDownloaded: Number(stats.totalBytesDownloaded),
      averageSpeed: Number(stats.averageSpeed),
      topPlatforms,
    },
    storage: {
      usedBytes: Number(stats.storageUsedBytes),
      totalBytes: Number(stats.storageLimitBytes),
      fileCount: stats.fileCount,
    },
  });
}
