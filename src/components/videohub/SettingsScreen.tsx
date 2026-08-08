'use client';

import { useVideoHub, formatBytes } from '@/store/videohub';
import { cn } from '@/lib/utils';
import { Settings, Gauge, HardDrive, Bell, Palette, Globe, Zap, Network } from 'lucide-react';
import type { MediaQuality, MediaType } from '@/lib/videohub/types';

const QUALITIES: MediaQuality[] = ['audio', '144p', '240p', '360p', '480p', '720p', '1080p', '4k'];
const MEDIA_TYPES: MediaType[] = ['video', 'audio', 'image', 'file'];

export function SettingsScreen() {
  const settings = useVideoHub((s) => s.settings);
  const updateSettings = useVideoHub((s) => s.updateSettings);

  return (
    <div className="space-y-3">
      <h2 className="text-lg font-bold text-zinc-100">الإعدادات</h2>

      {/* Default quality */}
      <SettingsCard icon={<Gauge className="h-4 w-4" />} title="الجودة الافتراضية">
        <div className="mb-2 text-[11px] text-zinc-500">الجودة المختارة مسبقاً عند ظهور نافذة المشاركة</div>
        <div className="grid grid-cols-4 gap-1.5">
          {QUALITIES.map((q) => (
            <button
              key={q}
              onClick={() => updateSettings({ defaultQuality: q })}
              className={cn(
                'h-10 rounded-lg border text-[11px] font-medium transition-colors',
                settings.defaultQuality === q
                  ? 'border-amber-500/50 bg-amber-500/15 text-amber-200'
                  : 'border-white/10 bg-white/5 text-zinc-300 hover:bg-white/10',
              )}
            >
              {q === 'audio' ? 'صوت' : q}
            </button>
          ))}
        </div>
      </SettingsCard>

      {/* Default media type */}
      <SettingsCard icon={<Gauge className="h-4 w-4" />} title="نوع الوسائط الافتراضي">
        <div className="grid grid-cols-4 gap-1.5">
          {MEDIA_TYPES.map((mt) => (
            <button
              key={mt}
              onClick={() => updateSettings({ defaultMediaType: mt })}
              className={cn(
                'h-10 rounded-lg border text-[11px] font-medium transition-colors',
                settings.defaultMediaType === mt
                  ? 'border-amber-500/50 bg-amber-500/15 text-amber-200'
                  : 'border-white/10 bg-white/5 text-zinc-300 hover:bg-white/10',
              )}
            >
              {mt === 'video' ? 'فيديو' : mt === 'audio' ? 'صوت' : mt === 'image' ? 'صورة' : 'ملف'}
            </button>
          ))}
        </div>
      </SettingsCard>

      {/* Concurrency */}
      <SettingsCard icon={<Zap className="h-4 w-4" />} title="التزامن والتنزيلات المتوازية">
        <div className="mb-2 flex items-baseline justify-between">
          <span className="text-[11px] text-zinc-500">التنزيلات المتزامنة</span>
          <span className="font-mono text-2xl font-bold text-amber-300">{settings.concurrentDownloads}</span>
        </div>
        <input
          type="range"
          min={1}
          max={6}
          value={settings.concurrentDownloads}
          onChange={(e) => updateSettings({ concurrentDownloads: Number(e.target.value) })}
          className="w-full accent-amber-500"
        />
        <div className="mt-1 flex justify-between text-[10px] text-zinc-500">
          <span>1</span>
          <span>6 (أقصى)</span>
        </div>
      </SettingsCard>

      {/* Per-domain concurrency */}
      <SettingsCard icon={<Network className="h-4 w-4" />} title="حد المنصة الواحدة">
        <div className="mb-2 flex items-baseline justify-between">
          <span className="text-[11px] text-zinc-500">أقصى تنزيل متزامن لكل منصة</span>
          <span className="font-mono text-2xl font-bold text-amber-300">{settings.concurrentPerDomain ?? 2}</span>
        </div>
        <input
          type="range"
          min={1}
          max={5}
          value={settings.concurrentPerDomain ?? 2}
          onChange={(e) => updateSettings({ concurrentPerDomain: Number(e.target.value) } as Partial<typeof settings>)}
          className="w-full accent-amber-500"
        />
        <div className="mt-1 text-[10px] text-zinc-500">
          يمنع الضغط على مصدر واحد — يوزّع الحمل على المنصات
        </div>
      </SettingsCard>

      {/* Smart Scheduling */}
      <SettingsCard icon={<Settings className="h-4 w-4" />} title="الجدولة الذكية">
        <ToggleRow
          label="تفعيل الجدولة الذكية"
          description="ترتيب المهام حسب الأولوية + حد لكل منصة"
          checked={settings.smartScheduling ?? true}
          onChange={(v) => updateSettings({ smartScheduling: v } as Partial<typeof settings>)}
        />
      </SettingsCard>

      {/* Bandwidth Limit */}
      <SettingsCard icon={<Gauge className="h-4 w-4" />} title="حد النطاق الترددي">
        <div className="mb-2 flex items-baseline justify-between">
          <span className="text-[11px] text-zinc-500">الحد الأقصى (Mbps) — 0 = غير محدود</span>
          <span className="font-mono text-2xl font-bold text-amber-300">{settings.bandwidthLimitMbps ?? 0}</span>
        </div>
        <input
          type="range"
          min={0}
          max={100}
          step={5}
          value={settings.bandwidthLimitMbps ?? 0}
          onChange={(e) => updateSettings({ bandwidthLimitMbps: Number(e.target.value) } as Partial<typeof settings>)}
          className="w-full accent-amber-500"
        />
      </SettingsCard>

      {/* Auto retry */}
      <SettingsCard icon={<Settings className="h-4 w-4" />} title="إعادة المحاولة التلقائية">
        <ToggleRow
          label="تفعيل إعادة المحاولة عند الفشل"
          description="إعادة محاولة المهام الفاشلة تلقائياً"
          checked={settings.autoRetry}
          onChange={(v) => updateSettings({ autoRetry: v })}
        />
        {settings.autoRetry && (
          <div className="mt-3 border-t border-white/5 pt-3">
            <div className="mb-2 flex items-baseline justify-between">
              <span className="text-[11px] text-zinc-500">الحد الأقصى لإعادة المحاولة</span>
              <span className="font-mono text-lg font-bold text-amber-300">{settings.maxRetries}</span>
            </div>
            <input
              type="range"
              min={1}
              max={10}
              value={settings.maxRetries}
              onChange={(e) => updateSettings({ maxRetries: Number(e.target.value) })}
              className="w-full accent-amber-500"
            />
          </div>
        )}
      </SettingsCard>

      {/* Storage */}
      <SettingsCard icon={<HardDrive className="h-4 w-4" />} title="التخزين">
        <div className="mb-2">
          <div className="mb-1 text-[11px] text-zinc-500">مسار التنزيل</div>
          <input
            type="text"
            value={settings.downloadPath}
            onChange={(e) => updateSettings({ downloadPath: e.target.value })}
            dir="ltr"
            className="w-full rounded-lg border border-white/10 bg-zinc-950/60 px-3 py-2 text-left font-mono text-xs text-zinc-100 focus:border-amber-500/40 focus:outline-none"
          />
        </div>
        <div className="mt-3">
          <div className="mb-2 flex items-baseline justify-between">
            <span className="text-[11px] text-zinc-500">حد التخزين (GB)</span>
            <span className="font-mono text-lg font-bold text-amber-300">{settings.storageLimitGb} GB</span>
          </div>
          <input
            type="range"
            min={8}
            max={128}
            step={8}
            value={settings.storageLimitGb}
            onChange={(e) => updateSettings({ storageLimitGb: Number(e.target.value) })}
            className="w-full accent-amber-500"
          />
        </div>
      </SettingsCard>

      {/* Notifications */}
      <SettingsCard icon={<Bell className="h-4 w-4" />} title="الإشعارات">
        <ToggleRow
          label="إشعارات النظام"
          description="إظهار الإشعارات عند اكتمال/فشل التنزيل"
          checked={settings.notificationsEnabled}
          onChange={(v) => updateSettings({ notificationsEnabled: v })}
        />
        <button
          onClick={async () => {
            if ('Notification' in window && Notification.permission === 'default') {
              await Notification.requestPermission();
            }
          }}
          className="mt-3 w-full rounded-lg border border-amber-500/30 bg-amber-500/10 py-2 text-[11px] font-medium text-amber-300 hover:bg-amber-500/20"
        >
          طلب إذن الإشعارات
        </button>
      </SettingsCard>

      {/* Theme & Language */}
      <SettingsCard icon={<Palette className="h-4 w-4" />} title="المظهر واللغة">
        <div className="mb-3">
          <div className="mb-1.5 text-[11px] text-zinc-500">السمة</div>
          <div className="grid grid-cols-3 gap-1.5">
            {([
              { v: 'dark', label: 'داكنة', bg: 'bg-zinc-800' },
              { v: 'light', label: 'فاتحة', bg: 'bg-zinc-100' },
              { v: 'amoled', label: 'AMOLED', bg: 'bg-black' },
            ] as const).map((theme) => (
              <button
                key={theme.v}
                onClick={() => updateSettings({ theme: theme.v as 'dark' | 'light' } as Partial<typeof settings>)}
                className={cn(
                  'flex h-12 flex-col items-center justify-center gap-1 rounded-lg border text-[11px] font-medium transition-colors',
                  (settings.theme === theme.v || (settings.theme === 'dark' && theme.v === 'amoled'))
                    ? 'border-amber-500/50 bg-amber-500/15 text-amber-200'
                    : 'border-white/10 bg-white/5 text-zinc-300 hover:bg-white/10',
                )}
              >
                <span className={cn('h-3 w-3 rounded-full border border-white/20', theme.bg)} />
                {theme.label}
              </button>
            ))}
          </div>
        </div>

        {/* Accent color picker */}
        <div className="mb-3">
          <div className="mb-1.5 text-[11px] text-zinc-500">اللون المميز</div>
          <div className="flex gap-2">
            {[
              { color: '#f59e0b', name: 'amber' },
              { color: '#10b981', name: 'emerald' },
              { color: '#3b82f6', name: 'blue' },
              { color: '#8b5cf6', name: 'purple' },
              { color: '#ec4899', name: 'pink' },
              { color: '#ef4444', name: 'red' },
            ].map((c) => (
              <button
                key={c.name}
                className="h-9 w-9 rounded-full border-2 border-white/20 transition-transform hover:scale-110 active:scale-95"
                style={{ backgroundColor: c.color }}
                aria-label={c.name}
              />
            ))}
          </div>
        </div>

        <div>
          <div className="mb-1.5 flex items-center gap-1 text-[11px] text-zinc-500">
            <Globe className="h-3 w-3" />
            اللغة
          </div>
          <div className="grid grid-cols-2 gap-1.5">
            {(['ar', 'en'] as const).map((lang) => (
              <button
                key={lang}
                onClick={() => updateSettings({ language: lang })}
                className={cn(
                  'h-10 rounded-lg border text-[11px] font-medium transition-colors',
                  settings.language === lang
                    ? 'border-amber-500/50 bg-amber-500/15 text-amber-200'
                    : 'border-white/10 bg-white/5 text-zinc-300 hover:bg-white/10',
                )}
              >
                {lang === 'ar' ? 'العربية' : 'English'}
              </button>
            ))}
          </div>
        </div>
      </SettingsCard>

      {/* Smart Mode */}
      <SettingsCard icon={<Zap className="h-4 w-4" />} title="الوضع الذكي Smart Mode">
        <ToggleRow
          label="تفعيل الوضع الذكي"
          description="إعدادات ذكية لكل منصة (يوتيوب: جودات، تيك توك: بدون علامة مائية، إنستغرام: صور+فيديو)"
          checked={settings.smartScheduling ?? true}
          onChange={(v) => updateSettings({ smartScheduling: v } as Partial<typeof settings>)}
        />
      </SettingsCard>
    </div>
  );
}

