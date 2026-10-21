# SKYORA ProGuard rules

# Keep Kotlin serialization generated serializers so Retrofit's kotlinx converter
# can resolve them at runtime in minified builds.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class com.skyora.weather.**$$serializer { *; }
-keepclassmembers class com.skyora.weather.** {
    *** Companion;
}
-keepclasseswithmembers class com.skyora.weather.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Retrofit / OkHttp
-keepattributes Signature, Exceptions
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-dontwarn okhttp3.**
-dontwarn javax.annotation.**
