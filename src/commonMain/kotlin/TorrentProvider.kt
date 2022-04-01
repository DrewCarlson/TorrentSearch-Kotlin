package drewcarlson.torrentsearch


interface TorrentProvider {

    /** The Provider's name. */
    val name: String

    /** The Provider's base url. (ex. `https://provider.link`) */
    val baseUrl: String

    /** The Provider's path to acquire a token. */
    val tokenPath: String

    /** The Provider's path to search data. */
    val searchPath: String

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
    suspend fun search(query: String, category: Category, limit: Int): List<TorrentDescription>

    fun enable(username: String? = null, password: String? = null, cookies: List<String> = emptyList())

    fun disable()
}
