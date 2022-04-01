package drewcarlson.torrentsearch

import drewcarlson.torrentsearch.providers.LibreProvider
import drewcarlson.torrentsearch.providers.PirateBayProvider
import drewcarlson.torrentsearch.providers.RarbgProvider
import drewcarlson.torrentsearch.providers.YtsProvider
import io.ktor.client.HttpClient
import io.ktor.client.plugins.*
import io.ktor.client.plugins.cookies.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.reduce
import kotlinx.coroutines.flow.take
import kotlinx.serialization.json.Json

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
    suspend fun search(query: String, category: Category, limit: Int): List<TorrentDescription> {
        return searchFlow(query, category, limit).reduce { acc, next -> acc + next }
    }

    /**
     * Search all enabled providers with [query] and [category],
     * emitting each set of results as the providers respond.
     *
     * [limit] is used when possible to limit the result count
     * from each provider.
     */
    fun searchFlow(query: String, category: Category, limit: Int): Flow<List<TorrentDescription>> {
        return providers
            .filter(TorrentProvider::isEnabled)
            .map { provider ->
                flow {
                    try {
                        emit(provider.search(query, category, limit))
                    } catch (e: ResponseException) {
                        //e.printStackTrace()
                    }
                }.onEach { results ->
                    if (results.isNotEmpty()) {
                        providerCache?.saveResults(provider, query, category, results)
                    }
                }.onStart {
                    val cacheResult = providerCache?.loadResults(provider, query, category)
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
