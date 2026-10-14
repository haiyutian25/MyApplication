################################################################################
# Models
################################################################################

# R8 currently has trouble with some Retrofit calls dealing with sealed classes.
# For now we will keep all network layer models.
-keepclassmembers class com.example.myapplication.**.network.model.** { *; }

# Keep all enums
-keepclassmembers enum * { *; }

################################################################################
# kotlinx.serialization
################################################################################

-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*, InnerClasses

# JsonElement serializers
-keep,includedescriptorclasses class kotlinx.serialization.json.**$$serializer { *; }
-keep,includedescriptorclasses class com.example.myapplication.**$$serializer { *; }
-keepclassmembers class com.example.myapplication.** {
    *** Companion;
}
-keepclasseswithmembers class com.example.myapplication.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# kotlinx-serialization-json specific.
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

################################################################################
# Okhttp/Retrofit https://square.github.io/okhttp/ & https://square.github.io/retrofit/
################################################################################

# Retrofit does reflection on generic parameters. InnerClasses is required to use Signature and
# EnclosingMethod is required to use InnerClasses.
-keepattributes Signature, InnerClasses, EnclosingMethod

# Retrofit does reflection on method and parameter annotations.
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations

# https://github.com/square/okhttp/blob/339732e3a1b78be5d792860109047f68a011b5eb/okhttp/src/jvmMain/resources/META-INF/proguard/okhttp3.pro#L11-L14
-dontwarn okhttp3.internal.platform.**
-dontwarn org.bouncycastle.**
# Related to this issue on https://github.com/square/retrofit/issues/3880
# Check https://github.com/square/retrofit/tags for new versions
-keep,allowobfuscation,allowshrinking class kotlin.Result
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
# This solves this issue https://github.com/square/retrofit/issues/3880
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# With R8 full mode, it sees no subtypes of Retrofit interfaces since they are created with a Proxy
# and replaces all potential values with null. Explicitly keeping the interfaces prevents this.
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface <1>

################################################################################
# Gradle generated rules
################################################################################
-dontwarn com.google.errorprone.annotations.CanIgnoreReturnValue
-dontwarn com.google.errorprone.annotations.CheckReturnValue
-dontwarn com.google.errorprone.annotations.Immutable
-dontwarn com.google.errorprone.annotations.RestrictedApi
