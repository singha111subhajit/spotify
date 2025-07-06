'use client';

import React from 'react';
import { Play, Pause, SkipBack, SkipForward, Volume2, Shuffle, Repeat } from 'lucide-react';
import { useMusicPlayer } from '@/hooks/useMusicPlayer';

interface MusicPlayerProps {
  className?: string;
}

export default function MusicPlayer({ className = '' }: MusicPlayerProps) {
  const {
    playlist,
    currentSong,
    isLoading,
    togglePlayPause,
    playNext,
    playPrevious,
    setVolume,
    seekTo,
    toggleShuffle,
    toggleRepeat,
  } = useMusicPlayer();

  const formatTime = (seconds: number) => {
    const mins = Math.floor(seconds / 60);
    const secs = Math.floor(seconds % 60);
    return `${mins}:${secs.toString().padStart(2, '0')}`;
  };

  const handleProgressChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const newTime = (parseFloat(e.target.value) / 100) * playlist.duration;
    seekTo(newTime);
  };

  const handleVolumeChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setVolume(parseFloat(e.target.value) / 100);
  };

  const progressPercentage = playlist.duration > 0 
    ? (playlist.currentTime / playlist.duration) * 100 
    : 0;

  return (
    <div className={`bg-gray-900 text-white p-4 rounded-lg shadow-lg ${className}`}>
      {/* Now Playing Info */}
      <div className="flex items-center mb-4">
        {currentSong?.thumbnail && (
          <img 
            src={currentSong.thumbnail} 
            alt={currentSong.title}
            className="w-16 h-16 rounded-lg mr-4 object-cover"
            onError={(e) => {
              e.currentTarget.style.display = 'none';
            }}
          />
        )}
        <div className="flex-1 min-w-0">
          <h3 className="font-semibold text-lg truncate">
            {currentSong?.title || 'No song selected'}
          </h3>
          <p className="text-gray-400 truncate">
            {currentSong?.artist || 'Unknown Artist'}
          </p>
          <p className="text-xs text-gray-500">
            {currentSong?.source === 'api' ? 'Internet Archive' : 'Local File'}
          </p>
        </div>
      </div>

      {/* Progress Bar */}
      <div className="mb-4">
        <div className="flex items-center justify-between text-sm text-gray-400 mb-1">
          <span>{formatTime(playlist.currentTime)}</span>
          <span>{formatTime(playlist.duration)}</span>
        </div>
        <input
          type="range"
          min="0"
          max="100"
          value={progressPercentage}
          onChange={handleProgressChange}
          className="w-full h-2 bg-gray-700 rounded-lg appearance-none cursor-pointer slider"
          disabled={!currentSong}
        />
      </div>

      {/* Controls */}
      <div className="flex items-center justify-center space-x-4 mb-4">
        <button
          onClick={toggleShuffle}
          className={`p-2 rounded-full transition-colors ${
            playlist.isShuffled 
              ? 'bg-green-600 text-white' 
              : 'bg-gray-700 hover:bg-gray-600 text-gray-300'
          }`}
          title="Shuffle"
        >
          <Shuffle size={20} />
        </button>

        <button
          onClick={playPrevious}
          className="p-2 bg-gray-700 hover:bg-gray-600 rounded-full transition-colors"
          disabled={!currentSong}
          title="Previous"
        >
          <SkipBack size={24} />
        </button>

        <button
          onClick={togglePlayPause}
          className="p-3 bg-green-600 hover:bg-green-500 rounded-full transition-colors disabled:opacity-50"
          disabled={!currentSong || isLoading}
          title={playlist.isPlaying ? 'Pause' : 'Play'}
        >
          {isLoading ? (
            <div className="w-6 h-6 border-2 border-white border-t-transparent rounded-full animate-spin" />
          ) : playlist.isPlaying ? (
            <Pause size={24} />
          ) : (
            <Play size={24} />
          )}
        </button>

        <button
          onClick={playNext}
          className="p-2 bg-gray-700 hover:bg-gray-600 rounded-full transition-colors"
          disabled={!currentSong}
          title="Next"
        >
          <SkipForward size={24} />
        </button>

        <button
          onClick={toggleRepeat}
          className={`p-2 rounded-full transition-colors ${
            playlist.isRepeating 
              ? 'bg-green-600 text-white' 
              : 'bg-gray-700 hover:bg-gray-600 text-gray-300'
          }`}
          title="Repeat"
        >
          <Repeat size={20} />
        </button>
      </div>

      {/* Volume Control */}
      <div className="flex items-center space-x-2">
        <Volume2 size={20} className="text-gray-400" />
        <input
          type="range"
          min="0"
          max="100"
          value={playlist.volume * 100}
          onChange={handleVolumeChange}
          className="flex-1 h-2 bg-gray-700 rounded-lg appearance-none cursor-pointer slider"
        />
        <span className="text-sm text-gray-400 w-8">
          {Math.round(playlist.volume * 100)}
        </span>
      </div>

      <style jsx>{`
        .slider::-webkit-slider-thumb {
          appearance: none;
          height: 16px;
          width: 16px;
          border-radius: 50%;
          background: #10b981;
          cursor: pointer;
          border: 2px solid #065f46;
        }

        .slider::-moz-range-thumb {
          height: 16px;
          width: 16px;
          border-radius: 50%;
          background: #10b981;
          cursor: pointer;
          border: 2px solid #065f46;
        }
      `}</style>
    </div>
  );
}