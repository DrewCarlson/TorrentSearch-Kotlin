package torrentsearch.providers

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import torrentsearch.Category
import torrentsearch.TorrentDescription

internal class YtsProvider(
    private val httpClient: HttpClient
) : BaseTorrentProvider() {

    override val name: String = "yts"
    override val baseUrl: String = "https://yts.mx/api/v2/"
    override val tokenPath: String = ""
    override val searchPath: String = "list_movies.json?query_term={query}&limit={limit}&sort_by=date_added"
    override val categories: Map<Category, String> = emptyMap()

    override suspend fun search(query: String, category: Category, limit: Int): List<TorrentDescription> {
        if (query.isBlank()) {
            return emptyList()
        }

        val response = httpClient.get {
            url {
                takeFrom(baseUrl)
                takeFrom(
                    searchPath
                        .replace("{query}", query.encodeURLParameter())
                        .replace("{limit}", limit.toString())
                )
            }
        }

        return if (response.status == HttpStatusCode.OK) {
            val ytsResponse = response.body<YtsResponse>()
            ytsResponse.data.movies.flatMap { movie ->
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
        @SerialName("movie_count")
        val movieCount: Int,
        val limit: Int,
        @SerialName("page_number")
        val page: Int,
        val movies: List<YtsMovie>,
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