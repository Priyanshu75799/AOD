package com.priyanshudeveloper.aod.phoenixengine.core

import android.app.Activity
import com.priyanshudeveloper.aod.phoenixengine.model.BatteryInfo
import kotlinx.coroutines.flow.MutableStateFlow
import java.lang.ref.WeakReference

object AodManager {
    val isServiceRunning = MutableStateFlow(value = false)
    val batteryInfo = MutableStateFlow(BatteryInfo())
    private var aodActivityRef: WeakReference<Activity>? = null

    fun setAodActivity(activity: Activity?) {
        aodActivityRef = activity?.let { WeakReference(it) }
    }

    fun finishAodActivity() {
        aodActivityRef?.get()?.finish()
        aodActivityRef = null
    }
}
