import { useState, useEffect, useRef, useCallback } from 'react';
import { Song, PlaylistState } from '@/types';

export function useMusicPlayer() {
  const audioRef = useRef<HTMLAudioElement | null>(null);
  const [playlist, setPlaylist] = useState<PlaylistState>({
    songs: [],
    currentIndex: 0,
    isPlaying: false,
    volume: 1,
    currentTime: 0,
    duration: 0,
    isShuffled: false,
    isRepeating: false,
  });

  const [isLoading, setIsLoading] = useState(false);

  // Initialize audio element
  useEffect(() => {
    if (!audioRef.current) {
      audioRef.current = new Audio();
      audioRef.current.preload = 'metadata';
    }

    const audio = audioRef.current;

    const handleTimeUpdate = () => {
      setPlaylist(prev => ({
        ...prev,
        currentTime: audio.currentTime,
      }));
    };

    const handleDurationChange = () => {
      setPlaylist(prev => ({
        ...prev,
        duration: audio.duration || 0,
      }));
    };

    const handleEnded = () => {
      if (playlist.isRepeating) {
        audio.currentTime = 0;
        audio.play();
      } else {
        playNext();
      }
    };

    const handleCanPlay = () => {
      setIsLoading(false);
    };

    const handleLoadStart = () => {
      setIsLoading(true);
    };

    const handleError = () => {
      setIsLoading(false);
      console.error('Audio loading error');
      playNext(); // Skip to next song on error
    };

    audio.addEventListener('timeupdate', handleTimeUpdate);
    audio.addEventListener('durationchange', handleDurationChange);
    audio.addEventListener('ended', handleEnded);
    audio.addEventListener('canplay', handleCanPlay);
    audio.addEventListener('loadstart', handleLoadStart);
    audio.addEventListener('error', handleError);

    return () => {
      audio.removeEventListener('timeupdate', handleTimeUpdate);
      audio.removeEventListener('durationchange', handleDurationChange);
      audio.removeEventListener('ended', handleEnded);
      audio.removeEventListener('canplay', handleCanPlay);
      audio.removeEventListener('loadstart', handleLoadStart);
      audio.removeEventListener('error', handleError);
    };
  }, [playlist.isRepeating]);

  // Load current song when playlist or current index changes
  useEffect(() => {
    if (playlist.songs.length > 0 && audioRef.current) {
      const currentSong = playlist.songs[playlist.currentIndex];
      if (currentSong) {
        audioRef.current.src = currentSong.url;
        audioRef.current.volume = playlist.volume;
      }
    }
  }, [playlist.songs, playlist.currentIndex, playlist.volume]);

  const play = useCallback(() => {
    if (audioRef.current && playlist.songs.length > 0) {
      audioRef.current.play()
        .then(() => {
          setPlaylist(prev => ({ ...prev, isPlaying: true }));
        })
        .catch((error) => {
          console.error('Error playing audio:', error);
          setIsLoading(false);
        });
    }
  }, [playlist.songs.length]);

  const pause = useCallback(() => {
    if (audioRef.current) {
      audioRef.current.pause();
      setPlaylist(prev => ({ ...prev, isPlaying: false }));
    }
  }, []);

  const togglePlayPause = useCallback(() => {
    if (playlist.isPlaying) {
      pause();
    } else {
      play();
    }
  }, [playlist.isPlaying, play, pause]);

  const playNext = useCallback(() => {
    if (playlist.songs.length > 0) {
      let nextIndex;
      if (playlist.isShuffled) {
        nextIndex = Math.floor(Math.random() * playlist.songs.length);
      } else {
        nextIndex = (playlist.currentIndex + 1) % playlist.songs.length;
      }
      
      setPlaylist(prev => ({
        ...prev,
        currentIndex: nextIndex,
        currentTime: 0,
      }));
    }
  }, [playlist.songs.length, playlist.currentIndex, playlist.isShuffled]);

  const playPrevious = useCallback(() => {
    if (playlist.songs.length > 0) {
      let prevIndex;
      if (playlist.isShuffled) {
        prevIndex = Math.floor(Math.random() * playlist.songs.length);
      } else {
        prevIndex = playlist.currentIndex - 1 < 0 
          ? playlist.songs.length - 1 
          : playlist.currentIndex - 1;
      }
      
      setPlaylist(prev => ({
        ...prev,
        currentIndex: prevIndex,
        currentTime: 0,
      }));
    }
  }, [playlist.songs.length, playlist.currentIndex, playlist.isShuffled]);

  const playSong = useCallback((index: number) => {
    if (index >= 0 && index < playlist.songs.length) {
      setPlaylist(prev => ({
        ...prev,
        currentIndex: index,
        currentTime: 0,
      }));
    }
  }, [playlist.songs.length]);

  const setVolume = useCallback((volume: number) => {
    const clampedVolume = Math.max(0, Math.min(1, volume));
    setPlaylist(prev => ({ ...prev, volume: clampedVolume }));
    if (audioRef.current) {
      audioRef.current.volume = clampedVolume;
    }
  }, []);

  const seekTo = useCallback((time: number) => {
    if (audioRef.current) {
      audioRef.current.currentTime = time;
      setPlaylist(prev => ({ ...prev, currentTime: time }));
    }
  }, []);

  const toggleShuffle = useCallback(() => {
    setPlaylist(prev => ({ ...prev, isShuffled: !prev.isShuffled }));
  }, []);

  const toggleRepeat = useCallback(() => {
    setPlaylist(prev => ({ ...prev, isRepeating: !prev.isRepeating }));
  }, []);

  const setSongs = useCallback((songs: Song[], autoPlay = false) => {
    setPlaylist(prev => ({
      ...prev,
      songs,
      currentIndex: 0,
      currentTime: 0,
    }));

    if (autoPlay && songs.length > 0) {
      setTimeout(() => play(), 100);
    }
  }, [play]);

  const addSongs = useCallback((songs: Song[]) => {
    setPlaylist(prev => ({
      ...prev,
      songs: [...prev.songs, ...songs],
    }));
  }, []);

  const getCurrentSong = useCallback(() => {
    return playlist.songs[playlist.currentIndex] || null;
  }, [playlist.songs, playlist.currentIndex]);

  return {
    playlist,
    isLoading,
    currentSong: getCurrentSong(),
    play,
    pause,
    togglePlayPause,
    playNext,
    playPrevious,
    playSong,
    setVolume,
    seekTo,
    toggleShuffle,
    toggleRepeat,
    setSongs,
    addSongs,
  };
}