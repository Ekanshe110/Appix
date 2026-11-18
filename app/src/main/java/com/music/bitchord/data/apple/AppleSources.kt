package com.music.bitchord.data.apple

import com.music.bitchord.data.model.BrowseItem
import com.music.bitchord.data.model.BrowseType
import com.music.bitchord.data.model.HomeShelf
import com.music.bitchord.data.model.PlaybackSourceType
import com.music.bitchord.data.model.RadioStation
import com.music.bitchord.data.model.SearchResult
import com.music.bitchord.data.model.ShelfItem
import com.music.bitchord.data.model.Song
import com.music.bitchord.data.sources.SourceRegistry
import com.music.bitchord.data.sources.addon.AddonManifest

/**
 * The Apple Music catalogue, as the app's own rows.
 *
 * [AppleCatalogue] speaks the addon's wire; this speaks the app's. A song
 * read from Apple becomes a [Song] whose id is packed for the addon that
 * holds the account, so pressing play asks that addon for the audio by the
 * very catalogue id it published — no search, no guessing.
 *
 * The rows carry Apple's own metadata whatever serves the audio later. That
 * is what the *source* axis means: Apple's spelling of a song, Apple's
 * cover, Apple's catalogue id. Where the bytes come from is a separate
 * question, and [SourceResolver] asks it separately.
 */
internal object AppleSources {

    /** A library playlist, opened as a page of its own. */
    const val PAGE_PREFIX = "apple:page:"

    /** An album, opened as a page of its own. */
    const val ALBUM_PREFIX = "apple:album:"

    /** An artist, opened as a page of its own. */
    const val ARTIST_PREFIX = "apple:artist:"

    /** A radio station, opened as a page of its own. */
    const val STATION_PREFIX = "apple:station:"

    /**
     * An album in the listener's own library.
     *
     * A library album is not a catalogue album: it is the listener's own
     * copy, and Apple knows it by a library id rather than a catalogue one.
     * It gets its own prefix so the two are never confused — opening a
     * library album has to ask the library, not the catalogue.
     */
    const val LIB_ALBUM_PREFIX = "apple:libalbum:"

    /**
     * The installed Apple Music manifest, as the two things the app needs
     * from it: where to ask, and which source its ids belong to.
     */
    data class Handle(
        /** The addon's own id in the source registry. */
        val configId: String,
        /** Where the addon lives. */
        val baseUrl: String,
        /** What the manifest says it publishes. */
        val manifest: AddonManifest,
    )

    /** The installed Apple Music manifest, or null when there is none. */
    suspend fun handle(): Handle? {
        val baseUrl = AppleManifestLocator.baseUrl() ?: return null
        val config = SourceRegistry.configs.value.firstOrNull { it.baseUrl == baseUrl } ?: return null
        val manifest = AppleManifestLocator.manifest()?.second ?: return null
        return Handle(configId = config.id, baseUrl = baseUrl, manifest = manifest)
    }

    /**
     * Apple's home, as the shelves this app draws.
     *
     * Apple's own order is kept. It is the order Apple's app draws, and the
     * order it draws it in is a large part of what it looks like.
     */
    suspend fun home(handle: Handle): Result<List<HomeShelf>> =
        AppleCatalogue.home(handle.baseUrl).map { home ->
            home.shelves.mapNotNull { shelf ->
                val items = shelf.items.mapNotNull { item -> item.shelfItem(handle.configId) }
                if (items.isEmpty()) null
                else HomeShelf(title = shelf.title, subtitle = shelf.subtitle, items = items)
            }
        }

    /**
     * One library collection — playlists, albums or songs — as rows.
     */
    suspend fun library(
        handle: Handle,
        collection: String,
        offset: Int = 0,
        limit: Int = 50,
    ): Result<List<AppleItem>> =
        AppleCatalogue.library(handle.baseUrl, collection, offset, limit).map { it.tracks }

    /**
     * The tracks of one library collection.
     */
    suspend fun libraryTracks(
        handle: Handle,
        collection: String,
        collectionId: String,
    ): Result<List<AppleItem>> =
        AppleCatalogue.libraryTracks(handle.baseUrl, collection, collectionId).map { it.tracks }

    /**
     * Apple's album, playlist or artist page, as rows.
     */
    suspend fun catalogTracks(
        handle: Handle,
        kind: String,
        id: String,
    ): Result<List<AppleItem>> =
        AppleCatalogue.catalogTracks(handle.baseUrl, kind, id)

