package com.sr.openbyd.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sr.openbyd.R
import com.sr.openbyd.WindowState
import com.sr.openbyd.WindowType
import com.sr.openbyd.ui.icons.FontAwesomeCarAlt
import com.sr.openbyd.ui.viewmodel.MainViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sr.openbyd.ui.viewmodel.CarControlsViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CarControlsDialog(
    mainViewModel: MainViewModel,
    onDismiss: () -> Unit,
    viewModel: CarControlsViewModel = viewModel()
) {
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth(0.7f) // Fits perfectly on both landscape car displays and standard screens
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
            ) {
                // Premium Dialog Header with Icon, Title, Info Button, and Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = FontAwesomeCarAlt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.car_controls),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        InfoButton(
                            dialogTitle = stringResource(R.string.info_title_car_controls),
                            dialogContent = stringResource(R.string.info_desc_car_controls)
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Windows Control Section
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = stringResource(R.string.header_windows),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val onWindowToggle: (WindowType, WindowState) -> Unit = { type, state ->
                                val resp = viewModel.setWindow(type, state)
                                val statusStr = context.getString(
                                    if (resp?.contains("error", ignoreCase = true) == true)
                                        R.string.status_error
                                    else
                                        R.string.status_ok
                                )
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.window_response, statusStr),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            WindowButton(WindowType.LEFT_FRONT, WindowState.OPEN, onWindowToggle)
                            WindowButton(WindowType.LEFT_FRONT, WindowState.CLOSE, onWindowToggle)
                            WindowButton(WindowType.ALL, WindowState.OPEN, onWindowToggle)
                            WindowButton(WindowType.ALL, WindowState.CLOSE, onWindowToggle)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))

                    // Climate Control Section
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = stringResource(R.string.header_climate_control),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val onAcToggle: (Boolean) -> Unit = { isOn ->
                                val resp = viewModel.setAcPower(isOn)
                                val statusStr = context.getString(
                                    if (resp?.contains("error", ignoreCase = true) == true)
                                        R.string.status_error
                                    else
                                        R.string.status_ok
                                )
                                val resId = if (isOn) R.string.ac_on_response else R.string.ac_off_response
                                Toast.makeText(
                                    context,
                                    context.getString(resId, statusStr),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            Button(
                                onClick = { onAcToggle(true) },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(stringResource(R.string.turn_on_ac))
                            }
                            Button(
                                onClick = { onAcToggle(false) },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(stringResource(R.string.turn_off_ac))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WindowButton(
    windowType: WindowType,
    state: WindowState,
    onClick: (WindowType, WindowState) -> Unit
) {
    FilledTonalButton(
        onClick = { onClick(windowType, state) },
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = "${stringResource(state.descriptionRes)} — ${stringResource(windowType.descriptionRes)}",
            style = MaterialTheme.typography.labelSmall
        )
    }
}
