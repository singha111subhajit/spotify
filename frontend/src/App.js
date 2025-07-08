import React, { useState, useEffect, useRef, useCallback } from 'react';
import axios from 'axios';

function App() {
  // Core state
  const [songs, setSongs] = useState([]);
  const [currentSong, setCurrentSong] = useState(null);
  const [isPlaying, setIsPlaying] = useState(false);
  const [currentIndex, setCurrentIndex] = useState(0);
  
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
  useEffect(() => {
    const initializeSongs = async () => {
      setIsLoading(true);
      try {
        await axios.get('/api/health');
        const response = await axios.get('/api/songs?per_page=50');
        setSongs(response.data.songs);
        
        if (response.data.songs.length > 0) {
          setCurrentSong(response.data.songs[0]);
          setCurrentIndex(0);
        }
        
        setError(null);
      } catch (error) {
        console.error('Error initializing app:', error);
        setError('Failed to connect to backend. Please make sure the Flask server is running on port 5600.');
      } finally {
        setIsLoading(false);
      }
    };

    initializeSongs();
  }, []);

  // Auto-play when song changes
  useEffect(() => {
    if (currentSong && audioRef.current) {
      audioRef.current.load();
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
  }, [currentSong]);

  // Audio event handlers
  useEffect(() => {
    const audio = audioRef.current;
    if (!audio) return;

    const updateTime = () => setCurrentTime(audio.currentTime);
    const updateDuration = () => setDuration(audio.duration);
    const onEnded = () => handleNext();
    const onPlay = () => setIsPlaying(true);
    const onPause = () => setIsPlaying(false);

    audio.addEventListener('timeupdate', updateTime);
    audio.addEventListener('loadedmetadata', updateDuration);
    audio.addEventListener('ended', onEnded);
    audio.addEventListener('play', onPlay);
    audio.addEventListener('pause', onPause);

    return () => {
      audio.removeEventListener('timeupdate', updateTime);
      audio.removeEventListener('loadedmetadata', updateDuration);
      audio.removeEventListener('ended', onEnded);
      audio.removeEventListener('play', onPlay);
      audio.removeEventListener('pause', onPause);
    };
  }, [currentSong]);

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
      // If more than 3 seconds played, restart current song
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

  const handleSearch = async (e) => {
    e.preventDefault();
    if (!searchQuery.trim()) return;
    
    setIsLoading(true);
    setError(null);
    
    try {
      const response = await axios.get(`/api/search?q=${encodeURIComponent(searchQuery)}&per_page=50`);
      setSongs(response.data.songs);
      
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
      background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
      color: 'white',
      fontFamily: 'system-ui, -apple-system, sans-serif'
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
      background: 'linear-gradient(45deg, #fff, #f0f9ff)',
      WebkitBackgroundClip: 'text',
      WebkitTextFillColor: 'transparent',
      textShadow: '0 2px 4px rgba(0,0,0,0.3)'
    },
    subtitle: {
      color: 'rgba(255,255,255,0.8)',
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
      border: 'none',
      borderRadius: '25px',
      backgroundColor: 'rgba(255,255,255,0.1)',
      color: 'white',
      width: '400px',
      backdropFilter: 'blur(10px)',
      outline: 'none'
    },
    searchButton: {
      padding: '12px 24px',
      fontSize: '16px',
      backgroundColor: 'rgba(255,255,255,0.2)',
      color: 'white',
      border: 'none',
      borderRadius: '25px',
      cursor: 'pointer',
      backdropFilter: 'blur(10px)',
      transition: 'all 0.3s ease'
    },
    playerCard: {
      backgroundColor: 'rgba(255,255,255,0.1)',
      borderRadius: '20px',
      padding: '30px',
      marginBottom: '30px',
      backdropFilter: 'blur(20px)',
      border: '1px solid rgba(255,255,255,0.1)',
      boxShadow: '0 8px 32px rgba(0,0,0,0.1)'
    },
    albumArt: {
      width: '120px',
      height: '120px',
      borderRadius: '15px',
      background: 'linear-gradient(45deg, #667eea, #764ba2)',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      fontSize: '3rem',
      margin: '0 auto 20px'
    },
    songTitle: {
      fontSize: '1.5rem',
      fontWeight: 'bold',
      margin: '0 0 5px 0',
      textAlign: 'center'
    },
    artistName: {
      color: 'rgba(255,255,255,0.8)',
      textAlign: 'center',
      marginBottom: '20px'
    },
    progressContainer: {
      marginBottom: '20px'
    },
    progressBar: {
      width: '100%',
      height: '6px',
      backgroundColor: 'rgba(255,255,255,0.2)',
      borderRadius: '3px',
      cursor: 'pointer',
      position: 'relative',
      marginBottom: '10px'
    },
    progressFill: {
      height: '100%',
      background: 'linear-gradient(90deg, #10b981, #06d6a0)',
      borderRadius: '3px',
      transition: 'width 0.1s ease'
    },
    timeDisplay: {
      display: 'flex',
      justifyContent: 'space-between',
      fontSize: '0.9rem',
      color: 'rgba(255,255,255,0.8)'
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
      backgroundColor: 'rgba(255,255,255,0.2)',
      color: 'white',
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
      backgroundColor: '#10b981',
      fontSize: '1.8rem'
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
      background: 'rgba(255,255,255,0.2)',
      outline: 'none',
      cursor: 'pointer'
    },
    songList: {
      backgroundColor: 'rgba(255,255,255,0.1)',
      borderRadius: '20px',
      overflow: 'hidden',
      backdropFilter: 'blur(20px)',
      border: '1px solid rgba(255,255,255,0.1)'
    },
    songListHeader: {
      padding: '20px',
      backgroundColor: 'rgba(0,0,0,0.2)',
      borderBottom: '1px solid rgba(255,255,255,0.1)'
    },
    songItem: {
      padding: '15px 20px',
      borderBottom: '1px solid rgba(255,255,255,0.1)',
      cursor: 'pointer',
      display: 'flex',
      justifyContent: 'space-between',
      alignItems: 'center',
      transition: 'all 0.3s ease'
    },
    songItemActive: {
      backgroundColor: 'rgba(16, 185, 129, 0.3)'
    },
    shortcuts: {
      marginTop: '20px',
      padding: '20px',
      backgroundColor: 'rgba(255,255,255,0.05)',
      borderRadius: '15px',
      fontSize: '0.9rem',
      color: 'rgba(255,255,255,0.7)'
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

  return (
    <div style={styles.container}>
      <div style={styles.content}>
        {/* Header */}
        <header style={styles.header}>
          <h1 style={styles.title}>🎵 Ultimate Music Player</h1>
          <p style={styles.subtitle}>
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

              {/* Progress Bar */}
              <div style={styles.progressContainer}>
                <div 
                  style={styles.progressBar}
                  onClick={handleSeek}
                >
                  <div 
                    style={{
                      ...styles.progressFill,
                      width: `${getProgressPercent()}%`
                    }}
                  />
                </div>
                <div style={styles.timeDisplay}>
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
          
          <div style={{ maxHeight: '400px', overflowY: 'auto' }}>
            {isLoading ? (
              <div style={{ padding: '40px', textAlign: 'center' }}>
                <p>Loading songs...</p>
              </div>
            ) : songs.length === 0 ? (
              <div style={{ padding: '40px', textAlign: 'center' }}>
                <p>No songs found</p>
              </div>
            ) : (
              songs.map((song, index) => (
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
              ))
            )}
          </div>
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