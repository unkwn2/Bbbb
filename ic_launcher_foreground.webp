package com.sr.openbyd.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sr.openbyd.R
import com.sr.openbyd.models.ActionType
import com.sr.openbyd.models.StartupAction
import com.sr.openbyd.ui.icons.FontAwesomeCarAlt
import com.sr.openbyd.ui.viewmodel.MainViewModel
import com.sr.openbyd.data.SplitScreenMode
import kotlin.math.roundToInt
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sr.openbyd.ui.viewmodel.StartupViewModel

// ---------------------------------------------------------------------------
// Constants
// ---------------------------------------------------------------------------

private const val DELAY_STEP_MS = 500L  // 0.5 s per step
private const val DELAY_MAX_MS  = 15_000L  // 15 s maximum
private const val DELAY_STEPS   = (DELAY_MAX_MS / DELAY_STEP_MS).toInt() // 30 steps

/** Format a millisecond delay as a human-readable string (e.g. "2.5s"). */
private fun formatDelay(ms: Long): String {
    val seconds = ms / 1000.0
    return if (seconds == seconds.toLong().toDouble()) "${seconds.toLong()}s" else "${seconds}s"
}

/** Convert a slider float position (0..DELAY_STEPS) to milliseconds. */
private fun sliderToMs(value: Float): Long = (value.roundToInt() * DELAY_STEP_MS)

/** Convert milliseconds to a slider float position. */
private fun msToSlider(ms: Long): Float = (ms.toFloat() / DELAY_STEP_MS)

// ---------------------------------------------------------------------------
// Screen
// ---------------------------------------------------------------------------

@Composable
fun StartupSequenceScreen(
    mainViewModel: MainViewModel,
    modifier: Modifier = Modifier,
    viewModel: StartupViewModel = viewModel()
) {
    var showAddDialog by remember { mutableStateOf(false) }

    if (showAddDialog) {
        AddStartupActionDialog(
            viewModel = viewModel,
            mainViewModel = mainViewModel,
            onDismiss = { showAddDialog = false }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            StartupSequenceHeader(
                onAddNew = { showAddDialog = true },
                onTestRun = { viewModel.runStartupSequence() }
            )
        }

        if (viewModel.startupActions.isEmpty()) {
            item {
                EmptyStartupHint()
            }
        }

        itemsIndexed(
            items = viewModel.startupActions,
            key = { _, action -> action.index }
        ) { _, action ->
            StartupActionItem(action = action, viewModel = viewModel, mainViewModel = mainViewModel)
        }
    }
}

// ---------------------------------------------------------------------------
// Header
// ---------------------------------------------------------------------------

