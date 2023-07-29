package torrentsearch

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.userAgent
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.json.Json
import torrentsearch.models.Category
import torrentsearch.models.SearchResult
import torrentsearch.models.TorrentQuery
import torrentsearch.providers.EztvProvider
import torrentsearch.providers.LibreProvider
import torrentsearch.providers.PirateBayProvider
import torrentsearch.providers.YtsProvider

internal const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:98.0) Gecko/20100101 Firefox/98.0"

/**
 *
 * @param httpClient The [HttpClient] to use for [TorrentProvider] search requests.
 * NOTE: If providing a [HttpClient] used elsewhere in your app,
 * do not call [dispose] as this will close your [HttpClient]
 * @param enableDefaultProviders When false, the built-in default providers will be disabled.
 * They can be enabled later with [enableProvider].
 * @param providers An optional list of custom [TorrentProvider] implementations.
 * @param providerCache An optional [TorrentProviderCache] for caching.
 */
public class TorrentSearch public constructor(
    httpClient: HttpClient = HttpClient(),
    enableDefaultProviders: Boolean = true,
    providers: List<TorrentProvider> = emptyList(),
    private val providerCache: TorrentProviderCache? = null,
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

    private val providers = providers + listOf(
        PirateBayProvider(http, enableDefaultProviders),
        YtsProvider(http, enableDefaultProviders),
        EztvProvider(http, enableDefaultProviders),
        LibreProvider(enabled = false),
    )

    /**
     * Search all enabled providers with the [TorrentQuery].
     */
    public fun search(buildQuery: TorrentQuery.() -> Unit): SearchResult {
        checkIsNotDisposed()
        val query = TorrentQuery().apply(buildQuery)
        val selectedProviders = providers.filter { provider ->
            provider.isEnabled && (
                query.category == null ||
                    query.category == Category.ALL ||
                    provider.categories.containsKey(query.category)
                )
        }
        return SearchResult(http, selectedProviders, providerCache, query)
    }

    /**
     * Returns a list of enabled providers.
     */
    public fun enabledProviders(): List<TorrentProvider> {
        checkIsNotDisposed()
        return providers.filter(TorrentProvider::isEnabled).toList()
    }

    /**
     * Returns a list of available [TorrentProvider] instances.
     */
    public fun providers(): List<TorrentProvider> {
        checkIsNotDisposed()
        return providers.toList()
    }

    /**
     * Enable the provider [name] with the included credentials and [cookies].
     */
    public fun enableProvider(
        name: String,
        username: String? = null,
        password: String? = null,
        cookies: List<String> = emptyList(),
    ) {
        checkIsNotDisposed()
        providers.singleOrNull { it.name == name }?.enable(username, password, cookies)
    }

    /**
     * Disable the provider [name], future queries will not be handled by this provider.
     */
    public fun disableProvider(name: String) {
        checkIsNotDisposed()
        providers.singleOrNull { it.name == name }?.disable()
    }

    /**
     * Release the [http] client and prevent future use of this instance.
     */
    public fun dispose() {
        disposed.value = true
        http.close()
    }

    private fun checkIsNotDisposed() {
        check(!disposed.value) { "TorrentSearch instance is disposed and cannot be reused." }
    }
}
