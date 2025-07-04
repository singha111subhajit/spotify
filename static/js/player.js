// Music Player JavaScript
class MusicPlayer {
    constructor() {
        this.songs = [];
        this.currentSongIndex = -1;
        this.isPlaying = false;
        this.audio = document.getElementById('audioPlayer');
        this.volume = 0.7;
        
        this.initializeElements();
        this.bindEvents();
        this.loadSongs();
        this.setupAudioEvents();
        
        // Set initial volume
        this.audio.volume = this.volume;
    }
    
    initializeElements() {
        // Control elements
        this.playPauseBtn = document.getElementById('playPauseBtn');
        this.prevBtn = document.getElementById('prevBtn');
        this.nextBtn = document.getElementById('nextBtn');
        this.volumeSlider = document.getElementById('volumeSlider');
        
        // Display elements
        this.currentTitle = document.getElementById('currentTitle');
        this.currentArtist = document.getElementById('currentArtist');
        this.miniTitle = document.getElementById('miniTitle');
        this.miniArtist = document.getElementById('miniArtist');
        this.albumIcon = document.getElementById('albumIcon');
        this.albumCover = document.getElementById('albumCover');
        this.miniAlbumIcon = document.getElementById('miniAlbumIcon');
        
        // Progress elements
        this.progressBar = document.getElementById('progressBar');
        this.currentTime = document.getElementById('currentTime');
        this.totalTime = document.getElementById('totalTime');
        
        // Other elements
        this.songList = document.getElementById('songList');
        this.loadingSpinner = document.getElementById('loadingSpinner');
    }
    
    bindEvents() {
        // Control button events
        this.playPauseBtn.addEventListener('click', () => this.togglePlayPause());
        this.prevBtn.addEventListener('click', () => this.previousSong());
        this.nextBtn.addEventListener('click', () => this.nextSong());
        
        // Volume control
        this.volumeSlider.addEventListener('input', (e) => {
            this.volume = e.target.value / 100;
            this.audio.volume = this.volume;
        });
        
        // Progress bar click
        document.querySelector('.progress').addEventListener('click', (e) => {
            const progressContainer = e.currentTarget;
            const clickX = e.offsetX;
            const width = progressContainer.offsetWidth;
            const percentage = clickX / width;
            
            if (this.audio.duration) {
                this.audio.currentTime = percentage * this.audio.duration;
            }
        });
        
        // Keyboard shortcuts
        document.addEventListener('keydown', (e) => {
            switch(e.code) {
                case 'Space':
                    e.preventDefault();
                    this.togglePlayPause();
                    break;
                case 'ArrowLeft':
                    e.preventDefault();
                    this.previousSong();
                    break;
                case 'ArrowRight':
                    e.preventDefault();
                    this.nextSong();
                    break;
            }
        });
    }
    
    setupAudioEvents() {
        // Audio element events
        this.audio.addEventListener('timeupdate', () => this.updateProgress());
        this.audio.addEventListener('loadedmetadata', () => this.updateDuration());
        this.audio.addEventListener('ended', () => this.nextSong());
        this.audio.addEventListener('play', () => this.onPlay());
        this.audio.addEventListener('pause', () => this.onPause());
        this.audio.addEventListener('error', (e) => this.onError(e));
        this.audio.addEventListener('loadstart', () => this.showLoading());
        this.audio.addEventListener('canplaythrough', () => this.hideLoading());
    }
    
    async loadSongs() {
        try {
            this.showLoading();
            const response = await fetch('/api/songs');
            
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            
            this.songs = await response.json();
            this.renderSongList();
            this.setupSearch(); 
            this.hideLoading();
            
            console.log('Songs loaded:', this.songs);
        } catch (error) {
            console.error('Error loading songs:', error);
            this.hideLoading();
            this.showError('Failed to load songs. Please refresh the page.');
        }
    }
    
    renderSongList(songsToRender = this.songs) {
    if (!songsToRender || songsToRender.length === 0) {
        this.songList.innerHTML = '<div class="text-muted text-center p-3">No songs found</div>';
        return;
    }

    this.songList.innerHTML = songsToRender.map((song, index) => `
        <div class="song-item" data-index="${index}" data-song-id="${song.id}">
            <div class="d-flex align-items-center">
                <div class="flex-grow-1">
                    <div class="song-title">${this.escapeHtml(song.title)}</div>
                    <div class="song-artist">${this.escapeHtml(song.artist)}</div>
                </div>
                <div class="song-duration ms-2">${song.duration}</div>
            </div>
        </div>
    `).join('');

    // Add click events to song items
    this.songList.querySelectorAll('.song-item').forEach(item => {
        item.addEventListener('click', (e) => {
            const index = parseInt(e.currentTarget.dataset.index);
            this.playSong(index);
        });
    });
}

    
    playSong(index) {
        if (index < 0 || index >= this.songs.length) {
            console.error('Invalid song index:', index);
            return;
        }
        
        const song = this.songs[index];
        
        // Update current song index
        this.currentSongIndex = index;
        
        // Update audio source
        this.audio.src = `/songs/${song.filename}`;
        
        // Update UI
        this.updateSongInfo(song);
        this.updateActiveItem();
        this.enableControls();
        
        // Play the audio
        this.audio.play().catch(error => {
            console.error('Error playing audio:', error);
            this.showError('Failed to play this song. The file might be missing or corrupted.');
        });
        
        console.log('Playing song:', song.title);
    }
    
