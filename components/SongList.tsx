'use client';

import React from 'react';
import { Play, Music, Globe } from 'lucide-react';
import { Song } from '@/types';

interface SongListProps {
  songs: Song[];
  currentSong: Song | null;
  onSongSelect: (index: number) => void;
  onPageChange: (page: number) => void;
  currentPage: number;
  totalPages: number;
  itemsPerPage: number;
  isLoading?: boolean;
}

export default function SongList({
  songs,
  currentSong,
  onSongSelect,
  onPageChange,
  currentPage,
  totalPages,
  itemsPerPage,
  isLoading = false
}: SongListProps) {
  const startIndex = (currentPage - 1) * itemsPerPage;
  const endIndex = startIndex + itemsPerPage;
  const paginatedSongs = songs.slice(startIndex, endIndex);

  const formatDuration = (seconds?: number) => {
    if (!seconds) return '';
    const mins = Math.floor(seconds / 60);
    const secs = Math.floor(seconds % 60);
    return `${mins}:${secs.toString().padStart(2, '0')}`;
  };

  const renderPagination = () => {
    const pages = [];
    const maxVisiblePages = 5;
    let startPage = Math.max(1, currentPage - Math.floor(maxVisiblePages / 2));
    let endPage = Math.min(totalPages, startPage + maxVisiblePages - 1);

    if (endPage - startPage < maxVisiblePages - 1) {
      startPage = Math.max(1, endPage - maxVisiblePages + 1);
    }

    // Previous button
    if (currentPage > 1) {
      pages.push(
        <button
          key="prev"
          onClick={() => onPageChange(currentPage - 1)}
          className="px-3 py-1 bg-gray-700 hover:bg-gray-600 rounded transition-colors"
        >
          ←
        </button>
      );
    }

    // Page numbers
    for (let i = startPage; i <= endPage; i++) {
      pages.push(
        <button
          key={i}
          onClick={() => onPageChange(i)}
          className={`px-3 py-1 rounded transition-colors ${
            i === currentPage
              ? 'bg-green-600 text-white'
              : 'bg-gray-700 hover:bg-gray-600'
          }`}
        >
          {i}
        </button>
      );
    }

    // Next button
    if (currentPage < totalPages) {
      pages.push(
        <button
          key="next"
          onClick={() => onPageChange(currentPage + 1)}
          className="px-3 py-1 bg-gray-700 hover:bg-gray-600 rounded transition-colors"
        >
          →
        </button>
      );
    }

    return pages;
  };

  if (isLoading) {
    return (
      <div className="bg-gray-800 rounded-lg p-6">
        <div className="flex items-center justify-center">
          <div className="w-8 h-8 border-4 border-green-600 border-t-transparent rounded-full animate-spin" />
          <span className="ml-3 text-gray-300">Loading songs...</span>
        </div>
      </div>
    );
  }

  if (songs.length === 0) {
    return (
      <div className="bg-gray-800 rounded-lg p-6 text-center">
        <Music size={48} className="mx-auto text-gray-500 mb-4" />
        <p className="text-gray-400">No songs found</p>
      </div>
    );
  }

  return (
    <div className="bg-gray-800 rounded-lg overflow-hidden">
      {/* Header */}
      <div className="bg-gray-700 px-4 py-3 border-b border-gray-600">
        <h2 className="text-lg font-semibold text-white">
          Songs ({songs.length} total)
        </h2>
        <p className="text-sm text-gray-400">
          Page {currentPage} of {totalPages}
        </p>
      </div>

      {/* Song List */}
      <div className="max-h-96 overflow-y-auto">
        {paginatedSongs.map((song, index) => {
          const actualIndex = startIndex + index;
          const isCurrentSong = currentSong?.id === song.id;
          
          return (
            <div
              key={song.id}
              className={`flex items-center p-3 border-b border-gray-700 hover:bg-gray-700 cursor-pointer transition-colors ${
                isCurrentSong ? 'bg-green-900 border-green-600' : ''
              }`}
              onClick={() => onSongSelect(actualIndex)}
            >
              <button
                className={`mr-3 p-2 rounded-full transition-colors ${
                  isCurrentSong 
                    ? 'bg-green-600 text-white' 
                    : 'bg-gray-600 hover:bg-gray-500 text-gray-200'
                }`}
              >
                <Play size={16} />
              </button>

              {song.thumbnail && (
                <img
                  src={song.thumbnail}
                  alt={song.title}
                  className="w-12 h-12 rounded-lg mr-3 object-cover"
                  onError={(e) => {
                    e.currentTarget.style.display = 'none';
                  }}
                />
              )}

              <div className="flex-1 min-w-0">
                <div className="flex items-center">
                  <h3 className={`font-medium truncate ${
                    isCurrentSong ? 'text-green-400' : 'text-white'
                  }`}>
                    {song.title}
                  </h3>
                  <div className="ml-2 flex items-center">
                    {song.source === 'api' ? (
                      <Globe size={14} className="text-blue-400" title="Internet Archive" />
                    ) : (
                      <Music size={14} className="text-purple-400" title="Local File" />
                    )}
                  </div>
                </div>
                <p className="text-gray-400 text-sm truncate">
                  {song.artist}
                </p>
                {song.album && (
                  <p className="text-gray-500 text-xs truncate">
                    {song.album}
                  </p>
                )}
              </div>

              <div className="text-right">
                {song.duration && (
                  <p className="text-gray-400 text-sm">
                    {formatDuration(song.duration)}
                  </p>
                )}
                {song.year && (
                  <p className="text-gray-500 text-xs">
                    {song.year}
                  </p>
                )}
              </div>
            </div>
          );
        })}
      </div>

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="bg-gray-700 px-4 py-3 border-t border-gray-600">
          <div className="flex items-center justify-center space-x-2">
            {renderPagination()}
          </div>
        </div>
      )}
    </div>
  );
}