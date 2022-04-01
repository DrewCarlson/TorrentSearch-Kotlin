package torrentsearch

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.cookies.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import torrentsearch.providers.PirateBayProvider
import torrentsearch.providers.RarbgProvider
import torrentsearch.providers.YtsProvider
import kotlin.test.Test
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
            prefetchToken = false,
            providerCache = null,
        )
        val token = assertNotNull(provider.readToken())

        assertTrue(token.isNotBlank())

        realDelay(RarbgProvider.API_REQUEST_DELAY)

        var results = emptyList<TorrentDescription>()

        withContext(Dispatchers.Default) {
            withTimeout(30.seconds) {
                while (results.isEmpty()) {
                    results = provider.search("Airplane", Category.MOVIES, 20)
                    delay(RarbgProvider.API_REQUEST_DELAY)
                }
            }
        }
        assertTrue(results.isNotEmpty())
    }

    @Test
    fun testPirateBayProvider() = runTest {
        val provider = PirateBayProvider(http)

        val results = provider.search("Airplane", Category.MOVIES, 20)

        assertTrue(results.isNotEmpty())
    }

    @Test
    fun testYtsProvider() = runTest {
        val provider = YtsProvider(http)

        val results = provider.search("Airplane", Category.MOVIES, 20)

        assertTrue(results.isNotEmpty())
    }
}
