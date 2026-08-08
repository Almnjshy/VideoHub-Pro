/**
 * GET /api/videohub/notifications — list notifications
 */

import { NextResponse } from 'next/server';
import { db } from '@/lib/db';

export async function GET() {
  const notifications = await db.notification.findMany({
    orderBy: { timestamp: 'desc' },
    take: 50,
  });

  const unread = await db.notification.count({ where: { read: false } });

  return NextResponse.json({
    ok: true,
    notifications: notifications.map((n) => ({
      ...n,
      timestamp: n.timestamp.getTime(),
    })),
    unreadCount: unread,
  });
}
