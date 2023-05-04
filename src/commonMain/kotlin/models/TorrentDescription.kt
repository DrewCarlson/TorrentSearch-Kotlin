package torrentsearch.models

import kotlinx.serialization.Serializable

/**
 * Represents a torrent listed on a [torrentsearch.TorrentProvider].
 * Contains all the provider's latest details for the torrent and
 * a [magnetUrl] which can be used to download the torrent.
 */
@Serializable
public data class TorrentDescription(
    /** The provider which returned the torrent. */
    val provider: String,
    /** The Bittorrent magnet url to download the torrent. */
    val magnetUrl: String,
    /** The provider's name for this torrent, not always the actual torrent file name. */
    val title: String,
    /** The size in bytes of the torrent contents. */
    val size: Long,
    /** The number of users seeding the torrent. */
    val seeds: Int,
    /** The number of users downloading the torrent. */
    val peers: Int,
    /** The themoviedb.org id for the content of the torrent. */
    val themoviedbId: Int? = null,
    /** The thetvdb.com id for the content of the torrent. */
    val tvdbId: Int? = null,
    /** The imdb.com id for the content of the torrent. */
    val imdbId: String? = null,
    /** The providers HTML info page for the torrent. */
    val infoUrl: String? = null,
    /** The torrent's hash. */
    val hash: String,
)
