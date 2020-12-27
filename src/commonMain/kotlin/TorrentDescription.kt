package drewcarlson.torrentsearch

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class TorrentDescription(
    val provider: String,
    val magnetUrl: String,
    val title: String,
    val size: Long,
    val seeds: Int,
    val peers: Int
) {
    @Transient
    val hash: String = magnetUrl
        .substringAfter("xt=urn:btih:")
        .substringBefore("&")
}
