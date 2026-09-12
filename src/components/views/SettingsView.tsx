'use client';
import { THEMES } from '@/lib/themes';
import { useSettings, GlowLevel, PlaybackMode } from '@/store/settings';
import { useLibrary } from '@/store/library';
import { useAuth } from '@/store/auth';
import { t, fmtSize } from '@/lib/i18n';
import { storageUsage } from '@/engine/downloader';
import { signInAndImport, resyncPlaylists } from '@/lib/signin';
import { googleClientId } from '@/lib/google-auth';
import { runDiagnostics, diagReportText, DiagStep } from '@/engine/diagnostics';
import { getApiBase, setApiBase } from '@/engine/apiBase';
import { useState, useEffect } from 'react';
import { Switch } from '@/components/ui/switch';
import { toast } from 'sonner';
import {
  Check, Globe, Radio, Download, Sparkles, ShieldOff, Info,
  LogOut, RefreshCw, UserRound, Activity, X, Loader2, Copy,
} from 'lucide-react';

function GoogleG({ size = 16 }: { size?: number }) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24">
      <path fill="#4285F4" d="M23.5 12.3c0-.9-.1-1.5-.3-2.2H12v4.1h6.5c-.1 1.1-.8 2.7-2.4 3.8l-.02.15 3.5 2.7.24.02c2.2-2.05 3.5-5.05 3.5-8.6z" />
      <path fill="#34A853" d="M12 24c3.2 0 5.9-1.1 7.9-2.9l-3.8-2.9c-1 .7-2.4 1.2-4.1 1.2-3.1 0-5.8-2.1-6.8-5l-.14.01-3.6 2.8-.05.13C3.4 21.3 7.4 24 12 24z" />
      <path fill="#FBBC05" d="M5.2 14.4c-.25-.75-.4-1.55-.4-2.4s.15-1.65.42-2.4l-.01-.16-3.65-2.8-.12.06C.5 8.2 0 10 0 12s.5 3.8 1.44 5.3l3.76-2.9z" />
      <path fill="#EA4335" d="M12 4.6c2.2 0 3.7.95 4.6 1.75l3.35-3.27C17.9 1.15 15.2 0 12 0 7.4 0 3.4 2.7 1.44 6.7l3.75 2.9c1-2.9 3.7-5 6.81-5z" />
    </svg>
  );
}

