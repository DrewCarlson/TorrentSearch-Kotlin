package torrentsearch

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.cookies.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.Json
import torrentsearch.providers.LibreProvider
import torrentsearch.providers.PirateBayProvider
import torrentsearch.providers.RarbgProvider
import torrentsearch.providers.YtsProvider

internal const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:98.0) Gecko/20100101 Firefox/98.0"

class TorrentSearch(
    private val providerCache: TorrentProviderCache? = null,
    httpClient: HttpClient = HttpClient(),
    installDefaultProviders: Boolean = true,
    vararg providers: TorrentProvider
) {

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
            LibreProvider(),
        ) + providers
    } else {
        providers.toList()
    }

    /**
     * Search all enabled providers with [query] and [category].
     *
     * All results are merged into a single list. [limit] is used
     * when possible to limit the result count from each provider.
     */
    suspend fun search(buildQuery: TorrentQuery.() -> Unit): List<TorrentDescription> {
        return searchFlow(buildQuery).reduce { acc, next -> acc + next }
    }

    /**
     * Search all enabled providers with [query] and [category],
     * emitting each set of results as the providers respond.
     *
     * [limit] is used when possible to limit the result count
     * from each provider.
     */
    fun searchFlow(buildQuery: TorrentQuery.() -> Unit): Flow<List<TorrentDescription>> {
        val query = TorrentQuery().apply(buildQuery)
        return providers
            .filter(TorrentProvider::isEnabled)
            .map { provider ->
                flow {
                    try {
                        emit(provider.search(query))
                    } catch (e: ResponseException) {
                        //e.printStackTrace()
                    }
                }.onEach { results ->
                    if (results.isNotEmpty()) {
                        providerCache?.saveResults(provider, query, results)
                    }
                }.onStart {
                    val cacheResult = providerCache?.loadResults(provider, query)
                    if (cacheResult != null) {
                        emit(cacheResult)
                    }
                }.take(1)
            }
            .merge()
            .flowOn(Dispatchers.Default)
    }

    /**
     * Returns a list of enabled providers.
     */
    fun enabledProviders() = providers.filter(TorrentProvider::isEnabled).toList()

    /**
     * Returns a list of available providers.
     */
    fun availableProviders() = providers.toList()

    /**
     * Enable the provider [name] with the included credentials and [cookies].
     */
    fun enableProvider(name: String, username: String?, password: String?, cookies: List<String>) {
        providers.singleOrNull { it.name == name }
            ?.enable(username, password, cookies)
    }

    /**
     * Disable the provider [name].
     */
    fun disableProvider(name: String) {
        providers.singleOrNull { it.name == name }?.disable()
    }
}
