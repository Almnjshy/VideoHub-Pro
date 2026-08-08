/**
 * GET /api/videohub/faults — list fault reports
 * DELETE /api/videohub/faults — clear all
 */

import { NextRequest, NextResponse } from 'next/server';
import { db } from '@/lib/db';

export async function GET(request: NextRequest) {
  const { searchParams } = new URL(request.url);
  const limit = Number(searchParams.get('limit') ?? 50);

  const faults = await db.faultReport.findMany({
    orderBy: { timestamp: 'desc' },
    take: Math.min(limit, 200),
  });

  return NextResponse.json({
    ok: true,
    faults: faults.map((f) => ({
      ...f,
      timestamp: f.timestamp.getTime(),
    })),
  });
}

export async function DELETE() {
  await db.faultReport.deleteMany({});
  return NextResponse.json({ ok: true });
}
