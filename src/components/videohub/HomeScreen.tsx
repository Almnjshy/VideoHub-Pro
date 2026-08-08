'use client';

import { useVideoHub, formatBytes, selectActiveTasks } from '@/store/videohub';
import { TaskCard, ProgressBar } from './TaskCard';
import { ShareSimulator } from './ShareSimulator';
import { HardDrive, Zap, CheckCircle2, AlertTriangle, Activity, Cpu, Gauge, Layers, Compass, Library, Search } from 'lucide-react';

type Tab = 'home' | 'downloads' | 'library' | 'discover' | 'search' | 'plugins' | 'stats' | 'notifications' | 'settings';

export function HomeScreen({ onNavigate }: { onNavigate?: (tab: Tab) => void }) {
  const tasks = useVideoHub((s) => s.tasks);
  const stats = useVideoHub((s) => s.stats);
  const storage = useVideoHub((s) => s.storage);
  const plugins = useVideoHub((s) => s.plugins);
  const pluginHealth = useVideoHub((s) => s.pluginHealth);
  const settings = useVideoHub((s) => s.settings);
  const activeCount = tasks.filter((t) => t.status === 'downloading' || t.status === 'queued').length;
  const activeTasks = tasks.filter((t) => t.status === 'downloading' || t.status === 'queued' || t.status === 'retrying');
  const recent = tasks.slice(0, 4);

  const storagePct = storage.totalBytes > 0 ? storage.usedBytes / storage.totalBytes : 0;
  const healthValues = Object.values(pluginHealth);
  const healthyPlugins = healthValues.filter((h) => h.status === 'healthy').length;
  const degradedPlugins = healthValues.filter((h) => h.status === 'degraded').length;
  const brokenPlugins = healthValues.filter((h) => h.status === 'broken').length;
  const successRate = stats.totalDownloads > 0 ? stats.completedDownloads / stats.totalDownloads : 0;

  return (
    <div className="space-y-4">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-lg font-bold text-zinc-100">مرحباً بك</h2>
          <p className="text-xs text-zinc-500">{activeCount > 0 ? `${activeCount} تنزيل نشط` : 'لا توجد تنزيلات نشطة'}</p>
        </div>
        <div className="flex items-center gap-1 rounded-full border border-emerald-500/30 bg-emerald-500/10 px-2.5 py-1 text-[10px] text-emerald-300">
          <span className="h-1.5 w-1.5 rounded-full bg-emerald-400 animate-pulse" />
          المحرك يعمل
        </div>
      </div>

      {/* KPI cards — 2x2 grid on mobile */}
      <div className="grid grid-cols-2 gap-2">
        <KpiCard
          icon={<Zap className="h-4 w-4" />}
          label="نشطة"
          value={String(activeCount)}
          sub={`${tasks.length} إجمالي`}
          accent={activeCount > 0 ? 'amber' : 'zinc'}
        />
        <KpiCard
          icon={<CheckCircle2 className="h-4 w-4" />}
          label="مكتملة"
          value={String(stats.completedDownloads)}
          sub={`${stats.failedDownloads} فشل`}
          accent="emerald"
        />
        <KpiCard
          icon={<Activity className="h-4 w-4" />}
          label="معدل النجاح"
          value={`${Math.round(successRate * 100)}%`}
          sub={`${stats.totalDownloads} عملية`}
          accent={successRate > 0.9 ? 'emerald' : successRate > 0.7 ? 'amber' : 'red'}
        />
        <KpiCard
          icon={<AlertTriangle className="h-4 w-4" />}
          label="أعطال الوحدات"
          value={String(brokenPlugins + degradedPlugins)}
          sub={`${healthyPlugins}/${plugins.length} سليم`}
          accent={brokenPlugins > 0 ? 'red' : degradedPlugins > 0 ? 'amber' : 'emerald'}
        />
      </div>

      {/* Storage card */}
      <div className="rounded-2xl border border-white/10 bg-zinc-900/60 p-4">
        <div className="mb-2 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <HardDrive className="h-4 w-4 text-zinc-400" />
            <span className="text-sm font-semibold text-zinc-200">التخزين</span>
          </div>
          <span className="text-[10px] text-zinc-500">{storage.fileCount} ملف</span>
        </div>
        <ProgressBar value={storagePct} />
        <div className="mt-1.5 flex items-center justify-between text-[11px]">
          <span className="font-mono text-zinc-300">{formatBytes(storage.usedBytes)}</span>
          <span className="font-mono text-zinc-500">/ {formatBytes(storage.totalBytes)}</span>
        </div>
      </div>

      {/* Share Simulator */}
      <ShareSimulator />

      {/* Quick Actions — الرئيسية فقط */}
      <div className="grid grid-cols-3 gap-2">
        <QuickAction
          icon={<Library className="h-5 w-5" />}
          label="مكتبتي"
          sub="الوسائط"
          onClick={() => onNavigate?.('library')}
          accent="emerald"
        />
        <QuickAction
          icon={<Compass className="h-5 w-5" />}
          label="اكتشف"
          sub="رائج الآن"
          onClick={() => onNavigate?.('discover')}
          accent="amber"
        />
        <QuickAction
          icon={<Search className="h-5 w-5" />}
          label="بحث"
          sub="عبر المنصات"
          onClick={() => onNavigate?.('search')}
          accent="blue"
        />
      </div>

      {/* Active downloads */}
      <section>
        <div className="mb-2 flex items-center justify-between">
          <h3 className="text-sm font-semibold text-zinc-200">التنزيلات النشطة</h3>
          {activeCount > 0 && (
            <span className="flex items-center gap-1 text-[10px] text-zinc-500">
              <Gauge className="h-3 w-3" />
              {settings.concurrentDownloads} متوازي
            </span>
          )}
        </div>
        {activeTasks.length > 0 ? (
          <div className="space-y-2">
            {activeTasks.slice(0, 3).map((t) => (
              <TaskCard key={t.id} task={t} />
            ))}
          </div>
        ) : (
          <div className="rounded-2xl border border-dashed border-white/10 bg-zinc-900/30 p-8 text-center">
            <Zap className="mx-auto h-8 w-8 text-zinc-700" />
            <p className="mt-2 text-sm text-zinc-500">لا توجد تنزيلات نشطة</p>
            <p className="text-[10px] text-zinc-600">استخدم محاكي المشاركة أعلاه لبدء تنزيل</p>
          </div>
        )}
      </section>

      {/* Recent activity */}
      {recent.length > 0 && (
        <section>
          <h3 className="mb-2 flex items-center gap-2 text-sm font-semibold text-zinc-200">
            <Layers className="h-3.5 w-3.5 text-zinc-500" />
            آخر العمليات
          </h3>
          <div className="space-y-2">
            {recent.map((t) => (
              <TaskCard key={t.id} task={t} />
            ))}
          </div>
        </section>
      )}
    </div>
  );
}

