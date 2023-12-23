package torrentsearch.providers

import io.ktor.http.encodeURLQueryComponent
import torrentsearch.TorrentProvider

@Suppress("HttpUrlsUsage")
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
 *
 * @param enabledByDefault If the provider requires authentication credentials, set
 * to false and the user will be required to call [enable] with authentication details.
 */
public abstract class BaseTorrentProvider(
    enabledByDefault: Boolean = true,
) : TorrentProvider {

    final override var isEnabled: Boolean = enabledByDefault
        private set

    override fun enable(
        username: String?,
        password: String?,
        cookies: List<String>,
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
        return "magnet:?xt=urn:btih:$infoHash&dn=${name.encodeURLQueryComponent()}$trackersQueryString"
    }

    protected fun hashFromMagnetUrl(magnetUrl: String): String {
        return magnetUrl.substringBefore("&").substringAfter("xt=urn:btih:").uppercase()
    }
}
