package com.sr.openbyd.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sr.openbyd.data.InstalledAppsRepository
import com.sr.openbyd.patcher.ApkPatcher
import com.sr.openbyd.proxy.ProxyManager
import com.sr.openbyd.utils.ShellCommandExecutor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

class SplitPatchViewModel(application: Application) : AndroidViewModel(application) {
    var installedApps by mutableStateOf<List<AppInfo>>(emptyList())
        private set

    var patchingPackageName by mutableStateOf<String?>(null)
        private set
    var patchingLogs by mutableStateOf("")
        private set
    var isPatchingComplete by mutableStateOf(false)
        private set
    var isPatchingProgress by mutableStateOf(false)
        private set
    var patchedApkFile by mutableStateOf<File?>(null)
        private set
    var patchedSplitApkFiles by mutableStateOf<List<File>>(emptyList())
        private set

    var isReinstallingProgress by mutableStateOf(false)
        private set
    var isReinstallingComplete by mutableStateOf(false)
        private set

    init {
        loadInstalledApps()
    }

    fun loadInstalledApps() {
        viewModelScope.launch {
            installedApps = InstalledAppsRepository.getInstance(getApplication()).getInstalledApps()
        }
    }

    fun startPatching(packageName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            patchingPackageName = packageName
            isPatchingProgress = true
            isPatchingComplete = false
            patchedApkFile = null
            patchedSplitApkFiles = emptyList()
            patchingLogs = "Initializing native split screen patch for $packageName…\n"

            try {
                val pm = getApplication<Application>().packageManager
                val appInfo = pm.getApplicationInfo(packageName, 0)
                val sourceApk = File(appInfo.sourceDir)
                
                // Query companion split APKs if any exist
                val splitSourceDirs = appInfo.splitSourceDirs
                val splitApks = splitSourceDirs?.map { File(it) } ?: emptyList()
                
                val cacheDir = getApplication<Application>().externalCacheDir ?: getApplication<Application>().cacheDir
                val outputApk = File(cacheDir, "patched_$packageName.apk")
                
                val outputSplitApks = splitApks.mapIndexed { idx, splitApk ->
                    File(cacheDir, "patched_${packageName}_split_$idx.apk")
                }

                ApkPatcher.patchApk(sourceApk, outputApk, splitApks, outputSplitApks) { logLine ->
                    Log.d(TAG, "PatchProgress: $logLine")
                    viewModelScope.launch(Dispatchers.Main) {
                        patchingLogs += "• $logLine\n"
                    }
                }

                viewModelScope.launch(Dispatchers.Main) {
                    patchedApkFile = outputApk
                    patchedSplitApkFiles = outputSplitApks
                    isPatchingComplete = true
                    isPatchingProgress = false
                    val successMsg = if (outputSplitApks.isNotEmpty()) {
                        "\n✓ SUCCESS: Base APK and ${outputSplitApks.size} splits are patched and ready!\n"
                    } else {
                        "\n✓ SUCCESS: Patched APK is ready at ${outputApk.name}!\n"
                    }
                    Log.d(TAG, "PatchSuccess: $successMsg")
                    patchingLogs += successMsg
                }
            } catch (e: Exception) {
                Log.e(TAG, "Patching failed for $packageName", e)
                viewModelScope.launch(Dispatchers.Main) {
                    isPatchingProgress = false
                    val errorMsg = "\n❌ FAILED: ${e.localizedMessage}\n"
                    Log.e(TAG, "PatchError: $errorMsg")
                    patchingLogs += errorMsg
                }
            }
        }
    }

    fun reinstallPatchedApp(packageName: String) {
        val apkFile = patchedApkFile ?: return
        viewModelScope.launch(Dispatchers.IO) {
            isReinstallingProgress = true
            isReinstallingComplete = false
            val startMsg = "\n----------------------------------------\n🔄 Starting Silent Re-installation...\n"
            Log.d(TAG, "Reinstall: $startMsg")
            viewModelScope.launch(Dispatchers.Main) {
                patchingLogs += startMsg
            }

            val control = ProxyManager.carControl
            if (control == null) {
                val errorMsg = "❌ ERROR: Proxy Manager is not connected! Cannot perform silent install.\n"
                Log.e(TAG, "Reinstall: $errorMsg")
                viewModelScope.launch(Dispatchers.Main) {
                    patchingLogs += errorMsg
                    isReinstallingProgress = false
                    isReinstallingComplete = true
                }
                return@launch
            }

            try {
                val apkPath = apkFile.absolutePath
                val tmpApkPath = "/data/local/tmp/patched_${packageName}.apk"
                val stagedApkPaths = mutableListOf<String>()

                // Step 1: Copy base APK and all splits to /data/local/tmp
                val copyProgress = "• Copying patched base APK and splits to staging directory...\n"
                Log.d(TAG, "Reinstall: $copyProgress")
                viewModelScope.launch(Dispatchers.Main) {
                    patchingLogs += copyProgress
                }
                
                // Copy base APK
                val cpRes = ShellCommandExecutor.copyFile(apkPath, tmpApkPath) ?: ""
                Log.d(TAG, "Reinstall cp base result: ${cpRes.trim()}")
                ShellCommandExecutor.chmod("666", tmpApkPath)
                stagedApkPaths.add(tmpApkPath)

                // Copy split APKs
                val splitFiles = patchedSplitApkFiles
                splitFiles.forEachIndexed { idx, splitFile ->
                    val tmpSplitPath = "/data/local/tmp/patched_${packageName}_split_$idx.apk"
                    val cpSplitRes = ShellCommandExecutor.copyFile(splitFile.absolutePath, tmpSplitPath) ?: ""
                    Log.d(TAG, "Reinstall cp split $idx result: ${cpSplitRes.trim()}")
                    ShellCommandExecutor.chmod("666", tmpSplitPath)
                    stagedApkPaths.add(tmpSplitPath)
                }

                // Step 2: Uninstall original app
                val uninstallProgress = "• Uninstalling original application...\n"
                Log.d(TAG, "Reinstall: $uninstallProgress")
                viewModelScope.launch(Dispatchers.Main) {
                    patchingLogs += uninstallProgress
                }
                val uninstallRes = ShellCommandExecutor.uninstallApp(packageName) ?: ""
                val uninstallResultMsg = "  Uninstall result: ${uninstallRes.trim()}\n"
                Log.d(TAG, "Reinstall: $uninstallResultMsg")
                viewModelScope.launch(Dispatchers.Main) {
                    patchingLogs += uninstallResultMsg
                }

                delay(1000) // Brief delay to let package manager stabilize

                // Step 3: Install patched APKs atomically from staging directory
                val installProgress = "• Installing patched application...\n"
                Log.d(TAG, "Reinstall: $installProgress")
                viewModelScope.launch(Dispatchers.Main) {
                    patchingLogs += installProgress
                }
                
                val installRes = if (stagedApkPaths.size > 1) {
                    val createRes = ShellCommandExecutor.createInstallSession() ?: ""
                    Log.d(TAG, "Reinstall pm install-create result: ${createRes.trim()}")
                    val sessionId = Regex("\\d+").find(createRes)?.value
                    
                    if (sessionId.isNullOrEmpty()) {
                        "Failure [INSTALL_SESSION_FAILED: Could not create install session: ${createRes.trim()}]"
                    } else {
                        var writeFailed = false
                        var writeErrorMsg = ""
                        
                        stagedApkPaths.forEachIndexed { idx, path ->
                            if (!writeFailed) {
                                val splitName = if (idx == 0) "base" else "split_${idx - 1}"
                                val writeRes = ShellCommandExecutor.writeToInstallSession(sessionId, splitName, path) ?: ""
                                Log.d(TAG, "Reinstall pm install-write $splitName result: ${writeRes.trim()}")
                                if (writeRes.contains("Failure", ignoreCase = true) || 
                                    writeRes.contains("Error", ignoreCase = true) || 
                                    writeRes.contains("Exception", ignoreCase = true)) {
                                    writeFailed = true
                                    writeErrorMsg = writeRes.trim()
                                }
                            }
                        }
                        
                        if (writeFailed) {
                            try {
                                ShellCommandExecutor.abandonInstallSession(sessionId)
                            } catch (e: Exception) {
                                Log.w(TAG, "Failed to abandon session $sessionId", e)
                            }
                            "Failure [INSTALL_SESSION_FAILED: pm install-write failed: $writeErrorMsg]"
                        } else {
                            val commitRes = ShellCommandExecutor.commitInstallSession(sessionId) ?: ""
                            Log.d(TAG, "Reinstall pm install-commit result: ${commitRes.trim()}")
                            commitRes
                        }
                    }
                } else {
                    ShellCommandExecutor.installApp(tmpApkPath) ?: ""
                }
                
                val installResultMsg = "  Install result: ${installRes.trim()}\n"
                Log.d(TAG, "Reinstall: $installResultMsg")
                viewModelScope.launch(Dispatchers.Main) {
                    patchingLogs += installResultMsg
                }

                // Step 4: Clean up staging files
                stagedApkPaths.forEach { path ->
                    ShellCommandExecutor.removeFile(path)
                }

                viewModelScope.launch(Dispatchers.Main) {
                    if (installRes.contains("Success", ignoreCase = true)) {
                        val successMsg = "\n✓ SUCCESS: App has been successfully patched and re-installed!\n"
                        Log.d(TAG, "ReinstallSuccess: $successMsg")
                        patchingLogs += successMsg
                    } else {
                        val warnMsg = "\n⚠️ Warning: Install completed, check result output above.\n"
                        Log.w(TAG, "ReinstallWarn: $warnMsg")
                        patchingLogs += warnMsg
                    }
                    isReinstallingProgress = false
                    isReinstallingComplete = true
                }
            } catch (e: Exception) {
                Log.e(TAG, "Re-installation failed", e)
                viewModelScope.launch(Dispatchers.Main) {
                    val failMsg = "\n❌ Re-install FAILED: ${e.localizedMessage}\n"
                    Log.e(TAG, "ReinstallError: $failMsg")
                    patchingLogs += failMsg
                    isReinstallingProgress = false
                    isReinstallingComplete = true
                }
            }
        }
    }

    fun clearPatchState() {
        patchingPackageName = null
        patchingLogs = ""
        isPatchingComplete = false
        isPatchingProgress = false
        isReinstallingProgress = false
        isReinstallingComplete = false
        patchedApkFile = null
        patchedSplitApkFiles = emptyList()
    }
}
