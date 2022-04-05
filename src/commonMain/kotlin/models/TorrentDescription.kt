package torrentsearch.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class TorrentDescription(
    val provider: String,
    val magnetUrl: String,
    val title: String,
    val size: Long,
    val seeds: Int,
    val peers: Int,
    val themoviedbId: Int? = null,
    val tvdbId: Int? = null,
    val imdbId: String? = null,
    val infoUrl: String? = null,
) {
    @Transient
    val hash: String = magnetUrl
        .substringAfter("xt=urn:btih:")
        .substringBefore("&")
}
