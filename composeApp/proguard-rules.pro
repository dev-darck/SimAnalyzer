-dontobfuscate
-optimizationpasses 3
-allowaccessmodification
-mergeinterfacesaggressively

# ==================== KOTLIN ====================
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# ==================== COMPOSE ====================
-keep class androidx.compose.** { *; }
-keep class org.jetbrains.skia.** { *; }
-keep class org.jetbrains.skiko.** { *; }

-dontwarn androidx.compose.**
-dontwarn org.jetbrains.skia.**
-dontwarn org.jetbrains.skiko.**

# ==================== JNA ====================
-keep class com.sun.jna.** { *; }
-keep class * implements com.sun.jna.Callback { *; }
-keep class * implements com.sun.jna.Structure { *; }
-keepclassmembers class * extends com.sun.jna.Structure {
    <fields>;
    <methods>;
}
-dontwarn com.sun.jna.**

# ==================== METRO DI ====================
-keep class dev.zacsweers.metro.** { *; }
-keep @dev.zacsweers.metro.Inject class * { *; }
-keep @dev.zacsweers.metro.DependencyGraph class * { *; }
-keep @dev.zacsweers.metro.ContributesTo class * { *; }
-keep @dev.zacsweers.metro.ContributesBinding class * { *; }
-keepclassmembers class * {
    @dev.zacsweers.metro.Inject <init>(...);
    @dev.zacsweers.metro.Inject <fields>;
    @dev.zacsweers.metro.Provides <methods>;
}
-dontwarn dev.zacsweers.metro.**
