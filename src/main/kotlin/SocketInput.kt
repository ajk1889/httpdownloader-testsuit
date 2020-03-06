import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.net.Socket

class SocketInput(private val socket: Socket) {
    suspend fun getType(): RequestType = withContext(Dispatchers.IO) {
        TODO()
    }

    suspend fun getHeaders(): Headers = withContext(Dispatchers.IO) {
        TODO()
    }

    suspend fun getInputData(): String = withContext(Dispatchers.IO) {
        TODO()
    }

    suspend fun getDataInput(): FileData {
        TODO()
    }

    private fun ByteArrayOutputStream.indexOf(subArray: ByteArray): Int {
        if (subArray.isEmpty()) return -1
        val bytes = this.toByteArray()
        var n = subArray.size - 1
        for (i in subArray.size downTo 0) {
            if (subArray[n] == bytes[i]) {
                n -= 1
                if (n == 0) return i
            } else n = 0
        }
        return -1
    }
}