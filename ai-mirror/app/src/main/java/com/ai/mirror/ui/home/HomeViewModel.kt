package com.ai.mirror.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ai.mirror.AiMirrorApplication
import com.ai.mirror.data.discovery.DeviceDiscoveryManager
import com.ai.mirror.data.model.DeviceRole
import com.ai.mirror.data.model.DiscoveredDevice
import com.ai.mirror.data.repository.SettingsRepository
import com.ai.mirror.utils.NetworkUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class HomeUiState(
    val localIp: String = "",
    val isWifiConnected: Boolean = false,
    val selectedRole: DeviceRole = DeviceRole.SENDER,
    val deviceName: String = "",
    val isScanning: Boolean = false
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository: SettingsRepository =
        (application as AiMirrorApplication).settingsRepository

    private val _uiState = MutableStateFlow(
        HomeUiState(
            deviceName = settingsRepository.deviceName,
            selectedRole = settingsRepository.settings.value.preferredRole
        )
    )
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var discoveryManager: DeviceDiscoveryManager? = null

    val discoveredDevices: StateFlow<List<DiscoveredDevice>> = MutableStateFlow<List<DiscoveredDevice>>(emptyList())
        get() = discoveryManager?.discoveredDevices ?: field

    init {
        checkNetworkStatus()
        startNetworkMonitoring()
        startDiscovery()
    }

    fun selectRole(role: DeviceRole) {
        _uiState.value = _uiState.value.copy(selectedRole = role)
        val currentSettings = settingsRepository.settings.value
        settingsRepository.updateSettings(currentSettings.copy(preferredRole = role))
        restartDiscovery()
    }

    fun refreshNetwork() {
        checkNetworkStatus()
    }

    private fun checkNetworkStatus() {
        val context = getApplication<Application>()
        val isWifi = NetworkUtils.isWifiConnected(context)
        val ip = NetworkUtils.getLocalIpAddress() ?: ""
        _uiState.value = _uiState.value.copy(
            isWifiConnected = isWifi,
            localIp = ip,
            deviceName = settingsRepository.deviceName
        )
    }

    private fun startNetworkMonitoring() {
        viewModelScope.launch {
            while (isActive) {
                delay(3000)
                checkNetworkStatus()
            }
        }
    }

    fun startDiscovery() {
        stopDiscovery()
        val context = getApplication<Application>()
        val settings = settingsRepository.settings.value
        discoveryManager = DeviceDiscoveryManager(
            context = context,
            deviceId = settingsRepository.deviceId,
            deviceName = settingsRepository.deviceName,
            role = _uiState.value.selectedRole,
            streamPort = settings.serverPort
        )
        discoveryManager?.startDiscovery()
        _uiState.value = _uiState.value.copy(isScanning = true)
    }

    fun stopDiscovery() {
        discoveryManager?.stopDiscovery()
        discoveryManager = null
        _uiState.value = _uiState.value.copy(isScanning = false)
    }

    private fun restartDiscovery() {
        startDiscovery()
    }

    override fun onCleared() {
        super.onCleared()
        stopDiscovery()
    }
}
