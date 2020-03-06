import java.io.InputStream
import kotlin.math.min

operator fun InputStream.plus(second: InputStream): InputStream {
    val first = this
    return object : InputStream() {
        var current = first
        override fun read() = throw RuntimeException("read() should not be used")
        override fun read(b: ByteArray, off: Int, len: Int): Int {
            if (Server.sleep > 0L) Thread.sleep(Server.sleep)
            val r = current.read(b, off, len)
            return if (r < 0 && current == first) {
                current = second
                current.read(b, off, len)
            } else r
        }
    }
}
typealias Headers = Map<String, List<String>>
typealias FileData = Pair<String, InputStream>

enum class RequestType { BLANK, POST, FILE }

/**
 * @return an inputStream that terminates when it reaches supplied limit (exclusive)
 */
fun InputStream.toLimitedStream(limit: Long): InputStream {
    val old = this
    return object : InputStream() {
        var bytesRead = 0L
        override fun read() = throw RuntimeException("read() should not be used")
        override fun read(b: ByteArray, off: Int, len: Int): Int {
            if (Server.sleep > 0L) Thread.sleep(Server.sleep)
            if (bytesRead >= limit) return -1
            val length = min(len.toLong(), limit - bytesRead).toInt()
            return old.read(b, off, length).also { bytesRead += it }
        }

        override fun skip(n: Long): Long {
            val length = min(n, limit - bytesRead)
            return super.skip(length).also { bytesRead += it }
        }
    }
}

fun Long.formatted(): String {
    if (this < 0) return "---"
    var s = this.toDouble()
    var i = 0
    while (s > 999 && i < 4) {
        s /= 1024L; i++
    }
    val value = if (i == 0) s.toInt().toString()
    else "%.2f".format(s)
    val unit = arrayOf("bytes", "KB", "MB", "GB", "TB")[i]
    return "$value $unit"
}