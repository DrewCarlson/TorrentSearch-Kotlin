package torrentsearch.providers

import io.ktor.http.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import torrentsearch.TorrentProvider
import kotlin.coroutines.CoroutineContext
import kotlin.native.concurrent.SharedImmutable


@Suppress("HttpUrlsUsage")
@SharedImmutable
internal val trackers = listOf(
    "udp://p4p.arenabg.ch:1337/announce",
    "udp://tracker.leechers-paradise.org:6969/announce",
    "udp://tracker.opentrackr.org:1337/announce",
    "udp://tracker.coppersurfer.tk:6969/announce",
    "udp://9.rarbg.to:2920/announce",
    "udp://tracker.internetwarriors.net:1337/announce",
    "udp://tracker.leechers-paradise.org:6969/announce",
    "udp://tracker.pirateparty.gr:6969/announce",
    "udp://tracker.cyberia.is:6969/announce",
    "udp://open.tracker.cl:1337/announce",
    "http://p4p.arenabg.com:1337/announce"
).map { it.encodeURLQueryComponent() }

abstract class BaseTorrentProvider(
    enabledByDefault: Boolean = true
) : TorrentProvider, CoroutineScope {

    private var enabled = enabledByDefault

    override val coroutineContext: CoroutineContext =
        Dispatchers.Default + SupervisorJob()

    final override val isEnabled: Boolean = enabled

    override fun enable(
        username: String?,
        password: String?,
        cookies: List<String>
    ) {
        enabled = true
    }

    override fun disable() {
        enabled = false
    }

    internal fun formatMagnet(infoHash: String, name: String): String {
        val trackersQueryString = "&tr=${trackers.joinToString("&tr=")}"
        return "magnet:?xt=urn:btih:${infoHash}&dn=${name.encodeURLQueryComponent()}${trackersQueryString}"
    }
}
