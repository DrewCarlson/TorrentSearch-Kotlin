package torrentsearch

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.time.Duration

suspend fun realDelay(duration: Duration) {
    withContext(Dispatchers.Default) {
        delay(duration)
    }
}