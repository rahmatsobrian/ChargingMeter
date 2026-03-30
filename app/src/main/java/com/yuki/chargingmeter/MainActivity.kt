package com.yuki.chargingmeter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.yuki.chargingmeter.ui.MainScreen
import com.yuki.chargingmeter.ui.theme.ChargingMeterTheme

class MainActivity : ComponentActivity() {

    private val viewModel: BatteryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ChargingMeterTheme {
                MainScreen(viewModel = viewModel)
            }
        }
    }
}
