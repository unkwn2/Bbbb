package com.sr.openbyd.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ScreenshotMonitor
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sr.openbyd.R
import com.sr.openbyd.models.ProjectionAutomation
import com.sr.openbyd.ui.viewmodel.MainViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sr.openbyd.ui.viewmodel.ProjectionViewModel
import kotlin.math.roundToInt

@Composable
fun ProjectionTab(
    mainViewModel: MainViewModel,
    modifier: Modifier = Modifier,
    viewModel: ProjectionViewModel = viewModel()
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ProjectionComfortCard(
            viewModel = mainViewModel,
            modifier = Modifier.fillMaxWidth()
        )
        OverlayTestCard(
            viewModel = viewModel,
            mainViewModel = mainViewModel,
            onCastApp = { viewModel.castAppToCluster(it) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun OverlayTestCard(
    viewModel: ProjectionViewModel,
    mainViewModel: MainViewModel,
    onCastApp: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var packageName by remember { mutableStateOf("") }

    LaunchedEffect(packageName) {
        if (packageName.isNotBlank()) {
            viewModel.loadConfigsFor(packageName)
        }
    }

    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LabelText(text = stringResource(R.string.cluster_projection), icon = Icons.Default.ScreenshotMonitor)
                InfoButton(
                    dialogTitle = stringResource(R.string.info_title_cluster_projection),
                    dialogContent = stringResource(R.string.info_desc_cluster_projection_simplified),
                    onDiagnosticsClick = { mainViewModel.showDiagnosticDialog = true }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.cluster_projection_desc),
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(R.string.projection_trouble_tip),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    OutlinedButton(
                        onClick = { mainViewModel.showDiagnosticDialog = true }
                    ) {
                        Text(
                            text = stringResource(R.string.btn_open_diagnostics),
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            AppSearchField(
                label = stringResource(R.string.app_to_cast),
                value = packageName,
                onValueChange = { packageName = it },
                allApps = viewModel.installedApps
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (packageName.isNotBlank()) {
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.cluster_layout_mode),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    InfoButton(
                        dialogTitle = stringResource(R.string.info_title_cluster_layout_mode),
                        dialogContent = stringResource(R.string.info_desc_cluster_layout_mode)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                var projMenuExpanded by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(
                        onClick = { projMenuExpanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(mainViewModel.clusterLayoutMode.labelRes),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    DropdownMenu(
                        expanded = projMenuExpanded,
                        onDismissRequest = { projMenuExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        com.sr.openbyd.data.ClusterLayoutMode.values().forEach { mode ->
                            DropdownMenuItem(
                                text = { Text(stringResource(mode.labelRes)) },
                                onClick = {
                                    mainViewModel.updateClusterLayoutMode(mode)
                                    projMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                if (mainViewModel.clusterLayoutMode != com.sr.openbyd.data.ClusterLayoutMode.FORCE_MINI) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = stringResource(R.string.fullscreen_mode_layout),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            InfoButton(
                                dialogTitle = stringResource(R.string.info_title_byd_map_sync),
                                dialogContent = stringResource(R.string.info_desc_byd_map_sync)
                            )
                        }
                        InfoButton(
                            dialogTitle = stringResource(R.string.info_title_projection_dimensions),
                            dialogContent = stringResource(R.string.info_desc_projection_dimensions)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = viewModel.activeFullWidth.toString(),
                            onValueChange = { newValue ->
                                newValue.toIntOrNull()?.let { viewModel.updateFullWidth(packageName, it) }
                            },
                            label = { Text(stringResource(R.string.width_px)) },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        OutlinedTextField(
                            value = viewModel.activeFullHeight.toString(),
                            onValueChange = { newValue ->
                                newValue.toIntOrNull()?.let { viewModel.updateFullHeight(packageName, it) }
                            },
                            label = { Text(stringResource(R.string.height_px)) },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = viewModel.activeFullXOffset.toString(),
                            onValueChange = { newValue ->
                                val parsed = newValue.toIntOrNull() ?: if (newValue == "-") 0 else null
                                if (parsed != null) {
                                    viewModel.updateFullXOffset(packageName, parsed)
                                }
                            },
                            label = { Text(stringResource(R.string.x_offset_px)) },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        OutlinedTextField(
                            value = viewModel.activeFullYOffset.toString(),
                            onValueChange = { newValue ->
                                val parsed = newValue.toIntOrNull() ?: if (newValue == "-") 0 else null
                                if (parsed != null) {
                                    viewModel.updateFullYOffset(packageName, parsed)
                                }
                            },
                            label = { Text(stringResource(R.string.y_offset_px)) },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = viewModel.activeFullDpi.toString(),
                        onValueChange = { newValue ->
                            newValue.toIntOrNull()?.let { viewModel.updateFullDpi(packageName, it) }
                        },
                        label = { Text(stringResource(R.string.density_dpi)) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                if (mainViewModel.clusterLayoutMode != com.sr.openbyd.data.ClusterLayoutMode.FORCE_FULLSCREEN) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = stringResource(R.string.mini_mode_layout),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            InfoButton(
                                dialogTitle = stringResource(R.string.info_title_byd_map_sync),
                                dialogContent = stringResource(R.string.info_desc_byd_map_sync)
                            )
                        }
                        InfoButton(
                            dialogTitle = stringResource(R.string.info_title_projection_dimensions),
                            dialogContent = stringResource(R.string.info_desc_projection_dimensions)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = viewModel.activeMiniWidth.toString(),
                            onValueChange = { newValue ->
                                newValue.toIntOrNull()?.let { viewModel.updateMiniWidth(packageName, it) }
                            },
                            label = { Text(stringResource(R.string.width_px)) },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        OutlinedTextField(
                            value = viewModel.activeMiniHeight.toString(),
                            onValueChange = { newValue ->
                                newValue.toIntOrNull()?.let { viewModel.updateMiniHeight(packageName, it) }
                            },
                            label = { Text(stringResource(R.string.height_px)) },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = viewModel.activeMiniXOffset.toString(),
                            onValueChange = { newValue ->
                                val parsed = newValue.toIntOrNull() ?: if (newValue == "-") 0 else null
                                if (parsed != null) {
                                    viewModel.updateMiniXOffset(packageName, parsed)
                                }
                            },
                            label = { Text(stringResource(R.string.x_offset_px)) },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        OutlinedTextField(
                            value = viewModel.activeMiniYOffset.toString(),
                            onValueChange = { newValue ->
                                val parsed = newValue.toIntOrNull() ?: if (newValue == "-") 0 else null
                                if (parsed != null) {
                                    viewModel.updateMiniYOffset(packageName, parsed)
                                }
                            },
                            label = { Text(stringResource(R.string.y_offset_px)) },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = viewModel.activeMiniDpi.toString(),
                        onValueChange = { newValue ->
                            newValue.toIntOrNull()?.let { viewModel.updateMiniDpi(packageName, it) }
                        },
                        label = { Text(stringResource(R.string.density_dpi)) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                ProjectionAutomationsSection(viewModel = viewModel, packageName = packageName)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val currentProjected = viewModel.currentProjectedPackage
                
                if (packageName.isNotBlank()) {
                    OutlinedButton(
                        onClick = { viewModel.resetAppConfig(packageName) },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        Text(stringResource(R.string.diag_btn_reset_defaults), style = MaterialTheme.typography.labelSmall)
                    }

                    if (currentProjected != null) {
                        Button(
                            onClick = { viewModel.pullAppBackToMain(currentProjected) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Text(stringResource(R.string.stop_projection), style = MaterialTheme.typography.labelSmall)
                        }

                        if (currentProjected == packageName) {
                            Button(
                                onClick = { viewModel.refreshClusterProjection(packageName) },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp)
                            ) {
                                Text(stringResource(R.string.refresh_projection), style = MaterialTheme.typography.labelSmall)
                            }
                        } else {
                            Button(
                                onClick = { onCastApp(packageName) },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp)
                            ) {
                                Text(stringResource(R.string.cast_and_save), style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    } else {
                        Button(
                            onClick = { onCastApp(packageName) },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Text(stringResource(R.string.cast_and_save), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                } else {
                    if (currentProjected != null) {
                        Button(
                            onClick = { viewModel.pullAppBackToMain(currentProjected) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Text(stringResource(R.string.stop_projection), style = MaterialTheme.typography.labelSmall)
                        }
                    } else {
                        Button(
                            onClick = { onCastApp(packageName) },
                            enabled = false,
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Text(stringResource(R.string.cast_and_save), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProjectionComfortCard(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LabelText(
                    text = stringResource(R.string.projection_gradients_title),
                    icon = Icons.Default.Layers
                )
                InfoButton(
                    dialogTitle = stringResource(R.string.info_title_projection_comfort),
                    dialogContent = stringResource(R.string.info_desc_projection_comfort)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.projection_gradients_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.projection_gradients_toggle),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Switch(
                    checked = viewModel.projectionGradientsEnabled,
                    onCheckedChange = { viewModel.updateProjectionGradientsEnabled(it) }
                )
            }

            AnimatedVisibility(visible = viewModel.projectionGradientsEnabled) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.projection_gradients_theme_label),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    var menuExpanded by remember { mutableStateOf(false) }
                    Box {
                        OutlinedButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(viewModel.projectionGradientMode.labelRes),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            com.sr.openbyd.data.ProjectionGradientMode.values().forEach { mode ->
                                DropdownMenuItem(
                                    text = { Text(stringResource(mode.labelRes)) },
                                    onClick = {
                                        viewModel.updateProjectionGradientMode(mode)
                                        menuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.projection_gradients_left),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stringResource(R.string.projection_gradients_left_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Switch(
                            checked = viewModel.projectionLeftGradientEnabled,
                            onCheckedChange = { viewModel.updateProjectionLeftGradientEnabled(it) }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.projection_gradients_intensity),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${viewModel.projectionGradientIntensity}%",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Slider(
                        value = viewModel.projectionGradientIntensity.toFloat(),
                        onValueChange = { viewModel.updateProjectionGradientIntensity(it.toInt()) },
                        valueRange = 0f..100f,
                        steps = 99
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// App Projection Automations Composable UI
// ---------------------------------------------------------------------------

private const val DELAY_STEP_MS = 500L
private const val DELAY_MAX_MS  = 15_000L
private const val DELAY_STEPS   = (DELAY_MAX_MS / DELAY_STEP_MS).toInt() // 30 steps

private fun sliderToMs(value: Float): Long = (value.roundToInt() * DELAY_STEP_MS)
private fun msToSlider(ms: Long): Float = (ms.toFloat() / DELAY_STEP_MS)

@Composable
fun ProjectionAutomationsSection(
    viewModel: ProjectionViewModel,
    packageName: String
) {
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }

    if (showAddDialog) {
        AddProjectionAutomationDialog(
            viewModel = viewModel,
            packageName = packageName,
            onDismiss = { showAddDialog = false }
        )
    }

    Spacer(modifier = Modifier.height(16.dp))
    HorizontalDivider()
    Spacer(modifier = Modifier.height(16.dp))

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LabelText(
                    text = stringResource(R.string.automations_title),
                    icon = Icons.Default.PlayArrow
                )
                Spacer(modifier = Modifier.width(4.dp))
                InfoButton(
                    dialogTitle = stringResource(R.string.info_title_app_automations),
                    dialogContent = stringResource(R.string.info_desc_app_automations)
                )
            }
            Button(
                onClick = { showAddDialog = true },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.btn_add_task), style = MaterialTheme.typography.labelMedium)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.automations_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (viewModel.activeProjectionAutomations.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.empty_automations_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                viewModel.activeProjectionAutomations.sortedBy { it.index }.forEach { action ->
                    ProjectionAutomationItem(
                        action = action,
                        packageName = packageName,
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

@Composable
fun ProjectionAutomationItem(
    action: ProjectionAutomation,
    packageName: String,
    viewModel: ProjectionViewModel
) {
    val context = LocalContext.current
    var isEditing by remember(action.index) { mutableStateOf(false) }

    // States for fields
    var delaySlider by remember(action) { mutableFloatStateOf(msToSlider(action.delayMs)) }
    var xPercentText by remember(action) { mutableStateOf(action.xPercent.toString()) }
    var yPercentText by remember(action) { mutableStateOf(action.yPercent.toString()) }

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Collapsed header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isEditing = !isEditing },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = "#${action.index + 1}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.label_delay_ms, action.delayMs.toInt()),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Tap Horizontal (X): ${String.format("%.1f", action.xPercent)}%, Vertical (Y): ${String.format("%.1f", action.yPercent)}%",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isEditing) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null
                    )
                }
            }

            AnimatedVisibility(visible = isEditing) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    // Delay slider/text
                    Text(
                        text = stringResource(R.string.label_delay_header_ms),
                        style = MaterialTheme.typography.labelMedium
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Slider(
                            value = delaySlider,
                            onValueChange = { delaySlider = it },
                            valueRange = 0f..30f, // up to 15 seconds (500ms steps)
                            steps = 29,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "${sliderToMs(delaySlider)} ms",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(80.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // X / Y coordinate fields
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = xPercentText,
                            onValueChange = { newValue ->
                                if (newValue.isEmpty() || newValue.toFloatOrNull() != null || newValue == ".") {
                                    xPercentText = newValue
                                }
                            },
                            label = { Text(stringResource(R.string.x_percent_label)) },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )

                        OutlinedTextField(
                            value = yPercentText,
                            onValueChange = { newValue ->
                                if (newValue.isEmpty() || newValue.toFloatOrNull() != null || newValue == ".") {
                                    yPercentText = newValue
                                }
                            },
                            label = { Text(stringResource(R.string.y_percent_label)) },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                viewModel.deleteProjectionAutomation(packageName, action.index)
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.task_deleted, action.index + 1),
                                    Toast.LENGTH_SHORT
                                ).show()
                                isEditing = false
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.btn_delete))
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { isEditing = false }) {
                                Text(stringResource(R.string.btn_cancel))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    val xVal = xPercentText.toFloatOrNull() ?: 0f
                                    val yVal = yPercentText.toFloatOrNull() ?: 0f
                                    // Clamp values to 0..100
                                    val xClamped = xVal.coerceIn(0f, 100f)
                                    val yClamped = yVal.coerceIn(0f, 100f)
                                    
                                    val updated = action.copy(
                                        delayMs = sliderToMs(delaySlider),
                                        xPercent = xClamped,
                                        yPercent = yClamped
                                    )
                                    viewModel.updateProjectionAutomation(packageName, updated)
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.task_saved),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    isEditing = false
                                }
                            ) {
                                Text(stringResource(R.string.save))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddProjectionAutomationDialog(
    viewModel: ProjectionViewModel,
    packageName: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var delaySlider by remember { mutableFloatStateOf(4f) } // default to 2000 ms (step 4)
    var xPercentText by remember { mutableStateOf("") }
    var yPercentText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_add_task_title)) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 350.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Delay header and slider
                Text(
                    text = stringResource(R.string.label_delay_header_ms),
                    style = MaterialTheme.typography.labelMedium
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Slider(
                        value = delaySlider,
                        onValueChange = { delaySlider = it },
                        valueRange = 0f..30f,
                        steps = 29,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "${sliderToMs(delaySlider)} ms",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(80.dp)
                    )
                }

                HorizontalDivider()

                // X & Y position
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = xPercentText,
                        onValueChange = { newValue ->
                            if (newValue.isEmpty() || newValue.toFloatOrNull() != null || newValue == ".") {
                                xPercentText = newValue
                            }
                        },
                        label = { Text(stringResource(R.string.x_percent_label)) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )

                    OutlinedTextField(
                        value = yPercentText,
                        onValueChange = { newValue ->
                            if (newValue.isEmpty() || newValue.toFloatOrNull() != null || newValue == ".") {
                                yPercentText = newValue
                            }
                        },
                        label = { Text(stringResource(R.string.y_percent_label)) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val xVal = xPercentText.toFloatOrNull() ?: 0f
                    val yVal = yPercentText.toFloatOrNull() ?: 0f
                    // Clamp values to 0..100
                    val xClamped = xVal.coerceIn(0f, 100f)
                    val yClamped = yVal.coerceIn(0f, 100f)

                    val action = ProjectionAutomation(
                        index = 0, // reindexed inside viewModel's add
                        delayMs = sliderToMs(delaySlider),
                        xPercent = xClamped,
                        yPercent = yClamped
                    )
                    viewModel.addProjectionAutomation(packageName, action)
                    Toast.makeText(
                        context,
                        context.getString(R.string.task_saved),
                        Toast.LENGTH_SHORT
                    ).show()
                    onDismiss()
                }
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancel))
            }
        }
    )
}
