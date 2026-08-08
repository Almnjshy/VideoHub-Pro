/**
 * GET /api/videohub/plugins — list all plugins with health
 */

import { NextResponse } from 'next/server';
import { getPluginsWithHealth } from '@/lib/videohub/backend';

export async function GET() {
  const plugins = await getPluginsWithHealth();
  return NextResponse.json({ ok: true, plugins });
}
