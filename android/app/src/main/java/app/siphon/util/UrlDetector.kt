package app.siphon.util

/**
 * Client-side platform detection — the Kotlin mirror of the backend's
 * lib/platform.ts. Used by the Home screen, clipboard watcher and the
 * share-sheet flow for instant feedback; the backend stays authoritative.
 */
enum class Platform(val displayName: String) {
    YOUTUBE("YouTube"),
    INSTAGRAM("Instagram"),
    TIKTOK("TikTok"),
    TWITTER("X"),
    FACEBOOK("Facebook");
}

object UrlDetector {

    private val hostRules: List<Pair<Platform, Regex>> = listOf(
        Platform.YOUTUBE to Regex("""(^|\.)((youtube\.com)|(youtu\.be)|(youtube-nocookie\.com))$""", RegexOption.IGNORE_CASE),
        Platform.INSTAGRAM to Regex("""(^|\.)instagram\.com$""", RegexOption.IGNORE_CASE),
        Platform.TIKTOK to Regex("""(^|\.)tiktok\.com$""", RegexOption.IGNORE_CASE),
        Platform.TWITTER to Regex("""(^|\.)((twitter\.com)|(x\.com)|(t\.co))$""", RegexOption.IGNORE_CASE),
        Platform.FACEBOOK to Regex("""(^|\.)((facebook\.com)|(fb\.watch)|(fb\.com))$""", RegexOption.IGNORE_CASE),
    )

    private val urlInText = Regex("""https?://\S+""")

    /** Detect the platform of [raw] if it is (or can be coerced into) a supported URL. */
    fun detect(raw: String): Platform? {
        val host = hostOf(raw) ?: return null
        return hostRules.firstOrNull { (_, rule) -> rule.containsMatchIn(host) }?.first
    }

    /**
     * Pull the first URL out of arbitrary shared text. Share intents often
     * carry "Check this out! https://…" style payloads.
     */
    fun extractUrl(sharedText: String?): String? {
        if (sharedText.isNullOrBlank()) return null
        val trimmed = sharedText.trim()
        urlInText.find(trimmed)?.let { return it.value.trimEnd(')', ']', '.', ',') }
        // Bare host without scheme ("youtu.be/abc")
        return if (detect(trimmed) != null && !trimmed.contains(' ')) "https://$trimmed" else null
    }

    fun isSupported(raw: String): Boolean = detect(raw) != null

    private fun hostOf(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty() || trimmed.contains(' ')) return null
        val candidate = if (Regex("^[a-zA-Z][a-zA-Z0-9+.-]*://").containsMatchIn(trimmed)) trimmed else "https://$trimmed"
        if (!candidate.startsWith("http://") && !candidate.startsWith("https://")) return null
        return try {
            val uri = java.net.URI(candidate)
            uri.host?.takeIf { it.contains('.') }?.lowercase()
        } catch (_: Exception) {
            null
        }
    }
}
