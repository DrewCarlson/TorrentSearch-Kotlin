package torrentsearch.providers

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.json.*
import torrentsearch.Category
import torrentsearch.TorrentDescription

internal class PirateBayProvider(
    private val httpClient: HttpClient
) : BaseTorrentProvider() {

    override val name: String = "ThePirateBay"
    override val baseUrl: String = "https://apibay.org"
    override val tokenPath: String = ""
    override val searchPath: String = "/q.php?q={query}&cat={category}"

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

    override suspend fun search(query: String, category: Category, limit: Int): List<TorrentDescription> {
        val categoryString = categories[category]

        if (query.isBlank() || categoryString.isNullOrBlank()) {
            return emptyList()
        }
        val response = httpClient.get {
            url {
                takeFrom(baseUrl)
                takeFrom(
                    searchPath
                        .replace("{query}", query.encodeURLQueryComponent())
                        .replace("{category}", categoryString)
                )
            }
        }

        return if (response.status == HttpStatusCode.OK) {
            val torrents = response.body<JsonArray>()
            val noResults = torrents.singleOrNull()
                ?.jsonObject
                ?.get("info_hash")
                ?.jsonPrimitive
                ?.content
                ?.all { it == '0' } ?: false
            if (noResults) {
                emptyList()
            } else {
                torrents.map { element ->
                    val id = element.jsonObject["id"]?.jsonPrimitive?.longOrNull
                    val torrentName = element.jsonObject["name"]?.jsonPrimitive?.content ?: "<unknown>"
                    TorrentDescription(
                        provider = name,
                        magnetUrl = formatMagnet(
                            name = torrentName,
                            infoHash = checkNotNull(element.jsonObject["info_hash"]).jsonPrimitive.content
                        ),
                        title = torrentName,
                        size = element.jsonObject["size"]?.jsonPrimitive?.long ?: -1,
                        seeds = element.jsonObject["seeders"]?.jsonPrimitive?.int ?: -1,
                        peers = element.jsonObject["leechers"]?.jsonPrimitive?.int ?: -1,
                        imdbId = element.jsonObject["imdb"]?.jsonPrimitive?.contentOrNull,
                        infoUrl = "https://thepiratebay.org/description.php?id=$id",
                    )
                }
            }
        } else {
            emptyList()
        }
    }
}
