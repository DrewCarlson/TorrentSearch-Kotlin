package torrentsearch.providers

import io.ktor.http.encodeURLQueryComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import torrentsearch.TorrentProvider
import kotlin.coroutines.CoroutineContext
import kotlin.native.concurrent.SharedImmutable


@Suppress("HttpUrlsUsage")
@SharedImmutable
internal val trackers = listOf(
    "udp://p4p.arenabg.ch:1337",
    "udp://tracker.leechers-paradise.org:6969",
    "udp://tracker.opentrackr.org:1337/announce",
    "udp://tracker.coppersurfer.tk:6969/announce",
    "udp://9.rarbg.to:2920/announce",
    "udp://tracker.internetwarriors.net:1337",
    "udp://tracker.leechers-paradise.org:6969/announce",
    "udp://tracker.pirateparty.gr:6969/announce",
    "udp://tracker.cyberia.is:6969/announce",
    "udp://open.tracker.cl:1337/announce",
    "http://p4p.arenabg.com:1337/announce",
    "udp://torrent.gresille.org:80/announce",
    "udp://tracker.openbittorrent.com:80",
    "udp://glotorrents.pw:6969/announce",
).map { it.encodeURLQueryComponent() }

/**
 * The base class for creating new [TorrentProvider]s.
 */
public abstract class BaseTorrentProvider(
    /**
     * If the provider requires authentication credentials,
     * set [enabledByDefault] to false and the user will be
     * required to call [enable] with authentication details.
     */
    enabledByDefault: Boolean = true
) : TorrentProvider, CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.Default + SupervisorJob()

    final override var isEnabled: Boolean = enabledByDefault
        private set

    override fun enable(
        username: String?,
        password: String?,
        cookies: List<String>
    ) {
        isEnabled = true
    }

    override fun disable() {
        isEnabled = false
    }

    /**
     * Combine the torrent [name] and [infoHash] hash to create a usable magnet link.
     */
    protected fun formatMagnet(infoHash: String, name: String): String {
        val trackersQueryString = "&tr=${trackers.joinToString("&tr=")}"
        return "magnet:?xt=urn:btih:${infoHash}&dn=${name.encodeURLQueryComponent()}${trackersQueryString}"
    }
}
