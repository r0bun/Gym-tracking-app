# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.trackapp.**$$serializer { *; }
-keepclassmembers class com.trackapp.** {
    *** Companion;
}
-keepclasseswithmembers class com.trackapp.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Supabase / Ktor
-keep class io.ktor.** { *; }
-keep class io.github.jan.supabase.** { *; }
