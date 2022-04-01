package torrentsearch.providers

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.*
import torrentsearch.*
import kotlin.time.Duration.Companion.seconds


internal class RarbgProvider(
    private val httpClient: HttpClient,
    private val providerCache: TorrentProviderCache?,
    prefetchToken: Boolean = true,
) : BaseTorrentProvider() {

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

    override suspend fun search(query: TorrentQuery): List<TorrentDescription> {
        val result = fetchSearchResults(query)

        launch { mutex.withLock { delay(API_REQUEST_DELAY) } }

        val errorCode = result["error_code"]?.jsonPrimitive?.intOrNull
        return if (errorCode == null) {
            result["torrent_results"]!!
                .jsonArray
                .map { it.asTorrentDescription() }
        } else {
            // TODO: Handle error codes
            //   - 20: No results
            emptyList()
        }
    }

    private suspend fun fetchSearchResults(query: TorrentQuery): JsonObject {
        val encodedQuery = query.content?.encodeURLQueryComponent()
        val categoryString = categories[query.category]
        val tokenString = readToken() ?: ""
        return mutex.withLock {
            httpClient.get {
                url {
                    takeFrom(baseUrl)
                    takeFrom(searchPath)
                    parameter(searchParams.getValue(SearchParam.TOKEN), tokenString)
                    parameter(searchParams.getValue(SearchParam.LIMIT), query.limit)

                    when {
                        encodedQuery != null -> parameter(searchParams.getValue(SearchParam.QUERY), encodedQuery)
                        query.tmdbId != null -> parameter(searchParams.getValue(SearchParam.TMDB_ID), query.tmdbId)
                        query.imdbId != null -> parameter(searchParams.getValue(SearchParam.IMDB_ID), query.imdbId)
                        query.tvdbId != null -> parameter(searchParams.getValue(SearchParam.TVDB_ID), query.tvdbId)
                    }

                    if (categoryString != null) {
                        parameter(searchParams.getValue(SearchParam.CATEGORY), categoryString)
                    }
                }
            }.body()
        }
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

    private fun JsonElement.asTorrentDescription(): TorrentDescription {
        val episodeInfo = jsonObject["episode_info"].let { element ->
            if (element !is JsonNull) {
                jsonObject
            } else {
                JsonObject(emptyMap())
            }
        }
        return TorrentDescription(
            provider = name,
            magnetUrl = jsonObject["download"]!!.jsonPrimitive.content,
            title = jsonObject["title"]!!.jsonPrimitive.content,
            seeds = jsonObject["seeders"]!!.jsonPrimitive.int,
            peers = jsonObject["leechers"]!!.jsonPrimitive.int,
            size = jsonObject["size"]!!.jsonPrimitive.long,
            themoviedbId = episodeInfo["themoviedb"]?.jsonPrimitive?.intOrNull,
            imdbId = episodeInfo["imdb"]?.jsonPrimitive?.contentOrNull,
            tvdbId = episodeInfo["tvdb"]?.jsonPrimitive?.intOrNull,
            infoUrl = jsonObject["info_page"]!!.jsonPrimitive.contentOrNull,
        )
    }
}
