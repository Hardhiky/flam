# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
-renamesourcefileattribute SourceFile

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep NativeLib class and its native methods
-keep class com.flam.edgedetector.NativeLib {
    public *;
    native <methods>;
}

# Keep OpenCV classes
-keep class org.opencv.** { *; }
-keepclassmembers class org.opencv.** { *; }

# Keep JNI interface classes
-keepclasseswithmembers class * {
    native <methods>;
}

# Keep enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep Parcelable implementations
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator CREATOR;
}

# Keep Serializable implementations
-keepnames class * implements java.io.Serializable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# Keep camera classes
-keep class androidx.camera.** { *; }
-keep interface androidx.camera.** { *; }
-dontwarn androidx.camera.**

# Keep Material Components
-keep class com.google.android.material.** { *; }
-dontwarn com.google.android.material.**

# Keep OpenGL classes
-keep class android.opengl.** { *; }
-keep interface android.opengl.** { *; }

# Keep GL renderer classes
-keep class com.flam.edgedetector.gl.** { *; }
-keepclassmembers class com.flam.edgedetector.gl.** { *; }

# Keep camera manager
-keep class com.flam.edgedetector.camera.** { *; }
-keepclassmembers class com.flam.edgedetector.camera.** { *; }

# Keep activity
-keep class com.flam.edgedetector.MainActivity { *; }

# Remove logging in release builds
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# Keep annotation for reflection
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions

# AndroidX rules
-dontwarn androidx.**
-keep class androidx.** { *; }
-keep interface androidx.** { *; }

# Kotlin
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings {
    <fields>;
}
-keep class kotlin.Metadata { *; }
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# Remove verbose logging
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
}
