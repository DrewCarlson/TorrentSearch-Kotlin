package torrentsearch

import torrentsearch.models.Category
import torrentsearch.models.ProviderResult
import torrentsearch.models.SearchParam
import torrentsearch.models.TorrentDescription
import torrentsearch.models.TorrentQuery

interface TorrentProvider {

    /** The Provider's name. */
    val name: String

    /** The Provider's base url. (ex. `https://provider.link`) */
    val baseUrl: String

    /** The Provider's path to acquire a token. */
    val tokenPath: String

    /** The Provider's path to search data. */
    val searchPath: String

    /** The Provider's available query parameters and names. */
    val searchParams: Map<SearchParam, String>

    /** Maps a url safe string of provider categories to a [Category]. */
    val categories: Map<Category, String>

    /** The result limit for search requests. */
    val resultsPerPage: Int get() = 100

    /** True if the provider is enabled. */
    val isEnabled: Boolean

    /**
     * Execute a search for the given [query] in [category], returning
     * [TorrentDescription]s for each of the Provider's entries.
     */
    suspend fun search(query: TorrentQuery): ProviderResult

    fun enable(username: String? = null, password: String? = null, cookies: List<String> = emptyList())

    fun disable()
}
