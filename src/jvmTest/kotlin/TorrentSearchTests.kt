package drewcarlson.torrentsearch

import drewcarlson.torrentsearch.providers.LibreProvider
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.cookies.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class TorrentSearchTests {

    private val http = HttpClient().config {
        install(ContentNegotiation) {
            json()
        }

        install(HttpCookies) {
            storage = AcceptAllCookiesStorage()
        }
    }
    private lateinit var torrentSearch: TorrentSearch

    @BeforeTest
    fun setup() {
        torrentSearch = TorrentSearch(null, http, installDefaultProviders = false, LibreProvider())
    }

    @Test
    fun testSearch() = runTest {
        val results = torrentSearch.search("Big Buck Bunny", Category.MOVIES, 10)
        val torrent = assertNotNull(results.firstOrNull())

        assertEquals("Big Buck Bunny", torrent.title)
        assertEquals("dd8255ecdc7ca55fb0bbf81323d87062db1f6d1c", torrent.hash)
        assertEquals("libre", torrent.provider)
    }
}