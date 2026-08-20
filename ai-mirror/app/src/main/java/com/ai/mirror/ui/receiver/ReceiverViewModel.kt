package com.ai.mirror.ui.receiver

import android.app.Application
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ai.mirror.AiMirrorApplication
import com.ai.mirror.data.model.AppSettings
import com.ai.mirror.data.repository.SettingsRepository
import com.ai.mirror.data.streaming.ConnectionState
import com.ai.mirror.data.streaming.FrameProcessor
import com.ai.mirror.data.streaming.StreamClient
import com.ai.mirror.data.streaming.StreamMetrics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

data class ReceiverUiState(
    val connectionState: ConnectionState = ConnectionState.IDLE,
    val statusMessage: String = "",
    val currentBitmap: Bitmap? = null,
    val isMirrorFlip: Boolean = true,
    val isFullscreen: Boolean = false,
    val metrics: StreamMetrics = StreamMetrics(),
    val targetIp: String = "",
    val targetPort: Int = 8888,
    val snapshotMessage: String? = null
)

class ReceiverViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository: SettingsRepository =
        (application as AiMirrorApplication).settingsRepository

    private val _uiState = MutableStateFlow(
        ReceiverUiState(
            isMirrorFlip = settingsRepository.settings.value.mirrorFlipHorizontal,
            targetPort = settingsRepository.settings.value.serverPort
        )
    )
    val uiState: StateFlow<ReceiverUiState> = _uiState.asStateFlow()

    private var streamClient: StreamClient? = null

    init {
        initClient()
    }

    private fun initClient() {
        streamClient = StreamClient(
            deviceId = settingsRepository.deviceId,
            deviceName = settingsRepository.deviceName
        )

        // Observe connection state
        viewModelScope.launch {
            streamClient?.connectionState?.collect { state ->
                _uiState.value = _uiState.value.copy(connectionState = state)
            }
        }

        // Observe status message
        viewModelScope.launch {
            streamClient?.statusMessage?.collect { msg ->
                _uiState.value = _uiState.value.copy(statusMessage = msg)
            }
        }

        // Observe received frames
        viewModelScope.launch(Dispatchers.Default) {
            streamClient?.latestFrame?.collect { frame ->
                if (frame != null) {
                    val rawBitmap = BitmapFactory.decodeByteArray(
                        frame.jpegData,
                        0,
                        frame.jpegData.size
                    )
                    if (rawBitmap != null) {
                        val processedBitmap = FrameProcessor.rotateBitmap(
                            source = rawBitmap,
                            angle = frame.rotation.toFloat(),
                            flipHorizontal = _uiState.value.isMirrorFlip
                        )
                        withContext(Dispatchers.Main) {
                            _uiState.value = _uiState.value.copy(currentBitmap = processedBitmap)
                        }
                    }
                }
            }
        }

        // Observe stream stats
        viewModelScope.launch {
            while (isActive) {
                kotlinx.coroutines.delay(1000)
                val metrics = streamClient?.stats?.getMetrics() ?: StreamMetrics()
                _uiState.value = _uiState.value.copy(metrics = metrics)
            }
        }
    }

    fun connect(ip: String, port: Int) {
        _uiState.value = _uiState.value.copy(targetIp = ip, targetPort = port)
        streamClient?.connect(ip, port)
    }

    fun reconnect() {
        val ip = _uiState.value.targetIp
        val port = _uiState.value.targetPort
        if (ip.isNotBlank()) {
            connect(ip, port)
        }
    }

    fun disconnect() {
        streamClient?.disconnect()
    }

    fun toggleMirrorFlip() {
        val newFlip = !_uiState.value.isMirrorFlip
        _uiState.value = _uiState.value.copy(isMirrorFlip = newFlip)
        val currentSettings = settingsRepository.settings.value
        settingsRepository.updateSettings(currentSettings.copy(mirrorFlipHorizontal = newFlip))
    }

    fun toggleFullscreen() {
        _uiState.value = _uiState.value.copy(isFullscreen = !_uiState.value.isFullscreen)
    }

    fun takeSnapshot() {
        val bitmap = _uiState.value.currentBitmap ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                val filename = "AIMirror_${System.currentTimeMillis()}.jpg"

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                        put(
                            MediaStore.MediaColumns.RELATIVE_PATH,
                            Environment.DIRECTORY_PICTURES + "/AIMirror"
                        )
                    }
                    val uri = context.contentResolver.insert(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        contentValues
                    )
                    if (uri != null) {
                        context.contentResolver.openOutputStream(uri)?.use { stream ->
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)
                        }
                    }
                } else {
                    val imagesDir = Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_PICTURES
                    )
                    val mirrorDir = File(imagesDir, "AIMirror")
                    if (!mirrorDir.exists()) mirrorDir.mkdirs()
                    val imageFile = File(mirrorDir, filename)
                    FileOutputStream(imageFile).use { stream ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)
                    }
                }
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(snapshotMessage = "saved")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(snapshotMessage = "failed")
                }
            }
        }
    }

    fun clearSnapshotMessage() {
        _uiState.value = _uiState.value.copy(snapshotMessage = null)
    }

    override fun onCleared() {
        super.onCleared()
        disconnect()
    }
}