function SettingsCard({
  icon, title, children,
}: {
  icon: React.ReactNode;
  title: string;
  children: React.ReactNode;
}) {
  return (
    <div className="rounded-2xl border border-white/10 bg-zinc-900/60 p-4">
      <div className="mb-3 flex items-center gap-2">
        <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-amber-500/10 text-amber-300">
          {icon}
        </span>
        <h3 className="text-sm font-semibold text-zinc-200">{title}</h3>
      </div>
      {children}
    </div>
  );
}

function ToggleRow({
  label, description, checked, onChange,
}: {
  label: string;
  description?: string;
  checked: boolean;
  onChange: (v: boolean) => void;
}) {
  return (
    <div className="flex items-center justify-between">
      <div>
        <div className="text-xs font-medium text-zinc-200">{label}</div>
        {description && <div className="text-[10px] text-zinc-500">{description}</div>}
      </div>
      <button
        onClick={() => onChange(!checked)}
        className={cn(
          'relative h-6 w-11 flex-shrink-0 rounded-full transition-colors',
          checked ? 'bg-emerald-500' : 'bg-zinc-700',
        )}
        aria-label={label}
      >
        <span
          className="absolute top-0.5 h-5 w-5 rounded-full bg-white transition-transform"
          style={{
            right: checked ? '0.125rem' : 'calc(100% - 1.375rem)',
          }}
        />
      </button>
    </div>
  );
}
