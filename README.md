# Music Platform: Flask Backend + Android App

## Contents
- Flask backend (`app.py`, `templates/`, `static/`)
- Android app (Kotlin/Compose/ExoPlayer) in `DhoonHubApp/`

## Backend (Flask)
- Run with Docker Compose (includes PostgreSQL):
```bash
docker compose up -d --build
```
- App: http://localhost:5600
- Postgres: localhost:5432 (user: `music`, password: `music`, db: `music_app`)

Run locally without Docker:
```bash
python3 -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt
export FLASK_APP=app.py FLASK_ENV=development DATABASE_URL="postgresql://music:music@localhost:5432/music_app"
flask run -h 0.0.0.0 -p 5600
```

## Android App
- Open `DhoonHubApp/` in Android Studio
- Set backend base URL in `app/src/main/java/com/example/DhoonHub/config/ApiConfig.kt`
- Run on device/emulator

Distribution: build a signed APK in Android Studio and host on your website.