// AudioPlayer.js
import React, { useState, useEffect } from 'react';

function AudioPlayer({
  currentSong,
  isPlaying,
  togglePlayPause,
  handleNext,
  handlePrevious,
  handleSeek, // Will be replaced by local handler
  toggleMute,
  toggleShuffle,
  toggleRepeat,
  isShuffled,
  repeatMode,
  isMuted,
  volume,
  setVolume,
  formatTime, // Can keep as prop or move local
  styles,
  audioRef,
}) {
  // Local state for progress
  const [currentTime, setCurrentTime] = useState(0);
  const [duration, setDuration] = useState(0);

  useEffect(() => {
    const audio = audioRef.current;
    if (!audio) return;
    const updateTime = () => setCurrentTime(audio.currentTime);
    const updateDuration = () => setDuration(audio.duration);
    const onEnded = () => handleNext && handleNext();
    const onPlay = () => setCurrentTime(audio.currentTime);
    const onPause = () => {};
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
  }, [audioRef, handleNext, currentSong]);

  useEffect(() => {
    // Reset progress when song changes
    setCurrentTime(0);
    setDuration(0);
  }, [currentSong]);

  // Local progress percent
  const getProgressPercent = () => duration ? (currentTime / duration) * 100 : 0;

  // Local seek handler
  const handleLocalSeek = (e) => {
    if (!audioRef.current || !duration) return;
    const rect = e.currentTarget.getBoundingClientRect();
    const percent = (e.clientX - rect.left) / rect.width;
    const newTime = percent * duration;
    audioRef.current.currentTime = newTime;
    setCurrentTime(newTime);
  };

  // Local formatTime if not passed as prop
  const localFormatTime = (seconds) => {
    if (!seconds || isNaN(seconds)) return '0:00';
    const mins = Math.floor(seconds / 60);
    const secs = Math.floor(seconds % 60);
    return `${mins}:${secs.toString().padStart(2, '0')}`;
  };

  if (!currentSong) {
    return (
      <div style={{ textAlign: 'center', padding: '50px' }}>
        <p>No song selected</p>
      </div>
    );
  }

  return (
    <>
      {/* Album Art */}
      <div style={styles.albumArt}>
        {!(currentSong.thumbnail) && '🎵'}
      </div>

      {/* Song Info */}
      <h3 style={styles.songTitle}>{currentSong.title || 'Unknown Title'}</h3>
      <div style={styles.artistName}>{currentSong.artist || 'Unknown Artist'}</div>

      {/* Progress Bar */}
      <div className="progress-container" onClick={handleLocalSeek} style={{ cursor: 'pointer', width: '100%' }}>
        <div className="equalizer" style={{ opacity: isPlaying ? 1 : 0.3, transition: 'opacity 0.3s' }}>
          {[...Array(5)].map((_, i) => (
            <div key={i} className="eq-bar" style={{ animationPlayState: isPlaying ? 'running' : 'paused' }} />
          ))}
        </div>
        <div className="progress">
          <div className="progress-bar" style={{ width: `${getProgressPercent()}%` }} />
        </div>
        <div className="time-display" style={{
          minWidth: 'clamp(60px, 15vw, 80px)',
          display: 'flex',
          justifyContent: 'space-between',
          fontSize: 'clamp(12px, 3vw, 14px)',
          marginLeft: 'clamp(4px, 2vw, 8px)'
        }}>
          <span>{formatTime ? formatTime(currentTime) : localFormatTime(currentTime)}</span>
          <span>{formatTime ? formatTime(duration) : localFormatTime(duration)}</span>
        </div>
      </div>

      {/* Controls */}
      <div style={styles.controls}>
        <button onClick={toggleShuffle} style={{
          ...styles.controlButton,
          backgroundColor: isShuffled ? '#10b981' : styles.controlButton.backgroundColor
        }} title="Shuffle (S)">🔀</button>

        <button onClick={handlePrevious} style={styles.controlButton} title="Previous (←)">⏮️</button>

        <button onClick={togglePlayPause} style={{ ...styles.controlButton, ...styles.playButton }} title="Play/Pause (Space)">
          {isPlaying ? '⏸️' : '▶️'}
        </button>

        <button onClick={handleNext} style={styles.controlButton} title="Next (→)">⏭️</button>

        <button onClick={toggleRepeat} style={{
          ...styles.controlButton,
          backgroundColor: repeatMode !== 'none' ? '#10b981' : styles.controlButton.backgroundColor
        }} title="Repeat (R)">
          {repeatMode === 'one' ? '🔂' : '🔁'}
        </button>
      </div>

      {/* Volume */}
      <div style={styles.volumeContainer}>
        <button onClick={toggleMute} style={styles.controlButton} title="Mute (M)">
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
        fontSize: 'clamp(0.7rem, 2vw, 0.8rem)',
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
        key={currentSong?.id || currentSong?.url}
        ref={audioRef}
        src={currentSong.url}
        preload="metadata"
        onError={(e) => {
          console.error('Audio error:', e);
          console.log('Failed URL:', currentSong.url);
        }}
      />
    </>
  );
}

export default AudioPlayer;
