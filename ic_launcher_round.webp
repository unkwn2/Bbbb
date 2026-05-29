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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sr.openbyd.MainActivity
import com.sr.openbyd.R
import com.sr.openbyd.data.SplitScreenMode
import com.sr.openbyd.models.ActionType
import com.sr.openbyd.models.ButtonMapping
import com.sr.openbyd.ui.icons.TablerSteeringWheel
import com.sr.openbyd.ui.viewmodel.MainViewModel
import com.sr.openbyd.ui.viewmodel.SteeringWheelViewModel

@Composable
fun SteeringWheelTab(
    mainViewModel: MainViewModel,
    modifier: Modifier = Modifier,
    viewModel: SteeringWheelViewModel = viewModel()
) {
    var showAddButtonDialog by remember { mutableStateOf(false) }

    if (showAddButtonDialog) {
        AddButtonDialog(
            onDismiss = { showAddButtonDialog = false },
            viewModel = viewModel
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SteeringWheelRemapperHeader(onAddNew = { showAddButtonDialog = true })
        }

        items(viewModel.activeKeyCodes) { keyCode ->
            MappingItem(keyCode, viewModel, mainViewModel)
        }
    }
}

@Composable
fun SteeringWheelRemapperHeader(onAddNew: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LabelText(text = stringResource(R.string.button_mappings), icon = TablerSteeringWheel)
                Spacer(modifier = Modifier.width(8.dp))
                InfoButton(
                    dialogTitle = stringResource(R.string.info_title_steering_wheel),
                    dialogContent = stringResource(R.string.info_desc_steering_wheel)
                )
            }
            Button(
                onClick = onAddNew,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.btn_add_new), style = MaterialTheme.typography.labelMedium)
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
                    text = stringResource(R.string.remapper_tip),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

@Composable
fun AddButtonDialog(
    onDismiss: () -> Unit,
    viewModel: SteeringWheelViewModel
) {
    var keyCodeText by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_add_button_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = keyCodeText,
                    onValueChange = { if (it.all { char -> char.isDigit() }) keyCodeText = it },
                    label = { Text(stringResource(R.string.hint_keycode)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.hint_description)) },
                    modifier = Modifier.fillMaxWidth()
                )

                val exists = remember(keyCodeText) {
                    keyCodeText.toIntOrNull()?.let { viewModel.activeKeyCodes.contains(it) } ?: false
                }
                if (exists) {
                    Text(stringResource(R.string.warning_keycode_exists), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val code = keyCodeText.toIntOrNull()
                    if (code != null) {
                        val mapping = ButtonMapping(
                            keyCode = code,
                            actionType = ActionType.NONE,
                            customDescription = description.trim().ifEmpty { null }
                        )
                        viewModel.saveMapping(mapping)
                        onDismiss()
                    }
                },
                enabled = keyCodeText.isNotEmpty()
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.btn_cancel)) }
        }
    )
}

@Composable
fun MappingItem(
    keyCode: Int,
    viewModel: SteeringWheelViewModel,
    mainViewModel: MainViewModel
) {
    val context = LocalContext.current
    val current = remember(keyCode, viewModel.activeKeyCodes) { viewModel.getMapping(keyCode) }

    val knownLabelRes = MainActivity.KNOWN_KEYCODES[keyCode]
    val keyName = if (knownLabelRes != null) stringResource(knownLabelRes)
                  else (current.customDescription ?: stringResource(R.string.unknown_button))

    var isEditing by remember { mutableStateOf(false) }
    var selectedAction by remember(current) { mutableStateOf(current.actionType) }
    var packageName by remember(current) { mutableStateOf(current.targetPackageName ?: "") }
    var secondPackageName by remember(current) { mutableStateOf(current.secondPackageName ?: "") }
    var isLeft by remember(current) { mutableStateOf(current.isLeft) }
    var projectFirstApp by remember(current) { mutableStateOf(current.projectFirstApp) }
    var splitRatio by remember(current, mainViewModel.splitScreenMode) {
        val initialRatio = current.splitRatio
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isEditing = !isEditing },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = keyName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Text(text = stringResource(R.string.label_keycode, keyCode), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    SuggestionChip(
                        onClick = { isEditing = !isEditing },
                        label = { Text(stringResource(selectedAction.labelRes)) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            labelColor = if (selectedAction == ActionType.NONE)
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

            AnimatedVisibility(visible = isEditing) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    Text(text = stringResource(R.string.header_assign_action), style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.height(8.dp))

                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(selectedAction.labelRes))
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            ActionType.entries.forEach { action ->
                                DropdownMenuItem(
                                    text = { Text(stringResource(action.labelRes)) },
                                    onClick = {
                                        selectedAction = action
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }

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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = {
                            viewModel.deleteMapping(keyCode)
                            Toast.makeText(context, context.getString(R.string.toast_deleted, keyCode), Toast.LENGTH_SHORT).show()
                            isEditing = false
                        }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
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
                                    val pkg1 = packageName.trim().ifEmpty { null }
                                    val pkg2 = secondPackageName.trim().ifEmpty { null }
                                    val mapping = ButtonMapping(
                                        keyCode = keyCode,
                                        actionType = selectedAction,
                                        targetPackageName = if (selectedAction == ActionType.LAUNCH_APP || selectedAction == ActionType.LAUNCH_APP_ON_CLUSTER || selectedAction == ActionType.LAUNCH_APP_ON_SPLIT_SCREEN) pkg1 else null,
                                        secondPackageName = if (selectedAction == ActionType.LAUNCH_APP_ON_SPLIT_SCREEN) pkg2 else null,
                                        customDescription = if (knownLabelRes == null) current.customDescription else null,
                                        isLeft = isLeft,
                                        projectFirstApp = projectFirstApp,
                                        splitRatio = splitRatio
                                    )
                                    viewModel.saveMapping(mapping)
                                    Toast.makeText(context, context.getString(R.string.saved_toast, keyName, context.getString(selectedAction.labelRes)), Toast.LENGTH_SHORT).show()
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
