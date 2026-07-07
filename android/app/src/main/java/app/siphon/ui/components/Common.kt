package app.siphon.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.siphon.util.Platform

/** Uppercase micro-label used above groups, mirroring the web's group headers. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

val Platform.brandColor: Color
    get() = when (this) {
        Platform.YOUTUBE -> Color(0xFFFF5252)
        Platform.INSTAGRAM -> Color(0xFFE15FED)
        Platform.TIKTOK -> Color(0xFF4CD9E8)
        Platform.TWITTER -> Color(0xFFE7E9EA)
        Platform.FACEBOOK -> Color(0xFF5B8DEF)
    }

/**
 * Colored-dot chip row showing supported platforms; active one glows.
 * Scrolls horizontally rather than squeezing chips — five platform badges
 * plus spacing don't reliably fit narrow screens at natural size.
 */
@Composable
fun PlatformChipsRow(active: Platform?, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Platform.entries.forEach { platform ->
            val isActive = platform == active
            Surface(
                shape = CircleShape,
                color = if (isActive) MaterialTheme.colorScheme.surfaceContainerHighest
                else MaterialTheme.colorScheme.surfaceContainer,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    else Color.White.copy(alpha = 0.07f),
                ),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Box(
                        Modifier
                            .size(7.dp)
                            .background(platform.brandColor, CircleShape),
                    )
                    Text(
                        platform.displayName,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isActive) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyState(message: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
