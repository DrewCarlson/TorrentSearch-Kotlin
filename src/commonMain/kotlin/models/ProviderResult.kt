package torrentsearch.models

import io.ktor.http.HttpStatusCode

/**
 * A container for the result of a single [torrentsearch.TorrentProvider] query.
 * [ProviderResult] is either a [Success] instance or any number of [Error] types.
 */
public sealed class ProviderResult {
    /** The name of the provider which produced this result. */
    public abstract val providerName: String

    /**
     * The data acquired from a successful [torrentsearch.TorrentProvider] query.
     */
    public data class Success(
        override val providerName: String,
        /** The torrents found on the provider. */
        val torrents: List<TorrentDescription>,
        /** True if this result is from a [torrentsearch.TorrentProviderCache]/ */
        val fromCache: Boolean = false,
        /** The page number containing the [torrents].  */
        val page: Int = 1,
        /** The size (or limit) of each page. */
        val pageSize: Int = torrents.size,
        /** The total number of torrents available on the provider. */
        val totalTorrents: Int = torrents.size,
    ) : ProviderResult() {
        /**
         * Returns true when the provider indicated more results are available
         * with additional requests.
         */
        val hasMoreResults: Boolean = totalTorrents > 0 && page * pageSize < totalTorrents
    }

    /**
     * The data acquired when an error occurred during a
     * [torrentsearch.TorrentProvider] query.
     */
    public sealed class Error : ProviderResult() {
        public abstract val message: String?

        /**
         * Indicates a network error when attempting a query.
         */
        public data class RequestError(
            override val providerName: String,
            val httpStatusCode: HttpStatusCode?,
            val body: String?,
            override val message: String? = body,
        ) : Error()

        /**
         * Indicates an error when creating the query details for the
         * provider's API.  This usually occurs when the query string
         * or id search method is not available for the provider.
         */
        public data class InvalidQueryError(
            override val providerName: String,
            override val message: String?,
        ) : Error()

        /**
         * Indicates an unhandled error occurred.
         */
        public data class UnknownError(
            override val providerName: String,
            override val message: String?,
            val exception: Throwable?,
        ) : Error()
    }
}