    /**
     * Apple's own search, as this app's rows.
     *
     * Apple's songs are playable rows: the id published is the id the
     * addon streams. Apple's albums, playlists and artists are pages.
     */
    suspend fun search(
        handle: Handle,
        query: String,
        types: String = "songs,albums,playlists,artists",
        limit: Int = 25,
    ): Result<List<SearchResult>> =
        AppleCatalogue.search(handle.baseUrl, query, types, limit).map { response ->
            buildList {
                response.tracks.forEach { item ->
                    add(SearchResult.Track(item.song(handle.configId)))
                }
                response.albums.forEach { item ->
                    add(SearchResult.Browse(item.browseItem()))
                }
                response.playlists.forEach { item ->
                    add(SearchResult.Browse(item.browseItem()))
                }
                response.artists.forEach { item ->
                    add(SearchResult.Browse(item.browseItem()))
                }
            }
        }

    /**
     * Apple's stations: the listener's own, and the ones Apple builds.
     */
    suspend fun stations(handle: Handle): Result<List<AppleStation>> =
        AppleCatalogue.stations(handle.baseUrl)

    /** Apple's live stations — continuous streams rather than song queues. */
    suspend fun liveStations(handle: Handle): Result<List<AppleStation>> =
        AppleCatalogue.liveStations(handle.baseUrl)

    /** Apple's station search. */
    suspend fun searchStations(
        handle: Handle,
        query: String,
    ): Result<List<AppleStation>> =
        AppleCatalogue.searchStations(handle.baseUrl, query)

    /** The tracks of one station — what plays after the seed. */
    suspend fun stationTracks(
        handle: Handle,
        stationId: String,
    ): Result<List<AppleItem>> =
        AppleCatalogue.stationTracks(handle.baseUrl, stationId)

    /**
     * The stations other broadcasters run, as the rows the Radio page draws.
     *
     * They are Apple's own page's "International Broadcasters" row: live
     * streams from other people, with the broadcaster's own name under the
     * station's.
     */
    suspend fun internationalStations(handle: Handle, limit: Int = 12): Result<List<RadioStation>> =
        AppleCatalogue.internationalStations(handle.baseUrl, limit).map { rows ->
            rows.map { it.station() }
        }

    /**
     * The listener's own playlists, as the rows the Library page draws.
     *
     * A playlist is opened as a page of its own tracks, and it is opened by
     * the library id: it is the listener's own copy, and the catalogue's id
     * for the same recording is not the same thing.
     */
    suspend fun libraryPlaylists(handle: Handle, limit: Int = 100): Result<List<ShelfItem>> =
        AppleCatalogue.library(handle.baseUrl, "playlists", 0, limit).map { page ->
            page.tracks.mapNotNull { item ->
                if (item.title.isBlank()) return@mapNotNull null
                ShelfItem(
                    title = item.title,
                    subtitle = item.artist,
                    thumbnailUrl = item.artworkURL,
                    videoId = null,
                    browseId = PAGE_PREFIX + item.id,
                )
            }
        }

    /** The listener's own albums, as the tiles the Library page shows. */
    suspend fun libraryAlbums(handle: Handle, limit: Int = 100): Result<List<ShelfItem>> =
        AppleCatalogue.library(handle.baseUrl, "albums", 0, limit).map { page ->
            page.tracks.mapNotNull { item ->
                if (item.title.isBlank()) return@mapNotNull null
                ShelfItem(
                    title = item.title,
                    subtitle = item.artist,
                    thumbnailUrl = item.artworkURL,
                    videoId = null,
                    browseId = LIB_ALBUM_PREFIX + item.id,
                )
            }
        }

    /** The listener's own songs, as the rows that play. */
    suspend fun librarySongs(handle: Handle, limit: Int = 100): Result<List<Song>> =
        AppleCatalogue.library(handle.baseUrl, "songs", 0, limit).map { page ->
            page.tracks.mapNotNull { item ->
                val id = item.catalogId?.takeIf { it.isNotBlank() } ?: item.id
                if (id.isBlank() || item.title.isBlank()) return@mapNotNull null
                Song(
                    videoId = SourceRegistry.trackKey(handle.configId, id),
                    title = item.title,
                    artist = item.artist,
                    thumbnailUrl = item.artworkURL,
                    durationText = item.runtimeMs.takeIf { it > 0 }?.let(::durationText),
                    albumName = item.album.takeIf { it.isNotBlank() },
                    sourceQuality = item.audioQuality,
                )
            }
        }

