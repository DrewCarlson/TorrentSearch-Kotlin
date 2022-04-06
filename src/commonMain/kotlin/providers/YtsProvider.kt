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

internal class YtsProvider(
    private val httpClient: HttpClient,
    enabled: Boolean = true,
) : BaseTorrentProvider(enabled) {

    override val name: String = "yts"
    override val baseUrl: String = "https://yts.mx/api/v2/"
    override val tokenPath: String = ""
    override val searchPath: String = "list_movies.json?sort_by=date_added"
    private val imdbIdPath: String = "movie_details.json?sort_by=date_added"
    override val categories: Map<Category, String> = mapOf(Category.MOVIES to "")
    override val searchParams: Map<SearchParam, String> = mapOf(
        SearchParam.QUERY to "query_term",
        SearchParam.LIMIT to "limit",
        SearchParam.IMDB_ID to "imdb_id",
    )

    override suspend fun search(query: TorrentQuery): ProviderResult {
        val queryString = query.content
        val imdbId = query.imdbId
        if (queryString.isNullOrBlank() && imdbId.isNullOrBlank()) {
            return ProviderResult.Error.InvalidQueryError(name, "Yts requires imdbId or a query content string.")
        }

        val response = try {
            httpClient.get {
                url {
                    takeFrom(baseUrl)
                    if (!queryString.isNullOrBlank()) {
                        takeFrom(searchPath)
                        parameter(searchParams.getValue(SearchParam.QUERY), queryString)
                    }
                    if (!imdbId.isNullOrBlank()) {
                        takeFrom(imdbIdPath)
                        parameter(searchParams.getValue(SearchParam.IMDB_ID), imdbId)
                    }
                }
            }
        } catch (e: ResponseException) {
            return ProviderResult.Error.RequestError(name, e.response.status, e.response.bodyAsText())
        }

        return if (response.status == HttpStatusCode.OK) {
            val ytsResponse = response.body<YtsResponse>()
            val movies = if (ytsResponse.data.movie == null) {
                ytsResponse.data.movies
            } else {
                listOf(ytsResponse.data.movie)
            }
            val torrentDescriptions = movies.flatMap { movie ->
                movie.torrents.map { torrent ->
                    TorrentDescription(
                        provider = name,
                        magnetUrl = formatMagnet(torrent.hash, movie.title),
                        hash = torrent.hash,
                        seeds = torrent.seeds,
                        peers = torrent.peers,
                        title = "${movie.title} ${torrent.quality} ${torrent.type}",
                        size = torrent.size,
                        infoUrl = torrent.url,
                        imdbId = movie.imdbId,
                    )
                }
            }
            ProviderResult.Success(name, torrentDescriptions)
        } else {
            ProviderResult.Error.RequestError(name, response.status, response.bodyAsText())
        }
    }

    @Serializable
    internal data class YtsResponse(val data: YtsData)

    @Serializable
    internal data class YtsData(
        val limit: Int = 1,
        @SerialName("page_number")
        val page: Int = 1,
        val movies: List<YtsMovie> = emptyList(),
        val movie: YtsMovie? = null,
        @SerialName("movie_count")
        val movieCount: Int = if (movie != null) 1 else 0,
    )

    @Serializable
    internal data class YtsMovie(
        val id: Int,
        @SerialName("title_long")
        val title: String,
        @SerialName("imdb_code")
        val imdbId: String?,
        val torrents: List<YtsTorrent> = emptyList(),
    )

    @Serializable
    internal data class YtsTorrent(
        val url: String,
        val hash: String,
        val seeds: Int,
        val peers: Int,
        val quality: String,
        val type: String,
        @SerialName("size_bytes")
        val size: Long,
    )
}