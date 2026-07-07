package app.siphon.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Wire types for the Siphon API. Field names mirror backend/src/services/extractor.ts. */

@Serializable
data class ResolvedFormatDto(
    val id: String,
    val kind: String, // "video" | "audio"
    val container: String, // "mp4" | "mp3" | "m4a"
    val qualityLabel: String,
    val width: Int? = null,
    val height: Int? = null,
    val fps: Double? = null,
    val sizeBytes: Long? = null,
    val sizeIsEstimate: Boolean = true,
    val directUrl: String? = null,
    val requiresProcessing: Boolean = false,
    val downloadUrl: String,
)

@Serializable
data class PlaylistEntryDto(
    val title: String,
    val url: String,
    val durationSeconds: Double? = null,
    val thumbnailUrl: String? = null,
)

/**
 * Union of the media and playlist resolve responses, discriminated by [type].
 * Optional fields cover whichever variant is absent.
 */
@Serializable
data class ResolveResponseDto(
    val type: String, // "media" | "playlist"
    val platform: String,
    val sourceUrl: String,
    val title: String,
    val kind: String? = null,
    val uploader: String? = null,
    val durationSeconds: Double? = null,
    val thumbnailUrl: String? = null,
    val video: List<ResolvedFormatDto> = emptyList(),
    val audio: List<ResolvedFormatDto> = emptyList(),
    val expiresAt: String? = null,
    val entryCount: Int? = null,
    val entries: List<PlaylistEntryDto> = emptyList(),
)

@Serializable
data class ApiErrorBodyDto(val error: ApiErrorDto? = null)

@Serializable
data class ApiErrorDto(
    val code: String = "INTERNAL",
    val message: String = "Something went wrong.",
    @SerialName("details") val details: Map<String, String>? = null,
)
