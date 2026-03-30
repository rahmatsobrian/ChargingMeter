package com.yuki.chargingmeter.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yuki.chargingmeter.BatteryViewModel
import com.yuki.chargingmeter.util.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: BatteryViewModel) {
    val info by viewModel.batteryInfo.collectAsStateWithLifecycle()
    val history by viewModel.currentHistory.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Charging Meter",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshBatteryInfo() }) {
                        Icon(
                            Icons.Rounded.Refresh,
                            contentDescription = "Refresh"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // === Status Badge ===
            AnimatedVisibility(visible = info.isCharging || info.chargeStatus == ChargeStatus.FULL) {
                StatusBadge(info = info)
            }

            // === Battery Gauge ===
            BatteryGauge(
                level = info.level,
                isCharging = info.isCharging,
                modifier = Modifier.padding(8.dp)
            )

            // === Estimasi Waktu ===
            if (info.estimatedMinutesRemaining >= 0) {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Schedule,
                            contentDescription = "Waktu",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                text = if (info.isCharging) "Penuh dalam" else "Habis dalam",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                            Text(
                                text = BatteryReader.formatTime(info.estimatedMinutesRemaining),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            // === Grafik Arus ===
            if (history.size >= 2) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Arus Real-time (60 detik)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        CurrentGraph(history = history)
                    }
                }
            }

            // === Grid Info Cards ===
            Text(
                "Detail Baterai",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.align(Alignment.Start)
            )

            // Row 1: Arus & Voltase
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                InfoCard(
                    icon = Icons.Rounded.ElectricBolt,
                    label = "Arus",
                    value = BatteryReader.formatCurrent(info.currentMicroAmps),
                    subtitle = if (info.currentMicroAmps > 0) "↑ Masuk" else "↓ Keluar",
                    modifier = Modifier.weight(1f),
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
                InfoCard(
                    icon = Icons.Rounded.OfflineBolt,
                    label = "Voltase",
                    value = "${info.voltageMilliVolts} mV",
                    subtitle = String.format("%.2f V", info.voltageMilliVolts / 1000f),
                    modifier = Modifier.weight(1f),
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            // Row 2: Daya & Suhu
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                InfoCard(
                    icon = Icons.Rounded.Power,
                    label = "Daya",
                    value = BatteryReader.formatPower(info.powerMilliWatts),
                    subtitle = "P = V × I",
                    modifier = Modifier.weight(1f),
                    containerColor = temperatureContainerColor(info.temperatureCelsius),
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                )
                InfoCard(
                    icon = Icons.Rounded.Thermostat,
                    label = "Suhu",
                    value = "${info.temperatureCelsius}°C",
                    subtitle = "${info.temperatureFahrenheit}°F",
                    modifier = Modifier.weight(1f),
                    containerColor = temperatureContainerColor(info.temperatureCelsius),
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }

            // Row 3: Kapasitas
            if (info.capacityMicroAmpHour > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    InfoCard(
                        icon = Icons.Rounded.Battery5Bar,
                        label = "Kapasitas Sisa",
                        value = BatteryReader.formatCapacity(info.capacityMicroAmpHour),
                        modifier = Modifier.weight(1f)
                    )
                    if (info.fullCapacityMicroAmpHour > 0) {
                        InfoCard(
                            icon = Icons.Rounded.BatteryFull,
                            label = "Kapasitas Penuh",
                            value = BatteryReader.formatCapacity(info.fullCapacityMicroAmpHour),
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            // Row 4: Health & Teknologi
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                InfoCard(
                    icon = Icons.Rounded.FavoriteRounded,
                    label = "Kesehatan",
                    value = healthLabel(info.health),
                    modifier = Modifier.weight(1f),
                    containerColor = healthContainerColor(info.health),
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
                InfoCard(
                    icon = Icons.Rounded.Info,
                    label = "Teknologi",
                    value = info.technology,
                    subtitle = plugLabel(info.plugType),
                    modifier = Modifier.weight(1f)
                )
            }

            // === Arus rata-rata ===
            if (abs(info.currentAvgMicroAmps) > 0) {
                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                "Arus Rata-rata",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                BatteryReader.formatCurrent(info.currentAvgMicroAmps),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                        Text(
                            BatteryReader.formatCurrent(info.currentMicroAmps),
                            style = MaterialTheme.typography.displaySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }

            // Spacer bawah
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun StatusBadge(info: com.yuki.chargingmeter.util.BatteryInfo) {
    val (icon, label, color) = when {
        info.chargeStatus == ChargeStatus.FULL -> Triple(
            Icons.Rounded.BatteryFull,
            "Penuh",
            MaterialTheme.colorScheme.tertiary
        )
        info.isCharging && info.plugType == PlugType.WIRELESS -> Triple(
            Icons.Rounded.SettingsInputAntenna,
            "Wireless Charging",
            MaterialTheme.colorScheme.primary
        )
        info.isCharging -> Triple(
            Icons.Rounded.BoltRounded,
            "${plugLabel(info.plugType)} Charging",
            MaterialTheme.colorScheme.primary
        )
        else -> Triple(Icons.Rounded.BatteryAlert, "", MaterialTheme.colorScheme.error)
    }
    if (label.isNotEmpty()) {
        Surface(
            color = color.copy(alpha = 0.12f),
            shape = MaterialTheme.shapes.small
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
                Text(label, style = MaterialTheme.typography.labelLarge, color = color)
            }
        }
    }
}

@Composable
private fun temperatureContainerColor(temp: Float): androidx.compose.ui.graphics.Color {
    return when {
        temp >= 45f -> MaterialTheme.colorScheme.errorContainer
        temp >= 38f -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.tertiaryContainer
    }
}

@Composable
private fun healthContainerColor(health: BatteryHealth): androidx.compose.ui.graphics.Color {
    return when (health) {
        BatteryHealth.GOOD    -> MaterialTheme.colorScheme.secondaryContainer
        BatteryHealth.OVERHEAT, BatteryHealth.OVER_VOLTAGE, BatteryHealth.FAILURE ->
            MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
}

private fun healthLabel(health: BatteryHealth): String = when (health) {
    BatteryHealth.GOOD        -> "Baik"
    BatteryHealth.OVERHEAT    -> "Terlalu Panas"
    BatteryHealth.DEAD        -> "Rusak"
    BatteryHealth.OVER_VOLTAGE -> "Tegangan Tinggi"
    BatteryHealth.FAILURE     -> "Gagal"
    BatteryHealth.COLD        -> "Terlalu Dingin"
    BatteryHealth.UNKNOWN     -> "Tidak Diketahui"
}

private fun plugLabel(plug: PlugType): String = when (plug) {
    PlugType.USB      -> "USB"
    PlugType.AC       -> "Adaptor AC"
    PlugType.WIRELESS -> "Wireless"
    PlugType.DOCK     -> "Dock"
    PlugType.NONE     -> "Tidak Terhubung"
}
