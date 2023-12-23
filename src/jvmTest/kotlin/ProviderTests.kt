package torrentsearch

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.*
import io.ktor.http.userAgent
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import torrentsearch.models.Category
import torrentsearch.models.ProviderResult
import torrentsearch.models.ResolveResult
import torrentsearch.models.TorrentQuery
import torrentsearch.providers.*
import torrentsearch.providers.EztvProvider
import torrentsearch.providers.PirateBayProvider
import torrentsearch.providers.YtsProvider
import kotlin.test.*

class ProviderTests {

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

    @Test
    fun testPirateBayProvider() = runTest {
        val provider = PirateBayProvider(http)

        val result = provider.search(
            TorrentQuery(
                content = "Airplane",
                category = Category.MOVIES,
            ),
        )

        assertIs<ProviderResult.Success>(result)
        assertTrue(result.torrents.isNotEmpty())
    }

    @Test
    fun testYtsProvider() = runTest {
        val provider = YtsProvider(http)

        val result = provider.search(
            TorrentQuery(
                content = "Airplane",
                category = Category.MOVIES,
            ),
        )

        assertIs<ProviderResult.Success>(result)
        assertTrue(result.torrents.isNotEmpty())
    }

    @Test
    fun testYtsImdbIdProvider() = runTest {
        val provider = YtsProvider(http)

        val result = provider.search(
            TorrentQuery(
                imdbId = "tt0080339",
                category = Category.MOVIES,
            ),
        )

        assertIs<ProviderResult.Success>(result)
        assertTrue(result.torrents.isNotEmpty())
    }

    @Test
    fun testEztvImdbIdProvider() = runTest {
        val provider = EztvProvider(http)

        val result = provider.search(
            TorrentQuery(
                imdbId = "tt4254242",
                category = Category.MOVIES,
            ),
        )

        assertIs<ProviderResult.Success>(result)
        assertTrue(result.torrents.isNotEmpty())
    }

    @Test
    fun test1337xProvider() = runTest {
        val provider = X1337Provider(http)

        val result = provider.search(
            TorrentQuery(
                content = "The Flash",
                category = Category.TV,
            ),
        )

        assertIs<ProviderResult.Success>(result)
        assertEquals(40, result.torrents.size)
        assertEquals(1, result.page)
        assertTrue(result.totalTorrents > 40)

        val torrent = result.torrents.first()

        assertEquals(provider.name, torrent.provider)
        assertFalse(torrent.isResolved)
        assertTrue(torrent.seeds > 0)
        assertTrue(torrent.peers > 0)
        assertNull(torrent.hash)
        assertNull(torrent.magnetUrl)
        assertTrue(torrent.title.isNotEmpty())

        val resolveResult = provider.resolve(listOf(torrent))
        assertIs<ResolveResult.Success>(resolveResult)
        val resolved = resolveResult.torrents.firstOrNull()
        assertNotNull(resolved)
        assertTrue(resolved.isResolved)
        assertFalse(resolved.magnetUrl.isNullOrBlank())
        assertFalse(resolved.hash.isNullOrBlank())
    }

    @Test
    fun testNyaaProvider() = runTest {
        val provider = NyaaProvider(http)

        val result = provider.search(
            TorrentQuery(
                content = "Bleach",
                category = Category.MOVIES,
            ),
        )

        assertIs<ProviderResult.Success>(result)
        assertTrue(result.torrents.isNotEmpty())
        assertFalse(result.requiresResolution)

        val torrent = result.torrents.firstOrNull()
        assertFalse(torrent?.magnetUrl.isNullOrBlank())
        assertFalse(torrent?.hash.isNullOrBlank())
    }
}
