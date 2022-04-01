package torrentsearch.providers

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import torrentsearch.Category
import torrentsearch.SearchParam
import torrentsearch.TorrentDescription
import torrentsearch.TorrentQuery

internal class YtsProvider(
    private val httpClient: HttpClient
) : BaseTorrentProvider() {

    override val name: String = "yts"
    override val baseUrl: String = "https://yts.mx/api/v2/"
    override val tokenPath: String = ""
    override val searchPath: String = "list_movies.json?sort_by=date_added"
    private val imdbIdPath: String = "movie_details.json?sort_by=date_added"
    override val categories: Map<Category, String> = emptyMap()
    override val searchParams: Map<SearchParam, String> = mapOf(
        SearchParam.QUERY to "query_term",
        SearchParam.LIMIT to "limit",
        SearchParam.IMDB_ID to "imdb_id",
    )

    override suspend fun search(query: TorrentQuery): List<TorrentDescription> {
        val queryString = query.content?.encodeURLParameter()
        val imdbId = query.imdbId
        if (queryString.isNullOrBlank() && imdbId.isNullOrBlank()) {
            return emptyList()
        }

        val response = httpClient.get {
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

        return if (response.status == HttpStatusCode.OK) {
            val ytsResponse = response.body<YtsResponse>()
            val movies = if (ytsResponse.data.movie == null) {
                ytsResponse.data.movies
            } else {
                listOf(ytsResponse.data.movie)
            }
            movies.flatMap { movie ->
                movie.torrents.map { torrent ->
                    TorrentDescription(
                        provider = name,
                        magnetUrl = formatMagnet(torrent.hash, movie.title),
                        seeds = torrent.seeds,
                        peers = torrent.peers,
                        title = "$name ${torrent.quality} ${torrent.type}",
                        size = torrent.size,
                        infoUrl = torrent.url,
                        imdbId = movie.imdbId,
                    )
                }
            }
        } else {
            emptyList()
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
        val torrents: List<YtsTorrent>,
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