# Add project specific ProGuard rules here.

# Keep BatteryInfo data class
-keepclassmembers class com.priyanshudeveloper.aod.BatteryInfo { *; }
-keep class com.priyanshudeveloper.aod.BatteryInfo { *; }

# Suppress reflection warnings for framework PowerProfile
-dontwarn com.android.internal.os.PowerProfile