    togglePlayPause() {
        if (this.currentSongIndex === -1) {
            // No song selected, play first song
            if (this.songs.length > 0) {
                this.playSong(0);
            }
            return;
        }
        
        if (this.isPlaying) {
            this.audio.pause();
        } else {
            this.audio.play().catch(error => {
                console.error('Error playing audio:', error);
                this.showError('Failed to play audio.');
            });
        }
    }
    
    previousSong() {
        if (this.songs.length === 0) return;
        
        let newIndex = this.currentSongIndex - 1;
        if (newIndex < 0) {
            newIndex = this.songs.length - 1; // Loop to last song
        }
        
        this.playSong(newIndex);
    }
    
    nextSong() {
        if (this.songs.length === 0) return;
        
        let newIndex = this.currentSongIndex + 1;
        if (newIndex >= this.songs.length) {
            newIndex = 0; // Loop to first song
        }
        
        this.playSong(newIndex);
    }
    
    updateSongInfo(song) {
        // Update main display
        this.currentTitle.textContent = song.title;
        this.currentArtist.textContent = song.artist;
        
        // Update mini player
        this.miniTitle.textContent = song.title;
        this.miniArtist.textContent = song.artist;
        
        // Update page title
        document.title = `${song.title} - ${song.artist} | Music Player`;
    }
    
    updateActiveItem() {
        // Remove active class from all items
        this.songList.querySelectorAll('.song-item').forEach(item => {
            item.classList.remove('active', 'playing');
        });
        
        // Add active class to current item
        if (this.currentSongIndex >= 0) {
            const activeItem = this.songList.querySelector(`[data-index="${this.currentSongIndex}"]`);
            if (activeItem) {
                activeItem.classList.add('active');
                if (this.isPlaying) {
                    activeItem.classList.add('playing');
                }
            }
        }
    }
    
    updateProgress() {
        if (!this.audio.duration) return;
        
        const progress = (this.audio.currentTime / this.audio.duration) * 100;
        this.progressBar.style.width = `${progress}%`;
        
        // Update time displays
        this.currentTime.textContent = this.formatTime(this.audio.currentTime);
    }
    
    updateDuration() {
        if (this.audio.duration) {
            this.totalTime.textContent = this.formatTime(this.audio.duration);
        }
    }
    
    onPlay() {
        this.isPlaying = true;
        this.playPauseBtn.innerHTML = '<i class="fas fa-pause"></i>';
        this.updateActiveItem();
    }
    
    onPause() {
        this.isPlaying = false;
        this.playPauseBtn.innerHTML = '<i class="fas fa-play"></i>';
        this.updateActiveItem();
    }
    
    onError(e) {
        console.error('Audio error:', e);
        this.showError('Error playing audio. Please try another song.');
        this.isPlaying = false;
        this.playPauseBtn.innerHTML = '<i class="fas fa-play"></i>';
    }
    
    enableControls() {
        this.playPauseBtn.disabled = false;
        this.prevBtn.disabled = false;
        this.nextBtn.disabled = false;
    }
    
    showLoading() {
        this.loadingSpinner.classList.remove('d-none');
    }
    
    hideLoading() {
        this.loadingSpinner.classList.add('d-none');
    }
    
    showError(message) {
        // Simple error display - you could enhance this with a proper modal or toast
        alert(message);
    }
    
    formatTime(seconds) {
        if (isNaN(seconds)) return '0:00';
        
        const minutes = Math.floor(seconds / 60);
        const remainingSeconds = Math.floor(seconds % 60);
        return `${minutes}:${remainingSeconds.toString().padStart(2, '0')}`;
    }
    
    escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    setupSearch() {
    const searchInput = document.getElementById('searchInput');
    if (!searchInput) return;

    searchInput.addEventListener('input', (e) => {
        const query = e.target.value.toLowerCase().trim();

        const filtered = this.songs.filter(song =>
            song.title.toLowerCase().includes(query) ||
            song.artist.toLowerCase().includes(query)
        );

        this.renderSongList(filtered);
    });
}

}

// Initialize the music player when the DOM is loaded
document.addEventListener('DOMContentLoaded', () => {
    console.log('Initializing Music Player...');
    window.musicPlayer = new MusicPlayer();
});

// Handle page visibility change (pause when tab is hidden)
document.addEventListener('visibilitychange', () => {
    if (window.musicPlayer && document.hidden && window.musicPlayer.isPlaying) {
        // Optionally pause when tab is hidden
        // window.musicPlayer.audio.pause();
    }
});

// Service Worker registration for PWA capabilities (optional)
if ('serviceWorker' in navigator) {
    window.addEventListener('load', () => {
        // You can add a service worker here for offline functionality
    });
}