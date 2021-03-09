package drewcarlson.torrentsearch

import drewcarlson.torrentsearch.providers.RarbgProvider
import io.ktor.client.HttpClient
import io.ktor.client.features.cookies.AcceptAllCookiesStorage
import io.ktor.client.features.cookies.HttpCookies
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ProviderTests {

    private val http = HttpClient().config {
        install(JsonFeature) {
            serializer = KotlinxSerializer()
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
