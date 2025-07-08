import React, { useState, useEffect, useRef, useCallback } from 'react';
import axios from 'axios';
import './App.css';

function useDarkMode() {
  const [darkMode, setDarkMode] = useState(() => {
    const saved = localStorage.getItem('darkMode');
    return saved ? JSON.parse(saved) : true;
  });
  useEffect(() => {
    localStorage.setItem('darkMode', JSON.stringify(darkMode));
    document.body.classList.remove('dark', 'light');
    if (darkMode) {
      document.body.classList.add('dark');
    } else {
      document.body.classList.add('light');
    }
    console.log('Body class:', document.body.className, 'DarkMode:', darkMode);
  }, [darkMode]);
  return [darkMode, setDarkMode];
}

function App() {
  const [darkMode, setDarkMode] = useDarkMode();
  // Core state
  const [songs, setSongs] = useState([]);
  const [currentSong, setCurrentSong] = useState(null);
  const [isPlaying, setIsPlaying] = useState(false);
  const [currentIndex, setCurrentIndex] = useState(0);
  // Infinite scroll state
  const [page, setPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const [hasMore, setHasMore] = useState(true);
  const [mode, setMode] = useState('local'); // 'local' or 'online'
  const [onlinePage, setOnlinePage] = useState(1); // for JioSaavn
  const [onlineHasMore, setOnlineHasMore] = useState(true);
  const [onlineQuery, setOnlineQuery] = useState('bollywood');
  const songListRef = useRef(null);
  
  // Audio state
  const [currentTime, setCurrentTime] = useState(0);
  const [duration, setDuration] = useState(0);
  const [volume, setVolume] = useState(0.8);
  const [isMuted, setIsMuted] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  
  // Player state
  const [isShuffled, setIsShuffled] = useState(false);
  const [repeatMode, setRepeatMode] = useState('none'); // 'none', 'one', 'all'
  const [searchQuery, setSearchQuery] = useState('');
  const [error, setError] = useState(null);
  
  const audioRef = useRef(null);

  // Initialize app
  // Helper to load shuffled/random songs (local/demo)
  const loadRandomSongs = async (reset = true, nextPage = 1) => {
    setIsLoading(true);
    try {
      const response = await axios.get(`/api/songs?per_page=20&page=${nextPage}`);
      if (reset) {
        setSongs(response.data.songs);
        setMode('local');
        setOnlinePage(1);
        setOnlineHasMore(true);
      } else {
        setSongs(prev => [...prev, ...response.data.songs]);
      }
      setPage(response.data.page);
      setTotalPages(response.data.total_pages);
      setHasMore(response.data.page < response.data.total_pages);
      if (response.data.songs.length > 0 && reset) {
        setCurrentSong(response.data.songs[0]);
        setCurrentIndex(0);
      }
      setError(null);
    } catch (error) {
      console.error('Error loading random songs:', error);
      setError('Failed to load random songs.');
    } finally {
      setIsLoading(false);
    }
  };

  // Helper to load more JioSaavn songs (online)
  const fetchMoreOnline = async (reset = false, nextPage = 1) => {
    setIsLoading(true);
    try {
      const response = await axios.get(`/api/search?q=${encodeURIComponent(onlineQuery)}&per_page=20&page=${nextPage}`);
      const fetchedSongs = response.data.songs || [];
      if (reset) {
        setSongs(fetchedSongs);
        setMode('online');
        setOnlinePage(1);
      } else {
        setSongs(prev => [...prev, ...fetchedSongs]);
      }
      setOnlinePage(nextPage);
      // If we got 0 songs, no more available
      if (fetchedSongs.length === 0) {
        setOnlineHasMore(false);
        setHasMore(false);
      } else {
        setOnlineHasMore(true);
        setHasMore(true);
      }
      if (fetchedSongs.length > 0 && reset) {
        setCurrentSong(fetchedSongs[0]);
        setCurrentIndex(0);
      }
    } catch (error) {
      setOnlineHasMore(false);
      setHasMore(false);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    // On mount, load random songs
    loadRandomSongs();
  }, []);

  // --- Player controls (move these above useEffect hooks) ---
  const togglePlayPause = useCallback(() => {
    if (!currentSong || !audioRef.current) return;
    if (isPlaying) {
      audioRef.current.pause();
    } else {
      audioRef.current.play().catch(error => {
        console.error('Playback failed:', error);
        setIsPlaying(false);
      });
    }
  }, [isPlaying, currentSong]);

  const handleSongSelect = useCallback((song, index) => {
    setCurrentSong(song);
    setCurrentIndex(index);
    setIsPlaying(true);
  }, []);

  const handleNext = useCallback(() => {
    if (songs.length === 0) return;
    let nextIndex;
    if (repeatMode === 'one') {
      nextIndex = currentIndex;
    } else if (isShuffled) {
      nextIndex = Math.floor(Math.random() * songs.length);
    } else {
      nextIndex = (currentIndex + 1) % songs.length;
    }
    setCurrentIndex(nextIndex);
    setCurrentSong(songs[nextIndex]);
  }, [currentIndex, songs, isShuffled, repeatMode]);

  const handlePrevious = useCallback(() => {
    if (songs.length === 0) return;
    if (currentTime > 3) {
      audioRef.current.currentTime = 0;
      return;
    }
    let prevIndex;
    if (isShuffled) {
      prevIndex = Math.floor(Math.random() * songs.length);
    } else {
      prevIndex = currentIndex === 0 ? songs.length - 1 : currentIndex - 1;
    }
    setCurrentIndex(prevIndex);
    setCurrentSong(songs[prevIndex]);
  }, [currentIndex, songs, isShuffled, currentTime]);

  const handleSeek = useCallback((e) => {
    if (!audioRef.current || !duration) return;
    const rect = e.currentTarget.getBoundingClientRect();
    const percent = (e.clientX - rect.left) / rect.width;
    const newTime = percent * duration;
    audioRef.current.currentTime = newTime;
    setCurrentTime(newTime);
  }, [duration]);

  const toggleMute = useCallback(() => {
    setIsMuted(prev => !prev);
  }, []);

  const toggleShuffle = useCallback(() => {
    setIsShuffled(prev => !prev);
  }, []);

  const toggleRepeat = useCallback(() => {
    setRepeatMode(prev => {
      switch (prev) {
        case 'none': return 'all';
        case 'all': return 'one';
        case 'one': return 'none';
        default: return 'none';
      }
    });
  }, []);

  // --- useEffect hooks ---
  // Auto-play when song changes
  useEffect(() => {
    if (currentSong && audioRef.current) {
      audioRef.current.load();
      setCurrentTime(0); // Reset progress
      setDuration(0);   // Reset duration
      if (isPlaying) {
        const playPromise = audioRef.current.play();
        if (playPromise !== undefined) {
          playPromise.catch(error => {
            console.error('Playback failed:', error);
            setIsPlaying(false);
          });
        }
      }
    }
  }, [currentSong, isPlaying]);

  // Audio event handlers
  useEffect(() => {
    const audio = audioRef.current;
    if (!audio) return;
    const updateTime = () => {
      setCurrentTime(audio.currentTime);
      console.log('audio.currentTime:', audio.currentTime);
    };
    const updateDuration = () => setDuration(audio.duration);
    const onEnded = () => handleNext();
    const onPlay = () => {
      setIsPlaying(true);
      setCurrentTime(audio.currentTime);
    };
    const onPause = () => setIsPlaying(false);
    const onSeeked = () => setCurrentTime(audio.currentTime);
    audio.addEventListener('timeupdate', updateTime);
    audio.addEventListener('loadedmetadata', updateDuration);
    audio.addEventListener('ended', onEnded);
    audio.addEventListener('play', onPlay);
    audio.addEventListener('pause', onPause);
    audio.addEventListener('seeked', onSeeked);
    return () => {
      audio.removeEventListener('timeupdate', updateTime);
      audio.removeEventListener('loadedmetadata', updateDuration);
      audio.removeEventListener('ended', onEnded);
      audio.removeEventListener('play', onPlay);
      audio.removeEventListener('pause', onPause);
      audio.removeEventListener('seeked', onSeeked);
    };
  }, [currentSong, handleNext]);

  // Volume control
  useEffect(() => {
    if (audioRef.current) {
      audioRef.current.volume = isMuted ? 0 : volume;
    }
  }, [volume, isMuted]);

  // Keyboard shortcuts
  useEffect(() => {
    const handleKeyPress = (e) => {
      if (e.target.tagName === 'INPUT') return;
      
      switch (e.code) {
        case 'Space':
          e.preventDefault();
          togglePlayPause();
          break;
        case 'ArrowLeft':
          handlePrevious();
          break;
        case 'ArrowRight':
          handleNext();
          break;
        case 'ArrowUp':
          e.preventDefault();
          setVolume(prev => Math.min(1, prev + 0.1));
          break;
        case 'ArrowDown':
          e.preventDefault();
          setVolume(prev => Math.max(0, prev - 0.1));
          break;
        case 'KeyM':
          toggleMute();
          break;
        case 'KeyS':
          toggleShuffle();
          break;
        case 'KeyR':
          toggleRepeat();
          break;
      }
    };

    window.addEventListener('keydown', handleKeyPress);
    return () => window.removeEventListener('keydown', handleKeyPress);
  }, []);

  // Player controls
  // ...existing code...

  // Search handler
  const handleSearch = async (e) => {
    e.preventDefault();
    if (!searchQuery.trim()) {
      // If search is empty, show random songs
      loadRandomSongs(true, 1);
      return;
    }
    setIsLoading(true);
    setError(null);
    try {
      const response = await axios.get(`/api/search?q=${encodeURIComponent(searchQuery)}&per_page=20&page=1`);
      setSongs(response.data.songs);
      setPage(1);
      setTotalPages(Math.ceil(response.data.total / 20));
      setHasMore(response.data.songs.length > 0 && response.data.total > response.data.songs.length);
      if (response.data.songs.length > 0) {
        setCurrentSong(response.data.songs[0]);
        setCurrentIndex(0);
      }
    } catch (error) {
      console.error('Search error:', error);
      setError('Search failed. Please try again.');
    } finally {
      setIsLoading(false);
    }
  };

  // If search box is cleared, instantly show random songs
  useEffect(() => {
    if (searchQuery === '') {
      loadRandomSongs(true, 1);
    }
    // eslint-disable-next-line
  }, [searchQuery]);

  // Infinite scroll is disabled. Only the button loads more songs.

  // Fetch more for infinite scroll (search)
  const fetchMoreSearch = async () => {
    if (!hasMore || isLoading) return;
    setIsLoading(true);
    try {
      const nextPage = page + 1;
      const response = await axios.get(`/api/search?q=${encodeURIComponent(searchQuery)}&per_page=20&page=${nextPage}`);
      setSongs(prev => [...prev, ...response.data.songs]);
      setPage(nextPage);
      setTotalPages(Math.ceil(response.data.total / 20));
      setHasMore(nextPage < Math.ceil(response.data.total / 20));
    } catch (error) {
      setHasMore(false);
    } finally {
      setIsLoading(false);
    }
  };

  // Modified fetchMoreRandom to switch to online mode if no more local songs
  const fetchMoreRandom = async () => {
    if (!hasMore || isLoading) return;
    const nextPage = page + 1;
    if (nextPage > totalPages) {
      // Switch to online mode
      setMode('online');
      fetchMoreOnline(true, 1);
    } else {
      await loadRandomSongs(false, nextPage);
    }
  };

  // Utility functions
  const formatTime = (seconds) => {
    if (!seconds || isNaN(seconds)) return '0:00';
    const mins = Math.floor(seconds / 60);
    const secs = Math.floor(seconds % 60);
    return `${mins}:${secs.toString().padStart(2, '0')}`;
  };

  const getProgressPercent = () => {
    return duration ? (currentTime / duration) * 100 : 0;
  };

  // Styles
  const styles = {
    container: {
      minHeight: '100vh',
      background: 'var(--bg-main)',
      color: 'var(--text-main)',
      fontFamily: 'system-ui, -apple-system, sans-serif',
      transition: 'background 0.3s, color 0.3s'
    },
    content: {
      maxWidth: '1200px',
      margin: '0 auto',
      padding: '20px'
    },
    header: {
      textAlign: 'center',
      marginBottom: '30px'
    },
    title: {
      fontSize: '3rem',
      margin: '0',
      color: 'var(--text-main)',
      textShadow: '0 2px 4px rgba(0,0,0,0.3)'
    },
    subtitle: {
      color: 'var(--text-secondary)',
      margin: '10px 0'
    },
    searchContainer: {
      display: 'flex',
      justifyContent: 'center',
      marginBottom: '30px',
      gap: '10px'
    },
    searchInput: {
      padding: '12px 16px',
      fontSize: '16px',
      border: '1px solid var(--border-main)',
      borderRadius: '25px',
      backgroundColor: 'var(--input-bg)',
      color: 'var(--input-text)',
      width: '400px',
      outline: 'none',
      transition: 'background 0.3s, color 0.3s'
    },
    searchButton: {
      padding: '12px 24px',
      fontSize: '16px',
      backgroundColor: 'var(--spotify-green)',
      color: '#fff',
      border: 'none',
      borderRadius: '25px',
      cursor: 'pointer',
      transition: 'all 0.3s ease'
    },
    playerCard: {
      backgroundColor: 'var(--bg-card)',
      borderRadius: '20px',
      padding: '30px',
      marginBottom: '30px',
      border: '1px solid var(--border-main)',
      boxShadow: '0 8px 32px rgba(0,0,0,0.1)'
    },
    albumArt: {
      width: '120px',
      height: '120px',
      borderRadius: '15px',
      background: 'var(--spotify-gray)',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      fontSize: '3rem',
      margin: '0 auto 20px',
      color: 'var(--spotify-green)'
    },
    songTitle: {
      fontSize: '1.5rem',
      fontWeight: 'bold',
      margin: '0 0 5px 0',
      textAlign: 'center',
      color: 'var(--text-main)'
    },
    artistName: {
      color: 'var(--text-secondary)',
      textAlign: 'center',
      marginBottom: '20px'
    },
    progressContainer: {
      marginBottom: '20px'
    },
    progressBar: {
      width: '100%',
      height: '6px',
      borderRadius: '3px',
      cursor: 'pointer',
      position: 'relative',
      marginBottom: '10px'
    },
    progressFill: {
      height: '100%',
      background: 'var(--spotify-green)',
      borderRadius: '3px',
      transition: 'width 0.1s ease'
    },
    timeDisplay: {
      display: 'flex',
      justifyContent: 'space-between',
      fontSize: '0.9rem',
      color: 'var(--text-secondary)'
    },
    controls: {
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      gap: '15px',
      marginBottom: '20px'
    },
    controlButton: {
      width: '50px',
      height: '50px',
      borderRadius: '50%',
      border: 'none',
      backgroundColor: 'var(--bg-card)',
      color: 'var(--text-main)',
      cursor: 'pointer',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      fontSize: '1.2rem',
      transition: 'all 0.3s ease'
    },
    playButton: {
      width: '70px',
      height: '70px',
      backgroundColor: 'var(--spotify-green)',
      fontSize: '1.8rem',
      color: '#fff'
    },
    volumeContainer: {
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      gap: '10px'
    },
    volumeSlider: {
      width: '100px',
      height: '4px',
      borderRadius: '2px',
      background: 'var(--border-main)',
      outline: 'none',
      cursor: 'pointer'
    },
    songList: {
      backgroundColor: 'var(--bg-card)',
      borderRadius: '20px',
      overflow: 'hidden',
      border: '1px solid var(--border-main)'
    },
    songListHeader: {
      padding: '20px',
      backgroundColor: 'var(--bg-main)',
      borderBottom: '1px solid var(--border-main)'
    },
    songItem: {
      padding: '15px 20px',
      borderBottom: '1px solid var(--border-main)',
      cursor: 'pointer',
      display: 'flex',
      justifyContent: 'space-between',
      alignItems: 'center',
      transition: 'all 0.3s ease',
      background: 'var(--bg-card)',
      color: 'var(--text-main)'
    },
    songItemActive: {
      backgroundColor: 'var(--spotify-green)',
      color: '#fff'
    },
    shortcuts: {
      marginTop: '20px',
      padding: '20px',
      backgroundColor: 'var(--bg-card)',
      borderRadius: '15px',
      fontSize: '0.9rem',
      color: 'var(--text-secondary)'
    }
  };

  if (error) {
    return (
      <div style={styles.container}>
        <div style={styles.content}>
          <div style={{ textAlign: 'center', padding: '50px' }}>
            <h1>⚠️ Connection Error</h1>
            <p>{error}</p>
            <button 
              onClick={() => window.location.reload()}
              style={styles.searchButton}
            >
              Retry
            </button>
          </div>
        </div>
      </div>
    );
  }

  // See more button should show in online mode as long as we have any songs and haven't hit 0 on last fetch
  const canShowSeeMore =
    (mode === 'local' && (hasMore || (!hasMore && songs.length > 0))) ||
    (mode === 'online' && songs.length > 0 && hasMore);

  return (
    <div style={styles.container}>
      <div style={styles.content}>
        {/* Header */}
        <header style={styles.header}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 16 }}>
            <h1 style={styles.title}>🎵 Ultimate Music Player</h1>
            <button
              onClick={() => setDarkMode((d) => !d)}
              style={{
                marginLeft: 24,
                border: 'none',
                borderRadius: 20,
                padding: '8px 18px',
                background: 'var(--bg-card)',
                color: 'var(--text-main)',
                fontWeight: 600,
                fontSize: 16,
                cursor: 'pointer',
                boxShadow: '0 2px 8px rgba(0,0,0,0.08)'
              }}
              title="Toggle dark mode"
            >
              {darkMode ? '🌙 Dark' : '☀️ Light'}
            </button>
          </div>
          <p style={{ ...styles.subtitle, color: 'var(--text-secondary)' }}>
            Production-ready music player with all modern features
          </p>
        </header>

        {/* Search */}
        <div style={styles.searchContainer}>
          <form onSubmit={handleSearch} style={{ display: 'flex', gap: '10px' }}>
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Search for music..."
              style={styles.searchInput}
              disabled={isLoading}
            />
            <button
              type="submit"
              style={styles.searchButton}
              disabled={isLoading}
            >
              {isLoading ? '🔍' : 'Search'}
            </button>
          </form>
        </div>

        {/* Music Player */}
        <div style={styles.playerCard}>
          {currentSong ? (
            <>
              {/* Album Art */}
              <div style={styles.albumArt}>
                🎵
              </div>

              {/* Song Info */}
              <h3 style={styles.songTitle}>{currentSong.title}</h3>
              <p style={styles.artistName}>{currentSong.artist}</p>


              {/* Progress Bar + Equalizer */}
              <div className="progress-container" onClick={handleSeek} style={{ cursor: 'pointer', width: '100%' }}>
                {/* Animated Equalizer (shows only when playing) */}
                <div className="equalizer" style={{ opacity: isPlaying ? 1 : 0.3, transition: 'opacity 0.3s' }}>
                  <div className="eq-bar" style={{ animationPlayState: isPlaying ? 'running' : 'paused' }} />
                  <div className="eq-bar" style={{ animationPlayState: isPlaying ? 'running' : 'paused' }} />
                  <div className="eq-bar" style={{ animationPlayState: isPlaying ? 'running' : 'paused' }} />
                  <div className="eq-bar" style={{ animationPlayState: isPlaying ? 'running' : 'paused' }} />
                  <div className="eq-bar" style={{ animationPlayState: isPlaying ? 'running' : 'paused' }} />
                </div>
                {/* Progress Bar */}
                <div className="progress">
                  <div
                    className="progress-bar"
                    style={{ width: `${getProgressPercent()}%` }}
                  />
                </div>
                <div className="time-display" style={{ minWidth: 80, display: 'flex', justifyContent: 'space-between', fontSize: 14, marginLeft: 8 }}>
                  <span>{formatTime(currentTime)}</span>
                  <span>{formatTime(duration)}</span>
                </div>
              </div>

              {/* Main Controls */}
              <div style={styles.controls}>
                <button
                  onClick={toggleShuffle}
                  style={{
                    ...styles.controlButton,
                    backgroundColor: isShuffled ? '#10b981' : 'rgba(255,255,255,0.2)'
                  }}
                  title="Shuffle (S)"
                >
                  🔀
                </button>

                <button
                  onClick={handlePrevious}
                  style={styles.controlButton}
                  title="Previous (←)"
                >
                  ⏮️
                </button>

                <button
                  onClick={togglePlayPause}
                  style={{...styles.controlButton, ...styles.playButton}}
                  disabled={!currentSong}
                  title="Play/Pause (Space)"
                >
                  {isPlaying ? '⏸️' : '▶️'}
                </button>

                <button
                  onClick={handleNext}
                  style={styles.controlButton}
                  title="Next (→)"
                >
                  ⏭️
                </button>

                <button
                  onClick={toggleRepeat}
                  style={{
                    ...styles.controlButton,
                    backgroundColor: repeatMode !== 'none' ? '#10b981' : 'rgba(255,255,255,0.2)'
                  }}
                  title="Repeat (R)"
                >
                  {repeatMode === 'one' ? '🔂' : '🔁'}
                </button>
              </div>

              {/* Volume Control */}
              <div style={styles.volumeContainer}>
                <button
                  onClick={toggleMute}
                  style={styles.controlButton}
                  title="Mute (M)"
                >
                  {isMuted ? '🔇' : volume > 0.5 ? '🔊' : '🔉'}
                </button>
                <input
                  type="range"
                  min="0"
                  max="1"
                  step="0.01"
                  value={isMuted ? 0 : volume}
                  onChange={(e) => setVolume(parseFloat(e.target.value))}
                  style={styles.volumeSlider}
                  title="Volume (↑↓)"
                />
                <span style={{ fontSize: '0.9rem', minWidth: '40px' }}>
                  {Math.round((isMuted ? 0 : volume) * 100)}%
                </span>
              </div>

              {/* Source Info */}
              <div style={{ fontSize: '0.8rem', color: 'rgba(255,255,255,0.6)', textAlign: 'center', marginTop: '15px' }}>
                Source: {currentSong.source === 'api' ? '🌐 Internet Archive' : '📁 Local File'}
                {currentSong.album && ` • ${currentSong.album}`}
                {currentSong.year && ` • ${currentSong.year}`}
              </div>

              <audio
                ref={audioRef}
                src={currentSong.url}
                preload="metadata"
                onError={(e) => {
                  console.error('Audio error:', e);
                  console.log('Failed URL:', currentSong.url);
                }}
              />
            </>
          ) : (
            <div style={{ textAlign: 'center', padding: '50px' }}>
              <p>No song selected</p>
            </div>
          )}
        </div>

        {/* Song List */}
        <div style={styles.songList}>
          <div style={styles.songListHeader}>
            <h2 style={{ margin: '0' }}>Songs ({songs.length})</h2>
          </div>
          
          <div style={{ maxHeight: '400px', overflowY: 'auto' }} ref={songListRef}>
            {songs.length === 0 && !isLoading ? (
              <div style={{ padding: '40px', textAlign: 'center' }}>
                <p>No songs found</p>
              </div>
            ) : (
              <>
                {songs.map((song, index) => (
                  <div
                    key={song.id}
                    onClick={() => handleSongSelect(song, index)}
                    style={{
                      ...styles.songItem,
                      ...(currentSong?.id === song.id ? styles.songItemActive : {}),
                    }}
                    onMouseEnter={(e) => {
                      if (currentSong?.id !== song.id) {
                        e.currentTarget.style.backgroundColor = 'rgba(255,255,255,0.1)';
                      }
                    }}
                    onMouseLeave={(e) => {
                      if (currentSong?.id !== song.id) {
                        e.currentTarget.style.backgroundColor = 'transparent';
                      }
                    }}
                  >
                    <div>
                      <div style={{ 
                        fontWeight: 'bold', 
                        color: currentSong?.id === song.id ? '#10b981' : 'white',
                        marginBottom: '5px'
                      }}>
                        {index + 1}. {song.title}
                      </div>
                      <div style={{ color: 'rgba(255,255,255,0.8)', fontSize: '0.9rem' }}>
                        {song.artist}
                      </div>
                    </div>
                    <div style={{ textAlign: 'right', fontSize: '0.8rem', color: 'rgba(255,255,255,0.6)' }}>
                      <div>{song.source === 'api' ? '🌐' : '📁'}</div>
                      {song.duration && <div>{formatTime(song.duration)}</div>}
                    </div>
                  </div>
                ))}
                {isLoading && (
                  <div style={{ padding: '20px', textAlign: 'center' }}>
                    <p>Loading more...</p>
                  </div>
                )}
              </>
            )}
          </div>
          {/* See more songs button always below the list */}
          {canShowSeeMore && !isLoading && (
            <div style={{ padding: '10px', textAlign: 'center' }}>
              <button
                style={{
                  padding: '10px 28px',
                  borderRadius: 20,
                  border: 'none',
                  background: 'var(--spotify-green)',
                  color: '#fff',
                  fontWeight: 600,
                  fontSize: 16,
                  cursor: 'pointer',
                  boxShadow: '0 2px 8px rgba(0,0,0,0.08)'
                }}
                onClick={() => {
                  if (mode === 'local' && hasMore) {
                    fetchMoreRandom();
                  } else if (mode === 'local' && !hasMore) {
                    // Switch to online mode
                    setMode('online');
                    fetchMoreOnline(true, 1);
                  } else {
                    fetchMoreOnline(false, onlinePage + 1);
                  }
                }}
              >
                See more songs
              </button>
            </div>
          )}
        </div>

        {/* Keyboard Shortcuts */}
        <div style={styles.shortcuts}>
          <h3 style={{ margin: '0 0 10px 0', color: 'rgba(255,255,255,0.9)' }}>⌨️ Keyboard Shortcuts</h3>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '10px' }}>
            <span><strong>Space:</strong> Play/Pause</span>
            <span><strong>←/→:</strong> Previous/Next</span>
            <span><strong>↑/↓:</strong> Volume Up/Down</span>
            <span><strong>M:</strong> Mute/Unmute</span>
            <span><strong>S:</strong> Shuffle</span>
            <span><strong>R:</strong> Repeat Mode</span>
          </div>
        </div>

        {/* Footer */}
        <footer style={{ marginTop: '30px', textAlign: 'center', fontSize: '0.9rem', color: 'rgba(255,255,255,0.6)' }}>
          <p>
            🎵 Built with React + Flask • Enhanced with modern UI/UX
          </p>
        </footer>
      </div>
    </div>
  );
}

export default App;