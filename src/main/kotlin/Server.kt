import kotlinx.coroutines.*
import java.io.InputStream
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException

class Server(
    private val port: Int = 1234,
    private val path: String = "/a.txt",
    private val size: Long = 100 * 1024 * 1024
) {
    private val server: ServerSocket by lazy { ServerSocket(port) }
    private val ioScope = CoroutineScope(Dispatchers.IO)
    private var shouldStop = false
    fun start() {
        ioScope.launch {
            try {
                println("Server started")
                while (!shouldStop)
                    serve(nextConnection())
                println("Server stopped")
            } catch (e: CancellationException) {
                println("Cancelled: Server stopped")
            }
        }
    }

    fun stop() {
        shouldStop = true
        ioScope.cancel("Server stopped")
    }

    private suspend fun nextConnection(): Socket = withContext(Dispatchers.IO) { server.accept() }

    private fun serve(client: Socket) {
        ioScope.launch {
            println("New connection from $client")
            client.soTimeout = 5000
            val inputData = client.readInput()
            val request = inputData.extractHeaders()
            val response = generateResponse(request)
            client.sendResponse(response)
            client.closeConnection()
            println("IO completed for $client")
        }
    }

    private suspend fun generateResponse(headers: Map<String, String>): InputStream = withContext(Dispatchers.Default) {
        if (headers.isEmpty())
            return@withContext toInputStream("No file specified")
        if (headers["path"] == path)
            return@withContext toInputStream(stream = Generator(0, size))
        return@withContext toInputStream("File not found")
    }

    private suspend fun String.extractHeaders(): Map<String, String> = withContext(Dispatchers.Default) {
        val headers = mutableMapOf<String, String>()
        if (isNullOrBlank()) return@withContext headers

        val items = split("\r\n")
        headers["path"] = items[0].substring(items[0].indexOf(" ") + 1, items[0].lastIndexOf(" "))
        for (i in 1 until items.size) {
            val separatorIndex = items[i].indexOf(": ")
            if (separatorIndex < 0) continue
            val key = items[i].substring(0, separatorIndex)
            val value = items[i].substring(separatorIndex + 2)
            headers[key] = value
        }
        headers
    }

    private suspend fun Socket.closeConnection() = withContext(Dispatchers.IO) {
        close()
    }

    private suspend fun Socket.readInput(): String = withContext(Dispatchers.IO) {
        try {
            val ip = getInputStream()
            val data = StringBuilder()
            val bfr = ByteArray(1024)
            var n = ip.read(bfr)
            while (n > 0) {
                data.append(String(bfr, 0, n))
                if (data.endsWith("\r\n\r\n"))
                    break
                n = ip.read(bfr)
            }
            data.toString()
        } catch (e: SocketTimeoutException) {
            ""
        }
    }

    private suspend fun Socket.sendResponse(response: InputStream) = withContext(Dispatchers.IO) {
        val op = getOutputStream()
        response.copyTo(op)
        op.close()
    }

    private fun toInputStream(
        content: String? = null,
        stream: Generator? = null,
        responseCode: Int = 200,
        contentDisposition: String = "attachment; filename=\"a.txt\"",
        contentLength: Long = -1,
        contentRange: Triple<Long, Long, Long>? = null,
        contentType: String = "text/html"
    ): InputStream {
        val builder = StringBuilder("HTTP/1.1 $responseCode OK\r\nServer: TestSuit\r\n")
        builder.append("Content-Type: $contentType\r\n")
        builder.append("Connection: keep-alive\r\n")
        builder.append("Accept-Ranges: bytes\r\n")

        if (content == null && stream != null)
            builder.append("Content-Disposition: $contentDisposition\r\n")

        val length = if (contentLength != -1L) contentLength
        else content?.length ?: (stream?.length ?: -1)
        if (length != -1) builder.append("Content-Length: $length\r\n")

        if (contentRange != null) {
            val (offset, limit, total) = contentRange
            builder.append("Content-Range: bytes $offset-$limit/$total\r\n")
        }

        builder.append("\r\n")
        if (content != null) builder.append(content)
        if (stream != null) return builder.toString().byteInputStream() + stream
        return builder.toString().byteInputStream()
    }
}