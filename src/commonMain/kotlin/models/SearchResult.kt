package torrentsearch.models

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import torrentsearch.TorrentProvider
import torrentsearch.TorrentProviderCache

/**
 * A container for [ProviderResult]s across multiple [TorrentProvider]s.
 * [SearchResult] eagerly executes the [query] with each [TorrentProvider]
 * in [providers].
 */
public class SearchResult internal constructor(
    private val scope: CoroutineScope,
    private val providers: List<TorrentProvider>,
    private val providerCache: TorrentProviderCache?,
    private val query: TorrentQuery,
    private val previousResults: List<ProviderResult>? = emptyList(),
) {
    private val resultsFlow = providers
        .map { provider ->
            flow {
                if (!query.skipCache) {
                    providerCache?.loadResults(provider, query)?.let { cacheResult ->
                        return@let emit(ProviderResult.Success(provider.name, cacheResult, fromCache = true))
                    }
                }
                val result = try {
                    provider.search(query)
                } catch (e: Throwable) {
                    ProviderResult.Error.UnknownError(provider.name, e.message)
                }
                emit(result)

                if (!query.skipCache && result is ProviderResult.Success && result.torrents.isNotEmpty()) {
                    providerCache?.saveResults(provider, query, result.torrents)
                }
            }
        }
        .merge()
        .flowOn(Dispatchers.Default)
        .shareIn(scope, SharingStarted.Eagerly, providers.size)

    /**
     * A flow of all [TorrentDescription]s from each [TorrentProvider]
     * selected to handle the [TorrentQuery].
     */
    @OptIn(FlowPreview::class)
    public fun torrents(): Flow<TorrentDescription> {
        if (providers.isEmpty() && previousResults.isNullOrEmpty()) {
            return emptyFlow()
        }
        return resultsFlow.take(providers.size)
            .filterIsInstance<ProviderResult.Success>()
            .map { result -> result.torrents }
            .run {
                if (previousResults == null) {
                    this
                } else {
                    onStart {
                        previousResults
                            .filterIsInstance<ProviderResult.Success>()
                            .forEach { result -> emit(result.torrents) }
                    }
                }
            }
            .flatMapMerge { it.asFlow() }
    }

    /**
     * A flow of raw [ProviderResult]s from each [TorrentProvider]
     * selected to handle the [TorrentQuery].
     */
    public fun providerResults(): Flow<ProviderResult> {
        if (providers.isEmpty() && previousResults.isNullOrEmpty()) {
            return emptyFlow()
        }
        return resultsFlow.take(providers.size).onStart {
            previousResults?.forEach { result -> emit(result) }
        }
    }

    /**
     * A flow of [ProviderResult.Error]s for any failed requests made
     * to any of the selected [TorrentProvider]s.
     */
    public fun errors(): Flow<ProviderResult.Error> {
        if (providers.isEmpty() && previousResults.isNullOrEmpty()) {
            return emptyFlow()
        }
        return providerResults().filterIsInstance()
    }

    /**
     * A list of the completed [ProviderResult]s at the current moment,
     * size may be less than [providerCount].
     */
    public fun currentProviderResults(): List<ProviderResult> {
        return previousResults.orEmpty() + resultsFlow.replayCache.toList()
    }

    /**
     * True when all providers have produced a [ProviderResult].
     */
    public fun isCompleted(): Boolean {
        return resultsFlow.replayCache.size == providers.size
    }

    /**
     * The number of [TorrentProvider]s that were selected to handle
     * the [TorrentQuery].
     */
    public fun providerCount(): Int {
        return providers.size
    }

    /**
     * The number of [ProviderResult]s that may be contained in this
     * [SearchResult].
     */
    public fun providerResultCount(): Int {
        return providers.size + previousResults.orEmpty().size
    }

    /**
     * Returns true when one or more providers has results that can be
     * retrieved with additional requests.
     */
    public suspend fun hasNextResult(): Boolean {
        return resultsFlow.take(providers.size).toList()
            .filterIsInstance<ProviderResult.Success>()
            .onEach {
                println("${it.providerName}: hasMoreResults=${it.hasMoreResults}, ${it}")
            }
            .any(ProviderResult.Success::hasMoreResults)
    }

    /**
     * Returns a new [SearchResult] that contains all torrents from the
     * current instance and will produce [ProviderResult]s for any providers
     * that have additional result pages.
     *
     * @return null if [hasNextResult] is false or the next [SearchResult] container.
     */
    public suspend fun nextResult(): SearchResult? {
        val nextProviders = resultsFlow.take(providers.size).toList()
            .filterIsInstance<ProviderResult.Success>()
            .filter(ProviderResult.Success::hasMoreResults)
            .map(ProviderResult::providerName)

        if (nextProviders.isEmpty()) {
            return null
        }

        return SearchResult(
            scope = scope,
            query = query.copy(page = query.page + 1),
            providers = providers.filter { nextProviders.contains(it.name) },
            providerCache = providerCache,
            previousResults = previousResults.orEmpty() + resultsFlow.replayCache,
        )
    }

    override fun toString(): String {
        return "SearchResult(" +
                "providers=${providers.joinToString { it.name }}, " +
                "query=$query, " +
                "completed=${resultsFlow.replayCache.size})"
    }
}
