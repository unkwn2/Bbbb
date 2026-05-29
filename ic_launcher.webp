package com.sr.openbyd.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.core.graphics.drawable.toBitmap
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.sr.openbyd.R
import com.sr.openbyd.ui.viewmodel.AppInfo

@Composable
fun InfoButton(
    dialogTitle: String,
    dialogContent: String,
    onDiagnosticsClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }
    IconButton(
        onClick = { showDialog = true },
        modifier = modifier.size(24.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = "Help Info",
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
            modifier = Modifier.size(18.dp)
        )
    }
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = {
                Text(
                    text = dialogTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = dialogContent,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (onDiagnosticsClick != null) {
                        OutlinedButton(
                            onClick = {
                                showDialog = false
                                onDiagnosticsClick()
                            }
                        ) {
                            Text(stringResource(R.string.menu_diagnostic_view))
                        }
                    }
                    TextButton(onClick = { showDialog = false }) {
                        Text(stringResource(R.string.btn_ok))
                    }
                }
            }
        )
    }
}

@Composable
fun LabelText(text: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun AppSearchField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    allApps: List<AppInfo>,
    showCompatibilityBadges: Boolean = false
) {
    val filteredApps = remember(value) {
        if (value.length >= 1) {
            allApps.filter {
                it.name.contains(value, ignoreCase = true) ||
                it.packageName.contains(value, ignoreCase = true)
            }
        } else {
            emptyList()
        }
    }

    Column {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (value.isNotEmpty()) {
                    IconButton(onClick = { onValueChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = null)
                    }
                }
            }
        )

        if (filteredApps.isNotEmpty() && !allApps.any { it.packageName == value }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp)
                    .padding(top = 4.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                LazyColumn {
                    items(filteredApps) { app ->
                        val context = LocalContext.current
                        val isSystem = remember(app.packageName) {
                            if (showCompatibilityBadges) {
                                try {
                                    val appInfo = context.packageManager.getApplicationInfo(app.packageName, 0)
                                    (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
                                } catch (e: Exception) {
                                    false
                                }
                            } else false
                        }

                        val isPatched = remember(app.packageName, allApps) {
                            if (showCompatibilityBadges) {
                                checkIfPatched(context, app.packageName)
                            } else false
                        }

                        ListItem(
                            modifier = Modifier.clickable {
                                onValueChange(app.packageName)
                            },
                            leadingContent = {
                                app.icon?.let {
                                    Image(
                                        bitmap = it.toBitmap().asImageBitmap(),
                                        contentDescription = null,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            },
                            headlineContent = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(app.name, style = MaterialTheme.typography.bodyMedium)
                                    if (showCompatibilityBadges) {
                                        if (isSystem) {
                                            Surface(
                                                color = Color.Gray.copy(alpha = 0.2f),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = stringResource(R.string.patch_status_system),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color.Gray,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }
                                        } else if (isPatched) {
                                            Surface(
                                                color = Color(0xFF4CAF50).copy(alpha = 0.2f),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = stringResource(R.string.patch_status_patched),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color(0xFF4CAF50),
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }
                                        } else {
                                            Surface(
                                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.2f),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = stringResource(R.string.patch_status_unpatched),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            },
                            supportingContent = { Text(app.packageName, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
        }
    }
}

private fun checkIfPatched(context: Context, packageName: String): Boolean {
    return try {
        val pm = context.packageManager
        val appInfo = pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
        val metaData = appInfo.metaData
        metaData != null && (metaData.containsKey("BYD_SUPPORT_SPLIT_ACTIVITY") || metaData.getInt("BYD_SUPPORT_SPLIT_ACTIVITY", 0) == 1)
    } catch (e: Exception) {
        false
    }
}
