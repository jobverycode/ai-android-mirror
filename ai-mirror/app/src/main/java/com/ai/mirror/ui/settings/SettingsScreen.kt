package com.ai.mirror.ui.settings

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ai.mirror.R
import com.ai.mirror.data.model.CompressionQuality
import com.ai.mirror.data.model.FpsPreset
import com.ai.mirror.data.model.ResolutionPreset
import com.ai.mirror.ui.theme.BackgroundDark
import com.ai.mirror.ui.theme.CardBorderDark
import com.ai.mirror.ui.theme.CardDark
import com.ai.mirror.ui.theme.PrimaryBlue
import com.ai.mirror.ui.theme.PrimaryBlueLight
import com.ai.mirror.ui.theme.SurfaceDark
import com.ai.mirror.ui.theme.TextSecondaryDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val settings by viewModel.settings.collectAsState()
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showResolutionDialog by remember { mutableStateOf(false) }
    var showFpsDialog by remember { mutableStateOf(false) }
    var showQualityDialog by remember { mutableStateOf(false) }
    var showDeviceNameDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_title),
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // General Settings
            item {
                SettingsSectionHeader(
                    title = stringResource(R.string.settings_general),
                    icon = Icons.Default.Language
                )
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurfaceDark)
                        .border(1.dp, CardBorderDark, RoundedCornerShape(16.dp))
                ) {
                    Column {
                        SettingsItem(
                            title = stringResource(R.string.settings_language),
                            subtitle = when (settings.language) {
                                "zh" -> stringResource(R.string.language_zh)
                                "en" -> stringResource(R.string.language_en)
                                else -> stringResource(R.string.language_system)
                            },
                            onClick = { showLanguageDialog = true }
                        )
                        HorizontalDivider(color = CardBorderDark, modifier = Modifier.padding(horizontal = 16.dp))
                        SettingsItem(
                            title = stringResource(R.string.device_name),
                            subtitle = settings.deviceName,
                            onClick = { showDeviceNameDialog = true }
                        )
                        HorizontalDivider(color = CardBorderDark, modifier = Modifier.padding(horizontal = 16.dp))
                        SettingsSwitchItem(
                            title = stringResource(R.string.keep_screen_on),
                            subtitle = stringResource(R.string.keep_screen_on_desc),
                            checked = settings.keepScreenOn,
                            onCheckedChange = { viewModel.setKeepScreenOn(it) }
                        )
                    }
                }
            }

            // Video & Stream Quality Settings
            item {
                SettingsSectionHeader(
                    title = stringResource(R.string.settings_video),
                    icon = Icons.Default.Videocam
                )
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurfaceDark)
                        .border(1.dp, CardBorderDark, RoundedCornerShape(16.dp))
                ) {
                    Column {
                        SettingsItem(
                            title = stringResource(R.string.stream_resolution),
                            subtitle = when (settings.resolution) {
                                ResolutionPreset.SD_480P -> stringResource(R.string.resolution_sd)
                                ResolutionPreset.HD_720P -> stringResource(R.string.resolution_hd)
                                ResolutionPreset.FHD_1080P -> stringResource(R.string.resolution_fhd)
                            },
                            onClick = { showResolutionDialog = true }
                        )
                        HorizontalDivider(color = CardBorderDark, modifier = Modifier.padding(horizontal = 16.dp))
                        SettingsItem(
                            title = stringResource(R.string.stream_fps),
                            subtitle = when (settings.fps) {
                                FpsPreset.FPS_15 -> stringResource(R.string.fps_15)
                                FpsPreset.FPS_24 -> stringResource(R.string.fps_24)
                                FpsPreset.FPS_30 -> stringResource(R.string.fps_30)
                                FpsPreset.FPS_60 -> stringResource(R.string.fps_60)
                            },
                            onClick = { showFpsDialog = true }
                        )
                        HorizontalDivider(color = CardBorderDark, modifier = Modifier.padding(horizontal = 16.dp))
                        SettingsItem(
                            title = stringResource(R.string.stream_quality),
                            subtitle = when (settings.quality) {
                                CompressionQuality.LOW -> stringResource(R.string.quality_low)
                                CompressionQuality.MEDIUM -> stringResource(R.string.quality_medium)
                                CompressionQuality.HIGH -> stringResource(R.string.quality_high)
                            },
                            onClick = { showQualityDialog = true }
                        )
                    }
                }
            }

            // Network & Pairing Settings
            item {
                SettingsSectionHeader(
                    title = stringResource(R.string.settings_network),
                    icon = Icons.Default.NetworkCheck
                )
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurfaceDark)
                        .border(1.dp, CardBorderDark, RoundedCornerShape(16.dp))
                ) {
                    Column {
                        SettingsSwitchItem(
                            title = stringResource(R.string.auto_accept_pairing),
                            subtitle = stringResource(R.string.auto_accept_desc),
                            checked = settings.autoAcceptPairing,
                            onCheckedChange = { viewModel.setAutoAcceptPairing(it) }
                        )
                        HorizontalDivider(color = CardBorderDark, modifier = Modifier.padding(horizontal = 16.dp))
                        SettingsItem(
                            title = stringResource(R.string.server_port),
                            subtitle = settings.serverPort.toString(),
                            onClick = { }
                        )
                    }
                }
            }

            // About Section
            item {
                SettingsSectionHeader(
                    title = stringResource(R.string.about),
                    icon = Icons.Default.Info
                )
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurfaceDark)
                        .border(1.dp, CardBorderDark, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.app_name) + " v1.0.0",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.about_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondaryDark,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Language Dialog
    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(stringResource(R.string.settings_language)) },
            text = {
                Column {
                    LanguageOption("system", stringResource(R.string.language_system), settings.language) {
                        viewModel.setLanguage("system")
                        showLanguageDialog = false
                    }
                    LanguageOption("zh", stringResource(R.string.language_zh), settings.language) {
                        viewModel.setLanguage("zh")
                        showLanguageDialog = false
                    }
                    LanguageOption("en", stringResource(R.string.language_en), settings.language) {
                        viewModel.setLanguage("en")
                        showLanguageDialog = false
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }

    // Resolution Dialog
    if (showResolutionDialog) {
        AlertDialog(
            onDismissRequest = { showResolutionDialog = false },
            title = { Text(stringResource(R.string.stream_resolution)) },
            text = {
                Column {
                    ResolutionOption(ResolutionPreset.SD_480P, stringResource(R.string.resolution_sd), settings.resolution) {
                        viewModel.setResolution(it)
                        showResolutionDialog = false
                    }
                    ResolutionOption(ResolutionPreset.HD_720P, stringResource(R.string.resolution_hd), settings.resolution) {
                        viewModel.setResolution(it)
                        showResolutionDialog = false
                    }
                    ResolutionOption(ResolutionPreset.FHD_1080P, stringResource(R.string.resolution_fhd), settings.resolution) {
                        viewModel.setResolution(it)
                        showResolutionDialog = false
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showResolutionDialog = false }) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }

    // FPS Dialog
    if (showFpsDialog) {
        AlertDialog(
            onDismissRequest = { showFpsDialog = false },
            title = { Text(stringResource(R.string.stream_fps)) },
            text = {
                Column {
                    FpsOption(FpsPreset.FPS_15, stringResource(R.string.fps_15), settings.fps) {
                        viewModel.setFps(it)
                        showFpsDialog = false
                    }
                    FpsOption(FpsPreset.FPS_24, stringResource(R.string.fps_24), settings.fps) {
                        viewModel.setFps(it)
                        showFpsDialog = false
                    }
                    FpsOption(FpsPreset.FPS_30, stringResource(R.string.fps_30), settings.fps) {
                        viewModel.setFps(it)
                        showFpsDialog = false
                    }
                    FpsOption(FpsPreset.FPS_60, stringResource(R.string.fps_60), settings.fps) {
                        viewModel.setFps(it)
                        showFpsDialog = false
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFpsDialog = false }) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }

    // Quality Dialog
    if (showQualityDialog) {
        AlertDialog(
            onDismissRequest = { showQualityDialog = false },
            title = { Text(stringResource(R.string.stream_quality)) },
            text = {
                Column {
                    QualityOption(CompressionQuality.LOW, stringResource(R.string.quality_low), settings.quality) {
                        viewModel.setQuality(it)
                        showQualityDialog = false
                    }
                    QualityOption(CompressionQuality.MEDIUM, stringResource(R.string.quality_medium), settings.quality) {
                        viewModel.setQuality(it)
                        showQualityDialog = false
                    }
                    QualityOption(CompressionQuality.HIGH, stringResource(R.string.quality_high), settings.quality) {
                        viewModel.setQuality(it)
                        showQualityDialog = false
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showQualityDialog = false }) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }

    // Device Name Dialog
    if (showDeviceNameDialog) {
        var tempName by remember { mutableStateOf(settings.deviceName) }
        AlertDialog(
            onDismissRequest = { showDeviceNameDialog = false },
            title = { Text(stringResource(R.string.device_name)) },
            text = {
                OutlinedTextField(
                    value = tempName,
                    onValueChange = { tempName = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (tempName.isNotBlank()) {
                        viewModel.setDeviceName(tempName.trim())
                    }
                    showDeviceNameDialog = false
                }) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeviceNameDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
fun SettingsSectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = PrimaryBlueLight,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = PrimaryBlueLight,
            fontSize = 14.sp
        )
    }
}

@Composable
fun SettingsItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondaryDark
            )
        }
    }
}

@Composable
fun SettingsSwitchItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondaryDark
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun LanguageOption(tag: String, label: String, currentTag: String, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = tag == currentTag, onClick = onSelect)
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun ResolutionOption(preset: ResolutionPreset, label: String, current: ResolutionPreset, onSelect: (ResolutionPreset) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect(preset) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = preset == current, onClick = { onSelect(preset) })
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun FpsOption(preset: FpsPreset, label: String, current: FpsPreset, onSelect: (FpsPreset) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect(preset) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = preset == current, onClick = { onSelect(preset) })
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun QualityOption(quality: CompressionQuality, label: String, current: CompressionQuality, onSelect: (CompressionQuality) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect(quality) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = quality == current, onClick = { onSelect(quality) })
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}
