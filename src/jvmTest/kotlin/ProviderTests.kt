package drewcarlson.torrentsearch

import drewcarlson.torrentsearch.providers.RarbgProvider
import io.ktor.client.HttpClient
import io.ktor.client.plugins.*
import io.ktor.client.plugins.cookies.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

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
    fun testRarbgProvider() = runBlocking {
        val provider = RarbgProvider(
            httpClient = http,
            prefetchToken = false
        )
        val token = assertNotNull(provider.readToken())

        assertTrue(token.isNotBlank())

        delay(1500L)

        val results = provider.search("Airplane", Category.MOVIES, 20)

        assertTrue(results.isNotEmpty())
    }
}
