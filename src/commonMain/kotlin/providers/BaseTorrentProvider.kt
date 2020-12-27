package drewcarlson.torrentsearch.providers

import drewcarlson.torrentsearch.TorrentProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlin.coroutines.CoroutineContext

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
}
