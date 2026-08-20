package com.ai.mirror.ui.sender

import android.app.Application
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageProxy
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ai.mirror.AiMirrorApplication
import com.ai.mirror.data.discovery.DeviceDiscoveryManager
import com.ai.mirror.data.model.DeviceRole
import com.ai.mirror.data.model.StreamConfig
import com.ai.mirror.data.protocol.PairRequestPayload
import com.ai.mirror.data.repository.SettingsRepository
import com.ai.mirror.data.streaming.FrameProcessor
import com.ai.mirror.data.streaming.StreamMetrics
import com.ai.mirror.data.streaming.StreamServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SenderUiState(
    val isStreaming: Boolean = false,
    val connectedReceivers: Int = 0,
    val isFrontCamera: Boolean = false,
    val isTorchOn: Boolean = false,
    val pendingPairRequest: PairRequestPayload? = null,
    val metrics: StreamMetrics = StreamMetrics(),
    val streamConfig: StreamConfig = StreamConfig()
)

class SenderViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository: SettingsRepository =
        (application as AiMirrorApplication).settingsRepository

    private val _uiState = MutableStateFlow(
        SenderUiState(
            streamConfig = StreamConfig(
                resolution = settingsRepository.settings.value.resolution,
                fps = settingsRepository.settings.value.fps,
                quality = settingsRepository.settings.value.quality
            )
        )
    )
    val uiState: StateFlow<SenderUiState> = _uiState.asStateFlow()

    private var streamServer: StreamServer? = null
    private var discoveryManager: DeviceDiscoveryManager? = null

    private var lastFrameTime = 0L
    private var pendingPairCallback: ((Boolean) -> Unit)? = null

    init {
        startServerAndDiscovery()
        startMetricsPolling()
    }

    private fun startServerAndDiscovery() {
        val settings = settingsRepository.settings.value

        streamServer = StreamServer(
            port = settings.serverPort,
            deviceId = settingsRepository.deviceId,
            deviceName = settingsRepository.deviceName,
            autoAcceptPairing = settings.autoAcceptPairing,
            onPairRequested = { request, callback ->
                pendingPairCallback = callback
                _uiState.value = _uiState.value.copy(pendingPairRequest = request)
            }
        )
        streamServer?.start()

        val context = getApplication<Application>()
        discoveryManager = DeviceDiscoveryManager(
            context = context,
            deviceId = settingsRepository.deviceId,
            deviceName = settingsRepository.deviceName,
            role = DeviceRole.SENDER,
            streamPort = settings.serverPort
        )
        discoveryManager?.startDiscovery()

        _uiState.value = _uiState.value.copy(isStreaming = true)

        // Observe connected clients
        viewModelScope.launch {
            streamServer?.connectedClientsCount?.collect { count ->
                _uiState.value = _uiState.value.copy(connectedReceivers = count)
            }
        }
    }

    fun onFrameAvailable(imageProxy: ImageProxy) {
        val targetFps = _uiState.value.streamConfig.fps.fps
        val frameIntervalMs = 1000L / targetFps
        val now = System.currentTimeMillis()

        if (now - lastFrameTime < frameIntervalMs) {
            imageProxy.close()
            return
        }
        lastFrameTime = now

        val quality = _uiState.value.streamConfig.quality.qualityPercent
        val rotation = imageProxy.imageInfo.rotationDegrees
        val width = imageProxy.width
        val height = imageProxy.height

        viewModelScope.launch(Dispatchers.Default) {
            val jpeg = FrameProcessor.processImageProxyToJpeg(imageProxy, quality = quality)
            imageProxy.close()

            if (jpeg != null) {
                withContext(Dispatchers.IO) {
                    streamServer?.broadcastFrame(
                        jpegBytes = jpeg,
                        width = width,
                        height = height,
                        rotation = rotation
                    )
                }
            }
        }
    }

    fun switchCamera() {
        _uiState.value = _uiState.value.copy(
            isFrontCamera = !_uiState.value.isFrontCamera,
            isTorchOn = false // Front camera usually doesn't have torch
        )
    }

    fun toggleTorch() {
        _uiState.value = _uiState.value.copy(isTorchOn = !_uiState.value.isTorchOn)
    }

    fun acceptPairRequest() {
        pendingPairCallback?.invoke(true)
        pendingPairCallback = null
        _uiState.value = _uiState.value.copy(pendingPairRequest = null)
    }

    fun rejectPairRequest() {
        pendingPairCallback?.invoke(false)
        pendingPairCallback = null
        _uiState.value = _uiState.value.copy(pendingPairRequest = null)
    }

    private fun startMetricsPolling() {
        viewModelScope.launch {
            while (isActive) {
                delay(1000)
                val metrics = streamServer?.stats?.getMetrics() ?: StreamMetrics()
                _uiState.value = _uiState.value.copy(metrics = metrics)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        discoveryManager?.stopDiscovery()
        discoveryManager = null
        streamServer?.stop()
        streamServer = null
    }
}
