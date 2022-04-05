package torrentsearch.providers

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.takeFrom
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import torrentsearch.models.Category
import torrentsearch.models.ProviderResult
import torrentsearch.models.SearchParam
import torrentsearch.models.TorrentDescription
import torrentsearch.models.TorrentQuery

internal class PirateBayProvider(
    private val httpClient: HttpClient
) : BaseTorrentProvider() {

    override val name: String = "ThePirateBay"
    override val baseUrl: String = "https://apibay.org"
    override val tokenPath: String = ""
    override val searchPath: String = "/q.php"
    override val categories = mapOf(
        Category.ALL to "",
        Category.AUDIO to "100",
        Category.MUSIC to "101",
        Category.VIDEO to "200",
        Category.MOVIES to "201",
        Category.TV to "205",
        Category.APPS to "300",
        Category.GAMES to "400",
        Category.XXX to "500",
        Category.OTHER to "600",
    )

    override val searchParams: Map<SearchParam, String> = mapOf(
        SearchParam.QUERY to "q",
        SearchParam.CATEGORY to "cat",
    )

    override suspend fun search(query: TorrentQuery): ProviderResult {
        val queryString = query.content?.filter { it.isLetter() || it.isWhitespace() }
        if (queryString.isNullOrBlank()) {
            return ProviderResult.Error.InvalidQueryError(name, "ThePirateBay requires a query content string.")
        }

        val categoryString = categories[query.category]
        val response = try {
            httpClient.get {
                url {
                    takeFrom(baseUrl)
                    takeFrom(searchPath)
                    parameter(searchParams.getValue(SearchParam.QUERY), queryString)
                    if (!categoryString.isNullOrBlank()) {
                        parameter(searchParams.getValue(SearchParam.CATEGORY), categoryString)
                    }
                }
            }
        } catch (e: ResponseException) {
            return ProviderResult.Error.RequestError(name, e.response.status, e.response.bodyAsText())
        }

        return if (response.status == HttpStatusCode.OK) {
            val torrents = response.body<List<TpbTorrent>>()
            val noResults = torrents.singleOrNull()?.isBlank() == true
            if (noResults) {
                ProviderResult.Success(name, emptyList())
            } else {
                val torrentDescriptions = torrents.map { torrent ->
                    TorrentDescription(
                        provider = name,
                        magnetUrl = formatMagnet(torrent.name, torrent.infoHash),
                        title = torrent.name,
                        size = torrent.size,
                        seeds = torrent.seeders,
                        peers = torrent.leechers,
                        imdbId = torrent.imdb,
                        infoUrl = "https://thepiratebay.org/description.php?id=${torrent.id}",
                    )
                }
                ProviderResult.Success(name, torrentDescriptions)
            }
        } else {
            ProviderResult.Error.RequestError(name, response.status, response.bodyAsText())
        }
    }

    @Serializable
    internal class TpbTorrent(
        val id: Long = -1,
        val name: String = "<unknown>",
        @SerialName("info_hash")
        val infoHash: String,
        val size: Long = -1,
        val seeders: Int = -1,
        val leechers: Int = -1,
        val imdb: String?,
    ) {
        fun isBlank(): Boolean {
            return infoHash.all { it == '0' }
        }
    }
}
