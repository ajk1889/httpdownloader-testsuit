import java.io.InputStream

operator fun InputStream.plus(gen: Generator): InputStream {
    val stream = this
    return object : InputStream() {
        var bytesRead = 0L
        override fun read(): Int {
            bytesRead += 1
            val r = stream.read()
            return if (r == -1) gen.read() else r
        }

        override fun read(b: ByteArray, off: Int, len: Int): Int {
            val r = stream.read(b, off, len)
            return if (r < 0) gen.read(b, off, len) else r
        }
    }
}