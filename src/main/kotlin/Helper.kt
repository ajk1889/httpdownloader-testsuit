import java.io.InputStream
import kotlin.math.min

operator fun InputStream.plus(second: InputStream): InputStream {
    val first = this
    return object : InputStream() {
        var bytesRead = 0L
        var current = first
        override fun read(): Int {
            bytesRead += 1
            val r = current.read()
            return if(r<0 && current==first) {
                current = second
                current.read()
            } else r
        }

        override fun read(b: ByteArray, off: Int, len: Int): Int {
            val r = current.read(b, off, len)
            return if(r<0 && current==first){
                current = second
                current.read(b, off, len)
            } else r
        }
    }
}

/**
 * @return an inputStream that terminates when it reaches supplied limit (exclusive)
 */
fun InputStream.setLimit(limit: Long): InputStream{
    val old = this
    var position = 0L
    return object : InputStream(){
        override fun read(): Int {
            if(position >= limit) return -1
            position+=1
            return old.read()
        }

        override fun read(b: ByteArray, off: Int, len: Int): Int {
            if(position >= limit) return -1
            val length = min(len.toLong(), limit - position).toInt()
            position += length
            println("off: $off len: $length b.length: ${b.size}")
            return old.read(b, off, length)
        }
    }
}