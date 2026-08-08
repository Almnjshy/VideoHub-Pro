/**
 * PATCH /api/videohub/plugins/[id] — toggle enable/disable, update version
 */

import { NextRequest, NextResponse } from 'next/server';
import { db } from '@/lib/db';

export async function PATCH(
  request: NextRequest,
  { params }: { params: Promise<{ id: string }> },
) {
  const { id } = await params;
  const body = await request.json();
  const { enabled, version } = body as { enabled?: boolean; version?: string };

  const update: { enabled?: boolean; version?: string; status?: string; failedAttempts?: number; successRate?: number; lastErrorJson?: string | null } = {};
  if (typeof enabled === 'boolean') update.enabled = enabled;
  if (typeof version === 'string') {
    update.version = version;
    update.status = 'healthy';
    update.failedAttempts = 0;
    update.successRate = 1.0;
    update.lastErrorJson = null;
  }

  await db.plugin.update({ where: { id }, data: update });
  return NextResponse.json({ ok: true });
}
