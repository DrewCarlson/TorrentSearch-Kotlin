package torrentsearch.models

/**
 * Represents a parameter that may be consumed by a
 * [torrentsearch.TorrentProvider] when executing a query.
 */
public enum class SearchParam {
    QUERY,
    CATEGORY,
    LIMIT,
    TOKEN,
    TMDB_ID,
    IMDB_ID,
    TVDB_ID,
    PAGE,
}
