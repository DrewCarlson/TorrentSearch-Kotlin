package torrentsearch

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.userAgent
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import torrentsearch.models.Category
import torrentsearch.models.ProviderResult
import torrentsearch.models.TorrentQuery
import torrentsearch.providers.EztvProvider
import torrentsearch.providers.PirateBayProvider
import torrentsearch.providers.RarbgProvider
import torrentsearch.providers.YtsProvider
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class ProviderTests {

    private val http = HttpClient().config {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }

        defaultRequest {
            userAgent(USER_AGENT)
        }

        install(HttpCookies) {
            storage = AcceptAllCookiesStorage()
        }
    }

    @Test
    fun testRarbgProvider() = runTest {
        val provider = RarbgProvider(
            httpClient = http,
            enabled = false,
            providerCache = null,
        )
        val token = assertNotNull(provider.readToken())

        assertTrue(token.isNotBlank())

        realDelay(RarbgProvider.API_REQUEST_DELAY)

        val result = searchWithTimeout(
            provider,
            TorrentQuery(
                content = "Airplane",
                category = Category.MOVIES,
            )
        )
        assertIs<ProviderResult.Success>(result)
        assertTrue(result.torrents.isNotEmpty())
    }

    @Test
    fun testRarbgImdbIdProvider() = runTest(timeout = 30.seconds) {
        val provider = RarbgProvider(
            httpClient = http,
            enabled = false,
            providerCache = null,
        )
        val token = assertNotNull(provider.readToken())

        assertTrue(token.isNotBlank())

        realDelay(RarbgProvider.API_REQUEST_DELAY)

        val result = searchWithTimeout(
            provider,
            TorrentQuery(
                imdbId = "tt0080339",
                category = Category.MOVIES,
            )
        )
        assertIs<ProviderResult.Success>(result)
        assertTrue(result.torrents.isNotEmpty())
    }

    @Test
    fun testRarbgTmdbIdProvider() = runTest(timeout = 30.seconds) {
        val provider = RarbgProvider(
            httpClient = http,
            enabled = false,
            providerCache = null,
        )
        val token = assertNotNull(provider.readToken())

        assertTrue(token.isNotBlank())

        realDelay(RarbgProvider.API_REQUEST_DELAY)

        val result = searchWithTimeout(
            provider,
            TorrentQuery(
                tmdbId = 813,
                category = Category.MOVIES,
            )
        )
        assertIs<ProviderResult.Success>(result)
        assertTrue(result.torrents.isNotEmpty())
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

    private suspend fun searchWithTimeout(
        provider: TorrentProvider,
        query: TorrentQuery,
    ): ProviderResult {
        var result = ProviderResult.Success("", emptyList())

        withContext(Dispatchers.Default) {
            withTimeout(30.seconds) {
                while (result.torrents.isEmpty()) {
                    (provider.search(query) as? ProviderResult.Success)?.let { nextResult ->
                        if (nextResult.torrents.isNotEmpty()) {
                            result = nextResult
                        }
                    }
                    delay(RarbgProvider.API_REQUEST_DELAY)
                }
            }
        }
        return checkNotNull(result)
    }
}
