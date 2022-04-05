package torrentsearch.providers

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.HttpStatusCode
import io.ktor.http.takeFrom
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import torrentsearch.Category
import torrentsearch.SearchParam
import torrentsearch.TorrentDescription
import torrentsearch.TorrentQuery

internal class EztvProvider(
    private val httpClient: HttpClient
) : BaseTorrentProvider() {

    override val name: String = "eztv"
    override val baseUrl: String = "https://eztv.re/api/"
    override val categories: Map<Category, String> = mapOf()
    override val tokenPath: String = ""
    override val searchParams: Map<SearchParam, String> = mapOf(
        SearchParam.IMDB_ID to "imdb_id",
        SearchParam.PAGE to "page",
        SearchParam.LIMIT to "limit",
    )
    override val searchPath: String = "get-torrents"

    override suspend fun search(query: TorrentQuery): List<TorrentDescription> {
        val imdbId = query.imdbId?.dropWhile { it == 't' }
        if (imdbId.isNullOrBlank()) {
            return emptyList()
        }

        val response = httpClient.get {
            url {
                takeFrom(baseUrl)
                takeFrom(searchPath)
                parameter(searchParams.getValue(SearchParam.IMDB_ID), imdbId)
                if (query.limit > -1) {
                    parameter(searchParams.getValue(SearchParam.LIMIT), query.limit)
                }
            }
        }

        return if (response.status == HttpStatusCode.OK) {
            val body = response.body<EztvResponse>()
            body.torrents.map { eztvTorrent ->
                TorrentDescription(
                    provider = name,
                    magnetUrl = eztvTorrent.magnetUrl,
                    title = eztvTorrent.title,
                    size = eztvTorrent.sizeBytes,
                    seeds = eztvTorrent.seeds,
                    peers = eztvTorrent.peers,
                    imdbId = "tt${eztvTorrent.imdbId}",
                    infoUrl = eztvTorrent.episodeUrl
                )
            }
        } else {
            emptyList()
        }
    }

    @Serializable
    internal class EztvResponse(
        val torrents: List<EztvTorrent> = emptyList(),
    )

    @Serializable
    internal class EztvTorrent(
        @SerialName("magnet_url")
        val magnetUrl: String,
        val seeds: Int,
        val peers: Int,
        @SerialName("size_bytes")
        val sizeBytes: Long,
        val title: String,
        @SerialName("imdb_id")
        val imdbId: String,
        @SerialName("episode_url")
        val episodeUrl: String,
    )
}