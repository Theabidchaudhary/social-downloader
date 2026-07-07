# kotlinx.serialization — keep generated serializers for API DTOs
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class app.siphon.data.remote.** {
    *** Companion;
}
-keepclasseswithmembers class app.siphon.data.remote.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# OkHttp platform warnings
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
