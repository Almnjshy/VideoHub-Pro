/**
 * /api/videohub/tasks/[id]
 * PATCH  — update task (pause/resume/retry/priority)
 * DELETE — cancel/delete task
 * GET    — get single task
 */

import { NextRequest, NextResponse } from 'next/server';
import { db } from '@/lib/db';

export async function GET(
  _request: NextRequest,
  { params }: { params: Promise<{ id: string }> },
) {
  const { id } = await params;
  const task = await db.task.findUnique({ where: { id } });
  if (!task) {
    return NextResponse.json({ error: 'Task not found' }, { status: 404 });
  }
  return NextResponse.json({ ok: true, task });
}

export async function PATCH(
  request: NextRequest,
  { params }: { params: Promise<{ id: string }> },
) {
  const { id } = await params;
  const body = await request.json();
  const { action, priority } = body as { action?: string; priority?: number };

  const task = await db.task.findUnique({ where: { id } });
  if (!task) {
    return NextResponse.json({ error: 'Task not found' }, { status: 404 });
  }

  if (action === 'pause') {
    await db.task.update({
      where: { id },
      data: { status: 'paused' },
    });
  } else if (action === 'resume') {
    await db.task.update({
      where: { id },
      data: { status: 'queued' },
    });
  } else if (action === 'retry') {
    await db.task.update({
      where: { id },
      data: {
        status: 'queued',
        retries: { increment: 1 },
        error: null,
        errorStage: null,
        segmentsJson: JSON.stringify(
          (JSON.parse(task.segmentsJson) as Array<{ status: string; downloadedByte: number; startByte: number; endByte: number }>).map((seg) => ({
            ...seg,
            status: seg.status === 'failed' ? 'pending' : seg.status,
          })),
        ),
      },
    });
  } else if (action === 'set_priority' && typeof priority === 'number') {
    await db.task.update({
      where: { id },
      data: { priority },
    });
  }

  return NextResponse.json({ ok: true });
}

export async function DELETE(
  _request: NextRequest,
  { params }: { params: Promise<{ id: string }> },
) {
  const { id } = await params;
  await db.task.delete({ where: { id } });
  return NextResponse.json({ ok: true });
}
