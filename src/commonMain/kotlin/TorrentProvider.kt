package torrentsearch

import torrentsearch.models.*

/**
 * [TorrentProvider]s define how to communicate with a single torrent provider.
 */
public interface TorrentProvider {

    /** The Provider's name. */
    public val name: String

    /** The Provider's base url. (ex. `https://provider.link`) */
    public val baseUrl: String

    /** The Provider's path to acquire a token. */
    public val tokenPath: String

    /** The Provider's path to search data. */
    public val searchPath: String

    /** The Provider's available query parameters and names. */
    public val searchParams: Map<SearchParam, String>

    /** Maps a url safe string of provider categories to a [Category]. */
    public val categories: Map<Category, String>

    /** The result limit for search requests. */
    public val resultsPerPage: Int get() = 100

    /** True if the provider is enabled. */
    public val isEnabled: Boolean

    /**
     * Execute a search for the given [query] in [category], returning
     * [TorrentDescription]s for each of the Provider's entries.
     */
    public suspend fun search(query: TorrentQuery): ProviderResult

    /**
     * Resolve missing details such as the torrent hash/magnetUrl for
     * [TorrentDescription]s which require an additional request to obtain.
     *
     * This is typically required for some HTML scraping based providers which
     * do not provide full details in the initial search results.
     *
     * @see TorrentDescription.isResolved to determine if a torrent needs additional resolution.
     */
    public suspend fun resolve(torrents: List<TorrentDescription>): ResolveResult {
        return ResolveResult.Success(name, emptyList())
    }

    /**
     * Enable this provider using the provided authentication details.
     */
    public fun enable(username: String? = null, password: String? = null, cookies: List<String> = emptyList())

    /**
     * Disable this provider, so it cannot be used until [enable] is called.
     */
    public fun disable()
}
