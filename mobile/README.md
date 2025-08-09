# Music Player Mobile (React Native / Expo)

Android app for the Flask backend music player.

## Features
- JWT login/signup (secure token storage)
- Fetch and list songs
- Playback via `react-native-track-player` with background, lockscreen and headset controls
- Offline downloads (play from local when available)
- Default playlist management (add/remove)
- React Navigation and clean UI components

## Prerequisites
- Node 18+
- Android Studio + SDKs
- Java 17

## Setup
```bash
cd mobile
npm install
# Configure backend API base URL (Android emulator default is 10.0.2.2)
# export EXPO_PUBLIC_API_BASE_URL=http://10.0.2.2:5600
```

## Run on Android (dev)
```bash
npm run android
```

## Native prebuild and release APK
This project uses Expo with config plugins. To generate native projects and build a release APK:
```bash
# Generate android project
npm run prebuild
# Open Android Studio or build via Gradle:
(cd android && ./gradlew assembleRelease)
```
Output APK will be under `android/app/build/outputs/apk/release/`.

## Notes
- The backend currently supports a single default playlist per user. Creating multiple playlists is not yet supported server-side.
- For device testing on Android, set `EXPO_PUBLIC_API_BASE_URL` to your machine IP, e.g. `http://192.168.1.10:5600`.