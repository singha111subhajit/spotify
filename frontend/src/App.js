import React, { useState, useEffect } from 'react';
import axios from 'axios';

function App() {
  const [songs, setSongs] = useState([]);
  const [currentSong, setCurrentSong] = useState(null);
  const [isPlaying, setIsPlaying] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState(null);

  const audioRef = React.useRef(null);

  // Initialize app with songs from Flask backend
  useEffect(() => {
    const initializeSongs = async () => {
      setIsLoading(true);
      try {
        // Check backend health first
        await axios.get('/api/health');
        
        // Load initial songs (static + popular API songs)
        const response = await axios.get('/api/songs?per_page=20');
        setSongs(response.data.songs);
        
        // Get and set a random song to start with
        try {
          const randomResponse = await axios.get('/api/random');
          setCurrentSong(randomResponse.data);
        } catch (randomError) {
          console.warn('Could not get random song:', randomError);
          if (response.data.songs.length > 0) {
            setCurrentSong(response.data.songs[0]);
          }
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

  const handleSearch = async (e) => {
    e.preventDefault();
    if (!searchQuery.trim()) return;
    
    setIsLoading(true);
    setError(null);
    
    try {
      const response = await axios.get(`/api/search?q=${encodeURIComponent(searchQuery)}&per_page=50`);
      setSongs(response.data.songs);
      
      // Auto-play first result if available
      if (response.data.songs.length > 0) {
        setCurrentSong(response.data.songs[0]);
      }
    } catch (error) {
      console.error('Search error:', error);
      setError('Search failed. Please try again.');
    } finally {
      setIsLoading(false);
    }
  };

  const handleSongSelect = (song) => {
    setCurrentSong(song);
    setIsPlaying(true);
  };

  const togglePlayPause = () => {
    if (!currentSong) return;
    
    if (isPlaying) {
      audioRef.current?.pause();
    } else {
      audioRef.current?.play();
    }
    setIsPlaying(!isPlaying);
  };

  const formatDuration = (seconds) => {
    if (!seconds) return '';
    const mins = Math.floor(seconds / 60);
    const secs = Math.floor(seconds % 60);
    return `${mins}:${secs.toString().padStart(2, '0')}`;
  };

  if (error) {
    return (
      <div style={{ padding: '20px', textAlign: 'center' }}>
        <h1>⚠️ Connection Error</h1>
        <p>{error}</p>
        <button onClick={() => window.location.reload()}>Retry</button>
      </div>
    );
  }

  return (
    <div style={{ padding: '20px', maxWidth: '1200px', margin: '0 auto' }}>
      {/* Header */}
      <header style={{ marginBottom: '30px', textAlign: 'center' }}>
        <h1 style={{ fontSize: '3em', margin: '0' }}>🎵 Enhanced Music Player</h1>
        <p style={{ color: '#9ca3af', margin: '10px 0' }}>
          Powered by Flask backend with Internet Archive integration
        </p>
      </header>

      {/* Search */}
      <div style={{ marginBottom: '30px', textAlign: 'center' }}>
        <form onSubmit={handleSearch} style={{ display: 'inline-flex', gap: '10px' }}>
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Search for songs from Internet Archive..."
            style={{
              padding: '12px 16px',
              fontSize: '16px',
              border: '2px solid #4b5563',
              borderRadius: '8px',
              backgroundColor: '#374151',
              color: 'white',
              width: '400px'
            }}
            disabled={isLoading}
          />
          <button
            type="submit"
            style={{
              padding: '12px 24px',
              fontSize: '16px',
              backgroundColor: '#10b981',
              color: 'white',
              border: 'none',
              borderRadius: '8px',
              cursor: 'pointer'
            }}
            disabled={isLoading}
          >
            {isLoading ? 'Searching...' : 'Search'}
          </button>
        </form>
      </div>

      {/* Music Player */}
      <div style={{ 
        backgroundColor: '#374151', 
        padding: '20px', 
        borderRadius: '12px', 
        marginBottom: '30px',
        textAlign: 'center'
      }}>
        {currentSong ? (
          <>
            <h3 style={{ margin: '0 0 10px 0' }}>{currentSong.title}</h3>
            <p style={{ color: '#9ca3af', margin: '0 0 20px 0' }}>{currentSong.artist}</p>
            
            <div style={{ marginBottom: '20px' }}>
              <button
                onClick={togglePlayPause}
                style={{
                  padding: '15px 30px',
                  fontSize: '18px',
                  backgroundColor: '#10b981',
                  color: 'white',
                  border: 'none',
                  borderRadius: '8px',
                  cursor: 'pointer'
                }}
                disabled={isLoading}
              >
                {isPlaying ? '⏸️ Pause' : '▶️ Play'}
              </button>
            </div>

            <div style={{ fontSize: '12px', color: '#6b7280' }}>
              Source: {currentSong.source === 'api' ? '🌐 Internet Archive' : '📁 Local File'}
              {currentSong.duration && ` • Duration: ${formatDuration(currentSong.duration)}`}
            </div>

            <audio
              ref={audioRef}
              src={currentSong.url}
              onPlay={() => setIsPlaying(true)}
              onPause={() => setIsPlaying(false)}
              crossOrigin="anonymous"
            />
          </>
        ) : (
          <p>No song selected</p>
        )}
      </div>

      {/* Song List */}
      <div style={{ backgroundColor: '#374151', borderRadius: '12px', overflow: 'hidden' }}>
        <div style={{ padding: '15px', backgroundColor: '#4b5563', borderBottom: '1px solid #6b7280' }}>
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
                onClick={() => handleSongSelect(song)}
                style={{
                  padding: '15px',
                  borderBottom: '1px solid #4b5563',
                  cursor: 'pointer',
                  backgroundColor: currentSong?.id === song.id ? '#065f46' : 'transparent',
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center'
                }}
                onMouseEnter={(e) => {
                  if (currentSong?.id !== song.id) {
                    e.target.style.backgroundColor = '#4b5563';
                  }
                }}
                onMouseLeave={(e) => {
                  if (currentSong?.id !== song.id) {
                    e.target.style.backgroundColor = 'transparent';
                  }
                }}
              >
                <div>
                  <div style={{ fontWeight: 'bold', color: currentSong?.id === song.id ? '#10b981' : 'white' }}>
                    {song.title}
                  </div>
                  <div style={{ color: '#9ca3af', fontSize: '14px' }}>
                    {song.artist}
                  </div>
                </div>
                <div style={{ textAlign: 'right', fontSize: '12px', color: '#6b7280' }}>
                  <div>{song.source === 'api' ? '🌐' : '📁'}</div>
                  {song.duration && <div>{formatDuration(song.duration)}</div>}
                </div>
              </div>
            ))
          )}
        </div>
      </div>

      {/* Footer */}
      <footer style={{ marginTop: '30px', textAlign: 'center', fontSize: '14px', color: '#6b7280' }}>
        <p>
          🔗 Backend: <span style={{ color: '#10b981' }}>Flask API</span>
          {' '} | Frontend: <span style={{ color: '#3b82f6' }}>React</span>
          {' '} | Music from{' '}
          <a 
            href="https://archive.org" 
            target="_blank" 
            rel="noopener noreferrer"
            style={{ color: '#8b5cf6' }}
          >
            Internet Archive
          </a>
        </p>
      </footer>
    </div>
  );
}

export default App;