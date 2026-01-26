package torrentsearch.providers

import io.ktor.client.HttpClient
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.encodeURLParameter
import io.ktor.http.takeFrom
import io.ktor.utils.io.readAvailable
import ktsoup.KtSoupParser
import torrentsearch.models.Category
import torrentsearch.models.ProviderResult
import torrentsearch.models.SearchParam
import torrentsearch.models.TorrentDescription
import torrentsearch.models.TorrentQuery


internal class NzbGeekProvider(
    private val httpClient: HttpClient,
    enabled: Boolean = false,
) : BaseTorrentProvider(enabled) {
    override val name: String = "nzbgeek"
    override val baseUrl: String = "https://api.nzbgeek.info/api"
    override val tokenPath: String = ""
    override val searchPath: String = ""
    override val searchParams: Map<SearchParam, String> = mapOf(
        SearchParam.QUERY to "q",
        SearchParam.LIMIT to "limit",
        SearchParam.PAGE to "offset",
        SearchParam.TOKEN to "apikey",
        SearchParam.CATEGORY to "cat",
    )
    override val categories: Map<Category, String> = mapOf(
        Category.OTHER to "0,8000,8010,108000,108010",
        Category.MOVIES to "2000,2010,2020,2030,2040,2050,2060,102000,102010,102020,102030,102040,102050,102060",
        Category.AUDIO to "3000,3010,3020,3030,3040,103000,103010,103020,103030,103040",
        Category.APPS to "4000,4010,4020,4030,4040,4060,4070,104000,104010,104020,104030,104040,104060,104070",
        Category.GAMES to "1000,1010,1020,1030,1040,1050,1060,1070,1080,1090,1100,1110,4050,104050,101000,101010,101020,101030,101040,101050,101060,101070,101080,101090,101100,101110",
        Category.TV to "5000,5010,5020,5030,5040,5050,5060,5070,5080,105000,105010,105020,105030,105040,105060,105070,105080",
        Category.XXX to "6000,6010,6020,6030,6040,6050,6060,6070,106000,106010,106020,106030,106040,106050,106060,106070",
        Category.BOOKS to "7000,7010,7020,7030,107000,107010,107020,107030",
    )

    private var apiKey: String? = null

    override fun enable(apiKey: String?) {
        super.enable(apiKey)
        this.apiKey = apiKey
    }

    override suspend fun search(query: TorrentQuery): ProviderResult {
        val queryString = query.content?.encodeURLParameter()
        if (queryString.isNullOrBlank()) {
            return ProviderResult.Error.InvalidQueryError(
                providerName = name,
                message = "NzbGeek requires a query content string."
            )
        }
        val categoryString = categories[query.category]
        val response = try {
            httpClient.get {
                url {
                    takeFrom(baseUrl)
                    takeFrom(searchPath)
                    parameter("t", "search")
                    parameter("extended", "1")
                    parameter(searchParams.getValue(SearchParam.QUERY), queryString)
                    if (!categoryString.isNullOrBlank()) {
                        parameter(searchParams.getValue(SearchParam.CATEGORY), categoryString)
                    }
                    parameter(searchParams.getValue(SearchParam.TOKEN), apiKey)
                }
            }
        } catch (e: ResponseException) {
            return ProviderResult.Error.RequestError(
                providerName = name,
                httpStatusCode = e.response.status,
                body = e.response.bodyAsText()
            )
        }

        return if (response.status == HttpStatusCode.OK) {
            val channel = response.bodyAsChannel()
            val document = KtSoupParser.parseChunkedAsync(1024) {
                channel.readAvailable(it)
            }
            document.use { document ->
                val totalItems = document.querySelector("newznab\\:response")?.attr("total")
                val items = document.querySelectorAll("channel item")
                val itemDescriptions = items.map { element ->
                    val title = element.querySelector("title")?.textContent()
                    val guid = element.querySelector("newznab\\:attr[name=guid]")?.attr("value")
                    val size = element.querySelector("newznab\\:attr[name=size]")?.attr("value")
                    val getUrl = element.querySelector("enclosure[type=application/x-nzb]")?.attr("url")
                    val infoUrl = element.querySelector("comments")?.textContent()
                    TorrentDescription(
                        provider = name,
                        title = title ?: "<Unknown>",
                        size = size?.toLongOrNull() ?: 0L,
                        hash = guid,
                        seeds = 0,
                        peers = 0,
                        magnetUrl = getUrl,
                        infoUrl = infoUrl,
                    )
                }

                ProviderResult.Success(
                    providerName = name,
                    torrents = itemDescriptions,
                    page = query.page,
                    pageSize = query.limit,
                    totalTorrents = totalItems?.toIntOrNull() ?: itemDescriptions.size,
                )
            }
        } else {
            ProviderResult.Error.RequestError(
                providerName = name,
                httpStatusCode = response.status,
                body = response.bodyAsText()
            )
        }
    }
}
