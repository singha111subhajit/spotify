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

## Run with Docker Compose (includes PostgreSQL)

1. **Start services**
   ```bash
   docker compose up -d --build
   ```

2. **Verify**
   - App: http://localhost:5600
   - Postgres: localhost:5432 (user: `music`, password: `music`, db: `music_app`)

3. **Stop services**
   ```bash
   docker compose down
   ```

---

**Enjoy your music! 🎵**

access db:-
psql -h localhost -p 5432 -U music -d music_app

## Database Migration to https://supabase.com/
I am using github singha111subhajit account here
1. **Create a Supabase project**
   dbpass:Smouli@1408     
   we can directly connect from local terminal with this dbpass like>
   
   terminal cmd>psql "postgresql://postgres:Smouli%401408@db.yvvslzdprrskzxurbcmi.supabase.co:5432/postgres"