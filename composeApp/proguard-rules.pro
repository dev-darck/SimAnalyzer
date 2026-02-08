-printmapping build/release-mapping.txt

# Reduce noise.
-dontnote *

# Kotlin metadata
-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,AnnotationDefault
-keep class kotlin.Metadata { *; }

-keepdirectories META-INF/services
-adaptresourcefilenames META-INF/services/**
-adaptresourcefilecontents META-INF/services/**

# SLF4J + Logback
-dontwarn jakarta.servlet.**
-dontwarn jakarta.mail.**
-dontwarn org.codehaus.janino.**
-dontwarn org.codehaus.commons.compiler.**
-dontwarn org.slf4j.**
-dontwarn ch.qos.logback.**
-keep class org.slf4j.** { *; }
-keep class ch.qos.logback.** { *; }

# JNA (reflection + native bindings)
-keep class com.sun.jna.** { *; }
-keep class com.sun.jna.platform.** { *; }
-keep class com.project.analyzer.app.win.nativeWin.** { *; }

-dontwarn com.sun.jna.**

-keep interface com.project.analyzer.impl.setup.jna.** { *; }

# Kotlinx Serialization
-keepclassmembers class <1> {
    public static <1> INSTANCE;
}

# Metro DI
-keep class dev.zacsweers.metro.** { *; }
-keepclassmembers class * { *; }
-dontwarn dev.zacsweers.metro.**

# kotlinx-datetime
-keep class kotlinx.datetime.** { *; }
-keepnames class kotlinx.datetime.** { *; }

# Compose
-dontwarn androidx.compose.material.**

-dontwarn kotlinx.coroutines.slf4j.**
-dontwarn com.oracle.svm.core.annotate.**
-keep class androidx.compose.material.** { *; }
-keepnames class androidx.compose.material.** { *; }

-keep class androidx.navigation3.scene.** { *; }
-keep class androidx.navigationevent.** { *; }

-keep class com.project.analyzer.** { *; }
-keepnames class com.project.analyzer.** { *; }

# Coroutines
-dontwarn kotlin.jvm.internal.EnhancedNullability
-dontwarn kotlin.concurrent.atomics.**
-keep class kotlin.concurrent.atomics.** { *; }
-keepnames class kotlin.concurrent.atomics.** { *; }
-keep class kotlinx.coroutines.** { *; }
-keepnames class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**
