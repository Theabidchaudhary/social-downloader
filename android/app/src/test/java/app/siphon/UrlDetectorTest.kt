package app.siphon

import app.siphon.util.Platform
import app.siphon.util.UrlDetector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UrlDetectorTest {

    @Test
    fun detectsYouTubeVariants() {
        assertEquals(Platform.YOUTUBE, UrlDetector.detect("https://www.youtube.com/watch?v=dQw4w9WgXcQ"))
        assertEquals(Platform.YOUTUBE, UrlDetector.detect("https://youtu.be/dQw4w9WgXcQ"))
        assertEquals(Platform.YOUTUBE, UrlDetector.detect("youtube.com/shorts/abc123"))
    }

    @Test
    fun detectsInstagramTikTokTwitterFacebook() {
        assertEquals(Platform.INSTAGRAM, UrlDetector.detect("https://www.instagram.com/reel/Cxyz/"))
        assertEquals(Platform.TIKTOK, UrlDetector.detect("https://vm.tiktok.com/ZM8abc/"))
        assertEquals(Platform.TWITTER, UrlDetector.detect("https://x.com/user/status/123"))
        assertEquals(Platform.TWITTER, UrlDetector.detect("https://twitter.com/user/status/123"))
        assertEquals(Platform.FACEBOOK, UrlDetector.detect("https://fb.watch/abc/"))
    }

    @Test
    fun rejectsUnsupportedAndJunk() {
        assertNull(UrlDetector.detect("https://vimeo.com/123"))
        assertNull(UrlDetector.detect("not a url"))
        assertNull(UrlDetector.detect(""))
        assertNull(UrlDetector.detect("https://notyoutube.com/watch?v=x"))
    }

    @Test
    fun extractsUrlFromSharedText() {
        assertEquals(
            "https://youtu.be/abc",
            UrlDetector.extractUrl("Check this out! https://youtu.be/abc"),
        )
        assertEquals(
            "https://www.tiktok.com/@u/video/1",
            UrlDetector.extractUrl("https://www.tiktok.com/@u/video/1"),
        )
        assertNull(UrlDetector.extractUrl("no links here"))
        assertNull(UrlDetector.extractUrl(null))
    }

    @Test
    fun coercesBareHostSharedText() {
        assertEquals("https://youtu.be/abc", UrlDetector.extractUrl("youtu.be/abc"))
    }
}
