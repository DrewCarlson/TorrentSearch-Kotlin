package drewcarlson.torrentsearch

import drewcarlson.torrentsearch.providers.PirateBayProvider
import drewcarlson.torrentsearch.providers.RarbgProvider
import io.ktor.client.HttpClient
import io.ktor.client.plugins.*
import io.ktor.client.plugins.cookies.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class ProviderTests {

    private val http = HttpClient().config {
        install(ContentNegotiation) {
            json()
        }

        install(HttpCookies) {
            storage = AcceptAllCookiesStorage()
        }
    }

    @Test
    fun testRarbgProvider() = runTest {
        realDelay(RarbgProvider.API_REQUEST_DELAY)

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
}
