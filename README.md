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
   docker run -e DATABASE_URL='postgresql://<user>:<password>@<host>:5432/music_app' -p 5600:5600 music-player
   ```
   - Ensure you have a reachable PostgreSQL instance and the `music_app` database created. Example to create DB:
     ```bash
     createdb music_app
     ```

4. **Open your browser**
   - Go to: http://localhost:5600

---

**Enjoy your music! 🎵**