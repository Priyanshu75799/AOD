# Add project specific ProGuard rules here.

# Keep all App Classes, UI components & Navigation Enums
-keep class com.priyanshudeveloper.aod.** { *; }
-keepclassmembers class com.priyanshudeveloper.aod.** { *; }
-keepclassmembers enum com.priyanshudeveloper.aod.** { *; }

# Keep PhoenixEngine Services, Core, Models, and Broadcast Receivers
-keep class com.priyanshudeveloper.aod.phoenixengine.** { *; }
-keepclassmembers class com.priyanshudeveloper.aod.phoenixengine.** { *; }

# Keep Coroutines StateFlow & Jetpack Compose
-keepclassmembers class * extends kotlinx.coroutines.flow.MutableStateFlow { *; }

# Suppress reflection warnings for framework PowerProfile
-dontwarn com.android.internal.os.PowerProfile
