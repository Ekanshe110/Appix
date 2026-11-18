package com.music.bitchord.playback

import android.net.Uri
import androidx.media3.common.MediaItem

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
 *
 * The address the player is handed is the *addon's*, not Apple's: the addon
 * publishes the playlist, because the key line in Apple's own playlist names
 * a key the audio is not encrypted under. So recognising only Apple's hosts
 * left every guard above switched off for the item actually playing, and the
 * hunt went after the station again. Both forms are recognised here, and
 * [isLive] is the one to ask when a [MediaItem] is in hand.
 */
internal object LiveRadio {

    /** The hosts Apple's live stations are served from. */
    private val HOSTS = setOf(
        "itsliveradio.apple.com",
        "liveradio.music.apple.com",
        "liveradiobackup.music.apple.com",
    )

    /**
     * The path an addon's live playlist is published under.
     *
     * `…/radio/playlist/<station>/master.m3u8` and the `variant.m3u8` it
     * names, on whichever host the listener's addon runs.
     */
    private const val ADDON_RADIO_PATH = "/radio/playlist/"

    /**
     * Whether this address is a live station's stream.
     *
     * True for the playlist, the media playlist and the segments: the audio
     * comes from Apple's own radio hosts, and the playlists come from the
     * addon that republishes them.
     */
    fun isLiveStream(uri: Uri?): Boolean {
        if (uri == null) return false
        val host = uri.host?.lowercase().orEmpty()
        if (host.isNotEmpty() && HOSTS.any { host == it || host.endsWith(".$it") }) {
            return true
        }
        val path = uri.path?.lowercase() ?: return false
        return path.contains(ADDON_RADIO_PATH)
    }

    /**
     * Whether this item is a live station.
     *
     * The DRM configuration is the second witness and the more reliable one:
     * a live station is the only item this app ever gives one, so an item
     * that carries a license URL is a station even if the address around it
     * changes shape.
     */
    fun isLive(item: MediaItem?): Boolean {
        val configuration = item?.localConfiguration ?: return false
        if (configuration.drmConfiguration != null) return true
        return isLiveStream(configuration.uri)
    }
}
