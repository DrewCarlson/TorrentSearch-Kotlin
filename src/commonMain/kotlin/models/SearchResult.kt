package torrentsearch.models

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import torrentsearch.TorrentProvider
import torrentsearch.TorrentProviderCache

/**
 * A container for [ProviderResult]s across multiple [TorrentProvider]s.
 * [SearchResult] eagerly executes the [query] with each [TorrentProvider]
 * in [providers].
 *
 * Provider requests begin executing immediately and run until all are
 * completed or [SearchResult.cancel] is called. Alternatively when
 * collecting results with [torrents], [providerResults], or [errors]
 * `cancelOnComplete` can be set to true which will call [cancel] after
 * the downstream flow is completed.
 */
public class SearchResult internal constructor(
    private val parentScope: CoroutineScope,
    private val providers: List<TorrentProvider>,
    private val providerCache: TorrentProviderCache?,
    private val query: TorrentQuery,
    private val previousResults: List<ProviderResult>? = emptyList(),
) {
    private val scope = CoroutineScope(parentScope.coroutineContext + SupervisorJob())
    private val resultsFlow = providers
        .map(::createProviderQueryFlow)
        .merge()
        .flowOn(Dispatchers.Default)
        .shareIn(parentScope, SharingStarted.Eagerly, providers.size)

    /**
     * A flow of all [TorrentDescription]s from each [TorrentProvider]
     * selected to handle the [TorrentQuery].
     *
     * @param cancelOnComplete When true, cancel pending provider task
     * when the returned flow is completed.
     */
    public fun torrents(cancelOnComplete: Boolean = false): Flow<TorrentDescription> {
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
            .onCompletion { if (cancelOnComplete) scope.cancel() }
    }

    /**
     * A flow of raw [ProviderResult]s from each [TorrentProvider]
     * selected to handle the [TorrentQuery]
     *
     * @param cancelOnComplete When true, cancel pending provider task
     * when the returned flow is completed.
     */
    public fun providerResults(cancelOnComplete: Boolean = false): Flow<ProviderResult> {
        if (providers.isEmpty() && previousResults.isNullOrEmpty()) {
            return emptyFlow()
        }
        return resultsFlow.take(providers.size).onStart {
            previousResults?.forEach { result -> emit(result) }
        }.onCompletion { if (cancelOnComplete) scope.cancel() }
    }

    /**
     * A flow of [ProviderResult.Error]s for any failed requests made
     * to any of the selected [TorrentProvider]s.
     *
     * @param cancelOnComplete When true, cancel pending provider task
     * when the returned flow is completed.
     */
    public fun errors(cancelOnComplete: Boolean = false): Flow<ProviderResult.Error> {
        if (providers.isEmpty() && previousResults.isNullOrEmpty()) {
            return emptyFlow()
        }
        return providerResults(cancelOnComplete).filterIsInstance()
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
     * True after [cancel] is called, no further requests will be completed.
     */
    public fun isCancelled(): Boolean {
        return scope.isActive
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
            .any(ProviderResult.Success::hasMoreResults)
    }

    private fun hasNextResultSync(): Boolean? {
        return if (isCompleted()) {
            resultsFlow.replayCache
                .filterIsInstance<ProviderResult.Success>()
                .any(ProviderResult.Success::hasMoreResults)
        } else {
            null
        }
    }

    /**
     * Returns a new [SearchResult] that contains all torrents from the
     * current instance and will produce [ProviderResult]s for any providers
     * that have additional result pages.
     *
     * @return null if [hasNextResult] is false or the [SearchResult] container is cancelled.
     */
    public suspend fun nextResult(): SearchResult? {
        if (isCompleted()) return null
        val nextProviders = resultsFlow.take(providers.size).toList()
            .filterIsInstance<ProviderResult.Success>()
            .filter(ProviderResult.Success::hasMoreResults)
            .map(ProviderResult::providerName)

        if (nextProviders.isEmpty()) {
            return null
        }

        return SearchResult(
            parentScope = parentScope,
            query = query.copy(page = query.page + 1),
            providers = providers.filter { nextProviders.contains(it.name) },
            providerCache = providerCache,
            previousResults = previousResults.orEmpty() + resultsFlow.replayCache,
        )
    }

    /**
     * Cancel pending provider requests.
     */
    public fun cancel() {
        scope.cancel()
    }

    override fun toString(): String {
        return "SearchResult(" +
            "isCompleted=${isCompleted()}, " +
            "isCancelled=${isCancelled()}, " +
            "hasNextResult=${hasNextResultSync() ?: "(pending)"}, " +
            "providers=${providers.joinToString { it.name }}, " +
            "query=$query, " +
            "completed=${resultsFlow.replayCache.size})"
    }

    private fun createProviderQueryFlow(provider: TorrentProvider): Flow<ProviderResult> = flow {
        if (!query.skipCache) {
            providerCache?.loadResults(provider, query)?.let { cacheResult ->
                return@flow emit(ProviderResult.Success(provider.name, cacheResult, fromCache = true))
            }
        }
        val result = try {
            provider.search(query)
        } catch (e: Throwable) {
            ProviderResult.Error.UnknownError(provider.name, e.message, e)
        }
        emit(result)

        if (!query.skipCache && result is ProviderResult.Success && result.torrents.isNotEmpty()) {
            providerCache?.saveResults(provider, query, result.torrents)
        }
    }
}
