package com.ai.mirror.ui.receiver

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ai.mirror.R
import com.ai.mirror.data.streaming.ConnectionState
import com.ai.mirror.ui.components.DirectConnectDialog
import com.ai.mirror.ui.components.StatsOverlay
import com.ai.mirror.ui.theme.BackgroundDark
import com.ai.mirror.ui.theme.CardBorderDark
import com.ai.mirror.ui.theme.ErrorRed
import com.ai.mirror.ui.theme.GlassBorder
import com.ai.mirror.ui.theme.GlassSurface
import com.ai.mirror.ui.theme.PrimaryBlue
import com.ai.mirror.ui.theme.PrimaryBlueDark
import com.ai.mirror.ui.theme.PrimaryBlueLight
import com.ai.mirror.ui.theme.SuccessGreen
import com.ai.mirror.ui.theme.SurfaceDark
import com.ai.mirror.ui.theme.TextPrimaryDark
import com.ai.mirror.ui.theme.TextSecondaryDark
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
            .background(BackgroundDark)
            .clickable { showControls = !showControls }
    ) {
        // Video Stream Canvas
        if (uiState.currentBitmap != null) {
            Image(
                bitmap = uiState.currentBitmap!!.asImageBitmap(),
                contentDescription = stringResource(R.string.receiver_title),
                modifier = Modifier.fillMaxSize(),
                contentScale = if (uiState.isFullscreen) ContentScale.Crop else ContentScale.Fit
            )
        } else {
            // Placeholder / Status Display Card
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(SurfaceDark)
                        .border(1.dp, CardBorderDark, RoundedCornerShape(20.dp))
                        .padding(24.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        when (uiState.connectionState) {
                            ConnectionState.CONNECTING -> {
                                CircularProgressIndicator(
                                    color = PrimaryBlueLight,
                                    strokeWidth = 3.dp,
                                    modifier = Modifier.size(48.dp)
                                )
                                Text(
                                    text = uiState.statusMessage.ifBlank { "正在连接发送端…" },
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "目标地址: ${uiState.targetIp}:${uiState.targetPort}",
                                    color = TextSecondaryDark,
                                    fontSize = 12.sp
                                )
                            }
                            ConnectionState.DISCONNECTED, ConnectionState.ERROR, ConnectionState.REJECTED -> {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(WarningOrange.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = WarningOrange,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Text(
                                    text = "连接提示",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = uiState.statusMessage.ifBlank { "未能连接到发送端手机" },
                                    color = WarningOrange,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 18.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Button(
                                        onClick = { viewModel.reconnect() },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("重试连接", fontSize = 13.sp)
                                    }
                                    Button(
                                        onClick = { showDirectConnectDialog = true },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlueDark)
                                    ) {
                                        Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("输入 IP", fontSize = 13.sp)
                                    }
                                }
                            }
                            else -> {
                                Text(
                                    text = "等待连接视频流",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = "请在另一台手机上选择【开启摄像头(发送端)】",
                                    color = TextSecondaryDark,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Button(
                                    onClick = { showDirectConnectDialog = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("手动输入 IP 直连", fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Overlay Controls (Safe insets aware)
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
            ) {
                // Top Action Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back Button
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(GlassSurface)
                            .border(1.dp, GlassBorder, CircleShape)
                            .clickable { onNavigateBack() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Connection Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(GlassSurface)
                            .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (uiState.connectionState == ConnectionState.PAIRED) SuccessGreen
                                        else WarningOrange
                                    )
                            )
                            Text(
                                text = if (uiState.connectionState == ConnectionState.PAIRED) "已连接镜像"
                                else uiState.connectionState.name,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Bottom Controls Dock
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Stats HUD
                    if (uiState.connectionState == ConnectionState.PAIRED) {
                        StatsOverlay(
                            metrics = uiState.metrics,
                            isSender = false,
                            modifier = Modifier.align(Alignment.Start)
                        )
                    }

                    // Bottom Floating Action Pill
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(GlassSurface)
                            .border(1.dp, GlassBorder, RoundedCornerShape(24.dp))
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Mirror Flip Toggle
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable { viewModel.toggleMirrorFlip() }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (uiState.isMirrorFlip) PrimaryBlue.copy(alpha = 0.35f)
                                            else Color.White.copy(alpha = 0.1f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Flip,
                                        contentDescription = "水平镜像翻转",
                                        tint = if (uiState.isMirrorFlip) PrimaryBlueLight else Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (uiState.isMirrorFlip) "镜子模式" else "原画模式",
                                    fontSize = 11.sp,
                                    color = if (uiState.isMirrorFlip) PrimaryBlueLight else TextSecondaryDark
                                )
                            }

                            // Take Snapshot
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable(enabled = uiState.currentBitmap != null) {
                                    viewModel.takeSnapshot()
                                }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Camera,
                                        contentDescription = "拍照保存",
                                        tint = if (uiState.currentBitmap != null) Color.White else Color.Gray,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "拍照截图",
                                    fontSize = 11.sp,
                                    color = TextSecondaryDark
                                )
                            }

                            // Fullscreen Toggle
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable { viewModel.toggleFullscreen() }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (uiState.isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                        contentDescription = "全屏切换",
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (uiState.isFullscreen) "退出全屏" else "全屏显示",
                                    fontSize = 11.sp,
                                    color = TextSecondaryDark
                                )
                            }

                            // Exit / Disconnect
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable {
                                    viewModel.disconnect()
                                    onNavigateBack()
                                }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(ErrorRed.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PowerSettingsNew,
                                        contentDescription = "断开连接",
                                        tint = ErrorRed,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "断开退出",
                                    fontSize = 11.sp,
                                    color = ErrorRed
                                )
                            }
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
