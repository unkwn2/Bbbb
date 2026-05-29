package com.sr.openbyd.data

import android.content.Context
import android.content.Intent
import com.sr.openbyd.ui.viewmodel.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class InstalledAppsRepository private constructor(context: Context) {
    private val appContext = context.applicationContext
    @Volatile
    private var cachedApps: List<AppInfo> = emptyList()

    suspend fun getInstalledApps(forceRefresh: Boolean = false): List<AppInfo> = withContext(Dispatchers.IO) {
        if (cachedApps.isNotEmpty() && !forceRefresh) {
            return@withContext cachedApps
        }
        val pm = appContext.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved = pm.queryIntentActivities(intent, 0)
        val apps = resolved.map { resolveInfo ->
            AppInfo(
                name = resolveInfo.loadLabel(pm).toString(),
                packageName = resolveInfo.activityInfo.packageName,
                icon = resolveInfo.loadIcon(pm)
            )
        }.distinctBy { it.packageName }.sortedBy { it.name }
        cachedApps = apps
        return@withContext apps
    }

    companion object {
        @Volatile
        private var INSTANCE: InstalledAppsRepository? = null

        fun getInstance(context: Context): InstalledAppsRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: InstalledAppsRepository(context).also { INSTANCE = it }
            }
        }
    }
}