export default function SettingsView() {
  const s = useSettings();
  const lang = s.lang;
  const offlinePlaysCount = useLibrary((st) => st.offlinePlays).length;
  const user = useAuth((st) => st.user);
  const signOut = useAuth((st) => st.signOut);
  const hasClientId = !!googleClientId();
  const [usage, setUsage] = useState(0);
  const [busy, setBusy] = useState(false);
  const [isAndroid, setIsAndroid] = useState(false);
  const [srv, setSrv] = useState('');
  const [fb, setFb] = useState('');
  const [diagSteps, setDiagSteps] = useState<DiagStep[] | null>(null);
  const [diagBusy, setDiagBusy] = useState(false);
  const [diagVerdict, setDiagVerdict] = useState<{ fa: string; en: string } | null>(null);
  const [diagMode, setDiagMode] = useState<'device' | 'server' | null>(null);
  useEffect(() => { storageUsage().then((u) => setUsage(u.usage)); }, []);
  useEffect(() => {
    const b = (window as any).AndroidBridge;
    setFb(getApiBase());
    if (!b) return;
    setIsAndroid(true);
    try { setSrv(b.getServerUrl?.() ?? ''); } catch { /* noop */ }
  }, []);

  const applyFb = () => {
    const u = fb.trim().replace(/\/+$/, '');
    if (u && !u.startsWith('http')) { toast(t(lang, 'advServerInvalid')); return; }
    setApiBase(u);
    setFb(u);
    toast(t(lang, 'fbApplied'));
  };
  const clearFb = () => {
    setApiBase('');
    setFb('');
    toast(t(lang, 'fbCleared'));
  };

  const applyServer = () => {
    const b = (window as any).AndroidBridge;
    if (!b) return;
    const u = srv.trim().replace(/\/+$/, '');
    if (u && !u.startsWith('http')) { toast(t(lang, 'advServerInvalid')); return; }
    toast(u ? t(lang, 'advServerApplied') : t(lang, 'advServerResetDone'));
    b.setServerUrl(u);
  };
  const resetServer = () => {
    setSrv('');
    const b = (window as any).AndroidBridge;
    if (!b) return;
    toast(t(lang, 'advServerResetDone'));
    b.clearServerUrl();
  };

  const doRealSignIn = async () => {
    setBusy(true);
    toast(t(lang, 'importingPlaylists'));
    const res = await signInAndImport(lang);
    setBusy(false);
    if (res.ok) {
      toast(`${res.imported} ${t(lang, 'importedN')}`);
    } else if (res.reason === 'no-playlists') {
      toast(t(lang, 'noPlaylistsFound'));
    } else {
      toast(lang === 'fa' ? 'ورود گوگل ناموفق بود' : 'Google sign-in failed');
    }
  };

  const doSync = async () => {
    setBusy(true);
    toast(t(lang, 'importingPlaylists'));
    const res = await resyncPlaylists(lang);
    setBusy(false);
    if (res.ok) toast(`${res.imported} ${t(lang, 'importedN')}`);
    else toast(t(lang, 'noPlaylistsFound'));
  };

  const runDiag = async () => {
    setDiagBusy(true);
    setDiagSteps([]);
    setDiagVerdict(null);
    setDiagMode(null);
    const report = await runDiagnostics((steps) => setDiagSteps(steps));
    setDiagVerdict({ fa: report.verdictFa, en: report.verdictEn });
    setDiagMode(report.mode);
    setDiagBusy(false);
  };

  const copyDiag = async () => {
    if (!diagSteps) return;
    const text = diagReportText(diagSteps, diagMode ?? '', diagVerdict ? (lang === 'fa' ? diagVerdict.fa : diagVerdict.en) : '');
    try {
      await navigator.clipboard.writeText(text);
      toast(t(lang, 'diagCopied'));
    } catch {
      (window as any).AndroidBridge?.notify?.(t(lang, 'diagCopied'));
    }
  };

  return (
    <div className="view-in mx-auto max-w-[760px] px-4 pb-10">
      <h1 className="mb-6 text-2xl font-black sm:text-3xl">{t(lang, 'settings')}</h1>

      {/* CONNECTION DIAGNOSTICS — first so users can self-diagnose search/playback issues */}
      <Section icon={Activity} title={t(lang, 'diagTitle')}>
        <div className="space-y-3">
          <div className="text-[11px] leading-relaxed text-dim">{t(lang, 'diagDesc')}</div>
          {diagSteps && diagSteps.length > 0 && (
            <div className="space-y-1.5 rounded-xl bg-surface2 p-3">
              {(['net', 'yt', 'search'] as const).map((id) => {
                const st = diagSteps.find((x) => x.id === id);
                const label = t(lang, id === 'net' ? 'diagNet' : id === 'yt' ? 'diagYt' : 'diagSearch');
                return (
                  <div key={id} className="flex items-center gap-2.5 text-[12px]">
                    {!st ? (
                      <span className="h-[15px] w-[15px] rounded-full border border-line" />
                    ) : st.state === 'run' ? (
                      <Loader2 size={15} className="animate-spin text-[var(--accent)]" />
                    ) : st.state === 'pass' ? (
                      <Check size={15} className="text-[var(--success)]" />
                    ) : (
                      <X size={15} className="text-[var(--danger)]" />
                    )}
                    <span className="font-semibold">{label}</span>
                    {st?.detail && <span dir="ltr" className="ms-auto font-mono text-[10px] text-dim">{st.detail}</span>}
                    {st && st.state !== 'run' && (
                      <span className={`shrink-0 text-[10px] font-semibold ${st.state === 'pass' ? 'text-[var(--success)]' : 'text-[var(--danger)]'}`}>
                        {t(lang, st.state === 'pass' ? 'diagPass' : 'diagFail')}
                      </span>
                    )}
                  </div>
                );
              })}
              {diagVerdict && (
                <div className="border-t border-line pt-2 text-[11.5px] leading-relaxed">
                  {lang === 'fa' ? diagVerdict.fa : diagVerdict.en}
                  {diagMode && (
                    <span className="ms-1 text-[10px] text-dim">
                      ({t(lang, diagMode === 'device' ? 'diagModeDevice' : 'diagModeServer')})
                    </span>
                  )}
                </div>
              )}
              {!diagBusy && diagSteps.length > 0 && (
                <button
                  onClick={copyDiag}
                  className="flex items-center gap-1.5 rounded-full border border-line px-3.5 py-1.5 text-[10.5px] font-semibold text-dim transition-colors hover:bg-surface hover:text-foreground"
                >
                  <Copy size={12} /> {t(lang, 'diagCopy')}
                </button>
              )}
            </div>
          )}
          <button
            onClick={runDiag}
            disabled={diagBusy}
            className="neon-play flex items-center gap-2 rounded-full px-5 py-2.5 text-[13px] font-bold transition-transform active:scale-95 disabled:opacity-60"
          >
            <Activity size={15} /> {diagSteps && diagSteps.length > 0 && !diagBusy ? t(lang, 'diagRerun') : t(lang, 'diagRun')}
          </button>
        </div>
      </Section>

      {/* ACCOUNT — Google sign-in + YTMusic playlist sync.
          HONESTY RULE (user request): no fake demo sign-in. When no real OAuth
          client is configured, the button is disabled and the truth is shown. */}
      <Section icon={UserRound} title={t(lang, 'account')}>
        {!user ? (
          <div className="flex flex-col items-start gap-3 sm:flex-row sm:items-center sm:justify-between">
            <div className="min-w-0">
              <div className="text-[13px] font-semibold">
                {hasClientId ? t(lang, 'signInGoogle') : t(lang, 'signInUnavailable')}
              </div>
              <div className="mt-0.5 max-w-[420px] text-[11.5px] leading-relaxed text-dim">
                {hasClientId
                  ? `${t(lang, 'fromYTMusic')} — YouTube Data API (youtube.readonly)`
                  : t(lang, 'signInUnavailableDesc')}
              </div>
            </div>
            {hasClientId ? (
              <button
                onClick={doRealSignIn}
                disabled={busy}
                className="flex shrink-0 items-center gap-2.5 rounded-full bg-white px-5 py-2.5 text-[13px] font-bold text-[#1f1f1f] shadow-md transition-all hover:shadow-lg hover:brightness-[0.98] active:scale-[0.97] disabled:opacity-60"
              >
                <GoogleG /> {t(lang, 'signInGoogle')}
              </button>
            ) : (
              <button
                disabled
                className="flex shrink-0 cursor-not-allowed items-center gap-2.5 rounded-full border border-line bg-surface px-5 py-2.5 text-[13px] font-bold text-dim opacity-70"
              >
                <GoogleG /> {t(lang, 'signInGoogle')}
              </button>
            )}
          </div>
        ) : (
          <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
            <div className="flex min-w-0 items-center gap-3">
              {user.picture ? (
                // eslint-disable-next-line @next/next/no-img-element
                <img src={user.picture} alt="" className="h-11 w-11 shrink-0 rounded-full object-cover" />
              ) : (
                <span
                  className="grid h-11 w-11 shrink-0 place-items-center rounded-full text-base font-black text-white"
                  style={{ background: 'linear-gradient(135deg, var(--accent), var(--accent-2))' }}
                >
                  {user.name.slice(0, 1).toUpperCase()}
                </span>
              )}
              <div className="min-w-0 flex-1">
                <div className="flex items-center gap-2 text-[13.5px] font-bold">
                  <span className="truncate">{user.name}</span>
                  <span className={`h-1.5 w-1.5 shrink-0 rounded-full ${user.demo ? 'bg-[var(--warning)]' : 'bg-[var(--success)]'}`} />
                  <span className="shrink-0 text-[10.5px] font-semibold text-dim">{user.demo ? t(lang, 'demoTitle') : t(lang, 'connected')}</span>
                </div>
                {user.email && <div className="truncate text-[11px] text-dim">{user.email}</div>}
              </div>
            </div>
            <div className="flex shrink-0 gap-2 max-sm:w-full">
              <button
                onClick={doSync}
                disabled={busy}
                className="flex flex-1 items-center justify-center gap-1.5 rounded-full border border-line bg-surface px-4 py-2 text-[11.5px] font-semibold transition-colors hover:bg-surface2 disabled:opacity-60 sm:flex-none"
              >
                <RefreshCw size={13} className={busy ? 'animate-spin' : ''} /> {t(lang, 'syncPlaylists')}
              </button>
              <button
                onClick={() => { signOut(); toast(lang === 'fa' ? 'از حساب خارج شدی' : 'Signed out'); }}
                className="flex flex-1 items-center justify-center gap-1.5 rounded-full border border-line bg-surface px-4 py-2 text-[11.5px] font-semibold text-[var(--danger)] transition-colors hover:bg-surface2 sm:flex-none"
              >
                <LogOut size={13} /> {t(lang, 'signOut')}
              </button>
            </div>
          </div>
        )}
        {user?.demo && (
          <div className="mt-3 rounded-xl bg-surface2 p-3 text-[11px] leading-relaxed text-dim">
            {t(lang, 'demoDesc')}
          </div>
        )}
      </Section>

      {/* THEMES */}
      <Section icon={Sparkles} title={t(lang, 'settingsThemes')}>
        <div className="stagger grid grid-cols-2 gap-3 sm:grid-cols-4">
          {THEMES.map((th) => (
            <button
              key={th.id}
              onClick={() => s.set({ theme: th.id })}
              className={`group relative overflow-hidden rounded-2xl border p-3 text-start transition-all ${
                s.theme === th.id ? 'border-[var(--accent)] shadow-[var(--glow-soft)]' : 'border-line hover:border-dim'
              }`}
              style={{ background: th.swatch[0] }}
            >
              <div className="mb-2 flex gap-1.5">
                <span className="h-4 w-4 rounded-full" style={{ background: th.swatch[1], boxShadow: `0 0 10px ${th.swatch[1]}` }} />
                <span className="h-4 w-4 rounded-full" style={{ background: th.swatch[2] }} />
                <span className="h-4 w-4 rounded-full opacity-80" style={{ background: th.swatch[3] }} />
              </div>
              <div className="text-[11px] font-bold" style={{ color: th.swatch[0] === '#f7f7f9' ? '#111' : '#fff' }}>
                {th.name[lang]}
              </div>
              {s.theme === th.id && (
                <span className="absolute end-2 top-2 grid h-5 w-5 place-items-center rounded-full" style={{ background: th.swatch[1] }}>
                  <Check size={12} className="text-black" />
                </span>
              )}
            </button>
          ))}
        </div>
        <div className="mt-4">
          <div className="mb-2 text-xs font-bold">{t(lang, 'glowIntensity')}</div>
          <div className="seg" role="radiogroup" aria-label={t(lang, 'glowIntensity')}>
            {(['low', 'mid', 'high'] as GlowLevel[]).map((g) => (
              <button key={g} data-on={s.glow === g} role="radio" aria-checked={s.glow === g} onClick={() => s.set({ glow: g })}>
                {t(lang, g === 'low' ? 'low' : g === 'mid' ? 'mid' : 'high')}
              </button>
            ))}
          </div>
        </div>
      </Section>

      {/* LANGUAGE */}
      <Section icon={Globe} title={t(lang, 'language')}>
        <div className="seg" role="radiogroup" aria-label={t(lang, 'language')}>
          {(['en', 'fa'] as const).map((l) => (
            <button key={l} data-on={s.lang === l} role="radio" aria-checked={s.lang === l} onClick={() => s.set({ lang: l })}>
              {l === 'fa' ? 'فارسی' : 'English'}
            </button>
          ))}
        </div>
      </Section>

      {/* PLAYBACK */}
      <Section icon={Radio} title={t(lang, 'playbackMode')}>
        <div className="seg mb-3 max-w-full flex-wrap" role="radiogroup" aria-label={t(lang, 'playbackMode')}>
          {([
            { id: 'auto', label: t(lang, 'modeAuto') },
            { id: 'official', label: t(lang, 'modeOfficial') },
            { id: 'adfree', label: t(lang, 'modeAdfree') },
          ] as { id: PlaybackMode; label: string }[]).map((m) => (
            <button key={m.id} data-on={s.playbackMode === m.id} role="radio" aria-checked={s.playbackMode === m.id} onClick={() => s.set({ playbackMode: m.id })}>
              {m.label}
            </button>
          ))}
        </div>
        <div className="flex items-start gap-2 rounded-xl bg-surface2 p-3 text-[11px] leading-relaxed text-dim">
          <ShieldOff size={14} className="mt-0.5 shrink-0 text-[var(--accent)]" />
          {lang === 'fa'
            ? 'حالت بی‌تبلیغ از موتور ایزوله استفاده می‌کند؛ اگر یوتیوب مسیر را ببندد، خودکار به پلیر رسمی برمی‌گردد. حالت رسمی همیشه پایدار است (با تبلیغ برای اکانت فری).'
            : 'Ad-free mode uses the isolated engine; if YouTube blocks it, playback auto-falls back to the official player. Official mode is always stable (with ads on free accounts).'}
        </div>
      </Section>

      {/* DOWNLOADS */}
      <Section icon={Download} title={t(lang, 'downloads')}>
        <Row label={t(lang, 'downloadQuality')} desc={`160 / 70 / 50 kbps — Opus`}>
          <div className="seg" role="radiogroup" aria-label={t(lang, 'downloadQuality')}>
            {(['high', 'mid', 'low'] as const).map((q) => (
              <button key={q} data-on={s.downloadQuality === q} role="radio" aria-checked={s.downloadQuality === q} onClick={() => s.set({ downloadQuality: q })}>
                {t(lang, q === 'high' ? 'high' : q === 'mid' ? 'mid' : 'low')}
              </button>
            ))}
          </div>
        </Row>
        <Row label={t(lang, 'askBeforeDownload')}>
          <Switch checked={s.askBeforeBatch} onCheckedChange={(v) => s.set({ askBeforeBatch: v })} />
        </Row>
        <Row label={t(lang, 'playSync')} desc={t(lang, 'playSyncDesc')}>
          <Switch checked={s.playSync} onCheckedChange={(v) => s.set({ playSync: v })} />
        </Row>
        {offlinePlaysCount > 0 && (
          <div className="mt-2 rounded-xl bg-surface2 p-3 text-[11px] text-dim">
            {lang === 'fa' ? 'در صف ثبت:' : 'Pending registration:'} {offlinePlaysCount}
          </div>
        )}
      </Section>

      {/* CUSTOM SERVER (Android advanced) — app is fully standalone by default */}
      {isAndroid && (
        <Section icon={Globe} title={t(lang, 'advServer')}>
          <div className="space-y-4">
            {/* helper server: search/playback fallback when YouTube is blocked on the phone */}
            <div className="space-y-2.5">
              <div className="text-[13px] font-bold">{t(lang, 'fbServer')}</div>
              <div className="text-[11px] leading-relaxed text-dim">{t(lang, 'fbServerDesc')}</div>
              <div className="flex gap-2">
                <div className="min-w-0 flex-1 rounded-2xl border border-line bg-surface2 px-4 py-2.5 transition-all duration-200 focus-within:border-[color-mix(in_srgb,var(--accent)_55%,transparent)]">
                  <input
                    value={fb}
                    onChange={(e) => setFb(e.target.value)}
                    placeholder={t(lang, 'fbServerHint')}
                    dir="ltr"
                    className="w-full bg-transparent text-[13px] outline-none placeholder:text-dim"
                  />
                </div>
                <button
                  onClick={applyFb}
                  className="neon-play rounded-full px-5 text-xs font-bold transition-transform active:scale-95"
                >
                  {t(lang, 'fbApply')}
                </button>
              </div>
              <div className="flex items-center justify-between gap-3">
                <div className="text-[10.5px] leading-relaxed text-dim">{t(lang, 'fbServerNote')}</div>
                <button
                  onClick={clearFb}
                  className="shrink-0 rounded-full border border-line px-4 py-2 text-[11px] font-semibold text-dim transition-colors hover:bg-surface hover:text-foreground"
                >
                  {t(lang, 'fbClear')}
                </button>
              </div>
            </div>

            <div className="border-t border-line" aria-hidden />

            {/* full remote app mode (unchanged) */}
            <div className="space-y-2.5">
              <div className="text-[13px] font-bold">{t(lang, 'advServerMode')}</div>
              <div className="text-[11px] leading-relaxed text-dim">{t(lang, 'advServerDesc')}</div>
              <div className="flex gap-2">
                <div className="min-w-0 flex-1 rounded-2xl border border-line bg-surface2 px-4 py-2.5 transition-all duration-200 focus-within:border-[color-mix(in_srgb,var(--accent)_55%,transparent)]">
                  <input
                    value={srv}
                    onChange={(e) => setSrv(e.target.value)}
                    placeholder={t(lang, 'advServerHint')}
                    dir="ltr"
                    className="w-full bg-transparent text-[13px] outline-none placeholder:text-dim"
                  />
                </div>
                <button
                  onClick={applyServer}
                  className="neon-play rounded-full px-5 text-xs font-bold transition-transform active:scale-95"
                >
                  {t(lang, 'advServerApply')}
                </button>
              </div>
              <div className="flex items-center justify-between gap-3">
                <div className="text-[10.5px] leading-relaxed text-dim">{t(lang, 'advServerWarn')}</div>
                <button
                  onClick={resetServer}
                  className="shrink-0 rounded-full border border-line px-4 py-2 text-[11px] font-semibold text-dim transition-colors hover:bg-surface hover:text-foreground"
                >
                  {t(lang, 'advServerReset')}
                </button>
              </div>
            </div>
          </div>
        </Section>
      )}

      {/* ABOUT */}
      <Section icon={Info} title={t(lang, 'about')}>
        <div className="space-y-1.5 text-xs text-dim">
          <div>JixOne — {lang === 'fa' ? 'پلیر موزیک متن‌باز با کاتالوگ YouTube Music' : 'Open-source player with YouTube Music catalog'}</div>
          <div>{user ? `${t(lang, 'welcomeUser')}, ${user.name}` : t(lang, 'guestBadge')}</div>
          {usage > 0 && <div>{t(lang, 'storageUsed')}: {fmtSize(usage, lang)}</div>}
        </div>
      </Section>
    </div>
  );
}

function Section({ icon: Icon, title, children }: { icon: any; title: string; children: React.ReactNode }) {
  return (
    <div className="mb-6 rounded-2xl border border-line bg-surface p-5">
      <div className="mb-4 flex items-center gap-2 text-sm font-black">
        <Icon size={16} className="text-[var(--accent)]" /> {title}
      </div>
      {children}
    </div>
  );
}

function Row({ label, desc, children }: { label: string; desc?: string; children: React.ReactNode }) {
  return (
    <div className="flex items-center justify-between gap-4 py-2">
      <div className="min-w-0">
        <div className="text-[13px] font-semibold">{label}</div>
        {desc && <div className="mt-0.5 text-[11px] leading-relaxed text-dim">{desc}</div>}
      </div>
      <div className="shrink-0">{children}</div>
    </div>
  );
}
