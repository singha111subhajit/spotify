keystore path is inside spotify_android folder >/home/subhajit/Desktop/personal_work/spotify_android/MusicPlayerApp/KeyStoreDhoonhub.jks

pass:Subhajit@1408


apk file location >app/release/app-release.apk

release of the apk link >https://github.com/singha111subhajit/dhoonhub-apk/releases/latest/download/app-release.apk


we can get a crash_report :
Filter logs only for your app.
Capture only FATAL EXCEPTION (crashes).
Save the crash trace to a file like crash_com.example.DhoonHub_20250907_084300.log.
we already have a get_crash_report.sh file just check the permision first 
>>>
chmod +x get_crash_report.sh
now just execute this> ./get_crash_report.sh com.example.DhoonHub

