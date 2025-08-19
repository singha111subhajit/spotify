# DhoonHubApp (Kotlin + Compose + ExoPlayer)

This is a sample Android music player app scaffold that integrates with a Flask/Django backend via Retrofit/OkHttp, supports JWT auth, online streaming, and offline playback using ExoPlayer. It includes lock screen and notification controls.

## Requirements implemented
- Authentication (login/register) with JWT stored via EncryptedSharedPreferences
- Online streaming with Retrofit + OkHttp (token via `Authorization: Bearer`)
- Offline downloads to app-specific external storage (`getExternalFilesDir(Environment.DIRECTORY_MUSIC)`)
- ExoPlayer with foreground service, media session, lock screen + notification controls
- Material 3 UI with Compose; screens: Login, Register, Online, Offline, Player

## Quick start
1. Open this folder in Android Studio (Giraffe+)
2. Set your backend base URL in `app/src/main/java/com/example/DhoonHub/config/ApiConfig.kt`
3. Adjust API paths in `network/api` interfaces to match your Flask/Django endpoints
4. Run the app on a device/emulator with internet access

## Distribution
You can sign and distribute the APK from Android Studio (Build > Generate Signed Bundle/APK) and host it on your website. The app does not depend on Play Store services.

## Notes
- Offline files are stored under the app-specific external directory and survive app updates, but are removed on uninstall.
- This scaffold uses ExoPlayer 2.x with `MediaSessionCompat` and a foreground service for broad compatibility.
- Replace placeholder icons and polish UI as desired.