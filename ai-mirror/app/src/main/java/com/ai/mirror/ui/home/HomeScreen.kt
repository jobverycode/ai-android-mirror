package com.ai.mirror.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ai.mirror.R
import com.ai.mirror.data.model.DeviceRole
import com.ai.mirror.data.model.DiscoveredDevice
import com.ai.mirror.ui.components.DirectConnectDialog
import com.ai.mirror.ui.theme.AccentCyan
import com.ai.mirror.ui.theme.AccentIndigo
import com.ai.mirror.ui.theme.BackgroundDark
import com.ai.mirror.ui.theme.CardBorderDark
import com.ai.mirror.ui.theme.CardDark
import com.ai.mirror.ui.theme.GlassBorder
import com.ai.mirror.ui.theme.GlassSurface
import com.ai.mirror.ui.theme.PrimaryBlue
import com.ai.mirror.ui.theme.PrimaryBlueDark
import com.ai.mirror.ui.theme.PrimaryBlueLight
import com.ai.mirror.ui.theme.SuccessGreen
import com.ai.mirror.ui.theme.SuccessGreenGlow
import com.ai.mirror.ui.theme.SurfaceDark
import com.ai.mirror.ui.theme.TextPrimaryDark
import com.ai.mirror.ui.theme.TextSecondaryDark
import com.ai.mirror.ui.theme.TextTertiaryDark
import com.ai.mirror.ui.theme.WarningOrange
import com.ai.mirror.ui.theme.WarningOrangeGlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToSender: () -> Unit,
    onNavigateToReceiver: (targetIp: String?, targetPort: Int?) -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val discoveredDevices by viewModel.discoveredDevices.collectAsState(initial = emptyList())
    var showDirectConnectDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(PrimaryBlue, AccentCyan)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = stringResource(R.string.app_name),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                color = Color.White
                            )
                            Text(
                                text = stringResource(R.string.app_description),
                                fontSize = 11.sp,
                                color = TextSecondaryDark
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showDirectConnectDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = stringResource(R.string.manual_connect),
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.settings),
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundDark
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Local Device & Network Status Card
            item {
                Spacer(modifier = Modifier.height(2.dp))
                LocalNetworkBadge(
                    deviceName = uiState.deviceName,
                    localIp = uiState.localIp,
                    isWifiConnected = uiState.isWifiConnected,
                    onRefresh = { viewModel.refreshNetwork() }
                )
            }

            // Section Header: Select Role
            item {
                Text(
                    text = stringResource(R.string.role_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Role 1: Sender (Camera)
            item {
                RoleHeroCard(
                    title = stringResource(R.string.role_sender_title),
                    subtitle = stringResource(R.string.role_sender_desc),
                    badgeText = "推流端",
                    icon = Icons.Default.CameraAlt,
                    gradient = listOf(PrimaryBlueDark, PrimaryBlue),
                    buttonText = "开启摄像头 (发送端)",
                    onClick = {
                        viewModel.selectRole(DeviceRole.SENDER)
                        onNavigateToSender()
                    }
                )
            }

            // Role 2: Receiver (Mirror Screen)
            item {
                RoleHeroCard(
                    title = stringResource(R.string.role_receiver_title),
                    subtitle = stringResource(R.string.role_receiver_desc),
                    badgeText = "显示端",
                    icon = Icons.Default.Tv,
                    gradient = listOf(Color(0xFF4338CA), Color(0xFF6366F1)),
                    buttonText = "打开镜子显示 (接收端)",
                    onClick = {
                        viewModel.selectRole(DeviceRole.RECEIVER)
                        onNavigateToReceiver(null, null)
                    }
                )
            }

            // Section Header: Discovered LAN Devices
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.discovered_devices),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        if (uiState.isScanning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = PrimaryBlueLight
                            )
                        }
                    }
                    IconButton(
                        onClick = { viewModel.startDiscovery() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.retry),
                            tint = PrimaryBlueLight
                        )
                    }
                }
            }

            // Discovered Devices List
            if (discoveredDevices.isEmpty()) {
                item {
                    NoDevicesBanner()
                }
            } else {
                items(discoveredDevices, key = { it.endpoint }) { device ->
                    DiscoveredDeviceCard(
                        device = device,
                        onConnect = {
                            if (device.isSender) {
                                onNavigateToReceiver(device.ip, device.port)
                            } else {
                                onNavigateToSender()
                            }
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showDirectConnectDialog) {
        DirectConnectDialog(
            onDismiss = { showDirectConnectDialog = false },
            onConnect = { ip, port ->
                showDirectConnectDialog = false
                onNavigateToReceiver(ip, port)
            }
        )
    }
}

@Composable
fun LocalNetworkBadge(
    deviceName: String,
    localIp: String,
    isWifiConnected: Boolean,
    onRefresh: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceDark)
            .border(1.dp, CardBorderDark, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PhoneAndroid,
                        contentDescription = null,
                        tint = PrimaryBlueLight,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = deviceName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (isWifiConnected) SuccessGreenGlow else WarningOrangeGlow
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (isWifiConnected) Icons.Default.Wifi else Icons.Default.WifiOff,
                            contentDescription = null,
                            tint = if (isWifiConnected) SuccessGreen else WarningOrange,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (isWifiConnected) "已连接 Wi-Fi" else "未连接 Wi-Fi",
                            fontSize = 11.sp,
                            color = if (isWifiConnected) SuccessGreen else WarningOrange,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            if (localIp.isNotBlank()) {
                Text(
                    text = "本机局域网 IP: $localIp",
                    fontSize = 13.sp,
                    color = TextSecondaryDark,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            } else {
                Text(
                    text = stringResource(R.string.wifi_warning),
                    fontSize = 12.sp,
                    color = WarningOrange
                )
            }
        }
    }
}

@Composable
fun RoleHeroCard(
    title: String,
    subtitle: String,
    badgeText: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    gradient: List<Color>,
    buttonText: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(SurfaceDark)
            .border(1.dp, CardBorderDark, RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.linearGradient(gradient)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.White.copy(alpha = 0.1f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = badgeText,
                                fontSize = 10.sp,
                                color = TextSecondaryDark,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = TextSecondaryDark,
                        lineHeight = 16.sp
                    )
                }
            }

            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = gradient.last()
                )
            ) {
                Text(
                    text = buttonText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun DiscoveredDeviceCard(
    device: DiscoveredDevice,
    onConnect: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CardDark)
            .border(1.dp, CardBorderDark, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = device.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White
                    )

                    if (device.isStreaming) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(SuccessGreenGlow)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "正在推流",
                                fontSize = 10.sp,
                                color = SuccessGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (device.isSender) "发送端" else "接收端",
                                fontSize = 10.sp,
                                color = TextSecondaryDark
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${device.ip}:${device.port}",
                    fontSize = 12.sp,
                    color = TextSecondaryDark,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = onConnect,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (device.isStreaming) SuccessGreen else PrimaryBlue
                )
            ) {
                Text(
                    text = if (device.isStreaming) "连接镜像" else "连接配对",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun NoDevicesBanner() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceDark.copy(alpha = 0.6f))
            .border(1.dp, CardBorderDark, RoundedCornerShape(14.dp))
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = TextSecondaryDark,
                modifier = Modifier.size(22.dp)
            )
            Column {
                Text(
                    text = "正在持续扫描局域网中的镜像设备…",
                    fontSize = 13.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "请确保两台手机连接同一 Wi-Fi，且一台手机已点击【开启摄像头】",
                    fontSize = 11.sp,
                    color = TextSecondaryDark,
                    lineHeight = 15.sp
                )
            }
        }
    }
}
