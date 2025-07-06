'use client';

import React, { useState, useEffect, useCallback } from 'react';
import MusicPlayer from '@/components/MusicPlayer';
import SongList from '@/components/SongList';
import SearchBar from '@/components/SearchBar';
import { useMusicPlayer } from '@/hooks/useMusicPlayer';
import { InternetArchiveAPI } from '@/lib/internetArchiveApi';
import { getStaticSongs, getRandomStaticSong } from '@/lib/staticSongs';
import { Song, PaginationState } from '@/types';

export default function HomePage() {
  const musicPlayer = useMusicPlayer();
  const [allSongs, setAllSongs] = useState<Song[]>([]);
  const [isSearching, setIsSearching] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [hasSearched, setHasSearched] = useState(false);
  
  const [pagination, setPagination] = useState<PaginationState>({
    currentPage: 1,
    totalPages: 1,
    itemsPerPage: 10,
    totalItems: 0
  });

  // Initialize with static songs and random selection
  useEffect(() => {
    const initializeSongs = async () => {
      const staticSongs = getStaticSongs();
      
      try {
        // Load some popular songs from Internet Archive for initial display
        const popularSongs = await InternetArchiveAPI.getPopularSongs(20);
        const combinedSongs = [...staticSongs, ...popularSongs];
        
        setAllSongs(combinedSongs);
        updatePagination(combinedSongs.length);
        
        // Set songs in player and auto-play a random song
        musicPlayer.setSongs(combinedSongs);
        
        // Select a random song to start with
        if (combinedSongs.length > 0) {
          const randomSong = combinedSongs.length > staticSongs.length 
            ? combinedSongs[Math.floor(Math.random() * combinedSongs.length)]
            : getRandomStaticSong();
          
          const randomIndex = combinedSongs.findIndex(song => song.id === randomSong.id);
          if (randomIndex !== -1) {
            musicPlayer.playSong(randomIndex);
          }
        }
      } catch (error) {
        console.error('Error initializing songs:', error);
        // Fallback to just static songs
        setAllSongs(staticSongs);
        updatePagination(staticSongs.length);
        musicPlayer.setSongs(staticSongs);
        
        if (staticSongs.length > 0) {
          const randomSong = getRandomStaticSong();
          const randomIndex = staticSongs.findIndex(song => song.id === randomSong.id);
          if (randomIndex !== -1) {
            musicPlayer.playSong(randomIndex);
          }
        }
      }
    };

    initializeSongs();
  }, []);

  const updatePagination = useCallback((totalItems: number) => {
    const totalPages = Math.ceil(totalItems / pagination.itemsPerPage);
    setPagination(prev => ({
      ...prev,
      totalItems,
      totalPages,
      currentPage: Math.min(prev.currentPage, totalPages || 1)
    }));
  }, [pagination.itemsPerPage]);

  const handleSearch = useCallback(async (query: string) => {
    if (!query.trim()) return;
    
    setIsSearching(true);
    setSearchQuery(query);
    setHasSearched(true);
    
    try {
      const { songs: searchResults } = await InternetArchiveAPI.searchSongs(query, 1, 50);
      const staticSongs = getStaticSongs();
      
      // Filter static songs that match the search query
      const matchingStaticSongs = staticSongs.filter(song => 
        song.title.toLowerCase().includes(query.toLowerCase()) ||
        song.artist.toLowerCase().includes(query.toLowerCase())
      );
      
      const combinedResults = [...matchingStaticSongs, ...searchResults];
      setAllSongs(combinedResults);
      updatePagination(combinedResults.length);
      
      // Update music player with new song list
      musicPlayer.setSongs(combinedResults);
      
      // If we have results, automatically start playing the first one
      if (combinedResults.length > 0) {
        musicPlayer.playSong(0);
      }
    } catch (error) {
      console.error('Search error:', error);
    } finally {
      setIsSearching(false);
    }
  }, [musicPlayer, updatePagination]);

  const handleSearchClear = useCallback(async () => {
    setSearchQuery('');
    setHasSearched(false);
    setIsSearching(true);
    
    try {
      // Reset to initial state with static songs and popular songs
      const staticSongs = getStaticSongs();
      const popularSongs = await InternetArchiveAPI.getPopularSongs(20);
      const combinedSongs = [...staticSongs, ...popularSongs];
      
      setAllSongs(combinedSongs);
      updatePagination(combinedSongs.length);
      musicPlayer.setSongs(combinedSongs);
      
      setPagination(prev => ({ ...prev, currentPage: 1 }));
    } catch (error) {
      console.error('Error resetting songs:', error);
      const staticSongs = getStaticSongs();
      setAllSongs(staticSongs);
      updatePagination(staticSongs.length);
      musicPlayer.setSongs(staticSongs);
    } finally {
      setIsSearching(false);
    }
  }, [musicPlayer, updatePagination]);

  const handleSongSelect = useCallback((index: number) => {
    musicPlayer.playSong(index);
  }, [musicPlayer]);

  const handlePageChange = useCallback((page: number) => {
    setPagination(prev => ({ ...prev, currentPage: page }));
  }, []);

  return (
    <div className="min-h-screen bg-gray-900 p-4">
      <div className="max-w-6xl mx-auto">
        {/* Header */}
        <header className="mb-8">
          <h1 className="text-4xl font-bold text-white mb-2">Enhanced Music Player</h1>
          <p className="text-gray-400">
            Discover and play music from your local collection and the Internet Archive
          </p>
        </header>

        {/* Search */}
        <div className="mb-6">
          <SearchBar
            onSearch={handleSearch}
            onClear={handleSearchClear}
            isLoading={isSearching}
            placeholder="Search for songs from Internet Archive..."
            className="max-w-2xl"
          />
          {hasSearched && searchQuery && (
            <p className="text-sm text-gray-400 mt-2">
              Showing results for: "{searchQuery}"
            </p>
          )}
        </div>

        {/* Music Player and Song List */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          {/* Music Player */}
          <div>
            <MusicPlayer className="sticky top-4" />
          </div>

          {/* Song List */}
          <div>
            <SongList
              songs={allSongs}
              currentSong={musicPlayer.currentSong}
              onSongSelect={handleSongSelect}
              onPageChange={handlePageChange}
              currentPage={pagination.currentPage}
              totalPages={pagination.totalPages}
              itemsPerPage={pagination.itemsPerPage}
              isLoading={isSearching}
            />
          </div>
        </div>

        {/* Footer */}
        <footer className="mt-12 text-center text-gray-500 text-sm">
          <p>
            Music powered by{' '}
            <a 
              href="https://archive.org" 
              target="_blank" 
              rel="noopener noreferrer"
              className="text-blue-400 hover:text-blue-300 transition-colors"
            >
              Internet Archive
            </a>
          </p>
        </footer>
      </div>
    </div>
  );
}