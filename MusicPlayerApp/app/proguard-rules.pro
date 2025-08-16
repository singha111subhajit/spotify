# Keep ExoPlayer classes used by reflection
-keep class com.google.android.exoplayer2.** { *; }
-keep class com.google.android.exoplayer2.ext.mediasession.** { *; }

# Retrofit/Gson model classes
-keep class com.example.musicplayer.model.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn okio.**