package com.ai.mirror.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ai.mirror.R
import com.ai.mirror.data.protocol.MirrorProtocol
import com.ai.mirror.utils.NetworkUtils

@Composable
fun DirectConnectDialog(
    onDismiss: () -> Unit,
    onConnect: (ip: String, port: Int) -> Unit
) {
    var ip by remember { mutableStateOf("") }
    var portStr by remember { mutableStateOf(MirrorProtocol.DEFAULT_STREAM_PORT.toString()) }
    var ipError by remember { mutableStateOf(false) }
    var portError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.manual_connect),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.manual_connect_desc),
                    style = MaterialTheme.typography.bodyMedium
                )

                OutlinedTextField(
                    value = ip,
                    onValueChange = {
                        ip = it.trim()
                        ipError = false
                    },
                    label = { Text(stringResource(R.string.target_ip)) },
                    placeholder = { Text("192.168.1.100") },
                    isError = ipError,
                    supportingText = {
                        if (ipError) {
                            Text(stringResource(R.string.invalid_ip), color = MaterialTheme.colorScheme.error)
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = portStr,
                    onValueChange = {
                        portStr = it.trim()
                        portError = false
                    },
                    label = { Text(stringResource(R.string.target_port)) },
                    placeholder = { Text("8888") },
                    isError = portError,
                    supportingText = {
                        if (portError) {
                            Text(stringResource(R.string.invalid_port), color = MaterialTheme.colorScheme.error)
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val isValidIp = NetworkUtils.isValidIp(ip)
                    val port = portStr.toIntOrNull() ?: -1
                    val isValidPort = NetworkUtils.isValidPort(port)

                    if (!isValidIp) ipError = true
                    if (!isValidPort) portError = true

                    if (isValidIp && isValidPort) {
                        onConnect(ip, port)
                    }
                }
            ) {
                Text(stringResource(R.string.connect))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun PairRequestDialog(
    deviceName: String,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onReject,
        title = {
            Text(
                text = stringResource(R.string.pair_request_title),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = stringResource(R.string.pair_request_message, deviceName),
                style = MaterialTheme.typography.bodyLarge
            )
        },
        confirmButton = {
            Button(onClick = onAccept) {
                Text(stringResource(R.string.accept))
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onReject) {
                Text(stringResource(R.string.reject))
            }
        }
    )
}

@Composable
fun PermissionRequiredCard(
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.permission_camera_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.permission_camera_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(stringResource(R.string.grant_permission))
            }
        }
    }
}
