package com.music.bitchord.playback

import android.net.Uri

/**
 * Apple's live radio, as the app recognises it.
 *
 * A live station is one continuous encrypted stream: there is no file behind
 * it, no length, no other rendition of it, and nothing to look for. So the
 * machinery that swaps a track for a better copy — the upgrade hunt, the
 * source substitution, the read-ahead — has to leave it alone.
 *
 * It used to treat a live station like any other source-backed row, which
 * meant it went looking for a "better copy" of it, found something (a 96 kHz
 * file of a different recording, because the station's id was read as a
 * catalogue id), and swapped it in. The stream was replaced every few
 * seconds and the station played as a hiss.
 */
internal object LiveRadio {

    /** The hosts Apple's live stations are served from. */
    private val HOSTS = setOf(
        "itsliveradio.apple.com",
        "liveradio.music.apple.com",
        "liveradiobackup.music.apple.com",
    )

    /**
     * Whether this address is a live station's stream.
     *
     * True for the playlist, the media playlist and the segments: they are
     * all on Apple's own radio hosts.
     */
    fun isLiveStream(uri: Uri?): Boolean {
        val host = uri?.host?.lowercase() ?: return false
        return HOSTS.any { host == it || host.endsWith(".$it") }
    }
}
