# ──────────────────────────────────────────────────────────────
# SmackCheck R8/ProGuard Rules
# ──────────────────────────────────────────────────────────────

# ── kotlinx.serialization ─────────────────────────────────────
# Keep all @Serializable classes and their generated serializers
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep project-specific @Serializable classes
-keep,includedescriptorclasses class com.example.smackcheck2.**$$serializer { *; }
-keepclassmembers class com.example.smackcheck2.** {
    *** Companion;
}
-keepclasseswithmembers class com.example.smackcheck2.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep all DTOs and models used with Supabase/Ktor
-keep class com.example.smackcheck2.data.dto.** { *; }
-keep class com.example.smackcheck2.model.** { *; }
-keep class com.example.smackcheck2.data.SupabaseRestaurantRow { *; }
-keep class com.example.smackcheck2.notifications.** { *; }

# ── Supabase ──────────────────────────────────────────────────
-keep class io.github.jan.supabase.** { *; }
-dontwarn io.github.jan.supabase.**

# ── Ktor ──────────────────────────────────────────────────────
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# ── OkHttp / Okio ────────────────────────────────────────────
-dontwarn okhttp3.**
-dontwarn okio.**

# ── Kamel Image Loading ──────────────────────────────────────
-keep class io.kamel.** { *; }
-dontwarn io.kamel.**

# ── Google Maps & Places ─────────────────────────────────────
-keep class com.google.android.gms.maps.** { *; }
-keep class com.google.android.libraries.places.** { *; }

# ── Firebase ─────────────────────────────────────────────────
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# ── Compose ──────────────────────────────────────────────────
-dontwarn androidx.compose.**

# ── gRPC (used internally by Google Places SDK) ──────────────
-dontwarn io.grpc.internal.DnsNameResolverProvider
-dontwarn io.grpc.internal.PickFirstLoadBalancerProvider

# ── General ──────────────────────────────────────────────────
-keepattributes Signature
-keepattributes Exceptions
-keepattributes SourceFile,LineNumberTable
