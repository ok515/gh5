# ProGuard / R8 Obfuscation & Security Rules for Pecmi

# 1. Code Shrinking, Optimization & Obfuscation
-repackageclasses 'a'
-allowaccessmodification
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-verbose

# Preserve attributes for proper crash reporting and serialization
-keepattributes SourceFile,LineNumberTable,*Annotation*,Signature,InnerClasses,EnclosingMethod

# Obscure source file attribute
-renamesourcefileattribute SourceFile

# Remove debug logs in Release builds
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# 2. Android Framework & Jetpack Compose
-keep class androidx.compose.** { *; }
-keepclassmembers class * extends androidx.compose.ui.node.ModifierNodeElement { *; }
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}

# 3. Room Database & KSP
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keepclassmembers class * {
    @androidx.room.Dao *;
}

# 4. Moshi / JSON Serialization
-keep class com.squareup.moshi.** { *; }
-keep @com.squareup.moshi.JsonClass class * { *; }
-keepclassmembers class * {
    @com.squareup.moshi.Json *;
}

# 5. AdMob (Google Mobile Ads)
-keep class com.google.android.gms.ads.** { *; }
-keepinterface com.google.android.gms.ads.** { *; }
-keep class com.google.android.gms.common.** { *; }

# 6. Security & Core Models Preservation
-keep class com.pecmi.studio.security.SecurityGuard { *; }
-keep class com.pecmi.studio.domain.model.** { *; }
-keep class com.pecmi.studio.data.entity.** { *; }
-keep class com.pecmi.studio.ads.AdConfig { *; }
