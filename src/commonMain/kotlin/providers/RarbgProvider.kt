package torrentsearch.providers

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.takeFrom
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import torrentsearch.TorrentProviderCache
import torrentsearch.models.Category
import torrentsearch.models.ProviderResult
import torrentsearch.models.SearchParam
import torrentsearch.models.TorrentDescription
import torrentsearch.models.TorrentQuery
import kotlin.time.Duration.Companion.seconds


internal class RarbgProvider(
    private val httpClient: HttpClient,
    private val providerCache: TorrentProviderCache?,
    prefetchToken: Boolean = true,
    enabled: Boolean = true,
) : BaseTorrentProvider(enabled) {

    internal companion object {
        val API_REQUEST_DELAY = 3.seconds
    }

    override val name = "Rarbg"
    override val baseUrl = "https://torrentapi.org"
    override val tokenPath = "/pubapi_v2.php?get_token=get_token&app_id=TorrentSearch"
    override val searchPath = "/pubapi_v2.php?app_id=TorrentSearch&mode=search&format=json_extended&sort=seeders"
    override val categories = mapOf(
        Category.ALL to "1;4;14;15;16;17;21;22;42;18;19;41;27;28;29;30;31;32;40;23;24;25;26;33;34;43;44;45;46;47;48;49;50;51;52",
        Category.MOVIES to "14;17;42;44;45;46;47;48;50;51;52",
        Category.XXX to "1;4",
        Category.GAMES to "1;27;28;29;30;31;32;40",
        Category.TV to "1;18;41;49",
        Category.MUSIC to "1;23;24;25;26",
        Category.APPS to "1;33;34;43",
        Category.BOOKS to "35"
    )

    override val searchParams = mapOf(
        SearchParam.QUERY to "search_string",
        SearchParam.CATEGORY to "category",
        SearchParam.LIMIT to "limit",
        SearchParam.TOKEN to "token",
        SearchParam.TMDB_ID to "search_themoviedb",
        SearchParam.TVDB_ID to "search_tvdb",
        SearchParam.IMDB_ID to "search_imdb",
    )

    private val mutex = Mutex()
    private var token: String? = null

    init {
        if (prefetchToken) {
            // Prefetch token
            launch { readToken() }
        }
    }

    override suspend fun search(query: TorrentQuery): ProviderResult {
        val response = try {
            fetchSearchResults(query)
        } catch (e: ResponseException) {
            return ProviderResult.Error.RequestError(name, e.response.status, e.response.bodyAsText())
        }

        return if (response.status == HttpStatusCode.OK) {
            val result = response.body<RarbgResult>()
            val torrentDescriptions = result.torrentResults.map { rarbgTorrent ->
                TorrentDescription(
                    provider = name,
                    magnetUrl = rarbgTorrent.download,
                    title = rarbgTorrent.title,
                    seeds = rarbgTorrent.seeders,
                    peers = rarbgTorrent.leechers,
                    size = rarbgTorrent.size,
                    themoviedbId = rarbgTorrent.episodeInfo?.themoviedb,
                    imdbId = rarbgTorrent.episodeInfo?.imdb,
                    tvdbId = rarbgTorrent.episodeInfo?.tvdb,
                    infoUrl = rarbgTorrent.infoPage,
                )
            }
            ProviderResult.Success(name, torrentDescriptions)
        } else {
            ProviderResult.Error.RequestError(name, response.status, response.bodyAsText())
        }
    }

    private suspend fun fetchSearchResults(query: TorrentQuery): HttpResponse {
        val queryString = query.content?.filter { it.isLetter() || it.isWhitespace() }
        val categoryString = categories[query.category]
        val tokenString = readToken() ?: ""
        val response = mutex.withLock {
            httpClient.get {
                url {
                    takeFrom(baseUrl)
                    takeFrom(searchPath)
                    parameter(searchParams.getValue(SearchParam.TOKEN), tokenString)
                    if (query.limit > -1) {
                        parameter(searchParams.getValue(SearchParam.LIMIT), query.limit)
                    }

                    when {
                        query.tmdbId != null -> parameter(searchParams.getValue(SearchParam.TMDB_ID), query.tmdbId)
                        query.imdbId != null -> parameter(searchParams.getValue(SearchParam.IMDB_ID), query.imdbId)
                        query.tvdbId != null -> parameter(searchParams.getValue(SearchParam.TVDB_ID), query.tvdbId)
                        queryString != null -> parameter(searchParams.getValue(SearchParam.QUERY), queryString)
                    }

                    if (categoryString != null) {
                        parameter(searchParams.getValue(SearchParam.CATEGORY), categoryString)
                    }
                }
            }
        }

        launch { mutex.withLock { delay(API_REQUEST_DELAY) } }
        return response
    }

    private suspend fun fetchToken(): String {
        return httpClient.get {
            url {
                takeFrom(baseUrl)
                takeFrom(tokenPath)
            }
        }.body<JsonObject>()["token"]!!
            .jsonPrimitive
            .content
    }

    internal suspend fun readToken(): String? {
        if (token == null) {
            token = mutex.withLock {
                token ?: providerCache?.loadToken(this) ?: try {
                    fetchToken().also { token ->
                        providerCache?.saveToken(this, token)
                        delay(API_REQUEST_DELAY)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }
        }

        return token
    }

    @Serializable
    internal class RarbgResult(
        @SerialName("torrent_results")
        val torrentResults: List<RarbgTorrent> = emptyList(),
        @SerialName("error_code")
        val errorCode: Int? = null
    )

    @Serializable
    internal class RarbgTorrent(
        val download: String,
        val title: String,
        val seeders: Int,
        val leechers: Int,
        val size: Long,
        @SerialName("info_page")
        val infoPage: String?,
        @SerialName("episode_info")
        val episodeInfo: RarbgEpisodeInfo? = null,
    )

    @Serializable
    internal class RarbgEpisodeInfo(
        val imdb: String? = null,
        val themoviedb: Int? = null,
        val tvdb: Int? = null,
    )
}
