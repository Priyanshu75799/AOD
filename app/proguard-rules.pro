# Add project specific ProGuard rules here.

# Keep PhoenixEngine Services, Core, and Model classes
-keep class com.priyanshudeveloper.aod.phoenixengine.** { *; }
-keepclassmembers class com.priyanshudeveloper.aod.phoenixengine.** { *; }

# Keep Main Activities
-keep class com.priyanshudeveloper.aod.MainActivity { *; }
-keep class com.priyanshudeveloper.aod.AodActivity { *; }

# Suppress reflection warnings for framework PowerProfile
-dontwarn com.android.internal.os.PowerProfile
