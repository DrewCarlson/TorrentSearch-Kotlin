package torrentsearch.models

import io.ktor.http.HttpStatusCode

sealed class ProviderResult {
    abstract val providerName: String

    data class Success(
        override val providerName: String,
        val torrents: List<TorrentDescription>,
        val fromCache: Boolean = false,
    ) : ProviderResult()

    sealed class Error : ProviderResult() {
        abstract val message: String?

        data class RequestError(
            override val providerName: String,
            val httpStatusCode: HttpStatusCode?,
            val body: String?,
            override val message: String? = body,
        ) : Error()

        data class InvalidQueryError(
            override val providerName: String,
            override val message: String?,
        ) : Error()

        data class UnknownError(
            override val providerName: String,
            override val message: String?,
        ) : Error()
    }
}