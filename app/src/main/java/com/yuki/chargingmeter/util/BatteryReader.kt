package com.yuki.chargingmeter.util

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import kotlin.math.abs

/**
 * Data class yang menyimpan semua informasi baterai secara detail dan akurat.
 */
data class BatteryInfo(
    // Level & Status
    val level: Int = 0,                     // 0–100 %
    val isCharging: Boolean = false,
    val isPlugged: Boolean = false,
    val plugType: PlugType = PlugType.NONE,
    val chargeStatus: ChargeStatus = ChargeStatus.UNKNOWN,

    // Voltase & Arus (dari BatteryManager API — paling akurat)
    val voltageMilliVolts: Int = 0,         // mV dari ACTION_BATTERY_CHANGED
    val currentMicroAmps: Long = 0,         // µA real-time (BatteryManager.PROPERTY_CURRENT_NOW)
    val currentAvgMicroAmps: Long = 0,      // µA rata-rata

    // Daya
    val powerMilliWatts: Float = 0f,        // mW = V * I

    // Kapasitas
    val capacityMicroAmpHour: Long = 0,     // µAh sisa charge (CHARGE_COUNTER)
    val fullCapacityMicroAmpHour: Long = 0, // µAh kapasitas penuh (ENERGY_COUNTER tidak akurat, gunakan charge_full dari sysfs bila ada)
    val chargePercent: Float = 0f,          // % dari charge counter

    // Suhu
    val temperatureCelsius: Float = 0f,     // °C
    val temperatureFahrenheit: Float = 0f,  // °F

    // Estimasi waktu
    val estimatedMinutesRemaining: Long = -1L, // menit

    // Health
    val health: BatteryHealth = BatteryHealth.UNKNOWN,

    // Teknologi
    val technology: String = "Unknown",

    // Timestamp
    val timestamp: Long = System.currentTimeMillis()
)

enum class PlugType { NONE, USB, AC, WIRELESS, DOCK }
enum class ChargeStatus { UNKNOWN, CHARGING, DISCHARGING, NOT_CHARGING, FULL }
enum class BatteryHealth { UNKNOWN, GOOD, OVERHEAT, DEAD, OVER_VOLTAGE, FAILURE, COLD }

/**
 * Utility untuk membaca informasi baterai secara akurat dari Android API.
 * Menggunakan BatteryManager.PROPERTY_* untuk nilai real-time yang lebih presisi
 * dibandingkan hanya mengandalkan ACTION_BATTERY_CHANGED intent.
 */
object BatteryReader {

    fun read(context: Context): BatteryInfo {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager

        // Sticky broadcast — aman dipanggil tanpa register receiver
        val intent: Intent? = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )

        // === Level ===
        val rawLevel = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val level = if (rawLevel >= 0 && scale > 0) (rawLevel * 100 / scale) else 0

        // === Status charging ===
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
        val chargeStatus = when (status) {
            BatteryManager.BATTERY_STATUS_CHARGING     -> ChargeStatus.CHARGING
            BatteryManager.BATTERY_STATUS_DISCHARGING  -> ChargeStatus.DISCHARGING
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> ChargeStatus.NOT_CHARGING
            BatteryManager.BATTERY_STATUS_FULL         -> ChargeStatus.FULL
            else                                        -> ChargeStatus.UNKNOWN
        }

        // === Plug type ===
        val plugged = intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
        val isPlugged = plugged != 0
        val plugType = when (plugged) {
            BatteryManager.BATTERY_PLUGGED_USB      -> PlugType.USB
            BatteryManager.BATTERY_PLUGGED_AC       -> PlugType.AC
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> PlugType.WIRELESS
            BatteryManager.BATTERY_PLUGGED_DOCK     -> PlugType.DOCK
            else                                    -> PlugType.NONE
        }

        // === Voltase (dari intent — dalam mV) ===
        val voltageMilliVolts = intent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0

