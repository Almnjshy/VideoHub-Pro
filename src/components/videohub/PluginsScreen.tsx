'use client';

import { useVideoHub } from '@/store/videohub';
import { cn } from '@/lib/utils';
import { Plug, RefreshCw, AlertTriangle, CheckCircle2, Activity, Clock, Cpu, ChevronDown, ChevronUp } from 'lucide-react';
import { useState } from 'react';

export function PluginsScreen() {
  const plugins = useVideoHub((s) => s.plugins);
  const pluginHealth = useVideoHub((s) => s.pluginHealth);
  const togglePlugin = useVideoHub((s) => s.togglePlugin);
  const runPluginAutoTest = useVideoHub((s) => s.runPluginAutoTest);
  const faultReports = useVideoHub((s) => s.faultReports);
  const autoTestResults = useVideoHub((s) => s.autoTestResults);

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-bold text-zinc-100">الوحدات</h2>
        <span className="text-[11px] text-zinc-500">{plugins.filter((p) => p.enabled).length}/{plugins.length} مفعّلة</span>
      </div>

      {/* Plugins list */}
      <section>
        <div className="mb-2 flex items-center gap-2">
          <Plug className="h-4 w-4 text-zinc-400" />
          <h3 className="text-sm font-semibold text-zinc-200">الوحدات المثبتة</h3>
        </div>

        <div className="space-y-2">
          {plugins.map((plugin) => {
            const health = pluginHealth[plugin.id];
            return (
              <PluginRow
                key={plugin.id}
                plugin={plugin}
                health={health}
                onToggle={() => togglePlugin(plugin.id)}
                onTest={() => runPluginAutoTest(plugin.id)}
              />
            );
          })}
        </div>
      </section>

      {/* Fault reports */}
      {faultReports.length > 0 && (
        <section>
          <div className="mb-2 flex items-center gap-2">
            <AlertTriangle className="h-4 w-4 text-amber-400" />
            <h3 className="text-sm font-semibold text-zinc-200">تقارير الأعطال</h3>
            <span className="text-[10px] text-zinc-500">آخر {Math.min(faultReports.length, 20)}</span>
          </div>
          <div className="space-y-1.5">
            {faultReports.slice(0, 20).map((fault) => (
              <div
                key={fault.id}
                className="flex items-start gap-2 rounded-lg border border-white/5 bg-zinc-900/40 p-2.5 text-[11px]"
              >
                <div className={cn(
                  'mt-0.5 h-1.5 w-1.5 flex-shrink-0 rounded-full',
                  fault.errorType === 'source_changed' ? 'bg-amber-400' : 'bg-red-400',
                )} />
                <div className="min-w-0 flex-1">
                  <div className="flex flex-wrap items-center gap-x-2 gap-y-0.5">
                    <span className="font-mono uppercase text-amber-300">{fault.pluginId}</span>
                    <span className="text-zinc-500">·</span>
                    <span className="text-zinc-400">{fault.stage}</span>
                    <span className="text-zinc-500">·</span>
                    <span className="text-zinc-500">{new Date(fault.timestamp).toLocaleTimeString('ar')}</span>
                  </div>
                  <div className="mt-0.5 text-zinc-400 line-clamp-2">{fault.message}</div>
                </div>
              </div>
            ))}
          </div>
        </section>
      )}

      {/* Auto-test results */}
      {autoTestResults.length > 0 && (
        <section>
          <div className="mb-2 flex items-center gap-2">
            <Activity className="h-4 w-4 text-emerald-400" />
            <h3 className="text-sm font-semibold text-zinc-200">نتائج الاختبارات التلقائية</h3>
          </div>
          <div className="space-y-1.5">
            {autoTestResults.slice(0, 10).map((test, idx) => (
              <div
                key={idx}
                className="flex items-center gap-2 rounded-lg border border-white/5 bg-zinc-900/40 p-2.5 text-[11px]"
              >
                {test.passed ? (
                  <CheckCircle2 className="h-3.5 w-3.5 flex-shrink-0 text-emerald-400" />
                ) : (
                  <AlertTriangle className="h-3.5 w-3.5 flex-shrink-0 text-red-400" />
                )}
                <span className="font-mono uppercase text-zinc-300">{test.pluginId}</span>
                <span className="text-zinc-500">·</span>
                <span className="text-zinc-400">{test.testName}</span>
                <span className="text-zinc-500">·</span>
                <span className="font-mono text-zinc-500">{test.durationMs}ms</span>
                <span className="mr-auto text-zinc-500">{new Date(test.timestamp).toLocaleTimeString('ar')}</span>
                {test.error && <span className="text-red-300 truncate max-w-[80px]">{test.error}</span>}
              </div>
            ))}
          </div>
        </section>
      )}
    </div>
  );
}

