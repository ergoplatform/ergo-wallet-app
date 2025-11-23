# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#    http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#    public *;
#}

# If you keep the line number information, uncomment this to
# hide the original source file name.
-renamesourcefileattribute SourceFile

# Essential rules for Retrofit to keep generic type signatures and classes
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-keepattributes Exceptions

-dontwarn retrofit2.**
-keep class retrofit2.** { *; }

-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response

-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-dontwarn okhttp3.**
-dontwarn okio.**

# GSON/Converter rules
-keep class com.google.gson.reflect.TypeToken
-keep class * extends com.google.gson.reflect.TypeToken
-keep class sun.misc.Unsafe { *; }

# Kotlin Data Class rules
-keepclassmembers class * {
    java.lang.Object component1();
    java.lang.Object component2();
    java.lang.Object copy(...);
    void <init>(...);
}

# Your existing project-specific rules start here

-dontwarn javax.annotation.Nullable
-dontwarn javax.annotation.ParametersAreNonnullByDefault

-keep class com.google.gson.**
-keep class org.ergoplatform.api.**
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

-keep class sigmastate.** { *; }
-keep class sigma.** { *; }
-keep class scalan.** { *; }
-keep class special.** { *; }
-keep class wrappers.** { *; }
-keep class org.ergoplatform.restapi.** { *; }
-keep class org.ergoplatform.mosaik.model.** { *; }
-keep class org.ergoplatform.appkit.** { *; }
-keep class scorex.util.encode.** { *; }
-keep class scala.util.control.** { *; }
-keep class scala.collection.** { *; }
-keep class scala.package** { *; }
-keep class fastparse.** { *; }

-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

-ignorewarnings

-dontwarn scala.**
-dontobfuscate
-dontoptimize
