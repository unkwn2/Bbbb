package com.sr.openbyd.ui.screens

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.sr.openbyd.R
import com.sr.openbyd.ui.viewmodel.AppInfo
import com.sr.openbyd.ui.viewmodel.MainViewModel
import androidx.core.graphics.createBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sr.openbyd.ui.viewmodel.SplitPatchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitPatchTab(
    mainViewModel: MainViewModel,
    viewModel: SplitPatchViewModel = viewModel()
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    // Refresh app list when screen opens
    LaunchedEffect(Unit) {
        viewModel.loadInstalledApps()
    }

    var showPatchConsole by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.tab_split_patching),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            IconButton(
                onClick = { viewModel.loadInstalledApps() }
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh"
                )
            }
        }
        Text(
            text = stringResource(R.string.split_screen_mode_dilink6_native_info),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text(stringResource(R.string.patch_search_hint)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() }),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        val filteredApps = viewModel.installedApps.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.packageName.contains(searchQuery, ignoreCase = true)
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(filteredApps) { app ->
                AppPatchCard(
                    app = app,
                    viewModel = viewModel,
                    onPatchClick = {
                        viewModel.startPatching(app.packageName)
                        showPatchConsole = true
                    }
                )
            }
        }
    }

    if (showPatchConsole && viewModel.patchingPackageName != null) {
        PatchConsoleDialog(
            viewModel = viewModel,
            onDismiss = {
                showPatchConsole = false
                viewModel.clearPatchState()
            }
        )
    }
}

@Composable
fun AppPatchCard(
    app: AppInfo,
    viewModel: SplitPatchViewModel,
    onPatchClick: () -> Unit
) {
    val context = LocalContext.current
    val pm = context.packageManager
    
    // Determine system app and patch compatibility
    val isSystem = remember(app.packageName) {
        try {
            val appInfo = pm.getApplicationInfo(app.packageName, 0)
            (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
        } catch (e: Exception) {
            false
        }
    }

    // Live metadata check
    var isPatched by remember(app.packageName, viewModel.installedApps) {
        mutableStateOf(checkIfPatched(context, app.packageName))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // App Icon
                Box(
                    modifier = Modifier.size(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (app.icon != null) {
                        androidx.compose.foundation.Image(
                            bitmap = remember(app.icon) { drawableToBitmap(app.icon).asImageBitmap() },
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = app.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = app.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Status Badge
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isSystem) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.patch_status_system),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                        } else if (isPatched) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.patch_status_patched),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF4CAF50),
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.patch_status_unpatched),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Patch Button
            Button(
                onClick = onPatchClick,
                enabled = !isSystem,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isPatched) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                )
            ) {
                Text(stringResource(R.string.patch_btn_patch))
            }
        }
    }
}

@Composable
fun PatchConsoleDialog(
    viewModel: SplitPatchViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Black,
                contentColor = Color(0xFF00FF00)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Terminal,
                            contentDescription = null,
                            tint = Color(0xFF00FF00),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.patch_console_title),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    TextButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF00FF00))
                    ) {
                        Text(stringResource(R.string.btn_ok))
                    }
                }

                HorizontalDivider(color = Color(0xFF00FF00).copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 8.dp))

                // Log output area with auto-scrolling
                val scrollState = rememberScrollState()
                LaunchedEffect(viewModel.patchingLogs) {
                    scrollState.animateScrollTo(scrollState.maxValue)
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color.Black)
                        .padding(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                    ) {
                        Text(
                            text = viewModel.patchingLogs,
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            color = Color(0xFF00FF00)
                        )
                    }
                }

                if (viewModel.isPatchingComplete && viewModel.patchedApkFile != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    if (viewModel.isReinstallingComplete) {
                        Button(
                            onClick = {
                                viewModel.loadInstalledApps()
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF00FF00),
                                contentColor = Color.Black
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.btn_ok))
                        }
                    } else {
                        Button(
                            onClick = {
                                viewModel.patchingPackageName?.let { pkg ->
                                    viewModel.reinstallPatchedApp(pkg)
                                }
                            },
                            enabled = !viewModel.isReinstallingProgress,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50),
                                contentColor = Color.White,
                                disabledContainerColor = Color(0xFF4CAF50).copy(alpha = 0.5f),
                                disabledContentColor = Color.White.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (viewModel.isReinstallingProgress) {
                                androidx.compose.material3.CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .padding(end = 8.dp),
                                    strokeWidth = 2.dp
                                )
                                Text(stringResource(R.string.patch_btn_reinstalling))
                            } else {
                                Text(stringResource(R.string.patch_btn_reinstall))
                            }
                        }
                    }
                }
            }
        }
    }
}

// Utility to convert Android Icon drawable to Bitmap for Compose Image
private fun drawableToBitmap(drawable: android.graphics.drawable.Drawable): android.graphics.Bitmap {
    if (drawable is android.graphics.drawable.BitmapDrawable) {
        return drawable.bitmap
    }
    val bitmap = createBitmap(
        drawable.intrinsicWidth.coerceAtLeast(1),
        drawable.intrinsicHeight.coerceAtLeast(1)
    )
    val canvas = android.graphics.Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}

// Check if package is already patched
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
