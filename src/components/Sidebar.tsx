'use client';
import { Search, Compass, Library, Settings as Cog, Plus, Heart, Clock3, X, ArrowRight, ArrowLeft, ListMusic, Check, Youtube } from 'lucide-react';
import { useView } from '@/store/view';
import { useLibrary } from '@/store/library';
import { useSettings } from '@/store/settings';
import { useAuth } from '@/store/auth';
import { t } from '@/lib/i18n';
import { cn } from '@/lib/utils';
import { toast } from 'sonner';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '@/components/ui/dialog';
import { TrackThumb } from '@/components/TrackThumb';
import { useState } from 'react';

function NavItem({ icon: Icon, label, active, onClick }: { icon: any; label: string; active: boolean; onClick: () => void }) {
  return (
    <button
      onClick={onClick}
      className={cn(
        'group relative flex w-full items-center gap-3.5 rounded-xl px-3.5 py-2.5 text-[13.5px] transition-all duration-150',
        active
          ? 'bg-surface2 font-bold text-[var(--accent)]'
          : 'text-dim hover:bg-surface hover:text-foreground'
      )}
    >
      {/* active indicator bar */}
      <span
        aria-hidden
        className={cn(
          'absolute inset-y-[9px] start-0 w-[3px] rounded-full bg-[var(--accent)] transition-all duration-200',
          active ? 'opacity-100 shadow-[var(--glow-soft)]' : 'opacity-0'
        )}
      />
      <Icon size={18} strokeWidth={active ? 2.4 : 2} className="shrink-0 transition-transform duration-150 group-hover:scale-105" />
      <span className="truncate">{label}</span>
    </button>
  );
}

