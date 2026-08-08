/**
 * POST /api/videohub/resolve
 * يحلل رابطًا ويستخرج بيانات الوسائط + الجودات المتاحة
 */

import { NextRequest, NextResponse } from 'next/server';
import { resolveUrl, ensureSeeded } from '@/lib/videohub/backend';

export async function POST(request: NextRequest) {
  try {
    await ensureSeeded();
    const body = await request.json();
    const { url } = body as { url?: string };

    if (!url || typeof url !== 'string') {
      return NextResponse.json(
        { error: 'url is required' },
        { status: 400 },
      );
    }

    const result = await resolveUrl(url);

    if ('error' in result) {
      return NextResponse.json(
        { ok: false, error: result.error },
        { status: 503 },
      );
    }

    const { metadata, plugin } = result;

    return NextResponse.json({
      ok: true,
      metadata,
      plugin: {
        id: plugin.id,
        name: plugin.name,
        nameAr: plugin.nameAr,
        icon: plugin.icon,
        color: plugin.color,
        version: plugin.version,
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
