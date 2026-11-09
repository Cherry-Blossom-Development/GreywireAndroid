# Retrofit / OkHttp (same rules as the Prosaurus app)
-dontwarn okhttp3.**
-dontwarn okio.**
# Signature is critical -- without it R8 strips generic return types from Retrofit
# interface methods, causing "Class cannot be cast to ParameterizedType" at runtime.
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes Exceptions
-keep class retrofit2.** { *; }
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface <1>
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*
-dontwarn kotlin.Unit

# Gson
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Data model classes used with Gson serialization
-keep class com.cherryblossomdev.greywire.data.** { *; }
-keep class com.cherryblossomdev.greywire.network.** { *; }

-dontwarn com.google.errorprone.annotations.**
