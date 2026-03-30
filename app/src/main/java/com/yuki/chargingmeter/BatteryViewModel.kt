package com.yuki.chargingmeter

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yuki.chargingmeter.util.BatteryInfo
import com.yuki.chargingmeter.util.BatteryReader
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BatteryViewModel(application: Application) : AndroidViewModel(application) {

    private val _batteryInfo = MutableStateFlow(BatteryInfo())
    val batteryInfo: StateFlow<BatteryInfo> = _batteryInfo.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    // History arus untuk grafik — simpan 60 titik (1 menit jika update tiap detik)
    private val _currentHistory = MutableStateFlow<List<Long>>(emptyList())
    val currentHistory: StateFlow<List<Long>> = _currentHistory.asStateFlow()

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            refreshBatteryInfo()
        }
    }

    init {
        // Register receiver untuk update real-time
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }
        application.registerReceiver(batteryReceiver, filter)

        // Baca langsung saat pertama kali
        refreshBatteryInfo()

        // Polling setiap 1 detik untuk arus real-time yang akurat
        startPolling()
    }

    private fun startPolling() {
        viewModelScope.launch {
            while (true) {
                delay(1000L)
                refreshBatteryInfo()
            }
        }
    }

    fun refreshBatteryInfo() {
        val info = BatteryReader.read(getApplication())
        _batteryInfo.value = info

        // Update history untuk grafik
        val history = _currentHistory.value.toMutableList()
        history.add(info.currentMicroAmps)
        if (history.size > 60) history.removeAt(0)
        _currentHistory.value = history
    }

    override fun onCleared() {
        super.onCleared()
        try {
            getApplication<Application>().unregisterReceiver(batteryReceiver)
        } catch (_: Exception) {}
    }
}