        // === Arus real-time (µA) — ini yang paling akurat ===
        // Positif = charging, Negatif = discharging (tergantung OEM)
        val currentNow = bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        val currentAvg = bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE)

        // Normalisasi: pastikan charging = positif, discharging = negatif
        val currentMicroAmps = if (isCharging) abs(currentNow) else -abs(currentNow)
        val currentAvgMicroAmps = if (isCharging) abs(currentAvg) else -abs(currentAvg)

        // === Daya (mW) ===
        // P = V(V) × I(A) = (mV/1000) × (µA/1_000_000) × 1000 = mW
        val powerMilliWatts = if (voltageMilliVolts > 0 && currentNow != 0L) {
            (voltageMilliVolts.toFloat() / 1000f) * (abs(currentNow).toFloat() / 1000f)
        } else 0f

        // === Kapasitas (µAh) ===
        val capacityMicroAmpHour = bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
        val fullCapacityMicroAmpHour = bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_ENERGY_COUNTER)

        // % dari charge counter (lebih akurat dari % OS untuk estimasi)
        val chargePercent = if (fullCapacityMicroAmpHour > 0 && capacityMicroAmpHour > 0) {
            (capacityMicroAmpHour.toFloat() / fullCapacityMicroAmpHour.toFloat() * 100f)
                .coerceIn(0f, 100f)
        } else level.toFloat()

        // === Suhu (dari intent — dalam 1/10 °C) ===
        val rawTemp = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
        val tempCelsius = rawTemp / 10f
        val tempFahrenheit = tempCelsius * 9f / 5f + 32f

        // === Estimasi waktu ===
        // Android API 28+ punya BATTERY_PROPERTY_REMAINING_ENERGY tapi tidak reliable di semua device
        // Hitung sendiri: lebih akurat
        val estimatedMinutes = calculateEstimatedMinutes(
            capacityMicroAmpHour = capacityMicroAmpHour,
            fullCapacityMicroAmpHour = fullCapacityMicroAmpHour,
            currentMicroAmps = currentNow,
            level = level,
            isCharging = isCharging
        )

        // === Health ===
        val healthInt = intent?.getIntExtra(BatteryManager.EXTRA_HEALTH, -1) ?: -1
        val health = when (healthInt) {
            BatteryManager.BATTERY_HEALTH_GOOD          -> BatteryHealth.GOOD
            BatteryManager.BATTERY_HEALTH_OVERHEAT      -> BatteryHealth.OVERHEAT
            BatteryManager.BATTERY_HEALTH_DEAD          -> BatteryHealth.DEAD
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE  -> BatteryHealth.OVER_VOLTAGE
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> BatteryHealth.FAILURE
            BatteryManager.BATTERY_HEALTH_COLD          -> BatteryHealth.COLD
            else                                        -> BatteryHealth.UNKNOWN
        }

        // === Teknologi ===
        val technology = intent?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "Unknown"

        return BatteryInfo(
            level = level,
            isCharging = isCharging,
            isPlugged = isPlugged,
            plugType = plugType,
            chargeStatus = chargeStatus,
            voltageMilliVolts = voltageMilliVolts,
            currentMicroAmps = currentMicroAmps,
            currentAvgMicroAmps = currentAvgMicroAmps,
            powerMilliWatts = powerMilliWatts,
            capacityMicroAmpHour = capacityMicroAmpHour,
            fullCapacityMicroAmpHour = fullCapacityMicroAmpHour,
            chargePercent = chargePercent,
            temperatureCelsius = tempCelsius,
            temperatureFahrenheit = tempFahrenheit,
            estimatedMinutesRemaining = estimatedMinutes,
            health = health,
            technology = technology,
            timestamp = System.currentTimeMillis()
        )
    }

    /**
     * Hitung estimasi waktu tersisa berdasarkan data real.
     * Jika charging: waktu sampai penuh.
     * Jika discharging: waktu sampai habis.
     */
    private fun calculateEstimatedMinutes(
        capacityMicroAmpHour: Long,
        fullCapacityMicroAmpHour: Long,
        currentMicroAmps: Long,
        level: Int,
        isCharging: Boolean
    ): Long {
        val absCurrentMicroAmps = abs(currentMicroAmps)
        if (absCurrentMicroAmps < 10_000L) return -1L // Arus terlalu kecil, tidak reliabel

        return if (isCharging) {
            // Waktu sampai penuh: (kapasitas sisa yang perlu diisi) / arus charging
            if (fullCapacityMicroAmpHour > 0 && capacityMicroAmpHour > 0) {
                val toFill = fullCapacityMicroAmpHour - capacityMicroAmpHour
                if (toFill > 0) (toFill * 60L / absCurrentMicroAmps) else 0L
            } else {
                // Fallback dari level %
                val percentLeft = (100 - level).toLong()
                if (percentLeft > 0) (percentLeft * 60L / (absCurrentMicroAmps / 10_000L)) else 0L
            }
        } else {
            // Waktu sampai habis: kapasitas sisa / arus discharge
            if (capacityMicroAmpHour > 0) {
                capacityMicroAmpHour * 60L / absCurrentMicroAmps
            } else {
                val percentLeft = level.toLong()
                if (percentLeft > 0) (percentLeft * 60L / (absCurrentMicroAmps / 10_000L)) else 0L
            }
        }
    }

    /**
     * Format arus dalam satuan yang mudah dibaca (mA atau A)
     */
    fun formatCurrent(microAmps: Long): String {
        val absVal = abs(microAmps)
        return when {
            absVal >= 1_000_000L -> String.format("%.2f A", absVal / 1_000_000f)
            else -> String.format("%d mA", absVal / 1000L)
        }
    }

    /**
     * Format daya dalam mW atau W
     */
    fun formatPower(milliWatts: Float): String {
        return when {
            milliWatts >= 1000f -> String.format("%.2f W", milliWatts / 1000f)
            else -> String.format("%.0f mW", milliWatts)
        }
    }

    /**
     * Format kapasitas dalam mAh
     */
    fun formatCapacity(microAmpHour: Long): String {
        return String.format("%d mAh", microAmpHour / 1000L)
    }

    /**
     * Format estimasi waktu
     */
    fun formatTime(minutes: Long): String {
        if (minutes < 0) return "Menghitung..."
        if (minutes == 0L) return "Selesai"
        val h = minutes / 60
        val m = minutes % 60
        return when {
            h > 0 && m > 0 -> "${h}j ${m}m"
            h > 0           -> "${h} jam"
            else            -> "${m} menit"
        }
    }
}