    // ── Rows ──────────────────────────────────────────────────────────────
}

/** The song as this app's player knows it. */
internal fun AppleItem.song(configId: String): Song = Song(
    videoId = SourceRegistry.trackKey(configId, id),
    title = title,
    artist = artist,
    thumbnailUrl = artworkURL,
    durationText = runtimeMs.takeIf { it > 0 }?.let(::durationText),
    albumName = album.takeIf { it.isNotBlank() },
    // What the player needs to know to offer the rendition switch: whether
    // Apple holds an Atmos mix of this recording, and whether it holds a
    // Hi-Res master.
    atmosAvailable = hasAtmos,
    hiResAvailable = hasHiRes,
)

/** The card this app draws for the row. */
internal fun AppleItem.shelfItem(
    configId: String,
    albumBrowsePrefix: String = AppleSources.ALBUM_PREFIX,
): ShelfItem? {
    if (title.isBlank()) return null
    return when (type.lowercase()) {
        "song" -> ShelfItem(
            title = title,
            subtitle = artist,
            thumbnailUrl = artworkURL,
            videoId = SourceRegistry.trackKey(configId, id),
            browseId = null,
        )
        "album" -> ShelfItem(
            title = title,
            subtitle = artist,
            thumbnailUrl = artworkURL,
            videoId = null,
            browseId = albumBrowsePrefix + id,
        )
        else -> ShelfItem(
            title = title,
            subtitle = artist.ifBlank { album },
            thumbnailUrl = artworkURL,
            videoId = null,
            browseId = browseIdOf(),
        )
    }
}

/** The page this row opens, when it is a page and not a track. */
internal fun AppleItem.browseIdOf(): String? = when (type.lowercase()) {
    "album" -> AppleSources.ALBUM_PREFIX + id
    "playlist" -> AppleSources.PAGE_PREFIX + id
    "artist" -> AppleSources.ARTIST_PREFIX + id
    "station" -> AppleSources.STATION_PREFIX + id
    else -> null
}

/** The page this row opens, as the model the detail stack holds. */
internal fun AppleItem.browseItem(): BrowseItem = BrowseItem(
    browseId = browseIdOf() ?: "",
    title = title,
    subtitle = artist.ifBlank { album },
    thumbnailUrl = artworkURL,
    type = when (type.lowercase()) {
        "album" -> BrowseType.ALBUM
        "playlist" -> BrowseType.PLAYLIST
        "artist" -> BrowseType.ARTIST
        else -> BrowseType.OTHER
    },
)

/** A runtime as a display string, the way the rows show it. */
internal fun durationText(ms: Long): String {
    val total = ms / 1000
    return "%d:%02d".format(total / 60, total % 60)
}

    /**
     * The station, as the radio page draws it.
 *
 * A live station carries Apple's own address rather than audio: its stream
 * is Widevine-protected and only Apple's player plays it, so the page opens
 * Apple's page for it and says so with the LIVE mark.
 */
internal fun AppleStation.station(): RadioStation = RadioStation(
    id = id,
    name = name,
    subtitle = subtitle,
    artworkUrl = artworkURL,
    isLive = isLive == true,
    deepLink = url?.takeIf { it.startsWith("http://") || it.startsWith("https://") },
    provider = provider,
)

/**
 * A live station, as a row this app can play.
 *
 * The row carries the stream itself: a live station is one continuous
 * playlist, not a song to be looked up, and its audio is Widevine-protected,
 * so the device needs the playlist and somewhere to get the license. The
 * license endpoint is the addon's, because the license can only be bought
 * with the listener's own Apple account — which never leaves the server.
 */
internal fun AppleLiveStream.song(
    handle: AppleSources.Handle,
    station: RadioStation,
): Song = Song(
    videoId = SourceRegistry.trackKey(handle.configId, station.id),
    title = station.name,
    artist = station.subtitle?.takeIf { it.isNotBlank() } ?: "Apple Music",
    thumbnailUrl = station.artworkUrl,
    durationText = null,
    radioName = station.name,
    playbackSource = station.name,
    playbackSourceType = PlaybackSourceType.RADIO,
    liveStreamUrl = streamURL,
    liveLicenseUrl = licenseUrl(handle.baseUrl, station.id),
)

/** Where the device asks the addon for a live stream's license. */
internal fun licenseUrl(baseUrl: String, stationId: String): String =
    baseUrl.trimEnd('/') + "/radio/license?station=" +
        java.net.URLEncoder.encode(stationId, "UTF-8")
