# Add project specific ProGuard rules here.

# Keep PhoenixEngine Services, Core, Models, and Broadcast Receivers
-keep class com.priyanshudeveloper.aod.phoenixengine.** { *; }
-keepclassmembers class com.priyanshudeveloper.aod.phoenixengine.** { *; }

# Keep Activities
-keep class com.priyanshudeveloper.aod.MainActivity { *; }
-keep class com.priyanshudeveloper.aod.AodActivity { *; }

# Keep Coroutines StateFlow & Jetpack Compose
-keepclassmembers class * extends kotlinx.coroutines.flow.MutableStateFlow { *; }

# Suppress reflection warnings for framework PowerProfile
-dontwarn com.android.internal.os.PowerProfile
