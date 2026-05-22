package com.dilaer.mobile.data

import android.content.Context
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat

class CallStateMonitor(private val context: Context) {

    fun interface CallEndedListener {
        fun onCallEnded()
    }

    private var callback: InternalCallback? = null

    fun register(listener: CallEndedListener) {
        if (callback != null) return
        val manager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val cb = InternalCallback(listener)
        manager.registerTelephonyCallback(ContextCompat.getMainExecutor(context), cb)
        callback = cb
    }

    fun unregister() {
        val cb = callback ?: return
        val manager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        manager.unregisterTelephonyCallback(cb)
        callback = null
    }

    private class InternalCallback(
        private val listener: CallEndedListener,
    ) : TelephonyCallback(), TelephonyCallback.CallStateListener {
        private var wasInCall = false

        override fun onCallStateChanged(state: Int) {
            when (state) {
                TelephonyManager.CALL_STATE_OFFHOOK -> wasInCall = true
                TelephonyManager.CALL_STATE_IDLE -> {
                    if (wasInCall) {
                        wasInCall = false
                        listener.onCallEnded()
                    }
                }
            }
        }
    }
}
