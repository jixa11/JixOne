'use client';
import { useEffect } from 'react';
import Sidebar, { MobileNav, TopBar } from '@/components/Sidebar';
import PlayerBar from '@/components/PlayerBar';
import QueuePanel from '@/components/QueuePanel';
import PlayerHost from '@/components/PlayerHost';
import ThemeApplier from '@/components/ThemeApplier';
import HomeView from '@/components/views/HomeView';
import SearchView from '@/components/views/SearchView';
import LibraryView from '@/components/views/LibraryView';
import PlaylistView from '@/components/views/PlaylistView';
import SettingsView from '@/components/views/SettingsView';
import DownloadsView from '@/components/views/DownloadsView';
import { LikedView, HistoryView } from '@/components/views/CollectionViews';
import { useView } from '@/store/view';
import { startDownloadRunner } from '@/engine/downloadsRunner';
import { usePlayer } from '@/store/player';
import { useLibrary } from '@/store/library';
import { useDownloads } from '@/store/downloads';
import { useSettings } from '@/store/settings';

export default function AppShell() {
  const view = useView((s) => s.current);

  useEffect(() => {
    startDownloadRunner();
    // E2E debug handle (?e2e=1)
    if (typeof window !== 'undefined' && window.location.search.includes('e2e=1')) {
      (window as any).__np = { player: usePlayer, library: useLibrary, downloads: useDownloads, settings: useSettings };
    }
  }, []);

  return (
    <div className="h-screen w-full overflow-hidden">
      <ThemeApplier />
      <PlayerHost />

      <div className="flex h-full">
        <Sidebar />
        <main className="h-full flex-1 overflow-y-auto pb-[150px] md:pb-[90px] md:ps-[248px]">
          <TopBar />
          <div key={`${view.name}-${view.id ?? ''}`}>
            {view.name === 'home' && <HomeView />}
            {view.name === 'search' && <SearchView />}
            {view.name === 'library' && <LibraryView />}
            {view.name === 'playlist' && <PlaylistView id={view.id!} />}
            {view.name === 'liked' && <LikedView />}
            {view.name === 'history' && <HistoryView />}
            {view.name === 'downloads' && <DownloadsView />}
            {view.name === 'settings' && <SettingsView />}
          </div>
        </main>
      </div>

      <QueuePanel />
      <MobileNav />
      <PlayerBar />
    </div>
  );
}
