package com.music.bitchord.data.apple

import com.music.bitchord.data.sources.addon.AddonManifest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The Apple Music catalogue, as the app sees it.
 *
 * Apple's own catalogue is not read from inside the app: it is read from an
 * addon the listener installs, which holds the account and the developer
 * token. So every Apple answer in this package comes from an installed
 * manifest — see [AppleManifest.detect].
 *
 * The shapes here are the addon's, not Apple's: the addon already speaks
 * Apple's own catalogue and republishes it in the shape this app reads.
 */
@Serializable
data class AppleShelf(
    @SerialName("title") val title: String = "",
    @SerialName("subtitle") val subtitle: String = "",
    @SerialName("artworkURL") val artworkURL: String? = null,
    @SerialName("items") val items: List<AppleItem> = emptyList(),
)

/**
 * One row in an Apple shelf.
 *
 * [type] is Apple's own word for the row: `song`, `album` or `playlist`. A
 * song row can be played directly; an album and a playlist are pages, opened
 * on a tap.
 */
@Serializable
data class AppleItem(
    @SerialName("type") val type: String = "song",
    @SerialName("id") val id: String = "",
    @SerialName("title") val title: String = "",
    @SerialName("artist") val artist: String = "",
    @SerialName("album") val album: String = "",
    @SerialName("artworkURL") val artworkURL: String? = null,
    @SerialName("durationMs") val durationMs: Long = 0,
    @SerialName("isrc") val isrc: String? = null,
    @SerialName("hasDrm") val hasDrm: Boolean? = null,
    @SerialName("url") val url: String? = null,
)

/** The addon's Apple home page: a list of shelves, in Apple's own order. */
@Serializable
data class AppleHome(
    @SerialName("shelves") val shelves: List<AppleShelf> = emptyList(),
)

/**
 * A page of the listener's Apple Music library.
 *
 * [tracks] holds the rows; [offset] and [total] are the page window, so the
 * library can be paged without loading all of it at once.
 */
@Serializable
data class AppleLibraryPage(
    @SerialName("tracks") val tracks: List<AppleItem> = emptyList(),
    @SerialName("offset") val offset: Int = 0,
    @SerialName("total") val total: Int = 0,
)

/**
 * One Apple Music station.
 *
 * The kinds are Apple's own names for them: `user` is the listener's personal
 * station, `songSeeded` and `playlistSeeded` are the stations Apple builds
 * from a song or a playlist, and `streaming` is a continuous live stream.
 *
 * A live station's audio is a Widevine-protected stream that only Apple's
 * own player plays, so [url] is Apple's page for it rather than a stream
 * address.
 */
@Serializable
data class AppleStation(
    @SerialName("id") val id: String = "",
    @SerialName("name") val name: String = "",
    @SerialName("kind") val kind: String? = null,
    @SerialName("isLive") val isLive: Boolean? = null,
    @SerialName("subtitle") val subtitle: String? = null,
    @SerialName("artworkURL") val artworkURL: String? = null,
    @SerialName("url") val url: String? = null,
)

/** A list of Apple stations. */
@Serializable
data class AppleStationList(
    @SerialName("stations") val stations: List<AppleStation> = emptyList(),
)

/** One Apple station's own metadata, when the addon published it. */
@Serializable
data class AppleStationResponse(
    @SerialName("station") val station: AppleStation? = null,
)

/**
 * The manifest fields this app reads to know an addon is Apple's.
 *
 * The addon publishes `catalogueProvider: "apple-music"` and a `library`
 * block; an addon that publishes neither is an ordinary addon and is left
 * alone.
 */
@Serializable
data class AppleManifest(
    @SerialName("catalogueProvider") val catalogueProvider: String? = null,
    @SerialName("library") val library: AppleLibraryBlock? = null,
) {
    /** Whether this manifest names Apple Music as its catalogue. */
    val isApple: Boolean get() = catalogueProvider == APPLE_CATALOGUE

    /** The library collections this addon publishes, when it publishes any. */
    val collections: List<String> get() = library?.collections.orEmpty()

    companion object {
        /** The catalogue name Apple Music's manifest uses. */
        const val APPLE_CATALOGUE = "apple-music"

        /**
         * Whether [manifest] is an Apple Music manifest.
         *
         * Read from the manifest itself rather than from the addon's name or
         * its address: a listener's own Apple Music server is a perfectly
         * ordinary addon that happens to serve Apple's catalogue, and it is
         * found by what it says, not by where it lives.
         */
        fun detect(manifest: AddonManifest): Boolean {
            val raw = manifest.rawCatalogueProvider ?: return false
            return raw == APPLE_CATALOGUE
        }
    }
}

/** The `library` block of an Apple Music manifest. */
@Serializable
data class AppleLibraryBlock(
    @SerialName("provider") val provider: String? = null,
    @SerialName("collections") val collections: List<String> = emptyList(),
)
