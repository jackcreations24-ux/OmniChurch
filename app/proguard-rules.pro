# OmniChurch Pro Production ProGuard / R8 Rules

# Preserve Line Numbers and SourceFile for actionable Crashlytics / Play Console stack traces
-keepattributes SourceFile,LineNumberTable,*Annotation*,Signature,InnerClasses,EnclosingMethod

# Firebase Firestore Data Models and Annotations
-keepattributes *Annotation*
-keepclassmembers class * {
    @com.google.firebase.firestore.PropertyName *;
    @com.google.firebase.firestore.ServerTimestamp *;
    @com.google.firebase.firestore.DocumentId *;
    @com.google.firebase.firestore.IgnoreExtraProperties *;
}

# OmniChurch Domain Models, Data Classes, and Network Payloads
-keep class com.zoutiw.omnichurch.data.models.** { *; }
-keepclassmembers class com.zoutiw.omnichurch.data.models.** { *; }

-keep class com.zoutiw.omnichurch.core.ads.** { *; }
-keepclassmembers class com.zoutiw.omnichurch.core.ads.** { *; }

-keep class com.zoutiw.omnichurch.core.utilities.** { *; }
-keepclassmembers class com.zoutiw.omnichurch.core.utilities.** { *; }

# Jetpack Compose Runtime and State Preservation
-keep class androidx.compose.** { *; }
-keepclassmembers class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Jetpack Credential Manager and Play Services Auth
-keep class androidx.credentials.** { *; }
-keepclassmembers class androidx.credentials.** { *; }
-dontwarn androidx.credentials.**

# ZXing QR Code Generation
-keep class com.google.zxing.** { *; }
-dontwarn com.google.zxing.**

# Kotlin Coroutines & Flow
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# Google Mobile Ads (AdMob)
-keep class com.google.android.gms.ads.** { *; }
-dontwarn com.google.android.gms.ads.**

# Coil Image Loading
-keep class coil.** { *; }
-dontwarn coil.**
