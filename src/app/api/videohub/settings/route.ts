/**
 * GET /api/videohub/settings — get settings
 * PATCH /api/videohub/settings — update settings
 */

import { NextRequest, NextResponse } from 'next/server';
import { db } from '@/lib/db';
import { ensureSeeded } from '@/lib/videohub/backend';

export async function GET() {
  await ensureSeeded();
  const settings = await db.setting.findUnique({ where: { id: 'singleton' } });
  if (!settings) {
    return NextResponse.json({ error: 'Settings not initialized' }, { status: 500 });
  }
  return NextResponse.json({
    ok: true,
    settings: {
      defaultQuality: settings.defaultQuality,
      defaultMediaType: settings.defaultMediaType,
      concurrentDownloads: settings.concurrentDownloads,
      concurrentPerDomain: settings.concurrentPerDomain,
      downloadPath: settings.downloadPath,
      autoRetry: settings.autoRetry,
      maxRetries: settings.maxRetries,
      notificationsEnabled: settings.notificationsEnabled,
      storageLimitGb: settings.storageLimitGb,
      smartScheduling: settings.smartScheduling,
      bandwidthLimitMbps: settings.bandwidthLimitMbps,
      theme: settings.theme as 'dark' | 'light',
      language: settings.language as 'ar' | 'en',
    },
  });
}

export async function PATCH(request: NextRequest) {
  await ensureSeeded();
  const body = await request.json();
  await db.setting.update({
    where: { id: 'singleton' },
    data: body,
  });
  return NextResponse.json({ ok: true });
}