export default function Sidebar() {
  const view = useView((s) => s.current);
  const push = useView((s) => s.push);
  const lang = useSettings((s) => s.lang);
  const playlists = useLibrary((s) => s.playlists);
  const history = useLibrary((s) => s.history);
  const create = useLibrary((s) => s.createPlaylist);
  const user = useAuth((s) => s.user);
  const [openNew, setOpenNew] = useState(false);
  const [name, setName] = useState('');

  return (
    <aside className="fixed inset-y-0 start-0 z-30 hidden w-[248px] flex-col border-e border-line bg-[var(--app-bg-2)]/70 p-3 backdrop-blur-xl md:flex">
      {/* brand */}
      <div className="mb-4 flex items-center gap-3 px-1.5 pb-1 pt-2.5">
        <div className="neon-play grid h-10 w-10 shrink-0 place-items-center rounded-2xl shadow-[var(--glow)]">
          <svg width="19" height="19" viewBox="0 0 24 24" fill="currentColor"><path d="M12 3v10.55A4 4 0 1 0 14 17V7h4V3h-6z" /></svg>
        </div>
        <div className="min-w-0">
          <div className="text-[17px] font-black leading-tight tracking-tight">{t(lang, 'appName')}</div>
          <div className="truncate text-[10.5px] leading-snug text-dim">
            {user ? user.name : `${t(lang, 'guest')} · ${t(lang, 'signInYTM')}`}
          </div>
        </div>
      </div>

      {/* primary nav — Search → Explore → Library (downloads merged inside); no Home */}
      <nav className="space-y-0.5">
        <NavItem icon={Search} label={t(lang, 'search')} active={view.name === 'search'} onClick={() => push('search')} />
        <NavItem icon={Compass} label={t(lang, 'explore')} active={view.name === 'explore'} onClick={() => push('explore')} />
        <NavItem icon={Library} label={t(lang, 'library')} active={view.name === 'library' || view.name === 'liked' || view.name === 'history'} onClick={() => push('library')} />
        <NavItem icon={Cog} label={t(lang, 'settings')} active={view.name === 'settings'} onClick={() => push('settings')} />
      </nav>

      {/* section header */}
      <div className="mb-1 mt-5 flex items-center justify-between px-3.5">
        <span className="text-[10.5px] font-bold uppercase tracking-[0.14em] text-dim">{t(lang, 'playlists')}</span>
        <button
          onClick={() => setOpenNew(true)}
          className="grid h-8 w-8 place-items-center rounded-full bg-surface2 text-dim transition-all hover:scale-105 hover:bg-surface-hover hover:text-foreground"
          aria-label={t(lang, 'newPlaylist')}
        >
          <Plus size={15} />
        </button>
      </div>

      <div className="-mx-1 flex-1 overflow-y-auto px-1 pb-1">
        {/* liked */}
        <button
          onClick={() => push('liked')}
          className={cn(
            'group mb-0.5 flex w-full items-center gap-3 rounded-xl p-2 text-start transition-colors hover:bg-surface',
            view.name === 'liked' && 'bg-surface2'
          )}
        >
          <span className="neon-play grid h-10 w-10 shrink-0 place-items-center rounded-xl shadow-[var(--glow-soft)]">
            <Heart size={16} fill="currentColor" />
          </span>
          <span className="min-w-0 flex-1">
            <span className="block text-[13px] font-semibold leading-snug">{t(lang, 'likedSongs')}</span>
            <span className="block text-[10.5px] text-dim">{t(lang, 'playlist')} · {t(lang, 'you')}</span>
          </span>
        </button>

        {/* history */}
        {history.length > 0 && (
          <button
            onClick={() => push('history')}
            className={cn(
              'group mb-0.5 flex w-full items-center gap-3 rounded-xl p-2 text-start transition-colors hover:bg-surface',
              view.name === 'history' && 'bg-surface2'
            )}
          >
            <span className="grid h-10 w-10 shrink-0 place-items-center rounded-xl bg-surface2 text-dim transition-colors group-hover:text-foreground">
              <Clock3 size={16} />
            </span>
            <span className="min-w-0 flex-1 truncate text-[13px] leading-snug">{t(lang, 'recentPlayed')}</span>
          </button>
        )}

        {/* user playlists (local first, imported ones get a YT badge) */}
        {playlists.map((p) => (
          <button
            key={p.id}
            onClick={() => push('playlist', p.id)}
            className={cn(
              'flex w-full items-center gap-3 rounded-xl p-2 text-start transition-colors hover:bg-surface',
              view.name === 'playlist' && view.id === p.id && 'bg-surface2'
            )}
            title={p.source === 'ytmusic' ? t(lang, 'fromYTMusic') : undefined}
          >
            <span className="relative shrink-0">
              {p.tracks[0] ? (
                <TrackThumb track={p.tracks[0]} rounded="rounded-xl" className="h-10 w-10 shadow-md" />
              ) : (
                <span className="grid h-10 w-10 place-items-center rounded-xl bg-surface2 text-dim">
                  <ListMusic size={15} />
                </span>
              )}
              {p.source === 'ytmusic' && (
                <span className="absolute -bottom-1 -end-1 grid h-4 w-4 place-items-center rounded-full bg-[#ff0033] text-white shadow ring-2 ring-[var(--app-bg-2)]">
                  <Youtube size={9} strokeWidth={2.5} />
                </span>
              )}
            </span>
            <span className="min-w-0 flex-1">
              <span className="block truncate text-[13px] leading-snug">{p.name}</span>
              <span className="block truncate text-[10.5px] text-dim">
                {p.source === 'ytmusic' ? `${t(lang, 'fromYTMusic')} · ` : ''}{p.tracks.length} {t(lang, 'songs')}
              </span>
            </span>
          </button>
        ))}
      </div>

      {/* account chip */}
      <button
        onClick={() => push('settings')}
        className="mt-2 flex items-center gap-2.5 rounded-xl border border-line bg-surface p-2.5 text-start transition-colors hover:bg-surface2"
      >
        {user ? (
          <>
            <span
              className="grid h-8 w-8 shrink-0 place-items-center rounded-full text-[12px] font-black text-white"
              style={{ background: 'linear-gradient(135deg, var(--accent), var(--accent-2))' }}
            >
              {user.name.slice(0, 1).toUpperCase()}
            </span>
            <span className="min-w-0 flex-1">
              <span className="block truncate text-[12px] font-bold leading-tight">{user.name}</span>
              <span className="block truncate text-[10px] text-dim">{t(lang, 'connected')}</span>
            </span>
          </>
        ) : (
          <>
            <span className="grid h-8 w-8 shrink-0 place-items-center rounded-full bg-surface2 text-dim">
              <Plus size={14} />
            </span>
            <span className="min-w-0 flex-1 truncate text-[12px] font-semibold text-dim">{t(lang, 'signInYTM')}</span>
          </>
        )}
      </button>

      <Dialog open={openNew} onOpenChange={setOpenNew}>
        <DialogContent className="border-line bg-[var(--app-bg-2)] sm:max-w-sm">
          <DialogHeader>
            <DialogTitle>{t(lang, 'createPlaylist')}</DialogTitle>
          </DialogHeader>
          <input
            autoFocus
            value={name}
            onChange={(e) => setName(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === 'Enter' && name.trim()) {
                const id = create(name.trim());
                setName(''); setOpenNew(false); push('playlist', id);
              }
            }}
            placeholder={t(lang, 'playlistName')}
            className="w-full rounded-xl border border-line bg-surface px-4 py-3 text-sm outline-none transition-colors focus:border-[var(--accent)]"
          />
          <DialogFooter>
            <button
              className="grid h-10 w-10 place-items-center rounded-lg text-dim transition-colors hover:bg-surface hover:text-foreground"
              onClick={() => setOpenNew(false)}
              aria-label={t(lang, 'cancel')}
            >
              <X size={17} />
            </button>
            <button
              className="neon-play grid h-10 w-16 place-items-center rounded-lg transition-transform active:scale-95"
              onClick={() => {
                if (!name.trim()) { toast(t(lang, 'needName')); return; }
                const id = create(name.trim());
                setName(''); setOpenNew(false); push('playlist', id);
                toast(t(lang, 'playlistCreated'));
              }}
              aria-label={t(lang, 'createPlaylist')}
            >
              <Check size={18} />
            </button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </aside>
  );
}

