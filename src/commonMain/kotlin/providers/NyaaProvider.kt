package torrentsearch.providers

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import ktsoup.KtSoupElement
import ktsoup.KtSoupParser
import ktsoup.KtSoupText
import torrentsearch.models.*

internal class NyaaProvider(
    private val httpClient: HttpClient,
    enabled: Boolean = true,
) : BaseTorrentProvider(enabled) {
    override val name: String = "nyaa"

    override val baseUrl: String = "https://nyaa.si/"
    override val tokenPath: String = ""
    override val searchPath: String = ""

    override val categories: Map<Category, String> = mapOf(
        Category.ALL to "0_0",
        Category.AUDIO to "2_0",
        Category.MOVIES to "1_0",
        Category.TV to "1_0",
        Category.ANIME to "1_0",
        Category.GAMES to "6_2",
        Category.MUSIC to "2_0",
        Category.APPS to "6_1",
        Category.BOOKS to "2_0",
    )

    override val searchParams: Map<SearchParam, String> = mapOf(
        SearchParam.CATEGORY to "c",
        SearchParam.QUERY to "q",
        SearchParam.PAGE to "p",
    )

    override suspend fun search(query: TorrentQuery): ProviderResult {
        val queryCategory = query.category
        val queryContent = query.content
        if (queryContent.isNullOrBlank()) {
            return ProviderResult.Error.InvalidQueryError(name, "nyaa requires a query string")
        }
        val response = try {
            httpClient.get {
                url {
                    takeFrom(baseUrl)
                    takeFrom(searchPath)
                    parameter(searchParams.getValue(SearchParam.QUERY), queryContent)
                    parameter(searchParams.getValue(SearchParam.PAGE), query.page)
                    parameter(
                        searchParams.getValue(SearchParam.CATEGORY),
                        categories[queryCategory] ?: categories[Category.ALL],
                    )
                }
            }
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

    private fun parseResultsList(html: String): ProviderResult {
        return KtSoupParser.parse(html).use { document ->
            val absoluteUrlBase = baseUrl.trimEnd('/')
            val rows = document.querySelectorAll("table.torrent-list tbody tr")
            val torrents = rows.mapNotNull { extractRowDetails(it, absoluteUrlBase) }
            val currentPage = document.querySelector("ul.pagination li.active")
                ?.textContent()
                ?.toIntOrNull() ?: 1
            val pagination = document.querySelector(".pagination-page-info")?.child(0) as? KtSoupText
            val totalCount = pagination?.textContent()?.split(' ')?.getOrNull(5)?.toIntOrNull() ?: torrents.size

            ProviderResult.Success(
                providerName = name,
                torrents = torrents,
                page = currentPage,
                totalTorrents = totalCount,
                requiresResolution = false,
            )
        }
    }

    private fun extractRowDetails(
        row: KtSoupElement,
        absoluteUrlBase: String,
    ): TorrentDescription? {
        val nameTd = row.querySelector("td:nth-child(2) a:last-child") ?: return null
        val nameLink = nameTd.attr("href") ?: return null
        val magnetUrl = row.querySelector("td:nth-child(3) a:last-child")?.attr("href") ?: return null
        val size = row.querySelector("td:nth-child(4)")?.textContent()?.run(::parseFileSizeToBytes) ?: 0L
        // val dateTd = row.querySelector("td:nth-child(5)") ?: return null
        val seeds = row.querySelector("td:nth-child(6)")?.textContent()?.toIntOrNull() ?: return null
        val peers = row.querySelector("td:nth-child(7)")?.textContent()?.toIntOrNull() ?: 0

        return TorrentDescription(
            provider = name,
            magnetUrl = magnetUrl,
            title = nameTd.textContent(),
            size = size,
            seeds = seeds,
            peers = peers,
            hash = hashFromMagnetUrl(magnetUrl),
            infoUrl = "${absoluteUrlBase}$nameLink",
        )
    }

    private fun parseFileSizeToBytes(fileSize: String): Long {
        val parts = fileSize.split(' ')
        require(parts.size == 2) { "Invalid file size format: $fileSize" }

        val sizeValue = requireNotNull(parts[0].toDoubleOrNull()) {
            "File size string did not contain a size number: $fileSize"
        }
        return when (val unit = parts[1].uppercase()) {
            "B" -> sizeValue.toLong()
            "KIB" -> (sizeValue * 1024).toLong()
            "MIB" -> (sizeValue * 1024 * 1024).toLong()
            "GIB" -> (sizeValue * 1024 * 1024 * 1024).toLong()
            "TIB" -> (sizeValue * 1024 * 1024 * 1024 * 1024).toLong()
            else -> throw IllegalArgumentException("Invalid file size unit: $unit")
        }
    }
}
