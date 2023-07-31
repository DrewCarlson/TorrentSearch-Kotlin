package torrentsearch.models

import io.ktor.http.*

/**
 * [ResolveResult] contains details and resolved torrents from a single provider.
 *
 * Because resolution may require a request for each [TorrentDescription], a
 * [ResolveResult] may be an [Error] but still contain [torrents] for any requests
 * that completed successfully.
 */
public sealed class ResolveResult {
    public abstract val providerName: String
    public abstract val torrents: List<TorrentDescription>

    /**
     * Indicates that all requested [TorrentDescription]s have been successfully
     * resolved.
     */
    public data class Success(
        override val providerName: String,
        override val torrents: List<TorrentDescription>,
    ) : ResolveResult()

    /**
     * Indicates that there was an error resolving at least one [TorrentDescription].
     *
     * Note that the [Error] may still contain some successfully resolved
     * [TorrentDescription]s in [torrents].
     */
    public sealed class Error : ResolveResult() {
        public abstract val message: String?

        /**
         * Indicates a network error when attempting to resolve one or more results.
         */
        public data class RequestError(
            override val providerName: String,
            override val torrents: List<TorrentDescription>,
            val httpStatusCode: HttpStatusCode?,
            val body: String?,
            override val message: String? = body,
        ) : Error()

        /**
         * Indicates an unhandled error occurred.
         */
        public data class UnknownError(
            override val providerName: String,
            override val message: String?,
            val exception: Throwable?,
            override val torrents: List<TorrentDescription>,
        ) : Error()
    }
}
