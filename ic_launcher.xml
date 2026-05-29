package com.sr.openbyd.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ScreenshotMonitor
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sr.openbyd.R
import com.sr.openbyd.data.LanguageMode
import com.sr.openbyd.data.SplitScreenMode
import com.sr.openbyd.data.ThemeMode
import com.sr.openbyd.services.SteeringWheelAccessibilityService
import com.sr.openbyd.ui.icons.FontAwesomeCarAlt
import com.sr.openbyd.ui.icons.TablerSteeringWheel
import com.sr.openbyd.ui.viewmodel.MainViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onLanguageChange: (LanguageMode) -> Unit
) {
    var showConfigDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    var menuExpanded by remember { mutableStateOf(false) }
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.Menu, contentDescription = "Settings")
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_configuration)) },
                            onClick = {
                                menuExpanded = false
                                showConfigDialog = true
                            },
                            leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_debug_wheel)) },
                            onClick = {
                                menuExpanded = false
                                viewModel.showDebugDialog = true
                            },
                            leadingIcon = { Icon(Icons.Default.BugReport, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_diagnostic_view)) },
                            onClick = {
                                menuExpanded = false
                                viewModel.showDiagnosticDialog = true
                            },
                            leadingIcon = { Icon(Icons.Default.Terminal, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.car_controls)) },
                            onClick = {
                                menuExpanded = false
                                viewModel.showCarControlsDialog = true
                            },
                            leadingIcon = { Icon(FontAwesomeCarAlt, contentDescription = null) }
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.ScreenshotMonitor, contentDescription = null) },
                    label = { Text(stringResource(R.string.tab_projection)) }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(TablerSteeringWheel, contentDescription = null) },
                    label = { Text(stringResource(R.string.tab_steering_wheel)) }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.PlayArrow, contentDescription = null) },
                    label = { Text(stringResource(R.string.tab_startup)) }
                )
                if (viewModel.splitScreenMode == SplitScreenMode.NATIVE_UI7_DILINK_6) {
                    NavigationBarItem(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        icon = { Icon(Icons.Default.Build, contentDescription = null) },
                        label = { Text(stringResource(R.string.tab_split_patching)) }
                    )
                }
            }
        }
    ) { paddingValues ->

        if (showConfigDialog) {
            ConfigurationDialog(
                viewModel = viewModel,
                onDismiss = { showConfigDialog = false },
                onThemeChange = { viewModel.updateTheme(it) },
                onLanguageChange = onLanguageChange
            )
        }

        if (viewModel.showDebugDialog) {
            DebugConsoleDialog(
                onDismiss = { viewModel.showDebugDialog = false }
            )
        }

        if (viewModel.showDiagnosticDialog) {
            DiagnosticDialog(
                mainViewModel = viewModel,
                onDismiss = { viewModel.showDiagnosticDialog = false }
            )
        }

        if (viewModel.showCarControlsDialog) {
            CarControlsDialog(
                mainViewModel = viewModel,
                onDismiss = { viewModel.showCarControlsDialog = false }
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            when (selectedTab) {
                0 -> {
                    ProjectionTab(mainViewModel = viewModel)
                }
                1 -> {
                    SteeringWheelTab(mainViewModel = viewModel)
                }
                2 -> {
                    StartupSequenceScreen(mainViewModel = viewModel)
                }
                3 -> {
                    if (viewModel.splitScreenMode == SplitScreenMode.NATIVE_UI7_DILINK_6) {
                        SplitPatchTab(mainViewModel = viewModel)
                    } else {
                        selectedTab = 0
                        ProjectionTab(mainViewModel = viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun ConfigurationDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onThemeChange: (ThemeMode) -> Unit,
    onLanguageChange: (LanguageMode) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.menu_configuration)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(stringResource(R.string.settings_theme), style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = false,
                            onClick = { onThemeChange(mode) },
                            label = { Text(stringResource(mode.labelRes)) }
                        )
                    }
                }

                HorizontalDivider()

                Text(stringResource(R.string.settings_language), style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LanguageMode.entries.forEach { lang ->
                        FilterChip(
                            selected = false,
                            onClick = { onLanguageChange(lang) },
                            label = { Text(lang.label) }
                        )
                    }
                }

                HorizontalDivider()

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    var showCompatInfoDialog by remember { mutableStateOf(false) }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.settings_split_screen_compatibility), style = MaterialTheme.typography.labelLarge)
                            Spacer(modifier = Modifier.width(8.dp))
                            InfoButton(
                                dialogTitle = stringResource(R.string.info_title_split_screen),
                                dialogContent = stringResource(R.string.info_desc_split_screen)
                            )
                        }
                        val currentMode = viewModel.splitScreenMode
                        if (currentMode == SplitScreenMode.COMPAT_TAP || currentMode == SplitScreenMode.PROJECTED) {
                            IconButton(onClick = { showCompatInfoDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = stringResource(
                                        if (currentMode == SplitScreenMode.COMPAT_TAP) R.string.split_screen_compat_tap_desc else R.string.split_screen_projected_desc
                                    )
                                )
                            }
                        }
                    }

                    if (showCompatInfoDialog) {
                        val currentMode = viewModel.splitScreenMode
                        val titleRes = if (currentMode == SplitScreenMode.COMPAT_TAP) R.string.split_screen_compat_tap_desc else R.string.split_screen_projected_desc
                        val textRes = if (currentMode == SplitScreenMode.COMPAT_TAP) R.string.split_screen_compat_tap_info else R.string.split_screen_projected_info
                        AlertDialog(
                            onDismissRequest = { showCompatInfoDialog = false },
                            title = { Text(stringResource(titleRes)) },
                            text = { Text(stringResource(textRes)) },
                            confirmButton = {
                                TextButton(onClick = { showCompatInfoDialog = false }) {
                                    Text(stringResource(R.string.btn_ok))
                                }
                            }
                        )
                    }

                    var splitScreenMenuExpanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { splitScreenMenuExpanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(viewModel.splitScreenMode.labelRes))
                        }
                        DropdownMenu(
                            expanded = splitScreenMenuExpanded,
                            onDismissRequest = { splitScreenMenuExpanded = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            SplitScreenMode.entries.forEach { mode ->
                                DropdownMenuItem(
                                    text = { Text(stringResource(mode.labelRes)) },
                                    onClick = {
                                        viewModel.updateSplitScreenMode(mode)
                                        splitScreenMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    if (viewModel.splitScreenMode == SplitScreenMode.COMPAT_TAP) {
                        Spacer(modifier = Modifier.height(4.dp))
                        var xInput by remember(viewModel.splitScreenCompatTapX) { mutableStateOf(viewModel.splitScreenCompatTapX.toString()) }
                        var yInput by remember(viewModel.splitScreenCompatTapY) { mutableStateOf(viewModel.splitScreenCompatTapY.toString()) }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = xInput,
                                onValueChange = { newValue ->
                                    xInput = newValue
                                    newValue.toIntOrNull()?.let { viewModel.updateSplitScreenCompatTapX(it) }
                                },
                                label = { Text(stringResource(R.string.split_screen_compat_tap_x)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = yInput,
                                onValueChange = { newValue ->
                                    yInput = newValue
                                    newValue.toIntOrNull()?.let { viewModel.updateSplitScreenCompatTapY(it) }
                                },
                                label = { Text(stringResource(R.string.split_screen_compat_tap_y)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.btn_ok)) }
        }
    )
}

@Composable
fun DebugConsoleDialog(onDismiss: () -> Unit) {
    DisposableEffect(Unit) {
        SteeringWheelAccessibilityService.isDebugMode = true
        onDispose {
            SteeringWheelAccessibilityService.isDebugMode = false
            SteeringWheelAccessibilityService.lastCapturedKeyCode = null
        }
    }

    var lastKey by remember { mutableStateOf<Int?>(null) }
    val isServiceRunning = remember { SteeringWheelAccessibilityService.isConnected }

    LaunchedEffect(Unit) {
        while (true) {
            lastKey = SteeringWheelAccessibilityService.lastCapturedKeyCode
            delay(100)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = Color.Black,
                contentColor = Color(0xFF00FF00)
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(240.dp)
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Terminal, contentDescription = null, tint = Color(0xFF00FF00), modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(stringResource(R.string.debug_console), style = MaterialTheme.typography.titleMedium)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF00FF00))
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (!isServiceRunning) {
                            Text(
                                text = stringResource(R.string.debug_service_warning),
                                color = Color.Yellow,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                        }

                        Text(
                            text = if (lastKey == null) stringResource(R.string.debug_waiting_key)
                                   else stringResource(R.string.debug_last_key, lastKey!!),
                            style = MaterialTheme.typography.headlineLarge.copy(fontFamily = FontFamily.Monospace),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
