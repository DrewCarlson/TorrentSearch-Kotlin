package torrentsearch

interface TorrentProviderCache {

    fun saveToken(provider: TorrentProvider, token: String)

    fun loadToken(provider: TorrentProvider): String?

    fun saveResults(
        provider: TorrentProvider,
        query: TorrentQuery,
        results: List<TorrentDescription>
    )

    fun loadResults(
        provider: TorrentProvider,
        query: TorrentQuery,
    ): List<TorrentDescription>?
}
