package torrentsearch

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.*
import io.ktor.http.userAgent
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import torrentsearch.models.Category
import kotlin.test.*

class TorrentSearchTests {

    private val http = HttpClient().config {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }

        defaultRequest {
            userAgent(USER_AGENT)
        }

        Logging {
            level = LogLevel.ALL
            logger = Logger.SIMPLE
        }

        install(HttpCookies) {
            storage = AcceptAllCookiesStorage()
        }
    }
    private lateinit var torrentSearch: TorrentSearch

    @BeforeTest
    fun setup() {
        torrentSearch = TorrentSearch(httpClient = http, enableDefaultProviders = false)
    }

    @AfterTest
    fun cleanup() {
        torrentSearch.providers().forEach { torrentSearch.disableProvider(it.name) }
    }

    @Test
    fun testSearch() = runTest {
        torrentSearch.enableProvider("libre")
        val result = torrentSearch.search {
            content = "Big Buck Bunny"
            category = Category.MOVIES
        }
        val torrent = assertNotNull(result.torrents().firstOrNull())

        assertEquals("Big Buck Bunny", torrent.title)
        assertEquals("dd8255ecdc7ca55fb0bbf81323d87062db1f6d1c", torrent.hash)
        assertEquals("libre", torrent.provider)
    }

    @Test
    fun testDefaultProviderSearch() = runTest {
        torrentSearch.providers().forEach { torrentSearch.enableProvider(it.name) }
        val result = torrentSearch.search {
            content = "Big Buck Bunny"
            category = Category.MOVIES
        }

        val results = result.providerResults().toList()
        assertEquals(result.providerCount(), results.size)
    }
}
