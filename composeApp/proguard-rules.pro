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
-keep class org.slf4j.spi.SLF4JServiceProvider { *; }
-keep class ch.qos.logback.classic.spi.LogbackServiceProvider { *; }

# JNA (reflection + native bindings)
-keep class com.sun.jna.** { *; }
-keep class com.sun.jna.platform.** { *; }
-keep interface * extends com.sun.jna.Library { *; }
-keep class * extends com.sun.jna.Structure { *; }
-keep class * implements com.sun.jna.Callback { *; }

-dontwarn com.sun.jna.**

-keep class com.project.analyzer.impl.setup.jna.User32Ex { *; }
-keep interface com.project.analyzer.impl.setup.jna.** { *; }

# Kotlinx Serialization
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}
-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclassmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# Metro DI
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

# kotlinx-datetime
-keep class kotlinx.datetime.** { *; }
-keepnames class kotlinx.datetime.** { *; }

# Compose
-dontwarn androidx.compose.material.**

-dontwarn kotlinx.coroutines.slf4j.**
-dontwarn com.oracle.svm.core.annotate.**
-keep class androidx.compose.material.** { *; }
-keepnames class androidx.compose.material.** { *; }

-keep class com.project.analyzer.** { *; }
-keepnames class com.project.analyzer.** { *; }

# Coroutines
-keep class kotlinx.coroutines.internal.MainDispatcherFactory { *; }
-keep class kotlinx.coroutines.swing.SwingDispatcherFactory { *; }
-dontwarn kotlin.jvm.internal.EnhancedNullability
-dontwarn kotlin.concurrent.atomics.**
-keep class kotlin.concurrent.atomics.** { *; }
-keepnames class kotlin.concurrent.atomics.** { *; }
-keep class kotlinx.coroutines.** { *; }
-keepnames class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**
