package torrentsearch.web

import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt

private const val KB = 1024.0
private const val RF = 100.0

fun Long.toHumanReadableSize(): String {
    if (this == 0L) return "0 B"
    val e = floor(ln(this.toDouble()) / ln(KB))
    val sizeString = ((this / KB.pow(e)) * RF).roundToInt() / RF
    return "$sizeString ${" KMGTP"[e.toInt()]}B"
}
