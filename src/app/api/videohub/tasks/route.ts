/**
 * POST /api/videohub/tasks — create new download task
 * GET  /api/videohub/tasks — list all tasks
 */

import { NextRequest, NextResponse } from 'next/server';
import { db } from '@/lib/db';
import { ensureSeeded, ensurePluginsSeeded } from '@/lib/videohub/backend';
import type { MediaFormat, MediaMetadata } from '@/lib/videohub/types';

export async function POST(request: NextRequest) {
  try {
    await ensureSeeded();
    const body = await request.json();
    const { metadata, format, priority } = body as {
      metadata: MediaMetadata;
      format: MediaFormat;
      priority?: number;
    };

    if (!metadata || !format) {
      return NextResponse.json(
        { error: 'metadata and format are required' },
        { status: 400 },
      );
    }

    const segments = Array.from({ length: 4 }, (_, i) => ({
      id: i,
      startByte: i * Math.floor(format.sizeBytes / 4),
      endByte: i === 3 ? format.sizeBytes : (i + 1) * Math.floor(format.sizeBytes / 4),
      downloadedByte: 0,
      status: 'pending',
    }));

    const task = await db.task.create({
      data: {
        sourceUrl: metadata.sourceUrl,
        platformId: metadata.platformId,
        title: metadata.title,
        author: metadata.author ?? null,
        thumbnailUrl: metadata.thumbnailUrl ?? null,
        durationSeconds: metadata.durationSeconds ?? null,
        description: metadata.description ?? null,
        formatId: format.id,
        formatQuality: format.quality,
        formatExt: format.ext,
        formatSizeBytes: BigInt(format.sizeBytes),
        formatMediaType: format.mediaType,
        status: 'queued',
        totalBytes: BigInt(format.sizeBytes),
        priority: priority ?? 1,
        segmentsJson: JSON.stringify(segments),
      },
    });

    // Create notification
    await db.notification.create({
      data: {
        type: 'task_started',
        title: 'تم بدء التنزيل',
        message: metadata.title,
        taskId: task.id,
      },
    });

    return NextResponse.json({ ok: true, taskId: task.id });
  } catch (err: unknown) {
    const e = err as Error;
    return NextResponse.json(
      { ok: false, error: e.message },
      { status: 500 },
    );
  }
}

export async function GET() {
  try {
    await ensureSeeded();
    const tasks = await db.task.findMany({
      orderBy: { createdAt: 'desc' },
      take: 200,
    });

    return NextResponse.json({
      ok: true,
      tasks: tasks.map((t) => ({
        id: t.id,
        sourceUrl: t.sourceUrl,
        platformId: t.platformId,
        title: t.title,
        author: t.author,
        thumbnailUrl: t.thumbnailUrl,
        durationSeconds: t.durationSeconds,
        description: t.description,
        selectedFormat: {
          id: t.formatId,
          quality: t.formatQuality,
          ext: t.formatExt,
          sizeBytes: Number(t.formatSizeBytes),
          mediaType: t.formatMediaType,
          hasAudio: true,
          label: `${t.formatQuality.toUpperCase()} · ${t.formatExt.toUpperCase()}`,
        },
        status: t.status,
        progress: t.progress,
        downloadedBytes: Number(t.downloadedBytes),
        totalBytes: Number(t.totalBytes),
        speedBytesPerSec: Number(t.speedBps),
        etaSeconds: t.etaSeconds,
        retries: t.retries,
        maxRetries: t.maxRetries,
        priority: t.priority,
        error: t.error,
        errorStage: t.errorStage,
        outputPath: t.outputPath,
        segments: JSON.parse(t.segmentsJson),
        createdAt: t.createdAt.getTime(),
        startedAt: t.startedAt?.getTime(),
        completedAt: t.completedAt?.getTime(),
        metadata: {
          title: t.title,
          author: t.author ?? undefined,
          thumbnailUrl: t.thumbnailUrl ?? undefined,
          durationSeconds: t.durationSeconds ?? undefined,
          sourceUrl: t.sourceUrl,
          platformId: t.platformId,
          formats: [],
          resolvedAt: t.createdAt.getTime(),
        },
      })),
    });
  } catch (err: unknown) {
    const e = err as Error;
    return NextResponse.json(
      { ok: false, error: e.message },
      { status: 500 },
    );
  }
}
