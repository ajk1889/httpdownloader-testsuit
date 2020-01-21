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