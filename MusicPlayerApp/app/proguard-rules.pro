# Keep Media3 classes used by reflection
-keep class androidx.media3.** { *; }

# Retrofit/Gson model classes
-keep class com.example.musicplayer.model.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn okio.**