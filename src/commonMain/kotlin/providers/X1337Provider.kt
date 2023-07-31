package torrentsearch.providers

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import ktsoup.KtSoupElement
import ktsoup.KtSoupParser
import torrentsearch.models.*

internal class X1337Provider(
    private val httpClient: HttpClient,
    enabled: Boolean = true,
) : BaseTorrentProvider(enabled) {
    override val name: String = "1337x"

    override val baseUrl: String = "https://1337x.to/"
    override val tokenPath: String = ""
    override val searchPath: String = "search"
    private val categorySearchPath = "category-search"

    override val categories: Map<Category, String> = mapOf(
        Category.ALL to "",
        Category.TV to "TV",
        Category.MOVIES to "Movies",
        Category.GAMES to "Games",
        Category.MUSIC to "Music",
        Category.APPS to "Apps",
        Category.XXX to "XXX",
    )

    override val searchParams: Map<SearchParam, String> = emptyMap()

    override suspend fun search(query: TorrentQuery): ProviderResult {
        val queryCategory = query.category
        val queryContent = query.content
        if (queryContent.isNullOrBlank()) {
            return ProviderResult.Error.InvalidQueryError(name, "1337x requires a query string")
        }
        val isCategorySearch = queryCategory != null && queryCategory != Category.ALL
        val page = query.page.toString()
        val response = try {
            httpClient.get { url { buildQueryUrl(isCategorySearch, queryContent, queryCategory, page) } }
        } catch (e: ResponseException) {
            return ProviderResult.Error.RequestError(name, e.response.status, e.response.bodyAsText())
        }
        return try {
            parseResultsList(response.bodyAsText())
        } catch (e: Throwable) {
            ProviderResult.Error.UnknownError(
                providerName = name,
                message = "Failed to parse response: ${response.call.request.url}",
                exception = e,
            )
        }
    }

    override suspend fun resolve(torrents: List<TorrentDescription>): ResolveResult {
        val resolved = torrents.mapNotNull { description ->
            val infoUrl = requireNotNull(description.infoUrl) {
                "TorrentDescription is missing an infoUrl: $description"
            }
            val response = try {
                httpClient.get { url(urlString = infoUrl) }
            } catch (e: ResponseException) {
                return@mapNotNull null
            }

            KtSoupParser.parse(response.bodyAsText()).use { document ->
                val magnetUrl = document.querySelector("a[href*=\"magnet:\"]")?.attr("href")
                val infoHash = document.querySelector(".infohash-box p span")?.textContent()
                description.copy(
                    hash = infoHash,
                    magnetUrl = magnetUrl,
                )
            }
        }

        return ResolveResult.Success(name, resolved)
    }

    private fun parseResultsList(html: String): ProviderResult {
        return KtSoupParser.parse(html).use { document ->
            val absoluteUrlBase = baseUrl.trimEnd('/')
            val rows = document.querySelectorAll("table.table-list tbody tr")
            val torrents = rows.mapNotNull { extractRowDetails(it, absoluteUrlBase) }
            val pagination = document.querySelector(".box-info-detail .pagination")
            val currentPage = pagination?.querySelector("ul li.active")?.textContent()?.toIntOrNull() ?: 1
            val pageCount = pagination?.querySelector("ul li.last a")?.attr("href")
                ?.trim('/')
                ?.split('/')
                ?.lastOrNull()
                ?.toIntOrNull() ?: 1

            ProviderResult.Success(
                providerName = name,
                torrents = torrents,
                page = currentPage,
                totalTorrents = torrents.size * pageCount,
                requiresResolution = true,
            )
        }
    }

    private fun extractRowDetails(
        row: KtSoupElement,
        absoluteUrlBase: String,
    ): TorrentDescription? {
        val nameTd = row.querySelector("td.name") ?: return null
        val nameLink = nameTd.querySelector("a[href*=\"/torrent\"]") ?: return null
        val seeds = row.querySelector("td.seeds")?.textContent()?.toIntOrNull() ?: return null
        val peers = row.querySelector("td.leeches")?.textContent()?.toIntOrNull() ?: 0
        // val dateTd = row.querySelector("td.date") ?: return null
        val size = row.querySelector("td.size")
            ?.textContent()
            ?.removeSuffix(seeds.toString())
            ?.run(::parseFileSizeToBytes)
            ?: 0L

        return TorrentDescription(
            provider = name,
            magnetUrl = null,
            title = nameLink.textContent(),
            size = size,
            seeds = seeds,
            peers = peers,
            hash = null,
            infoUrl = nameLink.attr("href")?.let { "${absoluteUrlBase}$it" },
        )
    }

    private fun URLBuilder.buildQueryUrl(
        isCategorySearch: Boolean,
        queryContent: String,
        queryCategory: Category?,
        page: String,
    ) {
        takeFrom(baseUrl)
        if (isCategorySearch) {
            appendPathSegments(
                categorySearchPath,
                queryContent,
                categories.getValue(queryCategory!!),
            )
        } else {
            appendPathSegments(searchPath, queryContent)
        }
        appendPathSegments(page, "")
    }

    private fun parseFileSizeToBytes(fileSize: String): Long {
        val parts = fileSize.split(' ')
        require(parts.size == 2) { "Invalid file size format: $fileSize" }

        val sizeValue = requireNotNull(parts[0].toDoubleOrNull()) {
            "File size string did not contain a size number: $fileSize"
        }
        return when (val unit = parts[1].uppercase()) {
            "B" -> sizeValue.toLong()
            "KB" -> (sizeValue * 1024).toLong()
            "MB" -> (sizeValue * 1024 * 1024).toLong()
            "GB" -> (sizeValue * 1024 * 1024 * 1024).toLong()
            "TB" -> (sizeValue * 1024 * 1024 * 1024 * 1024).toLong()
            else -> throw IllegalArgumentException("Invalid file size unit: $unit")
        }
    }
}
