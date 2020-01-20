import kotlinx.coroutines.*
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException

class Server(
    private val port: Int = 1234,
    private var path123: String = "/a.txt",
    private var htdocs: File? = null,
    private var cookies: String? = null,
    private var size: Long = 100 * 1024 * 1024
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
        val path = headers["path"]
        if (headers.isEmpty() || path==null) {
            return@withContext toInputStream("No file specified", 502)
        } else if (headers["path"] == path123) {
            return@withContext toInputStream(stream = Generator(0, size))
        } else htdocs?.also {
            val file = File(htdocs, path)
            println("Requested file: "+file.absolutePath)
            if(file.exists()) return@withContext toInputStream(file)
        }
        return@withContext toInputStream("File not found", 404)
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

    private fun toInputStream(content: String, responseCode: Int): InputStream {
        val builder = StringBuilder("HTTP/1.1 $responseCode ${errorCodes[responseCode]}\r\n")
        builder.append("Content-Type: text/html\r\n")
        builder.append("Connection: keep-alive\r\n")
        builder.append("Accept-Ranges: bytes\r\n")
        builder.append("Content-Length: ${content.length}\r\n")
        cookies?.also { builder.append("Set-Cookie: $it") }
        builder.append("\r\n")
        builder.append(content)
        return builder.toString().byteInputStream()
    }

    private fun toInputStream(
        file: File,
        contentRange: Pair<Long, Long>? = null
    ): InputStream{
        val builder = StringBuilder("HTTP/1.1 200 OK\r\n")
        builder.append("Server: TestSuit\r\n")
        builder.append("Content-Type: application/octet-stream\r\n")
        builder.append("Connection: keep-alive\r\n")
        builder.append("Accept-Ranges: bytes\r\n")
        builder.append("Content-Disposition: attachment; filename=\"${file.name}\"\r\n")
        cookies?.also { builder.append("Set-Cookie: $it") }

        var inputStream: InputStream = FileInputStream(file)
        if (contentRange != null) {
            val (offset, limit) = contentRange
            inputStream.skip(offset)
            inputStream = inputStream.setLimit(limit+1)
            builder.append("Content-Length: ${limit - offset + 1}\r\n")
            builder.append("Content-Range: bytes $offset-$limit/${file.length()}\r\n")
        } else builder.append("Content-Length: ${file.length()}\r\n")

        builder.append("\r\n")
        return builder.toString().byteInputStream() + inputStream
    }

    private fun toInputStream(
        stream: Generator,
        responseCode: Int = 200,
        contentDisposition: String = "attachment; filename=\"a.txt\"",
        contentLength: Long = -1,
        contentRange: Triple<Long, Long, Long>? = null,
        contentType: String = "text/html"
    ): InputStream {
        val builder = StringBuilder("HTTP/1.1 $responseCode ${errorCodes[responseCode]}\r\n")
        builder.append("Server: TestSuit\r\n")
        builder.append("Content-Type: $contentType\r\n")
        builder.append("Connection: keep-alive\r\n")
        builder.append("Accept-Ranges: bytes\r\n")
        builder.append("Content-Disposition: $contentDisposition\r\n")

        cookies?.also { builder.append("Set-Cookie: $it") }

        val length = if (contentLength == -1L) stream.length else contentLength
        builder.append("Content-Length: $length\r\n")

        if (contentRange != null) {
            val (offset, limit, total) = contentRange
            builder.append("Content-Range: bytes $offset-$limit/$total\r\n")
        }

        builder.append("\r\n")
        return builder.toString().byteInputStream() + stream
    }

    private val errorCodes = mapOf(
        100 to "Continue", 101 to "Switching Protocols",
        200 to "OK", 201 to "Created", 202 to "Accepted", 203 to "Non-Authoritative Information",
        204 to "No Content", 205 to "Reset Content", 206 to "Partial Content", 300 to "Multiple Choices",
        301 to "Moved Permanently", 302 to "Found", 303 to "See Other", 304 to "Not Modified",
        305 to "Use Proxy", 307 to "Temporary Redirect", 400 to "Bad Request", 401 to "Unauthorized",
        402 to "Payment Required", 403 to "Forbidden", 404 to "Not Found", 405 to "Method Not Allowed",
        406 to "Not Acceptable", 407 to "Proxy Authentication Required", 408 to "Request Timeout",
        409 to "Conflict", 410 to "Gone", 411 to "Length Required", 412 to "Precondition Failed",
        413 to "Payload Too Large", 414 to "URI Too Long", 415 to "Unsupported Media Type",
        416 to "Range Not Satisfiable", 417 to "Expectation Failed", 418 to "I'm a teapot",
        426 to "Upgrade Required", 500 to "Internal Server Error", 501 to "Not Implemented",
        502 to "Bad Gateway", 503 to "Service Unavailable", 504 to "Gateway Time-out",
        505 to "HTTP Version Not Supported", 102 to "Processing", 207 to "Multi-Status",
        226 to "IM Used", 308 to "Permanent Redirect", 422 to "Unprocessable Entity", 423 to "Locked",
        424 to "Failed Dependency", 428 to "Precondition Required", 429 to "Too Many Requests",
        431 to "Request Header Fields Too Large", 451 to "Unavailable For Legal Reasons",
        506 to "Variant Also Negotiates", 507 to "Insufficient Storage",
        511 to "Network Authentication Required"
    )
}