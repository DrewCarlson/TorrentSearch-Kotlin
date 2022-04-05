package torrentsearch.models

/**
 * [TorrentQuery] contains all the required details to search for
 * a specific torrent across all enabled providers.
 */
public data class TorrentQuery(
    /** The text query to run on each [torrentsearch.TorrentProvider]. */
    var content: String? = null,
    /** The [Category] to filter torrent results for. */
    var category: Category? = null,
    /** The imdb.com id to filter torrent results for. */
    var imdbId: String? = null,
    /** The themoviedb.org id to filter torrent results for. */
    var tmdbId: Int? = null,
    /** The thetvdb.com id to filter torrent results for. */
    var tvdbId: Int? = null,
    /** When true, ignore cached results and do not cache results for this query. */
    val skipCache: Boolean = false,
    /**
     * The limit applied to each provider query, expect a higher
     * [TorrentDescription] count when more than one provider
     * is enabled.
     */
    var limit: Int = -1,
)