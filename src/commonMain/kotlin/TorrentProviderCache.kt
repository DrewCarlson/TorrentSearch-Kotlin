package torrentsearch

import torrentsearch.models.TorrentDescription
import torrentsearch.models.TorrentQuery

/**
 * A [TorrentProviderCache] enables saving and restoring authentication tokens and search results.
 */
public interface TorrentProviderCache {

    /**
     * Save the [token] for [provider].
     */
    public fun saveToken(provider: TorrentProvider, token: String)

    /**
     * Load the cached token or null for the [provider].
     */
    public fun loadToken(provider: TorrentProvider): String?

    /**
     * Save the [results] from the [provider] and [query].
     */
    public fun saveResults(
        provider: TorrentProvider,
        query: TorrentQuery,
        results: List<TorrentDescription>,
    )

    /**
     * Load the cached results with the [provider] and [query].
     */
    public fun loadResults(
        provider: TorrentProvider,
        query: TorrentQuery,
    ): List<TorrentDescription>?
}
