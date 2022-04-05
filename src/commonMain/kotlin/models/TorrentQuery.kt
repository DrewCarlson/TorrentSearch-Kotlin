package torrentsearch.models

data class TorrentQuery(
    var content: String? = null,
    var category: Category? = null,
    var imdbId: String? = null,
    var tmdbId: Int? = null,
    var tvdbId: Int? = null,
    var contentYear: Int? = null,
    val skipCache: Boolean = false,
    /**
     * The limit applied to each provider query, expect a higher
     * [TorrentDescription] count when more than one provider
     * is enabled.
     */
    var limit: Int = -1,
)