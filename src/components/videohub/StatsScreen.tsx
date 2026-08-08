'use client';

import { useVideoHub, formatBytes } from '@/store/videohub';
import { BarChart3, TrendingUp, Clock, Layers, Zap, CheckCircle2, AlertTriangle } from 'lucide-react';
import {
  ResponsiveContainer, BarChart, Bar, XAxis, YAxis, Tooltip, CartesianGrid,
  PieChart, Pie, Cell, LineChart, Line,
} from 'recharts';

// Stable synthetic activity data (deterministic — no Date.now / Math.random)
// Generated once at module load to avoid hydration mismatch.
const ACTIVITY_DATA = Array.from({ length: 12 }, (_, i) => {
  const hourAgo = 11 - i;
  const seed = (hourAgo * 31) % 5;
  return {
    hour: `${hourAgo}س`,
    downloads: 2 + seed,
  };
});

export function StatsScreen() {
  const stats = useVideoHub((s) => s.stats);
  const pluginHealth = useVideoHub((s) => s.pluginHealth);
  const plugins = useVideoHub((s) => s.plugins);

  const platformData = stats.topPlatforms.map((p) => {
    const plugin = plugins.find((pl) => pl.id === p.pluginId);
    return {
      name: plugin?.nameAr ?? p.pluginId,
      value: p.count,
      color: plugin?.color ?? '#71717A',
    };
  });

  const healthData = plugins.map((p) => {
    const h = pluginHealth[p.id];
    return {
      name: p.nameAr,
      success: Math.round((h?.successRate ?? 0) * 100),
      attempts: h?.totalAttempts ?? 0,
    };
  });

  const successRate = stats.totalDownloads > 0 ? stats.completedDownloads / stats.totalDownloads : 0;
  const activityData = ACTIVITY_DATA;

  return (
    <div className="space-y-3">
      <h2 className="text-lg font-bold text-zinc-100">الإحصائيات</h2>

      {/* Top stats grid */}
      <div className="grid grid-cols-2 gap-2">
        <StatCard icon={<TrendingUp className="h-4 w-4" />} label="إجمالي التنزيلات" value={String(stats.totalDownloads)} color="amber" />
        <StatCard icon={<Layers className="h-4 w-4" />} label="الحجم المنزّل" value={formatBytes(stats.totalBytesDownloaded)} color="emerald" />
        <StatCard icon={<Clock className="h-4 w-4" />} label="متوسط السرعة" value={`${(stats.averageSpeed / (1024 * 1024)).toFixed(1)} MB/s`} color="blue" />
        <StatCard icon={<BarChart3 className="h-4 w-4" />} label="معدل النجاح" value={`${Math.round(successRate * 100)}%`} color={successRate > 0.9 ? 'emerald' : 'amber'} />
      </div>

      {/* Quick KPIs */}
      <div className="grid grid-cols-3 gap-2">
        <QuickStat icon={<Zap className="h-3 w-3" />} label="نشطة الآن" value={String(stats.totalDownloads - stats.completedDownloads - stats.failedDownloads)} />
        <QuickStat icon={<CheckCircle2 className="h-3 w-3" />} label="مكتملة" value={String(stats.completedDownloads)} />
        <QuickStat icon={<AlertTriangle className="h-3 w-3" />} label="فشلت" value={String(stats.failedDownloads)} />
      </div>

      {/* Platform distribution */}
      <ChartCard title="توزيع التنزيلات حسب المنصة">
        <ResponsiveContainer width="100%" height={200}>
          <BarChart data={platformData} margin={{ top: 8, right: 8, left: 0, bottom: 8 }}>
            <CartesianGrid strokeDasharray="3 3" stroke="#27272a" />
            <XAxis dataKey="name" tick={{ fill: '#a1a1aa', fontSize: 10 }} />
            <YAxis tick={{ fill: '#a1a1aa', fontSize: 10 }} />
            <Tooltip
              contentStyle={{
                backgroundColor: '#18181b',
                border: '1px solid #3f3f46',
                borderRadius: '8px',
                fontSize: '12px',
              }}
              labelStyle={{ color: '#e4e4e7' }}
            />
            <Bar dataKey="value" radius={[4, 4, 0, 0]}>
              {platformData.map((entry, idx) => (
                <Cell key={idx} fill={entry.color} />
              ))}
            </Bar>
          </BarChart>
        </ResponsiveContainer>
      </ChartCard>

      {/* Plugin health */}
      <ChartCard title="صحة الوحدات (معدل النجاح %)">
        <ResponsiveContainer width="100%" height={Math.max(200, healthData.length * 30)}>
          <BarChart data={healthData} layout="vertical" margin={{ top: 8, right: 16, left: 8, bottom: 8 }}>
            <CartesianGrid strokeDasharray="3 3" stroke="#27272a" horizontal={false} />
            <XAxis type="number" domain={[0, 100]} tick={{ fill: '#a1a1aa', fontSize: 10 }} />
            <YAxis type="category" dataKey="name" tick={{ fill: '#a1a1aa', fontSize: 10 }} width={70} />
            <Tooltip
              contentStyle={{
                backgroundColor: '#18181b',
                border: '1px solid #3f3f46',
                borderRadius: '8px',
                fontSize: '12px',
              }}
            />
            <Bar dataKey="success" radius={[0, 4, 4, 0]}>
              {healthData.map((entry, idx) => (
                <Cell
                  key={idx}
                  fill={entry.success >= 95 ? '#10b981' : entry.success >= 80 ? '#f59e0b' : '#ef4444'}
                />
              ))}
            </Bar>
          </BarChart>
        </ResponsiveContainer>
      </ChartCard>

      {/* Activity timeline */}
      <ChartCard title="النشاط خلال آخر 12 ساعة">
        <ResponsiveContainer width="100%" height={160}>
          <LineChart data={activityData} margin={{ top: 8, right: 8, left: 0, bottom: 8 }}>
            <CartesianGrid strokeDasharray="3 3" stroke="#27272a" />
            <XAxis dataKey="hour" tick={{ fill: '#a1a1aa', fontSize: 10 }} />
            <YAxis tick={{ fill: '#a1a1aa', fontSize: 10 }} allowDecimals={false} />
            <Tooltip
              contentStyle={{
                backgroundColor: '#18181b',
                border: '1px solid #3f3f46',
                borderRadius: '8px',
                fontSize: '12px',
              }}
            />
            <Line
              type="monotone"
              dataKey="downloads"
              stroke="#f59e0b"
              strokeWidth={2}
              dot={{ fill: '#f59e0b', r: 3 }}
              activeDot={{ r: 5 }}
            />
          </LineChart>
        </ResponsiveContainer>
      </ChartCard>

      {/* Platform pie */}
      {platformData.length > 0 && (
        <ChartCard title="توزيع المنصات">
          <ResponsiveContainer width="100%" height={180}>
            <PieChart>
              <Pie
                data={platformData}
                dataKey="value"
                nameKey="name"
                cx="50%"
                cy="50%"
                outerRadius={60}
                innerRadius={35}
                paddingAngle={2}
              >
                {platformData.map((entry, idx) => (
                  <Cell key={idx} fill={entry.color} />
                ))}
              </Pie>
              <Tooltip
                contentStyle={{
                  backgroundColor: '#18181b',
                  border: '1px solid #3f3f46',
                  borderRadius: '8px',
                  fontSize: '12px',
                }}
              />
            </PieChart>
          </ResponsiveContainer>
          <div className="mt-2 grid grid-cols-2 gap-1.5 text-[11px] sm:grid-cols-3">
            {platformData.map((p) => (
              <div key={p.name} className="flex items-center gap-1.5">
                <span className="h-2 w-2 rounded-sm" style={{ backgroundColor: p.color }} />
                <span className="truncate text-zinc-300">{p.name}</span>
                <span className="mr-auto font-mono text-zinc-500">{p.value}</span>
              </div>
            ))}
          </div>
        </ChartCard>
      )}
    </div>
  );
}