function KpiCard({
  icon, label, value, sub, accent,
}: {
  icon: React.ReactNode;
  label: string;
  value: string;
  sub?: string;
  accent: 'amber' | 'emerald' | 'blue' | 'red' | 'zinc';
}) {
  const accentMap = {
    amber: 'text-amber-300 bg-amber-500/10 border-amber-500/20',
    emerald: 'text-emerald-300 bg-emerald-500/10 border-emerald-500/20',
    blue: 'text-blue-300 bg-blue-500/10 border-blue-500/20',
    red: 'text-red-300 bg-red-500/10 border-red-500/20',
    zinc: 'text-zinc-400 bg-zinc-500/10 border-zinc-500/20',
  };
  return (
    <div className="rounded-2xl border border-white/10 bg-zinc-900/60 p-3">
      <div className="flex items-center justify-between">
        <span className="text-[10px] uppercase tracking-wider text-zinc-500">{label}</span>
        <span className={`flex h-7 w-7 items-center justify-center rounded-lg border ${accentMap[accent]}`}>
          {icon}
        </span>
      </div>
      <div className="mt-1.5 font-mono text-xl font-bold text-zinc-100">{value}</div>
      {sub && <div className="text-[10px] text-zinc-500">{sub}</div>}
    </div>
  );
}

function QuickAction({
  icon, label, sub, onClick, accent,
}: {
  icon: React.ReactNode;
  label: string;
  sub: string;
  onClick: () => void;
  accent: 'amber' | 'emerald' | 'blue' | 'red';
}) {
  const accentMap = {
    amber: 'text-amber-300 bg-amber-500/10 border-amber-500/30',
    emerald: 'text-emerald-300 bg-emerald-500/10 border-emerald-500/30',
    blue: 'text-blue-300 bg-blue-500/10 border-blue-500/30',
    red: 'text-red-300 bg-red-500/10 border-red-500/30',
  };
  return (
    <button
      onClick={onClick}
      className="flex flex-col items-center gap-1.5 rounded-2xl border border-white/10 bg-zinc-900/60 p-3 transition-all hover:border-white/20 hover:bg-zinc-900 active:scale-[0.97]"
    >
      <span className={`flex h-10 w-10 items-center justify-center rounded-xl border ${accentMap[accent]}`}>
        {icon}
      </span>
      <div className="text-center">
        <div className="text-xs font-semibold text-zinc-100">{label}</div>
        <div className="text-[9px] text-zinc-500">{sub}</div>
      </div>
    </button>
  );
}
