# Music Player Web App (Minimal Docker Guide)

## Quick Start with Docker

1. **Clone this repository**
   ```bash
   git clone <repository-url>
   cd <project-folder>
   ```

2. **Add your music files**
   - Place your MP3 files in the `static/songs/` directory.

3. **Build and run with Docker**
   ```bash
   docker build -t music-player .
   docker run -p 5000:5000 music-player
   ```

4. **Open your browser**
   - Go to: http://localhost:5000

---

**Enjoy your music! 🎵**