function StatCard({
  icon, label, value, color,
}: {
  icon: React.ReactNode;
  label: string;
  value: string;
  color: 'amber' | 'emerald' | 'blue' | 'red';
}) {
  const colorMap = {
    amber: 'text-amber-300 bg-amber-500/10',
    emerald: 'text-emerald-300 bg-emerald-500/10',
    blue: 'text-blue-300 bg-blue-500/10',
    red: 'text-red-300 bg-red-500/10',
  };
  return (
    <div className="rounded-2xl border border-white/10 bg-zinc-900/60 p-3">
      <div className="flex items-center justify-between">
        <span className="text-[10px] uppercase tracking-wider text-zinc-500">{label}</span>
        <span className={`flex h-7 w-7 items-center justify-center rounded-lg ${colorMap[color]}`}>
          {icon}
        </span>
      </div>
      <div className="mt-1.5 font-mono text-base font-bold text-zinc-100">{value}</div>
    </div>
  );
}

function QuickStat({ icon, label, value }: { icon: React.ReactNode; label: string; value: string }) {
  return (
    <div className="rounded-xl border border-white/5 bg-white/[0.02] p-2.5 text-center">
      <div className="flex items-center justify-center gap-1 text-amber-300">
        {icon}
      </div>
      <div className="mt-1 font-mono text-lg font-bold text-zinc-100">{value}</div>
      <div className="text-[9px] text-zinc-500">{label}</div>
    </div>
  );
}

function ChartCard({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <div className="rounded-2xl border border-white/10 bg-zinc-900/60 p-3">
      <h3 className="mb-2 text-sm font-semibold text-zinc-200">{title}</h3>
      {children}
    </div>
  );
}
