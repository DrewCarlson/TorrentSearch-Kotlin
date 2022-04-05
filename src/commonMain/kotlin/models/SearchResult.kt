package torrentsearch.models

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.take
import torrentsearch.TorrentProvider
import torrentsearch.TorrentProviderCache

/**
 * A container for [ProviderResult]s across multiple [TorrentProvider]s.
 * [SearchResult] eagerly executes the [query] with each [TorrentProvider]
 * in [providers].
 */
class SearchResult internal constructor(
    scope: CoroutineScope,
    private val providers: List<TorrentProvider>,
    private val providerCache: TorrentProviderCache?,
    private val query: TorrentQuery,
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
    fun torrents(): Flow<TorrentDescription> {
        return resultsFlow.take(providers.size)
            .mapNotNull { result -> (result as? ProviderResult.Success)?.torrents }
            .flatMapMerge { it.asFlow() }
    }

    /**
     * A flow of raw [ProviderResult]s from each [TorrentProvider]
     * selected to handle the [TorrentQuery].
     */
    fun providerResults(): Flow<ProviderResult> {
        return resultsFlow.take(providers.size)
    }

    /**
     * A flow of [ProviderResult.Error]s for any failed requests made
     * to any of the selected [TorrentProvider]s.
     */
    fun errors(): Flow<ProviderResult.Error> {
        return resultsFlow.take(providers.size)
            .mapNotNull { result -> (result as? ProviderResult.Error) }
    }

    /**
     * The number of [TorrentProvider]s that were selected to handle
     * the [TorrentQuery].
     */
    fun providerCount(): Int {
        return providers.size
    }

    override fun toString(): String {
        return "SearchResult(" +
                "providers=${providers.joinToString { it.name }}, " +
                "query=$query, " +
                "completed=${resultsFlow.replayCache.size})"
    }
}