@Composable
fun StartupSequenceHeader(onAddNew: () -> Unit, onTestRun: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LabelText(
                    text = stringResource(R.string.startup_sequence),
                    icon = Icons.Default.PlayArrow
                )
                Spacer(modifier = Modifier.width(8.dp))
                InfoButton(
                    dialogTitle = stringResource(R.string.info_title_startup_sequence),
                    dialogContent = stringResource(R.string.info_desc_startup_sequence)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(
                    onClick = onTestRun,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                ) {
                    Text(stringResource(R.string.btn_test_run), style = MaterialTheme.typography.labelMedium)
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onAddNew,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.btn_add_action), style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.startup_sequence_desc),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Empty state
// ---------------------------------------------------------------------------

@Composable
fun EmptyStartupHint() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = FontAwesomeCarAlt,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.startup_empty_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Action item
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartupActionItem(
    action: StartupAction,
    viewModel: StartupViewModel,
    mainViewModel: MainViewModel
) {
    val context = LocalContext.current

    var isEditing by remember(action.index) { mutableStateOf(false) }
    var selectedAction by remember(action) { mutableStateOf(action.actionType) }
    var packageName by remember(action) { mutableStateOf(action.targetPackageName ?: "") }
    var secondPackageName by remember(action) { mutableStateOf(action.secondPackageName ?: "") }
    var delaySlider by remember(action) { mutableFloatStateOf(msToSlider(action.delayMs)) }
    var isLeft by remember(action) { mutableStateOf(action.isLeft) }
    var projectFirstApp by remember(action) { mutableStateOf(action.projectFirstApp) }
    var splitRatio by remember(action, mainViewModel.splitScreenMode) {
        val initialRatio = action.splitRatio
        mutableStateOf(
            if (mainViewModel.splitScreenMode == SplitScreenMode.NATIVE_UI7_DILINK_6 && initialRatio == "1/2") "2/3"
            else initialRatio
        )
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {

            // ── Collapsed header row ──────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isEditing = !isEditing },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Index badge + delay + action chip
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
                            text = stringResource(R.string.label_delay_seconds, formatDelay(action.delayMs)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(action.actionType.labelRes),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    SuggestionChip(
                        onClick = { isEditing = !isEditing },
                        label = { Text(stringResource(action.actionType.labelRes)) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            labelColor = if (action.actionType == ActionType.NONE)
                                MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.primary
                        )
                    )
                    Icon(
                        imageVector = if (isEditing) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            // ── Expanded editor ───────────────────────────────────────────
            AnimatedVisibility(visible = isEditing) {
                Column(modifier = Modifier.padding(top = 16.dp)) {

                    // Delay slider
                    Text(
                        text = stringResource(R.string.label_delay_header),
                        style = MaterialTheme.typography.labelLarge
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Slider(
                            value = delaySlider,
                            onValueChange = { delaySlider = it },
                            valueRange = 0f..DELAY_STEPS.toFloat(),
                            steps = DELAY_STEPS - 1,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = formatDelay(sliderToMs(delaySlider)),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(40.dp)
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    // Action picker
                    Text(
                        text = stringResource(R.string.header_assign_action),
                        style = MaterialTheme.typography.labelLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        OutlinedButton(
                            onClick = { expanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(selectedAction.labelRes))
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            ActionType.entries.forEach { aType ->
                                DropdownMenuItem(
                                    text = { Text(stringResource(aType.labelRes)) },
                                    onClick = {
                                        selectedAction = aType
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Package fields (conditional)
                    if (selectedAction == ActionType.LAUNCH_APP || selectedAction == ActionType.LAUNCH_APP_ON_CLUSTER) {
                        Spacer(modifier = Modifier.height(8.dp))
                        AppSearchField(
                            label = stringResource(R.string.package_name_hint),
                            value = packageName,
                            onValueChange = { packageName = it },
                            allApps = viewModel.installedApps
                        )
                    } else if (selectedAction == ActionType.LAUNCH_APP_ON_SPLIT_SCREEN) {
                        Spacer(modifier = Modifier.height(8.dp))
                        AppSearchField(
                            label = stringResource(R.string.label_first_app),
                            value = packageName,
                            onValueChange = { packageName = it },
                            allApps = viewModel.installedApps,
                            showCompatibilityBadges = (mainViewModel.splitScreenMode == SplitScreenMode.NATIVE_UI7_DILINK_6)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        AppSearchField(
                            label = stringResource(R.string.label_second_app),
                            value = secondPackageName,
                            onValueChange = { secondPackageName = it },
                            allApps = viewModel.installedApps,
                            showCompatibilityBadges = (mainViewModel.splitScreenMode == SplitScreenMode.NATIVE_UI7_DILINK_6)
                        )

                        if (mainViewModel.splitScreenMode == SplitScreenMode.PROJECTED || mainViewModel.splitScreenMode == SplitScreenMode.NATIVE_UI7_DILINK_6) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = stringResource(R.string.ui7_split_options),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            if (mainViewModel.splitScreenMode == SplitScreenMode.PROJECTED) {
                                // Projected target selector
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = stringResource(R.string.split_projected_target),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        FilterChip(
                                            selected = projectFirstApp,
                                            onClick = { 
                                                projectFirstApp = true
                                                isLeft = true
                                            },
                                            label = { Text(stringResource(R.string.target_first_app)) }
                                        )
                                        FilterChip(
                                            selected = !projectFirstApp,
                                            onClick = { 
                                                projectFirstApp = false
                                                isLeft = false
                                            },
                                            label = { Text(stringResource(R.string.target_second_app)) }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            // Ratio selector
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = stringResource(if (mainViewModel.splitScreenMode == SplitScreenMode.NATIVE_UI7_DILINK_6) R.string.split_ratio_native_ui7_label else R.string.split_ratio_label),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    if (mainViewModel.splitScreenMode == SplitScreenMode.NATIVE_UI7_DILINK_6) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        InfoButton(
                                            dialogTitle = stringResource(R.string.split_ratio_native_ui7_info_title),
                                            dialogContent = stringResource(R.string.split_ratio_native_ui7_info_desc)
                                        )
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    val ratios = if (mainViewModel.splitScreenMode == SplitScreenMode.NATIVE_UI7_DILINK_6) {
                                        listOf("1/3", "2/3")
                                    } else {
                                        listOf("1/3", "1/2", "2/3")
                                    }
                                    ratios.forEach { ratio ->
                                        FilterChip(
                                            selected = splitRatio == ratio,
                                            onClick = { splitRatio = ratio },
                                            label = { Text(ratio) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action buttons row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                viewModel.deleteStartupAction(action.index)
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.startup_action_deleted, action.index + 1),
                                    Toast.LENGTH_SHORT
                                ).show()
                                isEditing = false
                            },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
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
                                    val pkg1 = packageName.trim().ifEmpty { null }
                                    val pkg2 = secondPackageName.trim().ifEmpty { null }
                                    val updated = action.copy(
                                        actionType        = selectedAction,
                                        delayMs           = sliderToMs(delaySlider),
                                        targetPackageName = if (selectedAction == ActionType.LAUNCH_APP ||
                                            selectedAction == ActionType.LAUNCH_APP_ON_CLUSTER ||
                                            selectedAction == ActionType.LAUNCH_APP_ON_SPLIT_SCREEN) pkg1 else null,
                                        secondPackageName = if (selectedAction == ActionType.LAUNCH_APP_ON_SPLIT_SCREEN) pkg2 else null,
                                        isLeft            = isLeft,
                                        projectFirstApp   = projectFirstApp,
                                        splitRatio        = splitRatio
                                    )
                                    viewModel.updateStartupAction(updated)
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.startup_action_saved),
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

// ---------------------------------------------------------------------------
// Add action dialog
// ---------------------------------------------------------------------------

@Composable
fun AddStartupActionDialog(
    viewModel: StartupViewModel,
    mainViewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    var selectedAction by remember { mutableStateOf(ActionType.NONE) }
    var packageName by remember { mutableStateOf("") }
    var secondPackageName by remember { mutableStateOf("") }
    var delaySlider by remember { mutableFloatStateOf(0f) }
    var isLeft by remember { mutableStateOf(true) }
    var projectFirstApp by remember { mutableStateOf(true) }
    var splitRatio by remember(mainViewModel.splitScreenMode) {
        mutableStateOf(
            if (mainViewModel.splitScreenMode == SplitScreenMode.NATIVE_UI7_DILINK_6) "2/3" else "1/2"
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_add_action_title)) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 350.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                // Delay slider
                Text(
                    text = stringResource(R.string.label_delay_header),
                    style = MaterialTheme.typography.labelLarge
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Slider(
                        value = delaySlider,
                        onValueChange = { delaySlider = it },
                        valueRange = 0f..DELAY_STEPS.toFloat(),
                        steps = DELAY_STEPS - 1,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = formatDelay(sliderToMs(delaySlider)),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(40.dp)
                    )
                }

                HorizontalDivider()

                // Action picker
                Text(
                    text = stringResource(R.string.header_assign_action),
                    style = MaterialTheme.typography.labelLarge
                )

                var expanded by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(
                        onClick = { expanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(selectedAction.labelRes))
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        ActionType.entries.forEach { aType ->
                            DropdownMenuItem(
                                text = { Text(stringResource(aType.labelRes)) },
                                onClick = {
                                    selectedAction = aType
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                // Package fields
                if (selectedAction == ActionType.LAUNCH_APP || selectedAction == ActionType.LAUNCH_APP_ON_CLUSTER) {
                    AppSearchField(
                        label = stringResource(R.string.package_name_hint),
                        value = packageName,
                        onValueChange = { packageName = it },
                        allApps = viewModel.installedApps
                    )
                } else if (selectedAction == ActionType.LAUNCH_APP_ON_SPLIT_SCREEN) {
                    AppSearchField(
                        label = stringResource(R.string.label_first_app),
                        value = packageName,
                        onValueChange = { packageName = it },
                        allApps = viewModel.installedApps,
                        showCompatibilityBadges = (mainViewModel.splitScreenMode == SplitScreenMode.NATIVE_UI7_DILINK_6)
                    )
                    AppSearchField(
                        label = stringResource(R.string.label_second_app),
                        value = secondPackageName,
                        onValueChange = { secondPackageName = it },
                        allApps = viewModel.installedApps,
                        showCompatibilityBadges = (mainViewModel.splitScreenMode == SplitScreenMode.NATIVE_UI7_DILINK_6)
                    )

                    if (mainViewModel.splitScreenMode == SplitScreenMode.PROJECTED || mainViewModel.splitScreenMode == SplitScreenMode.NATIVE_UI7_DILINK_6) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.ui7_split_options),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        if (mainViewModel.splitScreenMode == SplitScreenMode.PROJECTED) {
                            // Projected target selector
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = stringResource(R.string.split_projected_target),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilterChip(
                                        selected = projectFirstApp,
                                        onClick = { 
                                            projectFirstApp = true
                                            isLeft = true
                                        },
                                        label = { Text(stringResource(R.string.target_first_app)) }
                                    )
                                    FilterChip(
                                        selected = !projectFirstApp,
                                        onClick = { 
                                            projectFirstApp = false
                                            isLeft = false
                                        },
                                        label = { Text(stringResource(R.string.target_second_app)) }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // Ratio selector
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                  Text(
                                      text = stringResource(if (mainViewModel.splitScreenMode == SplitScreenMode.NATIVE_UI7_DILINK_6) R.string.split_ratio_native_ui7_label else R.string.split_ratio_label),
                                      style = MaterialTheme.typography.bodyMedium
                                  )
                                  if (mainViewModel.splitScreenMode == SplitScreenMode.NATIVE_UI7_DILINK_6) {
                                      Spacer(modifier = Modifier.width(4.dp))
                                      InfoButton(
                                          dialogTitle = stringResource(R.string.split_ratio_native_ui7_info_title),
                                          dialogContent = stringResource(R.string.split_ratio_native_ui7_info_desc)
                                      )
                                  }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                val ratios = if (mainViewModel.splitScreenMode == SplitScreenMode.NATIVE_UI7_DILINK_6) {
                                    listOf("1/3", "2/3")
                                } else {
                                    listOf("1/3", "1/2", "2/3")
                                }
                                ratios.forEach { ratio ->
                                    FilterChip(
                                        selected = splitRatio == ratio,
                                        onClick = { splitRatio = ratio },
                                        label = { Text(ratio) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val pkg1 = packageName.trim().ifEmpty { null }
                    val pkg2 = secondPackageName.trim().ifEmpty { null }
                    val newAction = StartupAction(
                        index             = 0, // will be reassigned by repository
                        actionType        = selectedAction,
                        delayMs           = sliderToMs(delaySlider),
                        targetPackageName = if (selectedAction == ActionType.LAUNCH_APP ||
                            selectedAction == ActionType.LAUNCH_APP_ON_CLUSTER ||
                            selectedAction == ActionType.LAUNCH_APP_ON_SPLIT_SCREEN) pkg1 else null,
                        secondPackageName = if (selectedAction == ActionType.LAUNCH_APP_ON_SPLIT_SCREEN) pkg2 else null,
                        isLeft            = isLeft,
                        projectFirstApp   = projectFirstApp,
                        splitRatio        = splitRatio
                    )
                    viewModel.addStartupAction(newAction)
                    onDismiss()
                }
            ) {
                Text(stringResource(R.string.btn_add_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancel))
            }
        }
    )
}
