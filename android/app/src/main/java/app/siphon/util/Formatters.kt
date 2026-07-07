package app.siphon.util

import java.util.Locale

object Formatters {

    fun bytes(value: Long?, estimate: Boolean = false): String {
        if (value == null || value <= 0) return "—"
        val units = arrayOf("B", "KB", "MB", "GB")
        var v = value.toDouble()
        var unit = 0
        while (v >= 1024 && unit < units.lastIndex) {
            v /= 1024
            unit++
        }
        val text = if (v >= 100) {
            String.format(Locale.US, "%.0f %s", v, units[unit])
        } else {
            String.format(Locale.US, "%.1f %s", v, units[unit])
        }
        return if (estimate) "≈ $text" else text
    }

    fun speed(bytesPerSecond: Long): String =
        if (bytesPerSecond <= 0) "—" else "${bytes(bytesPerSecond)}/s"

    fun duration(seconds: Long?): String {
        if (seconds == null || seconds <= 0) return ""
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return if (h > 0) {
            String.format(Locale.US, "%d:%02d:%02d", h, m, s)
        } else {
            String.format(Locale.US, "%d:%02d", m, s)
        }
    }

    fun eta(seconds: Long): String {
        if (seconds < 0) return ""
        if (seconds < 60) return "${seconds}s left"
        val m = seconds / 60
        if (m < 60) return "${m}m ${seconds % 60}s left"
        return "${m / 60}h ${m % 60}m left"
    }

    fun progressPercent(downloaded: Long, total: Long): Int =
        if (total <= 0) 0 else ((downloaded * 100) / total).toInt().coerceIn(0, 100)
}
