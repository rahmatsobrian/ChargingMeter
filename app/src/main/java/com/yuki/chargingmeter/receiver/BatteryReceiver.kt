package com.yuki.chargingmeter.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * BroadcastReceiver untuk mendeteksi perubahan status baterai.
 * Digunakan sebagai trigger update di MainActivity via LocalBroadcastManager.
 */
class BatteryReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Intent diteruskan ke ViewModel via callback — tidak perlu logika di sini
        // Receiver ini terdaftar di Manifest untuk menerima broadcast sistem
    }
}
