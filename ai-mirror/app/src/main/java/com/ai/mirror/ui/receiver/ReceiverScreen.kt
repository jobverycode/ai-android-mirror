package com.ai.mirror.ui.receiver

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ai.mirror.R
import com.ai.mirror.data.streaming.ConnectionState
import com.ai.mirror.ui.components.DirectConnectDialog
import com.ai.mirror.ui.components.StatsOverlay
import com.ai.mirror.ui.theme.PrimaryBlue
import com.ai.mirror.ui.theme.SuccessGreen
import com.ai.mirror.ui.theme.WarningOrange

@Composable
fun ReceiverScreen(
    targetIp: String?,
    targetPort: Int?,
    onNavigateBack: () -> Unit,
    viewModel: ReceiverViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var showControls by remember { mutableStateOf(true) }
    var showDirectConnectDialog by remember { mutableStateOf(false) }

    LaunchedEffect(targetIp, targetPort) {
        if (!targetIp.isNullOrBlank() && targetPort != null && targetPort > 0) {
            viewModel.connect(targetIp, targetPort)
        }
    }

    LaunchedEffect(uiState.snapshotMessage) {
        uiState.snapshotMessage?.let { msg ->
            val text = if (msg == "saved") {
                context.getString(R.string.snapshot_saved)
            } else {
                context.getString(R.string.snapshot_failed)
            }
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
            viewModel.clearSnapshotMessage()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable { showControls = !showControls }
    ) {
        // Video Stream Render Canvas
        if (uiState.currentBitmap != null) {
            Image(
                bitmap = uiState.currentBitmap!!.asImageBitmap(),
                contentDescription = stringResource(R.string.receiver_title),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        } else {
            // Idle / Connecting Placeholder
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                when (uiState.connectionState) {
                    ConnectionState.CONNECTING -> {
                        CircularProgressIndicator(color = PrimaryBlue)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = uiState.statusMessage.ifBlank { stringResource(R.string.connecting) },
                            color = Color.White,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    ConnectionState.DISCONNECTED, ConnectionState.ERROR, ConnectionState.REJECTED -> {
                        Text(
                            text = uiState.statusMessage.ifBlank { stringResource(R.string.waiting_stream) },
                            color = WarningOrange,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(onClick = { viewModel.reconnect() }) {
                                Icon(Icons.Default.Refresh, contentDescription = null)
                                Spacer(modifier = Modifier.size(6.dp))
                                Text(stringResource(R.string.reconnect))
                            }
                            Button(onClick = { showDirectConnectDialog = true }) {
                                Icon(Icons.Default.Link, contentDescription = null)
                                Spacer(modifier = Modifier.size(6.dp))
                                Text(stringResource(R.string.manual_connect))
                            }
                        }
                    }
                    else -> {
                        Text(
                            text = stringResource(R.string.waiting_stream),
                            color = Color.LightGray,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { showDirectConnectDialog = true }) {
                            Icon(Icons.Default.Link, contentDescription = null)
                            Spacer(modifier = Modifier.size(6.dp))
                            Text(stringResource(R.string.manual_connect))
                        }
                    }
                }
            }
        }

        // Overlay Controls
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Top Action Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp, start = 16.dp, end = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = Color.White
                        )
                    }

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color.Black.copy(alpha = 0.6f)
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (uiState.connectionState == ConnectionState.PAIRED) SuccessGreen
                                        else WarningOrange
                                    )
                            )
                            Text(
                                text = if (uiState.connectionState == ConnectionState.PAIRED) stringResource(R.string.connected)
                                else uiState.connectionState.name,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Bottom Controls HUD
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp, start = 16.dp, end = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Stats HUD
                    if (uiState.connectionState == ConnectionState.PAIRED) {
                        StatsOverlay(
                            metrics = uiState.metrics,
                            isSender = false,
                            modifier = Modifier.align(Alignment.Start)
                        )
                    }

                    // Bottom Action Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color.Black.copy(alpha = 0.6f))
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Mirror Flip Toggle
                        IconButton(onClick = { viewModel.toggleMirrorFlip() }) {
                            Icon(
                                imageVector = Icons.Default.Flip,
                                contentDescription = stringResource(R.string.mirror_flip_horizontal),
                                tint = if (uiState.isMirrorFlip) PrimaryBlue else Color.White
                            )
                        }

                        // Take Snapshot
                        IconButton(
                            onClick = { viewModel.takeSnapshot() },
                            enabled = uiState.currentBitmap != null
                        ) {
                            Icon(
                                imageVector = Icons.Default.Camera,
                                contentDescription = stringResource(R.string.take_snapshot),
                                tint = Color.White
                            )
                        }

                        // Fullscreen Toggle
                        IconButton(onClick = { viewModel.toggleFullscreen() }) {
                            Icon(
                                imageVector = if (uiState.isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                contentDescription = stringResource(R.string.fullscreen),
                                tint = Color.White
                            )
                        }

                        // Manual Connect
                        IconButton(onClick = { showDirectConnectDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Link,
                                contentDescription = stringResource(R.string.manual_connect),
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDirectConnectDialog) {
        DirectConnectDialog(
            onDismiss = { showDirectConnectDialog = false },
            onConnect = { ip, port ->
                showDirectConnectDialog = false
                viewModel.connect(ip, port)
            }
        )
    }
}
