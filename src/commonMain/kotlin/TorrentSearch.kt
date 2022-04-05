package torrentsearch

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.cookies.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.Json
import torrentsearch.models.Category
import torrentsearch.models.SearchResult
import torrentsearch.models.TorrentQuery
import torrentsearch.providers.EztvProvider
import torrentsearch.providers.LibreProvider
import torrentsearch.providers.PirateBayProvider
import torrentsearch.providers.RarbgProvider
import torrentsearch.providers.YtsProvider

internal const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:98.0) Gecko/20100101 Firefox/98.0"

class TorrentSearch(
    private val providerCache: TorrentProviderCache? = null,
    httpClient: HttpClient = HttpClient(),
    installDefaultProviders: Boolean = true,
    providers: List<TorrentProvider>
) {
    private val disposed = MutableStateFlow(false)
    private val http = httpClient.config {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }

        defaultRequest {
            userAgent(USER_AGENT)
        }

        install(HttpCookies) {
            storage = AcceptAllCookiesStorage()
        }
    }

    private val providers = if (installDefaultProviders) {
        listOf(
            RarbgProvider(http, providerCache),
            PirateBayProvider(http),
            YtsProvider(http),
            EztvProvider(http),
            LibreProvider(),
        ) + providers
    } else {
        providers.toList()
    }

    /**
     * Search all enabled providers with the [TorrentQuery].
     */
    fun search(buildQuery: TorrentQuery.() -> Unit): SearchResult {
        check(!disposed.value) { "TorrentSearch instance is disposed and cannot be reused." }
        val query = TorrentQuery().apply(buildQuery)
        val selectedProviders = providers.filter { provider ->
            provider.isEnabled && (query.category == null
                    || query.category == Category.ALL
                    || provider.categories.containsKey(query.category))
        }
        return SearchResult(http, selectedProviders, providerCache, query)
    }

    /**
     * Returns a list of enabled providers.
     */
    fun enabledProviders(): List<TorrentProvider> {
        check(!disposed.value) { "TorrentSearch instance is disposed and cannot be reused." }
        return providers.filter(TorrentProvider::isEnabled).toList()
    }

    /**
     * Returns a list of available [TorrentProvider] instances.
     */
    fun providers(): List<TorrentProvider> {
        check(!disposed.value) { "TorrentSearch instance is disposed and cannot be reused." }
        return providers.toList()
    }

    /**
     * Enable the provider [name] with the included credentials and [cookies].
     */
    fun enableProvider(
        name: String,
        username: String? = null,
        password: String? = null,
        cookies: List<String> = emptyList()
    ) {
        check(!disposed.value) { "TorrentSearch instance is disposed and cannot be reused." }
        providers.singleOrNull { it.name == name }
            ?.enable(username, password, cookies)
    }

    /**
     * Disable the provider [name], future queries will not be handled by this provider.
     */
    fun disableProvider(name: String) {
        check(!disposed.value) { "TorrentSearch instance is disposed and cannot be reused." }
        providers.singleOrNull { it.name == name }?.disable()
    }

    /**
     * Release the [http] client and prevent future use of this instance.
     */
    fun dispose() {
        disposed.value = true
        http.close()
    }
}
