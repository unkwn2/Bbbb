package com.sr.openbyd.ui.screens

import android.content.Context
import android.hardware.display.DisplayManager
import android.util.DisplayMetrics
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Monitor
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sr.openbyd.R
import com.sr.openbyd.ui.overlay.ClusterOverlayManager
import com.sr.openbyd.ui.overlay.MainOverlayManager
import com.sr.openbyd.ui.viewmodel.DiagnosticViewModel
import com.sr.openbyd.ui.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class DisplayInfo(
    val id: Int,
    val name: String,
    val width: Int,
    val height: Int,
    val densityDpi: Int,
    val isMain: Boolean,
    val isDashboard: Boolean,
    val nameMatches: Boolean,
    val isFallbackUsed: Boolean,
    val isMainManual: Boolean = false,
    val isDashboardManual: Boolean = false
)

@Composable
fun DiagnosticDialog(
    mainViewModel: MainViewModel,
    onDismiss: () -> Unit,
    viewModel: DiagnosticViewModel = viewModel()
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    var displaysList by remember { mutableStateOf(emptyList<DisplayInfo>()) }
    var searchQuery by remember { mutableStateOf("") }
    var copySuccess by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    var layoutMenuExpanded by remember { mutableStateOf(false) }

    // Helper to refresh recognized displays list using standard and custom criteria
    fun refreshDisplays() {
        val dm = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        val displays = dm.displays
        val mainDisplayId = MainOverlayManager.getMainDisplayId(context)
        val clusterDisplayId = ClusterOverlayManager.getClusterDisplayId(context)

        val overrideMainId = viewModel.overrideMainDisplayId
        val overrideClusterId = viewModel.overrideClusterDisplayId

        displaysList = displays.map { d ->
            val size = android.graphics.Point()
            @Suppress("DEPRECATION")
            d.getRealSize(size)
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            d.getRealMetrics(metrics)

            val nameMatches = d.name.contains("fission", ignoreCase = true) || 
                              d.name.contains("cluster", ignoreCase = true)
            val isDashboard = d.displayId == clusterDisplayId
            val isMain = d.displayId == mainDisplayId

            DisplayInfo(
                id = d.displayId,
                name = d.name,
                width = size.x,
                height = size.y,
                densityDpi = metrics.densityDpi,
                isMain = isMain,
                isDashboard = isDashboard,
                nameMatches = nameMatches,
                isFallbackUsed = isDashboard && !nameMatches && d.displayId == 2,
                isMainManual = isMain && (overrideMainId == d.displayId),
                isDashboardManual = isDashboard && (overrideClusterId == d.displayId)
            )
        }
    }

    // Auto-fetch on launch and whenever overrides change
    LaunchedEffect(viewModel.overrideMainDisplayId, viewModel.overrideClusterDisplayId) {
        refreshDisplays()
        viewModel.refreshDiagnostics()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Premium Header with subtle gradient
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary
                                )
                            )
                        )
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Terminal,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = stringResource(R.string.diagnostic_view_title),
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                // Split Pane / Responsive Row for landscape displays
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Left Column: Screen & Display Diagnostics (Weight 1)
                    val hasOverrides = viewModel.overrideMainDisplayId != -1 || viewModel.overrideClusterDisplayId != -1
                    val onSetMain: (Int) -> Unit = { id ->
                        viewModel.setOverrideMainDisplayId(id)
                        Toast.makeText(context, context.getString(R.string.diag_msg_main_set, id), Toast.LENGTH_SHORT).show()
                    }
                    val onSetDashboard: (Int) -> Unit = { id ->
                        viewModel.setOverrideClusterDisplayId(id)
                        Toast.makeText(context, context.getString(R.string.diag_msg_dashboard_set, id), Toast.LENGTH_SHORT).show()
                    }
                    val onTestOverlay: (Int) -> Unit = { id ->
                        viewModel.testOverlayLaunch(id)
                    }

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        item {
                            SystemHealthCard(viewModel = viewModel)
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.diag_module_screens),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (hasOverrides) {
                                        Button(
                                            onClick = {
                                                viewModel.resetDisplayOverrides()
                                                Toast.makeText(context, context.getString(R.string.diag_msg_resets), Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                                            ),
                                            modifier = Modifier.padding(end = 8.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = stringResource(R.string.diag_btn_reset_defaults),
                                                style = MaterialTheme.typography.labelMedium
                                            )
                                        }
                                    }
                                    IconButton(onClick = { refreshDisplays() }) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = stringResource(R.string.diag_btn_refresh),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.dilink_compatibility_mode),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
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
                                                    text = stringResource(mainViewModel.diLinkMode.labelRes),
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
                                            com.sr.openbyd.data.DiLinkMode.values().forEach { mode ->
                                                DropdownMenuItem(
                                                    text = { Text(stringResource(mode.labelRes)) },
                                                    onClick = {
                                                        mainViewModel.updateDiLinkMode(mode)
                                                        menuExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = stringResource(R.string.dilink_status_title),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = stringResource(mainViewModel.diLinkStatusRes),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.cluster_layout_mode),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Box {
                                        OutlinedButton(
                                            onClick = { layoutMenuExpanded = true },
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
                                            expanded = layoutMenuExpanded,
                                            onDismissRequest = { layoutMenuExpanded = false },
                                            modifier = Modifier.fillMaxWidth(0.9f)
                                        ) {
                                            com.sr.openbyd.data.ClusterLayoutMode.values().forEach { mode ->
                                                DropdownMenuItem(
                                                    text = { Text(stringResource(mode.labelRes)) },
                                                    onClick = {
                                                        mainViewModel.updateClusterLayoutMode(mode)
                                                        layoutMenuExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.projection_gradients_title),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = stringResource(R.string.projection_gradients_desc),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = stringResource(R.string.projection_gradients_toggle),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Switch(
                                            checked = mainViewModel.projectionGradientsEnabled,
                                            onCheckedChange = { mainViewModel.updateProjectionGradientsEnabled(it) }
                                        )
                                    }
                                    AnimatedVisibility(visible = mainViewModel.projectionGradientsEnabled) {
                                        Column(modifier = Modifier.fillMaxWidth()) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = stringResource(R.string.projection_gradients_theme_label),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            var gradMenuExpanded by remember { mutableStateOf(false) }
                                            Box {
                                                OutlinedButton(
                                                    onClick = { gradMenuExpanded = true },
                                                    modifier = Modifier.fillMaxWidth(),
                                                    shape = RoundedCornerShape(8.dp)
                                                 ) {
                                                     Row(
                                                         modifier = Modifier.fillMaxWidth(),
                                                         horizontalArrangement = Arrangement.SpaceBetween,
                                                         verticalAlignment = Alignment.CenterVertically
                                                     ) {
                                                         Text(
                                                             text = stringResource(mainViewModel.projectionGradientMode.labelRes),
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
                                                     expanded = gradMenuExpanded,
                                                     onDismissRequest = { gradMenuExpanded = false },
                                                     modifier = Modifier.fillMaxWidth(0.9f)
                                                 ) {
                                                     com.sr.openbyd.data.ProjectionGradientMode.values().forEach { mode ->
                                                         DropdownMenuItem(
                                                             text = { Text(stringResource(mode.labelRes)) },
                                                             onClick = {
                                                                 mainViewModel.updateProjectionGradientMode(mode)
                                                                 gradMenuExpanded = false
                                                             }
                                                         )
                                                     }
                                                 }
                                             }

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
                                                     checked = mainViewModel.projectionLeftGradientEnabled,
                                                     onCheckedChange = { mainViewModel.updateProjectionLeftGradientEnabled(it) }
                                                 )
                                             }

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
                                                     text = "${mainViewModel.projectionGradientIntensity}%",
                                                     style = MaterialTheme.typography.bodyMedium,
                                                     fontWeight = FontWeight.Bold,
                                                     color = MaterialTheme.colorScheme.primary
                                                 )
                                             }
                                             Spacer(modifier = Modifier.height(4.dp))
                                             Slider(
                                                 value = mainViewModel.projectionGradientIntensity.toFloat(),
                                                 onValueChange = { mainViewModel.updateProjectionGradientIntensity(it.toInt()) },
                                                 valueRange = 0f..100f,
                                                 steps = 99
                                             )
                                         }
                                     }
                                }
                            }
                        }

                        item {
                            Text(
                                text = stringResource(R.string.diag_displays_recognized, displaysList.size),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        items(displaysList) { display ->
                            DisplayDiagnosticCard(
                                display = display,
                                onSetMain = { onSetMain(display.id) },
                                onSetDashboard = { onSetDashboard(display.id) },
                                onTestOverlay = { onTestOverlay(display.id) }
                            )
                        }
                    }

                    // Divider line
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )

                    // Right Column: System Log Viewer (Weight 1.2 for more console room)
                    Column(
                        modifier = Modifier
                            .weight(1.2f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.diag_module_logs),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // Keyword filter field
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            label = { Text("Filter Logs") },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )

                        // Hacker style selective log window
                        ElevatedCard(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .border(
                                    1.dp,
                                    Color(0xFF333333),
                                    RoundedCornerShape(8.dp)
                                ),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = Color(0xFF0F0F0F) // Pitch black terminal console
                            )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp)
                            ) {
                                if (viewModel.diagnosticLogs.isEmpty()) {
                                    Text(
                                        text = stringResource(R.string.diag_no_logs),
                                        color = Color.Gray,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.align(Alignment.Center)
                                    )
                                } else {
                                    val filteredLogs = remember(viewModel.diagnosticLogs, searchQuery) {
                                        if (searchQuery.isEmpty()) {
                                            viewModel.diagnosticLogs
                                        } else {
                                            viewModel.diagnosticLogs.lines()
                                                .filter { it.contains(searchQuery, ignoreCase = true) }
                                                .joinToString("\n")
                                        }
                                    }

                                    val verticalScrollState = rememberScrollState()
                                    val horizontalScrollState = rememberScrollState()

                                    // SelectionContainer wraps Text to support highlighting and copy-selection gestures
                                    SelectionContainer {
                                        Text(
                                            text = filteredLogs.ifEmpty { "No matching logs found for query." },
                                            color = Color(0xFF00FF00), // Pure aesthetic hacker green!
                                            style = MaterialTheme.typography.bodySmall,
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .verticalScroll(verticalScrollState)
                                                .horizontalScroll(horizontalScrollState)
                                        )
                                    }
                                }
                            }
                        }

                        // Log actions row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { viewModel.executeLogcat() },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ),
                                enabled = !viewModel.isExecutingLogcat
                            ) {
                                if (viewModel.isExecutingLogcat) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(Icons.Default.BugReport, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(stringResource(R.string.diag_btn_execute_logcat))
                                }
                            }

                            Button(
                                onClick = {
                                    if (viewModel.diagnosticLogs.isNotEmpty()) {
                                        clipboardManager.setText(AnnotatedString(viewModel.diagnosticLogs))
                                        Toast.makeText(context, context.getString(R.string.diag_logs_copied), Toast.LENGTH_SHORT).show()
                                        scope.launch {
                                            copySuccess = true
                                            delay(2000)
                                            copySuccess = false
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                enabled = viewModel.diagnosticLogs.isNotEmpty() && !copySuccess,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (copySuccess) Color(0xFF2E7D32) else MaterialTheme.colorScheme.secondary
                                )
                            ) {
                                Icon(
                                    imageVector = if (copySuccess) Icons.Default.Check else Icons.Default.ContentCopy,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (copySuccess) "Copied!" else stringResource(R.string.diag_btn_copy_clipboard)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DisplayDiagnosticCard(
    display: DisplayInfo,
    onSetMain: () -> Unit,
    onSetDashboard: () -> Unit,
    onTestOverlay: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = when {
                    display.isMain -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    display.isDashboard -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                    else -> MaterialTheme.colorScheme.outlineVariant
                },
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Monitor,
                        contentDescription = null,
                        tint = when {
                            display.isMain -> MaterialTheme.colorScheme.primary
                            display.isDashboard -> MaterialTheme.colorScheme.secondary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.diag_display_id, display.id),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Explicit match label
                if (display.nameMatches) {
                    Text(
                        text = "Match: Name Pattern",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF2E7D32),
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .background(
                                color = Color(0xFFE8F5E9),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Text(
                text = stringResource(R.string.diag_display_name, display.name),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = stringResource(
                    R.string.diag_display_metrics,
                    display.width,
                    display.height,
                    display.densityDpi
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = FontFamily.Monospace
            )

            // Diagnostic role badges
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                when {
                    display.isMain -> {
                        Badge(
                            text = stringResource(R.string.diag_display_role_main),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            textColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Badge(
                            text = if (display.isMainManual) stringResource(R.string.diag_badge_manual) else stringResource(R.string.diag_badge_auto),
                            color = if (display.isMainManual) Color(0xFFE8F5E9) else Color(0xFFECEFF1),
                            textColor = if (display.isMainManual) Color(0xFF2E7D32) else Color(0xFF455A64)
                        )
                    }
                    display.isFallbackUsed -> {
                        Badge(
                            text = stringResource(R.string.diag_display_role_dashboard_fallback),
                            color = Color(0xFFFFF3E0), // Soft warning orange background
                            textColor = Color(0xFFE65100) // Deep warning orange text
                        )
                    }
                    display.isDashboard -> {
                        Badge(
                            text = stringResource(R.string.diag_display_role_dashboard),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            textColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Badge(
                            text = if (display.isDashboardManual) stringResource(R.string.diag_badge_manual) else stringResource(R.string.diag_badge_auto),
                            color = if (display.isDashboardManual) Color(0xFFE8F5E9) else Color(0xFFECEFF1),
                            textColor = if (display.isDashboardManual) Color(0xFF2E7D32) else Color(0xFF455A64)
                        )
                    }
                    else -> {
                        Badge(
                            text = stringResource(R.string.diag_display_role_none),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            textColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onSetMain,
                    enabled = !display.isMain,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Text(stringResource(R.string.diag_btn_set_main), style = MaterialTheme.typography.labelSmall)
                }

                Button(
                    onClick = onSetDashboard,
                    enabled = !display.isDashboard,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Text(stringResource(R.string.diag_btn_set_dashboard), style = MaterialTheme.typography.labelSmall)
                }

                Button(
                    onClick = onTestOverlay,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary
                    ),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Text(stringResource(R.string.diag_btn_test_overlay), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
fun SystemHealthCard(viewModel: DiagnosticViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.diag_health_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            HealthIndicator(
                label = stringResource(R.string.diag_health_proxy),
                status = if (viewModel.isProxyConnected) stringResource(R.string.diag_status_connected) else stringResource(R.string.diag_status_disconnected),
                isOk = viewModel.isProxyConnected,
                infoTitle = stringResource(R.string.diag_info_proxy_title),
                infoContent = stringResource(R.string.diag_info_proxy_desc)
            )

            HealthIndicator(
                label = stringResource(R.string.diag_health_sdk),
                status = if (viewModel.hasSdkAccess) stringResource(R.string.diag_status_ok) else stringResource(R.string.diag_status_no_access),
                isOk = viewModel.hasSdkAccess,
                infoTitle = stringResource(R.string.diag_info_sdk_title),
                infoContent = stringResource(R.string.diag_info_sdk_desc)
            )

            HealthIndicator(
                label = stringResource(R.string.diag_health_accessibility),
                status = if (viewModel.isAccessibilityServiceRunning) stringResource(R.string.diag_status_running) else stringResource(R.string.diag_status_not_running),
                isOk = viewModel.isAccessibilityServiceRunning,
                infoTitle = stringResource(R.string.diag_info_accessibility_title),
                infoContent = stringResource(R.string.diag_info_accessibility_desc)
            )
        }
    }
}

@Composable
fun HealthIndicator(
    label: String,
    status: String,
    isOk: Boolean,
    infoTitle: String? = null,
    infoContent: String? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (infoTitle != null && infoContent != null) {
                InfoButton(dialogTitle = infoTitle, dialogContent = infoContent)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        color = if (isOk) Color(0xFF4CAF50) else Color(0xFFF44336),
                        shape = RoundedCornerShape(4.dp)
                    )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = status,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = if (isOk) Color(0xFF2E7D32) else Color(0xFFD32F2F)
            )
        }
    }
}

@Composable
fun Badge(text: String, color: Color, textColor: Color) {
    Box(
        modifier = Modifier
            .background(color, RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = textColor,
            fontWeight = FontWeight.Bold
        )
    }
}
