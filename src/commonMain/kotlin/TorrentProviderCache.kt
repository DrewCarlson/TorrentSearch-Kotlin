package drewcarlson.torrentsearch

interface TorrentProviderCache {

    fun saveToken(provider: TorrentProvider, token: String)

    fun loadToken(provider: TorrentProvider): String?

    fun saveResults(
        provider: TorrentProvider,
        query: String,
        category: Category,
        results: List<TorrentDescription>
    )

    fun loadResults(
        provider: TorrentProvider,
        query: String,
        category: Category
    ): List<TorrentDescription>?
}
