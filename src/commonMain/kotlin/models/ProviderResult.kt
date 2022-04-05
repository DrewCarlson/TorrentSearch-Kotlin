package torrentsearch.models

import io.ktor.http.HttpStatusCode

/**
 * A container for the result of a single [torrentsearch.TorrentProvider] query.
 * [ProviderResult] is either a [Success] instance or any number of [Error] types.
 */
public sealed class ProviderResult {
    public abstract val providerName: String

    public data class Success(
        override val providerName: String,
        val torrents: List<TorrentDescription>,
        val fromCache: Boolean = false,
    ) : ProviderResult()

    public sealed class Error : ProviderResult() {
        public abstract val message: String?

        public data class RequestError(
            override val providerName: String,
            val httpStatusCode: HttpStatusCode?,
            val body: String?,
            override val message: String? = body,
        ) : Error()

        public data class InvalidQueryError(
            override val providerName: String,
            override val message: String?,
        ) : Error()

        public data class UnknownError(
            override val providerName: String,
            override val message: String?,
        ) : Error()
    }
}