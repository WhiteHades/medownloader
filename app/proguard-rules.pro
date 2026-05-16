# ProGuard / R8 rules for meDownloader release builds

# ─── kotlinx.serialization ───────────────────────────────────────────────────
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep @Serializable data classes
-keep,includedescriptorclasses class com.medownloader.data.model.**$$serializer { *; }
-keepclassmembers class com.medownloader.data.model.** {
    *** Companion;
    *** serializer(...);
}
-keep,includedescriptorclasses class com.medownloader.data.repository.QueuedDownload$$serializer { *; }
-keepclassmembers class com.medownloader.data.repository.QueuedDownload {
    *** Companion;
    *** serializer(...);
}

# ─── OkHttp ──────────────────────────────────────────────────────────────────
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# ─── Chaquopy / Python ───────────────────────────────────────────────────────
-keep class com.chaquo.python.** { *; }
-keep class org.python.** { *; }
-dontwarn com.chaquo.python.**

# ─── RevenueCat ──────────────────────────────────────────────────────────────
-keep class com.revenuecat.purchases.** { *; }

# ─── aria2c native binary ────────────────────────────────────────────────────
-keep class com.medownloader.data.source.Aria2ProcessManager { *; }

# ─── Compose ─────────────────────────────────────────────────────────────────
# Compose compiler handles most of this, but keep stability annotations
-dontwarn androidx.compose.**

# ─── DataStore ───────────────────────────────────────────────────────────────
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite { *; }

# ─── General ─────────────────────────────────────────────────────────────────
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