/** mobile bottom nav — fixed LTR order so the layout is IDENTICAL in fa/en.
 *  Per user request: Search first, then Explore, then Library (downloads are
 *  merged into Library); no Home page; Settings moved to the top-corner gear. */
export function MobileNav() {
  const view = useView((s) => s.current);
  const push = useView((s) => s.push);
  const lang = useSettings((s) => s.lang);
  const items = [
    { icon: Search, label: t(lang, 'search'), name: 'search' as const },
    { icon: Compass, label: t(lang, 'explore'), name: 'explore' as const },
    { icon: Library, label: t(lang, 'library'), name: 'library' as const },
  ];
  return (
    <nav dir="ltr" className="glass fixed bottom-[76px] z-20 w-full border-t border-line md:hidden" aria-label="mobile nav">
      <div className="flex items-center justify-around py-1.5">
        {items.map((it) => {
          const active = view.name === it.name || (it.name === 'library' && (view.name === 'liked' || view.name === 'history'));
          return (
            <button
              key={it.name}
              onClick={() => push(it.name)}
              className={cn(
                'relative flex min-w-[88px] flex-col items-center gap-1 rounded-xl py-1.5 text-[10.5px] transition-colors',
                active ? 'font-bold text-[var(--accent)]' : 'text-dim'
              )}
            >
              {active && <span className="absolute -top-1 h-[3px] w-6 rounded-full bg-[var(--accent)] shadow-[var(--glow-soft)]" />}
              <it.icon size={21} strokeWidth={active ? 2.4 : 2} />
              {it.label}
            </button>
          );
        })}
      </div>
    </nav>
  );
}

export function TopBar() {
  const back = useView((s) => s.back);
  const canBack = useView((s) => s.canBack());
  const lang = useSettings((s) => s.lang);
  const push = useView((s) => s.push);
  const view = useView((s) => s.current);
  return (
    <header className="sticky top-0 z-20 flex items-center gap-2 bg-gradient-to-b from-[var(--app-bg)] via-[var(--app-bg)]/70 to-transparent px-4 py-3">
      <button
        onClick={back}
        disabled={!canBack}
        className={cn(
          'grid h-9 w-9 place-items-center rounded-full bg-surface2 transition-all hover:bg-surface-hover',
          canBack ? 'opacity-100 hover:scale-105 active:scale-95' : 'pointer-events-none opacity-0'
        )}
        aria-label="back"
      >
        {lang === 'fa' ? <ArrowRight size={17} /> : <ArrowLeft size={17} />}
      </button>
      <div className="flex-1" />
      <button
        onClick={() => push('search')}
        className="flex items-center gap-2 rounded-full bg-surface px-4 py-2 text-xs text-dim transition-colors hover:bg-surface2 hover:text-foreground sm:hidden"
      >
        <Search size={14} /> {t(lang, 'searchPlaceholder')}
      </button>
      {/* settings gear — top corner on mobile (moved out of the bottom nav) */}
      <button
        onClick={() => push('settings')}
        className={cn(
          'grid h-9 w-9 shrink-0 place-items-center rounded-full transition-all hover:scale-105 active:scale-95 md:hidden',
          view.name === 'settings' ? 'bg-surface2 text-[var(--accent)]' : 'bg-surface2 text-dim hover:text-foreground'
        )}
        aria-label={t(lang, 'settings')}
      >
        <Cog size={18} />
      </button>
    </header>
  );
}
