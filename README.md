# 🎵 Music Player Web App

A modern, responsive music player web application built with Flask and vanilla JavaScript. Features a Spotify-inspired dark theme and full audio playback controls.

## ✨ Features

- **Modern UI**: Dark theme with Spotify-inspired design
- **Responsive Design**: Works on desktop, tablet, and mobile devices
- **Full Audio Controls**: Play, pause, next, previous, volume control
- **Progress Tracking**: Visual progress bar with time display
- **Playlist View**: Sidebar showing all available songs
- **Keyboard Shortcuts**: Space (play/pause), Arrow keys (next/previous)
- **API Endpoint**: REST API for song data at `/api/songs`

## 🚀 Quick Start

### Prerequisites

- Python 3.7 or higher
- pip (Python package installer)

### Installation

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd music-player
   ```

2. **Install dependencies**
   ```bash
   pip install -r requirements.txt
   ```

3. **Add your music files**
   - Place MP3 files in the `static/songs/` directory
   - Update the song list in `app.py` if using different files
   - See `static/songs/README.md` for more details

4. **Run the application**
   ```bash
   python app.py
   ```

5. **Open your browser**
   - Navigate to `http://localhost:5000`
   - Enjoy your music! 🎶

## 📁 Project Structure

```
music-player/
├── app.py                 # Flask backend application
├── requirements.txt       # Python dependencies
├── templates/
│   └── index.html        # Main HTML template
├── static/
│   ├── css/
│   │   └── style.css     # Custom styling
│   ├── js/
│   │   └── player.js     # Music player functionality
│   └── songs/            # Directory for MP3 files
│       └── README.md     # Instructions for adding songs
└── README.md            # This file
```

## 🎮 Usage

### Basic Controls

- **Play/Pause**: Click the central play button or press `Space`
- **Next Song**: Click next button or press `→` (right arrow)
- **Previous Song**: Click previous button or press `←` (left arrow)
- **Volume**: Adjust using the volume slider
- **Seek**: Click anywhere on the progress bar to jump to that position

### Song Selection

- Click any song in the playlist sidebar to play it
- The currently playing song is highlighted in green
- Playing songs show a musical note animation

### Mobile Support

- Fully responsive design that adapts to mobile screens
- Touch-friendly controls
- Works with mobile browsers' HTML5 audio support

## 🔧 Configuration

### Adding Custom Songs

1. Place your audio files in `static/songs/`
2. Edit the `get_song_list()` function in `app.py`:

```python
sample_songs = [
    {
        "id": 1,
        "title": "Your Song Title",
        "artist": "Artist Name",
        "filename": "your_file.mp3",
        "duration": "3:24"
    },
    # Add more songs...
]
```

### Supported Audio Formats

- MP3 (.mp3) - Recommended
- WAV (.wav)
- OGG (.ogg)

### Customizing the Theme

Edit `static/css/style.css` to customize:
- Colors (CSS variables at the top)
- Layout and spacing
- Responsive breakpoints
- Animations and transitions

## 🌐 API Endpoints

### GET `/`
Returns the main music player interface.

### GET `/api/songs`
Returns JSON array of available songs:

```json
[
  {
    "id": 1,
    "title": "Summer Vibes",
    "artist": "Chill Beats",
    "filename": "summer_vibes.mp3",
    "duration": "3:24"
  }
]
```

### GET `/songs/<filename>`
Serves audio files directly for playback.

## 🛠️ Development

### Running in Development Mode

```bash
python app.py
```

The app runs with `debug=True` by default, enabling:
- Automatic reloading on code changes
- Detailed error messages
- Flask development server

### Browser Compatibility

- Chrome 60+ ✅
- Firefox 55+ ✅
- Safari 11+ ✅
- Edge 79+ ✅
- Mobile browsers with HTML5 audio support ✅

## ⚡ Performance Tips

1. **Audio Format**: Use MP3 for best compatibility and file size
2. **File Size**: Compress audio files appropriately for web delivery
3. **Caching**: Browser will cache audio files after first load
4. **Network**: Consider CDN for serving audio files in production

## 🔒 Security Notes

- Static file serving is handled by Flask (not suitable for production)
- For production, use a proper web server (nginx, Apache) to serve static files
- Implement proper authentication if needed for private music collections

## 📱 Mobile Considerations

- The app uses HTML5 audio which works on modern mobile browsers
- Some mobile browsers may require user interaction before playing audio
- Volume control may be limited on some mobile devices (system volume only)

## 🎨 Customization Ideas

- Add album artwork support
- Implement shuffle and repeat modes
- Add equalizer controls
- Create playlists functionality
- Add lyrics display
- Implement user favorites/ratings
- Add social sharing features

## 🐛 Troubleshooting

### Audio Not Playing
- Check console for JavaScript errors
- Verify MP3 files are in `static/songs/` directory
- Ensure filenames match exactly in `app.py`
- Check browser audio permissions

### Styling Issues
- Clear browser cache
- Check for CSS errors in developer tools
- Verify Bootstrap CDN is loading

### Mobile Issues
- Test user interaction before playing audio
- Check mobile browser compatibility
- Verify touch events are working

## 📄 License

This project is open source and available under the [MIT License](LICENSE).

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test thoroughly
5. Submit a pull request

## 📞 Support

If you encounter any issues or have questions:
1. Check the troubleshooting section above
2. Review browser console for errors
3. Ensure all files are properly placed
4. Verify Python dependencies are installed

---

**Enjoy your music! 🎵🎶**