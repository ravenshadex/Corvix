package com.corvix.player.audio

import android.app.usage.StorageStatsManager
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.storage.StorageManager
import java.util.Locale

data class DeviceStorageSnapshot(
    val deviceName: String,
    val isNothingPhone: Boolean,
    val usedGb: Double,
    val totalGb: Double
) {
    val summaryLabel: String
        get() = "%.1f / %.0f GB".format(Locale.US, usedGb, totalGb)

    val usedFraction: Float
        get() = if (totalGb > 0.0) (usedGb / totalGb).toFloat().coerceIn(0.03f, 1f) else 0.1f
}

object DeviceStorageInfo {

    private val modelDisplayMap = mapOf(
        "A063" to "Nothing Phone (1)",
        "AIN065" to "Nothing Phone (2)",
        "A142" to "Nothing Phone (2a)",
        "A142P" to "Nothing Phone (2a) Plus",
        "A059" to "Nothing Phone (3a)",
        "A059P" to "Nothing Phone (3a) Pro"
    )

    fun readSnapshot(context: Context): DeviceStorageSnapshot {
        val rawMfg = Build.MANUFACTURER.orEmpty().trim()
        val rawModel = Build.MODEL.orEmpty().trim()
        val deviceName = resolveFriendlyModelName(rawMfg, rawModel)
        val isNothing = rawMfg.equals("Nothing", ignoreCase = true) ||
            deviceName.contains("Nothing", ignoreCase = true)

        val (usedBytes, totalBytes) = readStorageBytes(context)
        val gbDiv = 1_000_000_000.0
        val totalGb = (totalBytes / gbDiv).coerceAtLeast(16.0)
        val usedGb = (usedBytes / gbDiv).coerceIn(0.0, totalGb)

        return DeviceStorageSnapshot(
            deviceName = deviceName,
            isNothingPhone = isNothing,
            usedGb = usedGb,
            totalGb = totalGb
        )
    }

    private fun resolveFriendlyModelName(manufacturer: String, model: String): String {
        val upperModel = model.uppercase(Locale.US)
        modelDisplayMap[upperModel]?.let { return it }
        if (model.contains(manufacturer, ignoreCase = true)) return model
        val cleanMfg = manufacturer.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString()
        }
        return "$cleanMfg $model".trim().ifEmpty { "Android Device" }
    }

    private fun readStorageBytes(context: Context): Pair<Long, Long> {
        runCatching {
            val stats = context.getSystemService(Context.STORAGE_STATS_SERVICE) as? StorageStatsManager
            if (stats != null) {
                val total = stats.getTotalBytes(StorageManager.UUID_DEFAULT)
                val free = stats.getFreeBytes(StorageManager.UUID_DEFAULT)
                if (total > 0L) {
                    return (total - free).coerceAtLeast(0L) to total
                }
            }
        }
        return runCatching {
            val stat = StatFs(Environment.getDataDirectory().path)
            val total = stat.totalBytes
            val free = stat.availableBytes
            (total - free).coerceAtLeast(0L) to total
        }.getOrElse { 18_400_000_000L to 128_000_000_000L }
    }
}
