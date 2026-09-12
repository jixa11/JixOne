'use client';
import { useEffect, useState } from 'react';
import { useDownloads } from '@/store/downloads';
import { useSettings } from '@/store/settings';
import { fmtSize, t, num } from '@/lib/i18n';
import { storageUsage, deleteLocalFile } from '@/engine/downloader';
import { Trash2, CheckCircle2, AlertCircle, PauseCircle, Loader2, HardDrive, Download, Clock3, TriangleAlert, RotateCcw } from 'lucide-react';
import { toast } from 'sonner';
import { Button } from '@/components/ui/button';
import { TrackThumb } from '@/components/TrackThumb';

export default function DownloadsView({ embedded = false }: { embedded?: boolean }) {
  const lang = useSettings((s) => s.lang);
  const items = useDownloads((s) => s.items);
  const remove = useDownloads((s) => s.remove);
  const clearAll = useDownloads((s) => s.clearAll);
  const update = useDownloads((s) => s.update);
  const [usage, setUsage] = useState({ usage: 0, quota: 0 });
  const list = Object.values(items).sort((a, b) => b.addedAt - a.addedAt);
  const done = list.filter((i) => i.status === 'done');
  const active = list.filter((i) => ['pending', 'extracting', 'downloading', 'waiting-net'].includes(i.status));
  const failed = list.filter((i) => i.status === 'error');

  useEffect(() => { storageUsage().then(setUsage); const iv = setInterval(() => storageUsage().then(setUsage), 5000); return () => clearInterval(iv); }, []);

  const retry = (videoId: string) => { update(videoId, { status: 'pending', progress: 0, error: undefined }); };
  const removeOne = async (videoId: string) => { await deleteLocalFile(videoId); remove(videoId); };

  return (
    <div className={embedded ? '' : 'view-in mx-auto max-w-[900px] px-4 pb-8'}>
      {!embedded && (
        <div className="mb-4 flex items-center justify-between">
          <h1 className="text-2xl font-black sm:text-3xl">{t(lang, 'downloads')}</h1>
          {list.length > 0 && (
            <Button variant="ghost" size="sm" className="text-dim hover:text-red-400" onClick={() => { clearAll(); toast(t(lang, 'deleted')); }}>
              <Trash2 size={14} className="me-1" /> {t(lang, 'clearDownloads')}
            </Button>
          )}
        </div>
      )}
      {embedded && list.length > 0 && (
        <div className="mb-3 flex items-center justify-between">
          <span />
          <Button variant="ghost" size="sm" className="text-dim hover:text-red-400" onClick={() => { clearAll(); toast(t(lang, 'deleted')); }}>
            <Trash2 size={14} className="me-1" /> {t(lang, 'clearDownloads')}
          </Button>
        </div>
      )}

      <div className="mb-6 flex items-center gap-3 rounded-2xl border border-line bg-surface p-4 text-xs text-dim">
        <HardDrive size={18} className="text-[var(--accent)]" />
        <div className="flex-1">
          <div>{t(lang, 'storageUsed')}</div>
          <div className="mt-1 h-1.5 w-full overflow-hidden rounded-full bg-surface2">
            <div className="h-full bg-[var(--accent)]" style={{ width: `${Math.min(100, (usage.usage / Math.max(1, usage.quota)) * 100).toFixed(1)}%` }} />
          </div>
        </div>
        <span className="tabular-nums">{fmtSize(usage.usage, lang)}</span>
      </div>

      {list.length === 0 && (
        <div className="flex flex-col items-center gap-4 rounded-2xl border border-line bg-surface py-14 text-center">
          <span className="grid h-14 w-14 place-items-center rounded-2xl bg-surface2 text-dim">
            <Download size={24} />
          </span>
          <p className="max-w-[320px] text-[13px] leading-relaxed text-dim">{t(lang, 'emptyDownloads')}</p>
        </div>
      )}

      {active.length > 0 && (
        <>
          <SectionTitle icon={Clock3} label={`${t(lang, 'queue')} (${num(active.length, lang)})`} />
          <div className="mb-6 flex flex-col gap-2">
            {active.map((it) => (
              <div key={it.videoId} className="flex items-center gap-3 rounded-2xl border border-line bg-surface p-3">
                <TrackThumb track={it} rounded="rounded-lg" className="h-11 w-11 shrink-0" />
                <div className="min-w-0 flex-1">
                  <div className="truncate text-[13px] font-semibold">{it.title}</div>
                  <div className="truncate text-[11px] text-dim">{it.artist} · ~{fmtSize(it.sizeBytes ?? (it.durationSec || 210) * (it.quality === 'high' ? 20000 : it.quality === 'mid' ? 8750 : 6250), lang)}</div>
                  <div className="mt-1.5 h-1.5 w-full overflow-hidden rounded-full bg-surface2">
                    <div
                      className={`h-full transition-all ${it.status === 'waiting-net' ? 'bg-dim' : 'bg-[var(--accent)] shadow-[var(--glow-soft)]'}`}
                      style={{ width: `${Math.round((it.status === 'extracting' ? 0.02 : it.progress) * 100)}%` }}
                    />
                  </div>
                </div>
                <StatusChip status={it.status} lang={lang} />
              </div>
            ))}
          </div>
        </>
      )}

      {failed.length > 0 && (
        <>
          <SectionTitle icon={TriangleAlert} label={`${t(lang, 'error')} (${num(failed.length, lang)})`} />
          <div className="mb-6 flex flex-col gap-2">
            {failed.map((it) => (
              <div key={it.videoId} className="flex items-center gap-3 rounded-2xl border border-line bg-surface p-3">
                <TrackThumb track={it} rounded="rounded-lg" className="h-11 w-11 shrink-0" />
                <div className="min-w-0 flex-1">
                  <div className="truncate text-[13px] font-semibold">{it.title}</div>
                  <div className="text-[11px] text-red-400">{t(lang, 'error')}: {it.error ?? '?'}</div>
                </div>
                <Button variant="ghost" size="sm" onClick={() => retry(it.videoId)} className="gap-1 text-xs">
                  <RotateCcw size={13} /> {t(lang, 'retry')}
                </Button>
                <Button variant="ghost" size="icon" className="h-9 w-9 text-dim" onClick={() => removeOne(it.videoId)} aria-label="remove"><Trash2 size={14} /></Button>
              </div>
            ))}
          </div>
        </>
      )}

      {done.length > 0 && (
        <>
          <SectionTitle icon={CheckCircle2} label={`${t(lang, 'downloaded')} (${num(done.length, lang)})`} />
          <div className="flex flex-col gap-2">
            {done.map((it) => (
              <div key={it.videoId} className="flex items-center gap-3 rounded-2xl border border-line bg-surface p-3">
                <TrackThumb track={it} rounded="rounded-lg" className="h-11 w-11 shrink-0" />
                <div className="min-w-0 flex-1">
                  <div className="truncate text-[13px] font-semibold">{it.title}</div>
                  <div className="truncate text-[11px] text-dim">{it.artist} · {fmtSize(it.sizeBytes, lang)} · {it.quality === 'high' ? t(lang, 'high') : it.quality === 'mid' ? t(lang, 'mid') : t(lang, 'low')}</div>
                </div>
                <CheckCircle2 size={17} className="shrink-0 text-[var(--accent)]" />
                <Button variant="ghost" size="icon" className="h-9 w-9 text-dim" onClick={() => removeOne(it.videoId)} aria-label="remove"><Trash2 size={14} /></Button>
              </div>
            ))}
          </div>
        </>
      )}
    </div>
  );
}

function SectionTitle({ icon: Icon, label }: { icon: any; label: string }) {
  return (
    <div className="mb-2 flex items-center gap-1.5 text-xs font-black uppercase tracking-wider text-dim">
      <Icon size={13} /> {label}
    </div>
  );
}

function StatusChip({ status, lang }: { status: string; lang: 'fa' | 'en' }) {
  if (status === 'downloading') return <Loader2 size={16} className="animate-spin text-[var(--accent)]" />;
  if (status === 'extracting') return <Loader2 size={16} className="animate-spin text-dim" />;
  if (status === 'waiting-net') return <span title={t(lang, 'waitingNet')} className="inline-flex"><PauseCircle size={16} className="text-dim" /></span>;
  if (status === 'pending') return <Loader2 size={16} className="animate-pulse text-dim" />;
  return <AlertCircle size={16} className="text-red-400" />;
}
