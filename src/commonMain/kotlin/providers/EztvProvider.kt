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

internal class EztvProvider(
    private val httpClient: HttpClient,
    enabled: Boolean = true,
) : BaseTorrentProvider(enabled) {

    override val name: String = "eztv"
    override val baseUrl: String = "https://eztvx.to/api/"
    override val categories: Map<Category, String> = mapOf(Category.TV to "")
    override val tokenPath: String = ""
    override val searchParams: Map<SearchParam, String> = mapOf(
        SearchParam.IMDB_ID to "imdb_id",
        SearchParam.PAGE to "page",
        SearchParam.LIMIT to "limit",
    )
    override val searchPath: String = "get-torrents"

    override suspend fun search(query: TorrentQuery): ProviderResult {
        val imdbId = query.imdbId?.dropWhile { it == 't' }
        if (imdbId.isNullOrBlank()) {
            return ProviderResult.Error.InvalidQueryError(name, "Eztv requires imdbId")
        }

        val response = try {
            httpClient.get {
                url {
                    takeFrom(baseUrl)
                    takeFrom(searchPath)
                    parameter(searchParams.getValue(SearchParam.PAGE), query.page)
                    parameter(searchParams.getValue(SearchParam.IMDB_ID), imdbId)
                    if (query.limit > -1) {
                        parameter(searchParams.getValue(SearchParam.LIMIT), query.limit)
                    }
                }
            }
        } catch (e: ResponseException) {
            return ProviderResult.Error.RequestError(name, e.response.status, e.response.bodyAsText())
        }

        return if (response.status == HttpStatusCode.OK) {
            val body = response.body<EztvResponse>()
            val torrentDescriptions = body.torrents.map { eztvTorrent ->
                TorrentDescription(
                    provider = name,
                    magnetUrl = eztvTorrent.magnetUrl,
                    title = eztvTorrent.title,
                    size = eztvTorrent.sizeBytes,
                    seeds = eztvTorrent.seeds,
                    peers = eztvTorrent.peers,
                    imdbId = "tt${eztvTorrent.imdbId}",
                    infoUrl = eztvTorrent.episodeUrl,
                    hash = hashFromMagnetUrl(eztvTorrent.magnetUrl),
                )
            }
            ProviderResult.Success(
                name,
                torrentDescriptions,
                totalTorrents = body.torrentsCount,
                pageSize = body.limit,
                page = body.page,
            )
        } else {
            ProviderResult.Error.RequestError(name, response.status, response.bodyAsText())
        }
    }

    @Serializable
    internal class EztvResponse(
        val torrents: List<EztvTorrent> = emptyList(),
        val page: Int = 1,
        @SerialName("torrents_count")
        val torrentsCount: Int = 0,
        val limit: Int = -1,
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
