import React, { useState, useEffect, useRef, useCallback } from 'react';
import axios from 'axios';
import './App.css';

// API base URL for backend (set via env in production)
const API_BASE = process.env.REACT_APP_API_BASE || '';

function useDarkMode() {
  const [theme, setTheme] = useState(() => {
    const saved = localStorage.getItem('theme');
    return saved ? saved : 'dark';
  });
  
  useEffect(() => {
    localStorage.setItem('theme', theme);
    document.body.setAttribute('data-theme', theme);
    console.log('Body data-theme:', document.body.getAttribute('data-theme'), 'Theme:', theme);
  }, [theme]);
  
  return [theme, setTheme];
}

let renderCount = 0;
function App() {
  renderCount += 1;
  console.log('[DEBUG] App rendered', renderCount);
  const [theme, setTheme] = useDarkMode();
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
  const [suggestions, setSuggestions] = useState([]);
  const [showSuggestions, setShowSuggestions] = useState(false);
  const [activeSuggestion, setActiveSuggestion] = useState(-1);
  const suggestionTimeout = useRef(null);
  const searchInputRef = useRef(null);
  
  const audioRef = useRef(null);

  // --- Auth & Playlist State ---
  const [authModalOpen, setAuthModalOpen] = useState(false);
  const [authMode, setAuthMode] = useState('login'); // 'login' or 'register'
  const [authLoading, setAuthLoading] = useState(false);
  const [authError, setAuthError] = useState('');
  const [jwt, setJwt] = useState(() => localStorage.getItem('jwt') || '');
  const [user, setUser] = useState(null);
  const [playlistSidebarOpen, setPlaylistSidebarOpen] = useState(false);
  // Debug print for sidebar open state
  /*
  useEffect(() => {
    console.log('[DEBUG] playlistSidebarOpen changed:', playlistSidebarOpen);
  }, [playlistSidebarOpen]);
  // Debug print for isPlaying
  useEffect(() => {
    console.log('[DEBUG] isPlaying changed:', isPlaying);
  }, [isPlaying]);
  */
  const [playlist, setPlaylist] = useState(null); // Only one playlist per user
  const [playlistSongs, setPlaylistSongs] = useState([]);
  const [playlistLoading, setPlaylistLoading] = useState(false);
  const [playlistError, setPlaylistError] = useState('');

  // --- Toast for feedback ---
  const [toast, setToast] = useState('');
  const showToast = (msg) => { setToast(msg); setTimeout(() => setToast(''), 2500); };

  // Initialize app
  // Helper to load shuffled/random songs (local/demo)
  const loadRandomSongs = async (reset = true, nextPage = 1) => {
    setIsLoading(true);
    try {
      const response = await axios.get(`${API_BASE}/api/songs?per_page=20&page=${nextPage}`);
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
      
      // Only set current song if there's no current song playing
      if (response.data.songs.length > 0 && reset && !currentSong) {
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
      const response = await axios.get(`${API_BASE}/api/search?q=${encodeURIComponent(onlineQuery)}&per_page=20&page=${nextPage}`);
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
      
      // Only set current song if there's no current song playing
      if (fetchedSongs.length > 0 && reset && !currentSong) {
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
    // On mount, try to load random online songs first
    const shuffleArray = (array) => {
      const arr = array.slice();
      for (let i = arr.length - 1; i > 0; i--) {
        const j = Math.floor(Math.random() * (i + 1));
        [arr[i], arr[j]] = [arr[j], arr[i]];
      }
      return arr;
    };
    const loadInitialSongs = async () => {
      setIsLoading(true);
      try {
        // Try fetching random online songs (e.g., bollywood as default)
        const response = await axios.get(`${API_BASE}/api/search?q=bollywood&per_page=20&page=1`);
        let fetchedSongs = response.data.songs || [];
        if (fetchedSongs.length > 0) {
          fetchedSongs = shuffleArray(fetchedSongs); // Shuffle for randomness
          setSongs(fetchedSongs);
          setMode('online');
          setOnlinePage(1);
          setOnlineHasMore(true);
          setPage(1);
          setTotalPages(Math.ceil((response.data.total || 20) / 20));
          setHasMore(fetchedSongs.length > 0 && (response.data.total > fetchedSongs.length));
          if (!currentSong) {
            setCurrentSong(fetchedSongs[0]);
            setCurrentIndex(0);
          }
        } else {
          // Fallback to static songs if API returns nothing
          await loadRandomSongs(true, 1);
        }
      } catch (err) {
        // Fallback to static songs if API fails
        await loadRandomSongs(true, 1);
      } finally {
        setIsLoading(false);
      }
    };
    loadInitialSongs();
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
    // Don't set isPlaying here - let the audio element handle it
  }, []);

  // Enhanced fetchMoreOnline with better error handling
  const fetchMoreOnlineEnhanced = useCallback(async (reset = false, nextPage = 1) => {
    if (isLoading) return;
    
    try {
      await fetchMoreOnline(reset, nextPage);
    } catch (error) {
      console.error('Error loading online songs:', error);
      setOnlineHasMore(false);
      setHasMore(false);
    }
  }, [isLoading, fetchMoreOnline]);

  // Simple function to load more songs automatically
  const loadMoreSongsAutomatically = useCallback(async () => {
    if (isLoading) return;
    
    console.log('Auto-loading more songs for continuous playback...');
    
    if (mode === 'local' && hasMore) {
      // Load more local songs
      const nextPage = page + 1;
      if (nextPage <= totalPages) {
        await loadRandomSongs(false, nextPage);
      } else {
        // Switch to online mode
        setMode('online');
        await fetchMoreOnline(true, 1);
      }
    } else if (mode === 'local' && !hasMore) {
      // Switch to online mode for continuous playback
      setMode('online');
      await fetchMoreOnline(true, 1);
    } else if (mode === 'online' && onlineHasMore) {
      // Load more online songs
      await fetchMoreOnline(false, onlinePage + 1);
    }
  }, [isLoading, mode, hasMore, page, totalPages, onlineHasMore, onlinePage, loadRandomSongs, fetchMoreOnline]);

  const handleNext = useCallback(async () => {
    if (songs.length === 0) return;
    
    let nextIndex;
    if (repeatMode === 'one') {
      nextIndex = currentIndex;
    } else if (isShuffled) {
      nextIndex = Math.floor(Math.random() * songs.length);
    } else {
      nextIndex = currentIndex + 1;
      
      // If we've reached the end of current songs, try to load more
      if (nextIndex >= songs.length) {
        if ((hasMore || onlineHasMore) && !isLoading) {
          console.log('Reached end of songs, loading more automatically...');
          try {
            await loadMoreSongsAutomatically();
            // After loading, check if we have more songs
            if (songs.length > currentIndex + 1) {
              nextIndex = currentIndex + 1;
            } else {
              // If still no more songs, wrap to beginning
              nextIndex = 0;
            }
          } catch (error) {
            console.error('Failed to load more songs:', error);
            // Wrap to beginning if loading fails
            nextIndex = 0;
          }
        } else {
          // No more songs available, wrap to beginning
          nextIndex = 0;
        }
      }
    }
    
    setCurrentIndex(nextIndex);
    setCurrentSong(songs[nextIndex]);
    // Don't set isPlaying here - let the audio element handle it
  }, [currentIndex, songs, isShuffled, repeatMode, hasMore, onlineHasMore, isLoading, loadMoreSongsAutomatically]);

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

  // Cycle through themes
  const toggleTheme = useCallback(() => {
    setTheme(prev => {
      switch (prev) {
        case 'dark': return 'light';
        case 'light': return 'gradient';
        case 'gradient': return 'dark';
        default: return 'dark';
      }
    });
  }, [setTheme]);

  // --- useEffect hooks ---
  // Restore auto-play when song changes
  useEffect(() => {
    if (currentSong && audioRef.current) {
      audioRef.current.pause();
      audioRef.current.src = currentSong.url;
      audioRef.current.load();
      setCurrentTime(0); // Reset progress
      setDuration(0);   // Reset duration
      // Auto-play when song changes (user clicked on a song)
      audioRef.current.play().catch(error => {
        console.error('Playback failed:', error);
        setIsPlaying(false);
      });
    }
    // eslint-disable-next-line
  }, [currentSong]);



  // Restore user fetch on jwt change
  useEffect(() => {
    if (jwt) {
      axios.get(`${API_BASE}/me`, { headers: { Authorization: `Bearer ${jwt}` } })
        .then(res => setUser(res.data))
        .catch(() => { setUser(null); setJwt(''); localStorage.removeItem('jwt'); });
    } else {
      setUser(null);
    }
  }, [jwt]);

  // Restore playlist fetch when sidebar opens
  useEffect(() => { 
    if (playlistSidebarOpen) {
      fetchPlaylistAndSongs();
    }
  }, [playlistSidebarOpen, jwt]);

  // Volume control
  useEffect(() => {
    if (audioRef.current) {
      audioRef.current.volume = isMuted ? 0 : volume;
    }
  }, [volume, isMuted]);

  // Keyboard shortcuts
  /*
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
  */

  // Player controls
  // ...existing code...

  // Search handler - FIXED: Don't change current song when searching
  const handleSearch = async (e) => {
    e.preventDefault();
    if (!searchQuery.trim()) {
      // If search is empty, show random songs but keep current song playing
      loadRandomSongs(true, 1);
      return;
    }
    setIsLoading(true);
    setError(null);
    try {
      const response = await axios.get(`${API_BASE}/api/search?q=${encodeURIComponent(searchQuery)}&per_page=20&page=1`);
      setSongs(response.data.songs);
      setPage(1);
      setTotalPages(Math.ceil(response.data.total / 20));
      setHasMore(response.data.songs.length > 0 && response.data.total > response.data.songs.length);
      
      // FIXED: Only set current song if there's no song currently playing
      if (response.data.songs.length > 0 && !currentSong) {
        setCurrentSong(response.data.songs[0]);
        setCurrentIndex(0);
      }
      // Update current index if current song is in new results
      if (currentSong) {
        const newIndex = response.data.songs.findIndex(song => song.id === currentSong.id);
        if (newIndex !== -1) {
          setCurrentIndex(newIndex);
        }
      }
    } catch (error) {
      console.error('Search error:', error);
      setError('Search failed. Please try again.');
    } finally {
      setIsLoading(false);
    }
  };

  // REMOVED: Auto-load random songs when search is cleared to prevent current song changing

  // Infinite scroll is disabled. Only the button loads more songs.

  // Fetch more for infinite scroll (search)
  const fetchMoreSearch = async () => {
    if (!hasMore || isLoading) return;
    setIsLoading(true);
    try {
      const nextPage = page + 1;
      const response = await axios.get(`${API_BASE}/api/search?q=${encodeURIComponent(searchQuery)}&per_page=20&page=${nextPage}`);
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

  // Debounced fetch for suggestions
  useEffect(() => {
    if (!searchQuery.trim()) {
      setSuggestions([]);
      setShowSuggestions(false);
      return;
    }
    // Only fetch if input is not empty and not loading
    if (isLoading) return;
    // Debounce
    if (suggestionTimeout.current) clearTimeout(suggestionTimeout.current);
    suggestionTimeout.current = setTimeout(async () => {
      try {
        const resp = await axios.get(`${API_BASE}/api/search?q=${encodeURIComponent(searchQuery)}&per_page=5&page=1`);
        setSuggestions(resp.data.songs || []);
        setShowSuggestions(true);
      } catch (e) {
        setSuggestions([]);
        setShowSuggestions(false);
      }
    }, 300); // 300ms debounce
    return () => {
      if (suggestionTimeout.current) clearTimeout(suggestionTimeout.current);
    };
  }, [searchQuery]);

  // Hide suggestions on blur (with delay to allow click)
  const handleInputBlur = () => {
    setTimeout(() => setShowSuggestions(false), 150);
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

  // Get theme label for button
  const getThemeLabel = () => {
    switch (theme) {
      case 'dark': return '🌙 Dark';
      case 'light': return '☀️ Light';
      case 'gradient': return '🌈 Gradient';
      default: return '🌙 Dark';
    }
  };

  // Get background style with song image
  const getContainerStyle = () => {
    const baseStyle = {
      minHeight: '100vh',
      background: 'var(--bg-main)',
      color: 'var(--text-main)',
      fontFamily: 'system-ui, -apple-system, sans-serif',
      transition: 'background 0.3s, color 0.3s',
      position: 'relative'
    };

    // Add background image if song has thumbnail
    if (currentSong && currentSong.thumbnail && isPlaying) {
      return {
        ...baseStyle,
        backgroundImage: `linear-gradient(rgba(0,0,0,0.7), rgba(0,0,0,0.8)), url(${currentSong.thumbnail})`,
        backgroundSize: 'cover',
        backgroundPosition: 'center',
        backgroundAttachment: 'fixed'
      };
    }

    return baseStyle;
  };

  // Styles with mobile responsiveness
  const styles = {
    container: getContainerStyle(),
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
      fontSize: 'clamp(1.8rem, 5vw, 3rem)', // Responsive font size
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
      gap: '10px',
      padding: '0 10px', // Mobile padding
      position: 'relative',
      zIndex: 20
    },
    searchInput: {
      padding: '12px 16px',
      fontSize: '16px',
      border: '1px solid var(--border-main)',
      borderRadius: '25px',
      backgroundColor: 'var(--input-bg)',
      color: 'var(--input-text)',
      width: '100%', // FIXED: Responsive width
      maxWidth: '400px', // Maximum width for desktop
      minWidth: '200px', // Minimum width
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
      transition: 'all 0.3s ease',
      whiteSpace: 'nowrap' // Prevent text wrapping
    },
    playerCard: {
      backgroundColor: 'var(--bg-card)',
      borderRadius: '20px',
      padding: 'clamp(20px, 4vw, 30px)', // Responsive padding
      marginBottom: '30px',
      border: '1px solid var(--border-main)',
      boxShadow: '0 8px 32px rgba(0,0,0,0.3)', // Enhanced shadow for background images
      backdropFilter: currentSong && currentSong.thumbnail ? 'blur(10px)' : 'none'
    },
    albumArt: {
      width: 'clamp(100px, 20vw, 120px)', // Responsive size
      height: 'clamp(100px, 20vw, 120px)',
      borderRadius: '15px',
      background: currentSong && currentSong.thumbnail 
        ? `url(${currentSong.thumbnail}) center/cover` 
        : 'var(--spotify-gray)',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      fontSize: '3rem',
      margin: '0 auto 20px',
      color: currentSong && currentSong.thumbnail ? 'transparent' : 'var(--spotify-green)',
      boxShadow: '0 4px 15px rgba(0,0,0,0.2)'
    },
    songTitle: {
      fontSize: 'clamp(1.2rem, 4vw, 1.5rem)', // Responsive font size
      fontWeight: 'bold',
      margin: '0 0 5px 0',
      textAlign: 'center',
      color: 'var(--text-main)'
    },
    artistName: {
      color: 'var(--text-secondary)',
      textAlign: 'center',
      marginBottom: '20px',
      fontSize: 'clamp(0.9rem, 3vw, 1rem)' // Responsive font size
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
      fontSize: 'clamp(0.8rem, 2.5vw, 0.9rem)', // Responsive font size
      color: 'var(--text-secondary)'
    },
    controls: {
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      gap: 'clamp(10px, 3vw, 15px)', // Responsive gap
      marginBottom: '20px',
      flexWrap: 'wrap' // Allow wrapping on very small screens
    },
    controlButton: {
      width: 'clamp(45px, 10vw, 50px)', // Responsive size
      height: 'clamp(45px, 10vw, 50px)',
      borderRadius: '50%',
      border: 'none',
      backgroundColor: 'var(--bg-card)',
      color: 'var(--text-main)',
      cursor: 'pointer',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      fontSize: 'clamp(1rem, 2.5vw, 1.2rem)', // Responsive font size
      transition: 'all 0.3s ease'
    },
    playButton: {
      width: 'clamp(60px, 15vw, 70px)', // Responsive size
      height: 'clamp(60px, 15vw, 70px)',
      backgroundColor: 'var(--spotify-green)',
      fontSize: 'clamp(1.5rem, 4vw, 1.8rem)', // Responsive font size
      color: '#fff'
    },
    volumeContainer: {
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      gap: '10px',
      flexWrap: 'wrap' // Allow wrapping on small screens
    },
    volumeSlider: {
      width: 'clamp(80px, 20vw, 100px)', // Responsive width
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
      border: '1px solid var(--border-main)',
      maxHeight: '60vh', // INCREASED: More space for song list
      display: 'flex',
      flexDirection: 'column'
    },
    songListHeader: {
      padding: '20px',
      backgroundColor: 'var(--bg-main)',
      borderBottom: '1px solid var(--border-main)',
      flexShrink: 0 // Don't shrink
    },
    songItem: {
      padding: 'clamp(12px, 3vw, 15px) clamp(15px, 4vw, 20px)', // Responsive padding
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
    }
  };

  // --- Auth Functions ---
  useEffect(() => {
    if (jwt) {
      axios.get(`${API_BASE}/me`, { headers: { Authorization: `Bearer ${jwt}` } })
        .then(res => setUser(res.data))
        .catch(() => { setUser(null); setJwt(''); localStorage.removeItem('jwt'); });
    } else {
      setUser(null);
    }
  }, [jwt]);

  const handleAuth = async (e) => {
    e.preventDefault();
    setAuthLoading(true);
    setAuthError('');
    const form = e.target;
    const username = form.username?.value;
    const user_id = form.user_id.value;
    const password = form.password.value;
    try {
      if (authMode === 'register') {
        await axios.post(`${API_BASE}/register`, { username, user_id, password });
        setAuthMode('login');
        setAuthError('Registered! Please log in.');
      } else {
        const res = await axios.post(`${API_BASE}/login`, { user_id, password });
        setJwt(res.data.token);
        localStorage.setItem('jwt', res.data.token);
        setAuthModalOpen(false);
      }
    } catch (err) {
      setAuthError(err.response?.data?.error || 'Auth failed');
    } finally {
      setAuthLoading(false);
    }
  };

  const handleLogout = () => {
    setJwt('');
    setUser(null);
    localStorage.removeItem('jwt');
    setPlaylistSidebarOpen(false);
  };

  // --- Playlist Functions ---
  // Fetch default playlist and its songs
  const fetchPlaylistAndSongs = async () => {
    console.log('[DEBUG] fetchPlaylistAndSongs called');
    if (!jwt) return;
    setPlaylistLoading(true);
    setPlaylistError('');
    try {
      const res = await axios.get(`${API_BASE}/playlists`, { headers: { Authorization: `Bearer ${jwt}` } });
      if (res.data.playlists && res.data.playlists.length > 0) {
        setPlaylist(res.data.playlists[0]);
        // Fetch songs for this playlist
        const songsRes = await axios.get(`${API_BASE}/playlists/${res.data.playlists[0].id}/songs`, { headers: { Authorization: `Bearer ${jwt}` } });
        setPlaylistSongs(songsRes.data.songs);
      } else {
        setPlaylist(null);
        setPlaylistSongs([]);
      }
    } catch (err) {
      setPlaylistError('Failed to load playlist');
    } finally {
      setPlaylistLoading(false);
    }
  };

  // Add song to default playlist
  const handleAddSongToPlaylist = async (song) => {
    if (!jwt || !playlist) return;
    setPlaylistLoading(true);
    try {
      await axios.post(`${API_BASE}/playlists/${playlist.id}/songs`, { song_id: song.id, song_title: song.title }, { headers: { Authorization: `Bearer ${jwt}` } });
      showToast('Song added to playlist!');
      fetchPlaylistAndSongs();
    } catch (err) {
      setPlaylistError('Failed to add song');
    } finally {
      setPlaylistLoading(false);
    }
  };

  // Remove song from default playlist
  const handleRemoveSongFromPlaylist = async (songDbId) => {
    if (!jwt || !playlist) return;
    setPlaylistLoading(true);
    try {
      await axios.delete(`${API_BASE}/playlists/${playlist.id}/songs/${songDbId}`, { headers: { Authorization: `Bearer ${jwt}` } });
      fetchPlaylistAndSongs();
    } catch (err) {
      setPlaylistError('Failed to remove song');
    } finally {
      setPlaylistLoading(false);
    }
  };

  // Fetch playlist when sidebar opens
  useEffect(() => { 
    if (playlistSidebarOpen) {
      console.log('[DEBUG] useEffect: playlistSidebarOpen is true, calling fetchPlaylistAndSongs');
      fetchPlaylistAndSongs();
    }
  }, [playlistSidebarOpen, jwt]);

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

  // See more button should always show if there are any songs
  const canShowSeeMore = songs.length > 0;

  // --- UI Components ---
  // Auth Modal
  const AuthModal = () => (
    <div className="modal-backdrop" onClick={() => setAuthModalOpen(false)}>
      <div className="modal" onClick={e => e.stopPropagation()}>
        <h2>{authMode === 'login' ? 'Login' : 'Register'}</h2>
        <form onSubmit={handleAuth}>
          {authMode === 'register' && (
            <input name="username" placeholder="Username" required style={{ marginBottom: 8 }} />
          )}
          <input name="user_id" placeholder="User ID" required style={{ marginBottom: 8 }} />
          <input name="password" type="password" placeholder="Password" required style={{ marginBottom: 8 }} />
          <button type="submit" disabled={authLoading} style={{ width: '100%' }}>
            {authLoading ? 'Loading...' : (authMode === 'login' ? 'Login' : 'Register')}
          </button>
        </form>
        <div style={{ marginTop: 8, color: 'red' }}>{authError}</div>
        <div style={{ marginTop: 12 }}>
          {authMode === 'login' ? (
            <span>New user? <button onClick={() => { setAuthMode('register'); setAuthError(''); }} style={{ color: '#1db954', background: 'none', border: 'none', cursor: 'pointer' }}>Register</button></span>
          ) : (
            <span>Already have an account? <button onClick={() => { setAuthMode('login'); setAuthError(''); }} style={{ color: '#1db954', background: 'none', border: 'none', cursor: 'pointer' }}>Login</button></span>
          )}
        </div>
      </div>
    </div>
  );

  // Playlist Sidebar (function component, not affected by player state)
  const PlaylistSidebar = React.memo(function PlaylistSidebar({ open, onClose }) {
    return (
      <div
        className="playlist-sidebar-backdrop"
        style={{ display: open ? 'block' : 'none' }}
        onClick={onClose}
      >
        <div className="playlist-sidebar" onClick={e => e.stopPropagation()}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 }}>
            <h2 style={{ margin: 0 }}>My Playlist</h2>
            <button onClick={onClose} style={{ background: 'none', border: 'none', color: '#fff', fontSize: 22, cursor: 'pointer', marginLeft: 8 }} title="Close">✖</button>
          </div>
          {playlistError && <div style={{ color: 'red', marginBottom: 8 }}>{playlistError}</div>}
          {playlistLoading ? <div>Loading...</div> : !playlist ? <div>No playlist found</div> : (
            <>
              <h3 style={{margin: 0, fontSize: '1.1em', color: 'var(--spotify-green)'}}>{playlist.name}</h3>
              <div style={{ maxHeight: 320, overflowY: 'auto', marginTop: 10 }}>
                {playlistSongs.length === 0 ? <div style={{color:'#aaa'}}>No songs</div> : playlistSongs.map(song => {
                  // Try to find full song info from main song list for better display
                  const fullSong = songs.find(s => s.id === song.song_id);
                  return (
                    <div key={song.id} style={{
                      display: 'flex', alignItems: 'center', gap: 10, padding: '8px 0', borderBottom: '1px solid #232323', cursor: 'pointer', background: currentSong?.id === song.song_id ? 'var(--spotify-green)' : 'transparent', color: currentSong?.id === song.song_id ? '#fff' : 'var(--text-main)'
                    }}
                      onClick={e => {
                        // Only play song, do not close sidebar
                        e.stopPropagation();
                        const idx = songs.findIndex(s => s.id === song.song_id);
                        if (idx !== -1) {
                          setCurrentSong(songs[idx]);
                          setCurrentIndex(idx);
                          setIsPlaying(true);
                        }
                      }}
                    >
                      {/* Thumbnail */}
                      {fullSong && fullSong.thumbnail ? (
                        <img src={fullSong.thumbnail} alt="thumb" style={{ width: 32, height: 32, borderRadius: 6, objectFit: 'cover', boxShadow: '0 1px 4px rgba(0,0,0,0.10)' }} />
                      ) : (
                        <span style={{ width: 32, height: 32, display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 18, background: '#232323', borderRadius: 6, color: '#1db954' }}>🎵</span>
                      )}
                      <div style={{ flex: 1, minWidth: 0 }}>
                        <div style={{ fontWeight: 600, fontSize: '1em', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>{song.song_title}</div>
                        <div style={{ fontSize: '0.92em', color: currentSong?.id === song.song_id ? '#e0ffe0' : '#b3b3b3', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>{fullSong?.artist || ''}</div>
                      </div>
                      <div style={{ fontSize: '0.9em', color: currentSong?.id === song.song_id ? '#e0ffe0' : '#b3b3b3', minWidth: 40, textAlign: 'right' }}>{fullSong?.duration ? formatTime(fullSong.duration) : ''}</div>
                      <button onClick={e => { e.stopPropagation(); handleRemoveSongFromPlaylist(song.id); }} style={{ color: '#fff', background: '#e74c3c', border: 'none', borderRadius: 6, padding: '4px 10px', marginLeft: 8, cursor: 'pointer', fontWeight: 700, fontSize: 16 }} title="Remove from playlist">−</button>
                    </div>
                  );
                })}
              </div>
            </>
          )}
          <button onClick={handleLogout} style={{ marginTop: 16, width: '100%', background: '#232323', color: '#fff', border: 'none', borderRadius: 8, padding: '10px 0', fontWeight: 600, fontSize: 15, cursor: 'pointer' }}>Logout</button>
        </div>
      </div>
    );
  });

  return (
    <div style={styles.container}>
      <div style={styles.content}>
        {/* Header */}
        <header style={styles.header}>
          <h1 style={styles.title}>🎵 DhoonHub</h1>
          <div style={{ position: 'absolute', top: 20, right: 20, display: 'flex', gap: 12 }}>
            <button onClick={toggleTheme} style={{ border: 'none', background: 'none', color: 'var(--text-main)', fontSize: 18, cursor: 'pointer' }}>{getThemeLabel()}</button>
            {user ? (
              <button onClick={() => setPlaylistSidebarOpen(true)} style={{ border: 'none', background: '#1db954', color: '#fff', borderRadius: 20, padding: '8px 18px', fontWeight: 600, fontSize: 16, cursor: 'pointer' }}>My Playlists</button>
            ) : (
              <button onClick={() => { setAuthModalOpen(true); setAuthMode('login'); }} style={{ border: 'none', background: '#1db954', color: '#fff', borderRadius: 20, padding: '8px 18px', fontWeight: 600, fontSize: 16, cursor: 'pointer' }}>Login / Register</button>
            )}
          </div>
          <p style={{ ...styles.subtitle, color: 'var(--text-secondary)' }}>
            Play, search & enjoy your favorite tracks in one click
          </p>
        </header>
        {authModalOpen && <AuthModal />}
        <PlaylistSidebar open={playlistSidebarOpen} onClose={() => setPlaylistSidebarOpen(false)} />
        {/* No add-to-playlist modal needed for single playlist */}
        {toast && <div className="toast">{toast}</div>}

        {/* Search */}
        <div style={styles.searchContainer}>
          <form onSubmit={handleSearch} style={{ 
            display: 'flex', 
            gap: '10px', 
            width: '100%', 
            maxWidth: '500px' // Responsive form width
          }} autoComplete="off">
            <div style={{ position: 'relative', width: '100%', display: 'flex', alignItems: 'center' }}>
              <input
                type="text"
                value={searchQuery}
                onChange={(e) => {
                  setSearchQuery(e.target.value);
                  setShowSuggestions(!!e.target.value);
                  setActiveSuggestion(-1);
                }}
                placeholder="Search for music..."
                style={{
                  ...styles.searchInput,
                  paddingLeft: '38px',
                  background: 'var(--bg-card)',
                  color: 'var(--text-main)',
                  border: 'none',
                  boxShadow: '0 2px 8px rgba(0,0,0,0.12)',
                  marginRight: 0,
                  flex: 1,
                  minWidth: 0
                }}
                disabled={isLoading}
                ref={searchInputRef}
                onFocus={() => searchQuery && setShowSuggestions(true)}
                onBlur={handleInputBlur}
              />
              {/* Search icon inside input */}
              <span style={{
                position: 'absolute',
                left: 12,
                top: '50%',
                transform: 'translateY(-50%)',
                color: 'var(--spotify-green)',
                fontSize: 20,
                pointerEvents: 'none',
                opacity: 0.85
              }}>🔍</span>
              {/* Suggestions dropdown */}
              {showSuggestions && suggestions.length > 0 && (
                <div style={{
                  position: 'absolute',
                  top: '110%',
                  left: 0,
                  right: 0,
                  background: '#181818',
                  border: '1px solid #282828',
                  borderTop: 'none',
                  zIndex: 10,
                  maxHeight: 260,
                  overflowY: 'auto',
                  borderRadius: '0 0 16px 16px',
                  boxShadow: '0 8px 32px rgba(0,0,0,0.35)',
                  padding: '4px 0',
                  animation: 'fadeIn 0.2s',
                }}>
                  {suggestions.map((s, i) => (
                    <div
                      key={s.id || s.url || i}
                      style={{
                        padding: '12px 20px',
                        cursor: 'pointer',
                        background: i === activeSuggestion ? 'var(--spotify-green)' : 'transparent',
                        color: i === activeSuggestion ? '#fff' : '#fff',
                        fontWeight: i === activeSuggestion ? 600 : 400,
                        display: 'flex',
                        flexDirection: 'column',
                        borderBottom: i !== suggestions.length - 1 ? '1px solid #232323' : 'none',
                        transition: 'background 0.15s',
                        borderRadius: i === suggestions.length - 1 ? '0 0 16px 16px' : 0
                      }}
                      onMouseDown={() => {
                        setSearchQuery(''); // Clear input so dropdown does not reappear
                        setShowSuggestions(false);
                        setActiveSuggestion(-1);
                        setSuggestions([]); // Hide dropdown until new input
                        setCurrentSong(s);
                        setCurrentIndex(0);
                        setIsPlaying(true);
                      }}
                      onMouseEnter={() => setActiveSuggestion(i)}
                    >
                      <div style={{ fontWeight: 500, fontSize: '1.05em' }}>{s.title}</div>
                      <div style={{ fontSize: '0.92em', color: i === activeSuggestion ? '#e0ffe0' : '#b3b3b3', marginTop: 2 }}>{s.artist || ''}</div>
                    </div>
                  ))}
                </div>
              )}
            </div>
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
              {/* Album Art with image */}
              <div style={styles.albumArt}>
                {!(currentSong.thumbnail) && '🎵'}
              </div>

              {/* Song Info with better metadata display */}
              <h3 style={styles.songTitle}>{currentSong.title || 'Unknown Title'}</h3>
              <div style={styles.artistName}>{currentSong.artist || 'Unknown Artist'}</div>

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
                <div className="time-display" style={{ 
                  minWidth: 'clamp(60px, 15vw, 80px)', // Responsive width
                  display: 'flex', 
                  justifyContent: 'space-between', 
                  fontSize: 'clamp(12px, 3vw, 14px)', // Responsive font size
                  marginLeft: 'clamp(4px, 2vw, 8px)' // Responsive margin
                }}>
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
                    backgroundColor: isShuffled ? '#10b981' : styles.controlButton.backgroundColor
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
                    backgroundColor: repeatMode !== 'none' ? '#10b981' : styles.controlButton.backgroundColor
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
                <span style={{ fontSize: 'clamp(0.8rem, 2vw, 0.9rem)', minWidth: '40px' }}>
                  {Math.round((isMuted ? 0 : volume) * 100)}%
                </span>
              </div>

              {/* Source Info */}
              <div style={{ 
                fontSize: 'clamp(0.7rem, 2vw, 0.8rem)', // Responsive font size
                color: 'var(--text-secondary)', 
                textAlign: 'center', 
                marginTop: '15px',
                opacity: 0.8
              }}>
                Source: {currentSong.source === 'jiosaavn' ? '🎵 ' : currentSong.source === 'api' ? '🌐 Internet Archive' : '📁 Local File'}
                {currentSong.album && currentSong.album !== 'Unknown Album' && ` • ${currentSong.album}`}
                {currentSong.year && ` • ${currentSong.year}`}
              </div>

              <audio
                ref={audioRef}
                src={currentSong?.url}
                preload="metadata"
                onError={(e) => {
                  console.error('Audio error:', e);
                  console.log('Failed URL:', currentSong.url);
                }}
                onPlay={() => setIsPlaying(true)}
                onPause={() => setIsPlaying(false)}
                onTimeUpdate={e => setCurrentTime(e.target.currentTime)}
                onLoadedMetadata={e => setDuration(e.target.duration)}
                onEnded={() => {
                  if (repeatMode === 'one') {
                    audioRef.current.currentTime = 0;
                    audioRef.current.play();
                  } else if (isShuffled) {
                    const nextIndex = Math.floor(Math.random() * songs.length);
                    if (nextIndex !== currentIndex) setCurrentIndex(nextIndex);
                  } else if (currentIndex + 1 < songs.length) {
                    setCurrentIndex(currentIndex + 1);
                  } else {
                    setIsPlaying(false);
                  }
                }}
              />
            </>
          ) : (
            <div style={{ textAlign: 'center', padding: '50px' }}>
              <p>No song selected</p>
            </div>
          )}
        </div>

        {/* Song List - ENLARGED by removing keyboard shortcuts */}
        <div style={styles.songList}>
          <div style={styles.songListHeader}>
            <h2 style={{ margin: '0', fontSize: 'clamp(1.1rem, 4vw, 1.3rem)' }}>Songs ({songs.length})</h2>
          </div>
          
          <div
            style={{ flex: 1, overflowY: 'auto' }}
            ref={songListRef}
            onScroll={e => {
              const el = e.target;
              if (el.scrollHeight - el.scrollTop - el.clientHeight < 80 && !isLoading) {
                if (mode === 'local' && hasMore) {
                  fetchMoreRandom();
                } else if (mode === 'local' && !hasMore) {
                  setMode('online');
                  fetchMoreOnline(true, 1);
                } else {
                  fetchMoreOnline(false, onlinePage + 1);
                }
              }
            }}
          >
            {songs.length === 0 && !isLoading ? (
              <div style={{ padding: '40px', textAlign: 'center' }}>
                <p>No songs found</p>
              </div>
            ) : (
              <>
                {songs.map((song, index) => {
                  // Check if song is in playlist
                  const inPlaylist = playlistSongs.some(ps => ps.song_id === song.id);
                  return (
                    <div
                      key={song.id}
                      onClick={() => handleSongSelect(song, index)}
                      className={`song-list-item${currentSong?.id === song.id ? ' active' : ''}`}
                      style={styles.songItem}
                    >
                      <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                        {/* Show thumbnail if available, else music note */}
                        {song.thumbnail ? (
                          <img src={song.thumbnail} alt="thumb" style={{ width: 38, height: 38, borderRadius: 8, objectFit: 'cover', boxShadow: '0 2px 8px rgba(0,0,0,0.12)' }} />
                        ) : (
                          <span style={{ width: 38, height: 38, display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 24, background: '#232323', borderRadius: 8, color: '#1db954' }}>🎵</span>
                        )}
                        <div>
                          <div style={{ 
                            fontWeight: 'bold', 
                            marginBottom: '5px',
                            fontSize: 'clamp(0.9rem, 3vw, 1rem)',
                            display: 'flex', alignItems: 'center', gap: 8
                          }}>
                            <span>{song.title || 'Unknown Title'}</span>
                            {user && playlist && (
                              inPlaylist ? (
                                <button
                                  className="add-to-playlist-btn"
                                  title="Remove from Playlist"
                                  onClick={e => { e.stopPropagation();
                                    // Find the playlistSong id
                                    const ps = playlistSongs.find(ps => ps.song_id === song.id);
                                    if (ps) handleRemoveSongFromPlaylist(ps.id);
                                  }}
                                  style={{ color: '#fff', background: '#e74c3c', border: 'none', borderRadius: 6, padding: '2px 8px', marginLeft: 4, cursor: 'pointer', fontWeight: 700, fontSize: 15 }}
                                >
                                  −
                                </button>
                              ) : (
                                <button
                                  className="add-to-playlist-btn"
                                  title="Add to Playlist"
                                  onClick={e => { e.stopPropagation(); handleAddSongToPlaylist(song); }}
                                  style={{ color: '#fff', background: '#1db954', border: 'none', borderRadius: 6, padding: '2px 8px', marginLeft: 4, cursor: 'pointer', fontWeight: 700, fontSize: 15 }}
                                >
                                  ➕
                                </button>
                              )
                            )}
                          </div>
                          <div style={{ fontSize: 'clamp(0.8rem, 2.5vw, 0.9rem)' }}>
                            {song.artist || 'Unknown Artist'}
                          </div>
                        </div>
                      </div>
                      <div style={{ textAlign: 'right', fontSize: 'clamp(0.7rem, 2vw, 0.8rem)' }}>
                        <div>{song.source === 'jiosaavn' ? '🎵' : song.source === 'api' ? '🌐' : '📁'}</div>
                        {song.duration && <div>{formatTime(song.duration)}</div>}
                      </div>
                    </div>
                  );
                })}
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
            <div style={{ padding: '10px', textAlign: 'center', flexShrink: 0 }}>
              <button
                style={{
                  padding: 'clamp(8px, 2vw, 10px) clamp(20px, 5vw, 28px)', // Responsive padding
                  borderRadius: 20,
                  border: 'none',
                  background: 'var(--spotify-green)',
                  color: '#fff',
                  fontWeight: 600,
                  fontSize: 'clamp(14px, 3vw, 16px)', // Responsive font size
                  cursor: 'pointer',
                  boxShadow: '0 2px 8px rgba(0,0,0,0.08)'
                }}
                onClick={() => {
                  if (mode === 'local' && hasMore) {
                    // This button now only loads more local songs
                    loadMoreSongsAutomatically();
                  } else if (mode === 'local' && !hasMore) {
                    // Switch to online mode
                    setMode('online');
                    fetchMoreOnlineEnhanced(true, 1);
                  } else {
                    fetchMoreOnlineEnhanced(false, onlinePage + 1);
                  }
                }}
              >
                See more songs
              </button>
            </div>
          )}
        </div>

        {/* REMOVED: Keyboard Shortcuts section to save space */}

        {/* Footer */}
        <footer style={{ 
          marginTop: '30px', 
          textAlign: 'center', 
          fontSize: 'clamp(0.8rem, 2vw, 0.9rem)', // Responsive font size
          color: 'var(--text-secondary)',
          opacity: 0.8
        }}>
          <p>
            🎵 M.S Music 🎵
          </p>
        </footer>
      </div>
    </div>
  );
}

export default App;