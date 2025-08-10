

import React from 'react';

function Playlist({
  playlistSidebarOpen,
  setPlaylistSidebarOpen,
  playlist,
  playlistSongs,
  playlistLoading,
  playlistError,
  handlePlaylistSongSelect,
  isPlaylistMode,
  playlistPlayIndex,
  setPlaylistPlayIndex,
  showToast,
  songs = [], // Pass songs from App.js for full details
}) {
  // Helper to format time
  const formatTime = (seconds) => {
    if (!seconds || isNaN(seconds)) return '';
    const mins = Math.floor(seconds / 60);
    const secs = Math.floor(seconds % 60);
    return `${mins}:${secs.toString().padStart(2, '0')}`;
  };

  return (
    <div className="playlist-sidebar-backdrop" onClick={() => setPlaylistSidebarOpen(false)}>
      <div className="playlist-sidebar" onClick={e => e.stopPropagation()}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 }}>
          <h2 style={{ margin: 0 }}>My Playlist</h2>
          <button onClick={() => setPlaylistSidebarOpen(false)} style={{ background: 'none', border: 'none', color: '#fff', fontSize: 22, cursor: 'pointer', marginLeft: 8 }} title="Close">✖</button>
        </div>
        {playlistError && <div style={{ color: 'red', marginBottom: 8 }}>{playlistError}</div>}
        {playlistLoading ? <div>Loading...</div> : !playlist ? <div>No playlist found</div> : (
          <>
            <h3 style={{margin: 0, fontSize: '1.1em', color: 'var(--spotify-green)'}}>{playlist.name}</h3>
            <div style={{ maxHeight: 320, overflowY: 'auto', marginTop: 10 }}>
              {playlistSongs.length === 0 ? <div style={{color:'#aaa'}}>No songs</div> : playlistSongs.map((song, index) => {
                // Find full song info from main song list
                const fullSong = songs.find(s => s.id === song.song_id);
                const displaySong = fullSong || song;
                return (
                  <div key={song.id} style={{
                    display: 'flex', alignItems: 'center', gap: 10, padding: '8px 0', borderBottom: '1px solid #232323', cursor: 'pointer', background: isPlaylistMode && playlistPlayIndex === index ? 'var(--spotify-green)' : 'transparent', color: isPlaylistMode && playlistPlayIndex === index ? '#fff' : 'var(--text-main)'
                  }}
                    onClick={() => handlePlaylistSongSelect(displaySong, index)}
                  >
                    {/* Thumbnail */}
                    {displaySong.thumbnail ? (
                      <img src={displaySong.thumbnail} alt="thumb" style={{ width: 32, height: 32, borderRadius: 6, objectFit: 'cover', boxShadow: '0 1px 4px rgba(0,0,0,0.10)' }} />
                    ) : (
                      <span style={{ width: 32, height: 32, display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 18, background: '#232323', borderRadius: 6, color: '#1db954' }}>🎵</span>
                    )}
                    <div style={{ flex: 1, minWidth: 0 }}>
                      <div style={{ fontWeight: 600, fontSize: '1em', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>{displaySong.title || displaySong.song_title || 'Unknown Title'}</div>
                      <div style={{ fontSize: '0.92em', color: isPlaylistMode && playlistPlayIndex === index ? '#e0ffe0' : '#b3b3b3', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>{displaySong.artist || ''}</div>
                    </div>
                    <div style={{ fontSize: '0.9em', color: isPlaylistMode && playlistPlayIndex === index ? '#e0ffe0' : '#b3b3b3', minWidth: 40, textAlign: 'right' }}>{displaySong.duration ? formatTime(displaySong.duration) : ''}</div>
                  </div>
                );
              })}
            </div>
          </>
        )}
      </div>
    </div>
  );
}

export default Playlist;