function PluginRow({
  plugin, health, onToggle, onTest,
}: {
  plugin: ReturnType<typeof useVideoHub.getState>['plugins'][number];
  health: ReturnType<typeof useVideoHub.getState>['pluginHealth'][string];
  onToggle: () => void;
  onTest: () => Promise<void>;
}) {
  const [testing, setTesting] = useState(false);
  const [expanded, setExpanded] = useState(false);

  const handleTest = async () => {
    setTesting(true);
    try {
      await onTest();
    } finally {
      setTesting(false);
    }
  };

  if (!health) return null;

  return (
    <div className={cn(
      'rounded-2xl border bg-zinc-900/60 transition-colors',
      plugin.enabled ? 'border-white/10' : 'border-white/5 opacity-60',
    )}>
      <div className="p-3">
        <div className="flex items-center gap-3">
          {/* Icon */}
          <div
            className="flex h-11 w-11 flex-shrink-0 items-center justify-center rounded-xl text-base font-bold text-white"
            style={{ backgroundColor: plugin.color }}
          >
            {plugin.icon}
          </div>

          {/* Info */}
          <div className="min-w-0 flex-1">
            <div className="flex flex-wrap items-center gap-1.5">
              <h4 className="text-sm font-semibold text-zinc-100">{plugin.nameAr}</h4>
              <span className="rounded bg-white/5 px-1.5 py-0.5 font-mono text-[9px] text-zinc-400">v{plugin.version}</span>
            </div>
            <div className="mt-0.5 flex items-center gap-2 text-[10px]">
              <span className="text-zinc-400">{plugin.name}</span>
              <span className={cn(
                'rounded px-1.5 py-0.5 font-medium',
                health.status === 'healthy' && 'bg-emerald-500/10 text-emerald-300',
                health.status === 'degraded' && 'bg-amber-500/10 text-amber-300',
                health.status === 'broken' && 'bg-red-500/10 text-red-300',
              )}>
                {Math.round(health.successRate * 100)}%
              </span>
            </div>
          </div>

          {/* Toggle */}
          <button
            onClick={onToggle}
            className={cn(
              'relative h-6 w-11 flex-shrink-0 rounded-full transition-colors',
              plugin.enabled ? 'bg-emerald-500' : 'bg-zinc-700',
            )}
            aria-label={plugin.enabled ? 'تعطيل' : 'تفعيل'}
          >
            <span
              className="absolute top-0.5 h-5 w-5 rounded-full bg-white transition-transform"
              style={{
                right: plugin.enabled ? '0.125rem' : 'calc(100% - 1.375rem)',
              }}
            />
          </button>
        </div>

        {/* Action row */}
        <div className="mt-2 grid grid-cols-3 gap-1.5">
          <button
            onClick={handleTest}
            disabled={testing || !plugin.enabled}
            className="flex h-9 items-center justify-center gap-1 rounded-lg border border-white/10 text-[11px] text-zinc-300 hover:bg-white/5 disabled:opacity-50"
          >
            <RefreshCw className={cn('h-3 w-3', testing && 'animate-spin')} />
            اختبار
          </button>
          <button
            onClick={() => setExpanded((v) => !v)}
            className="flex h-9 items-center justify-center gap-1 rounded-lg border border-white/10 text-[11px] text-zinc-300 hover:bg-white/5"
          >
            {expanded ? <ChevronUp className="h-3 w-3" /> : <ChevronDown className="h-3 w-3" />}
            تفاصيل
          </button>
          <div className="flex h-9 items-center justify-center rounded-lg border border-white/5 bg-white/[0.02] text-[11px] text-zinc-400">
            <Activity className="ml-1 h-3 w-3" />
            {health.totalAttempts}
          </div>
        </div>

        {/* Expanded details */}
        {expanded && (
          <div className="mt-2 space-y-2 rounded-lg bg-white/5 p-3 text-[11px]">
            <div className="grid grid-cols-3 gap-2">
              <div>
                <div className="text-zinc-500">محاولات</div>
                <div className="font-mono text-zinc-300">{health.totalAttempts}</div>
              </div>
              <div>
                <div className="text-zinc-500">نجاح</div>
                <div className="font-mono text-emerald-300">{health.successfulAttempts}</div>
              </div>
              <div>
                <div className="text-zinc-500">فشل</div>
                <div className="font-mono text-red-300">{health.failedAttempts}</div>
              </div>
            </div>
            <div className="flex items-center gap-1.5 border-t border-white/5 pt-2 text-[10px] text-zinc-500">
              <Clock className="h-3 w-3" />
              آخر اختبار: {health.lastAutoTestAt ? new Date(health.lastAutoTestAt).toLocaleString('ar') : '—'}
            </div>
            {health.nextAutoTestAt && (
              <div className="flex items-center gap-1.5 text-[10px] text-zinc-500">
                <Cpu className="h-3 w-3" />
                الاختبار التالي: {new Date(health.nextAutoTestAt).toLocaleString('ar')}
              </div>
            )}
          </div>
        )}

        {/* Last error */}
        {health.lastError && (
          <div className="mt-2 flex items-start gap-1.5 rounded-lg bg-red-500/5 px-2 py-1.5 text-[10px] text-red-300">
            <AlertTriangle className="mt-0.5 h-3 w-3 flex-shrink-0" />
            <span className="flex-1 line-clamp-2">{health.lastError.message}</span>
          </div>
        )}
      </div>
    </div>
  );
}
