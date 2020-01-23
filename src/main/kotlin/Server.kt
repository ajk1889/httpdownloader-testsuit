import kotlinx.coroutines.*
import java.io.*
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import kotlin.math.min

class Server(
    private val port: Int = 1234
) {
    companion object {
        var contentLengthMode = false
        var noLengthMode = false
        var path123: String = "/a.txt"
        var htdocs: File? = null
        var cookies: String? = null
        var size123: Long = 100 * 1024 * 1024
        var ping: Long = 0L
        var sleep: Long = 0L
        var bufferSize: Int = 8 * 1024
    }

    private val server: ServerSocket by lazy { ServerSocket(port) }
    private val ioScope = CoroutineScope(Dispatchers.IO)
    private var shouldStop = false
    fun start() {
        ioScope.launch {
            while (!shouldStop) serve(nextConnection())
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
            if (ping > 0) delay(ping)
            client.sendResponse(response)
            client.closeConnection()
            println("IO completed for $client")
        }
    }

    private fun Map<String, String>.getContentRange(): Pair<Long, Long>? {
        val range = get("Range") ?: return null
        if (!range.startsWith("bytes=")) return null
        var start = 0L
        try {
            start = range.substring(6, range.indexOf('-')).toLong()
        } catch (ignored: Exception) {
        }
        var end = -1L
        try {
            end = range.substring(range.indexOf('-') + 1).toLong()
        } catch (ignored: Exception) {
        }
        return start to end
    }

    private fun areCookiesValid(headers: Map<String, String>): Boolean {
        val requestCookies = headers["Cookie"]
        val acceptableCookies = cookies
        return acceptableCookies == null
                || requestCookies != null && requestCookies.contains(acceptableCookies)
    }

    private suspend fun generateResponse(headers: Map<String, String>): InputStream = withContext(Dispatchers.Default) {
        if (headers.isEmpty())
            return@withContext toInputStream("<h2>Invalid request</h2>", 500)
        if (!areCookiesValid(headers))
            return@withContext toInputStream("<h2>Invalid cookies</h2>", 403)

        when (val path = headers["path"]) {
            null -> return@withContext toInputStream("<h2>No file specified</h2>", 500)
            path123 -> {
                println("Requested 123 file; size = $size123")
                return@withContext toInputStream(
                    Generator(size123),
                    headers.getContentRange()
                )
            }
            else -> htdocs?.also {
                val file = File(htdocs, URLDecoder.decode(path, StandardCharsets.UTF_8))
                println("Requested file: " + file.absolutePath + " length=" + file.length())
                if (file.exists()) return@withContext toInputStream(
                    file,
                    headers.getContentRange()
                )
            }
        }
        return@withContext toInputStream("<h2>Can't access the file</h2>", 404)
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
        if (contentLengthMode) headers.remove("Range")
        headers
    }

    private suspend fun Socket.closeConnection() = withContext(Dispatchers.IO) {
        close()
    }

    private suspend fun Socket.readInput(
        alreadyRead: String = "",
        retryCount: Int = 2
    ): String = withContext(Dispatchers.IO) {
        try {
            val ip = getInputStream()
            val data = StringBuilder(alreadyRead)
            val bfr = ByteArray(1024)
            try {
                var n = ip.read(bfr)
                while (n > 0) {
                    data.append(String(bfr, 0, n))
                    if (data.endsWith("\r\n\r\n"))
                        break
                    n = ip.read(bfr)
                }
            } catch (e: IOException) {
                if (retryCount > 0) {
                    println("$e Retrying read")
                    return@withContext readInput(data.toString(), retryCount - 1)
                }
            }
            data.toString()
        } catch (e: IOException) {
            ""
        }
    }

    private suspend fun Socket.sendResponse(response: InputStream) = withContext(Dispatchers.IO) {
        try {
            this@sendResponse.use { response.copyTo(it.getOutputStream(), bufferSize) }
        } catch (e: IOException) {
            println(e)
        }
    }

    private fun toInputStream(content: String, responseCode: Int): InputStream {
        val builder = StringBuilder("HTTP/1.1 $responseCode ${errorCodes[responseCode]}\r\n")
        builder.append("Content-Type: text/html\r\n")
        builder.append("Connection: keep-alive\r\n")
        builder.append("Accept-Ranges: bytes\r\n")
        if (!noLengthMode)
            builder.append("Content-Length: ${content.length}\r\n")
        cookies?.also { builder.append("Set-Cookie: $it\r\n") }
        builder.append("\r\n")
        builder.append(content)
        return builder.toString().byteInputStream()
    }

    private fun toInputStream(
        file: File,
        contentRange: Pair<Long, Long>? = null
    ): InputStream {
        if (file.isDirectory) return folderListStream(file)
        try {
            var inputStream: InputStream = FileInputStream(file)
            val builder = StringBuilder("HTTP/1.1 200 OK\r\n")
            builder.append("Server: TestSuit\r\n")
            builder.append("Content-Type: application/octet-stream\r\n")
            builder.append("Connection: keep-alive\r\n")
            builder.append("Accept-Ranges: bytes\r\n")
            builder.append("Content-Disposition: attachment; filename=\"${file.name}\"\r\n")
            cookies?.also { builder.append("Set-Cookie: $it\r\n") }

            if (contentRange != null && !noLengthMode && !contentLengthMode) {
                val (offset, limit) = contentRange
                inputStream.skip(offset)
                if (limit > 0) {
                    inputStream = inputStream.toLimitedStream(limit + 1)
                    builder.append("Content-Length: ${limit - offset + 1}\r\n")
                    builder.append("Content-Range: bytes $offset-$limit/${file.length()}\r\n")
                } else {
                    val len = file.length()
                    builder.append("Content-Length: ${len - offset}\r\n")
                    builder.append("Content-Range: bytes $offset-${len - 1}/$len\r\n")
                }
            } else if (!noLengthMode)
                builder.append("Content-Length: ${file.length()}\r\n")

            builder.append("\r\n")
            return builder.toString().byteInputStream() + inputStream
        } catch (e: FileNotFoundException) {
            return toInputStream("<h2>File not found exception</h2>", 404)
        }
    }

    private fun folderListStream(file: File): InputStream {
        val files = file.listFiles()
            ?: return toInputStream("<h2>Folder not accessible</h2>", 500)
        if (files.isEmpty())
            return toInputStream("<h2>Empty folder</h2>", 200)
        val builder = StringBuilder()
        val subLen = htdocs?.canonicalPath?.length ?: 0
        for (f in files) {
            builder.append("<a href='/")
                .append(f.canonicalPath.substring(subLen))
                .append("'>")
            if (f.isDirectory) builder.append("<font color='red' size=4>")
            else builder.append("<font color='black'>")
            builder.append(f.name).append("</font></a><br/>")
        }
        return toInputStream(
            """
            |<html>
            |<title>${file.name}</title>
            |<body>$builder</body>
            |</html>
        """.trimMargin(), 200
        )
    }

    private fun toInputStream(
        stream: Generator,
        contentRange: Pair<Long, Long>? = null
    ): InputStream {
        val builder = StringBuilder("HTTP/1.1 200 OK\r\n")
        builder.append("Server: TestSuit\r\n")
        builder.append("Content-Type: application/octet-stream\r\n")
        builder.append("Connection: keep-alive\r\n")
        builder.append("Accept-Ranges: bytes\r\n")
        builder.append("Content-Disposition: attachment; filename=\"a.txt\"\r\n")

        cookies?.also { builder.append("Set-Cookie: $it\r\n") }

        if (contentRange != null) {
            var (offset, limit) = contentRange
            stream.offset = offset
            if (limit < 0L) limit = stream.limit - 1
            stream.limit = min(limit + 1, size123 - 1)
            builder.append("Content-Range: bytes ${stream.offset}-${stream.limit}/$size123\r\n")
        }
        if (!noLengthMode)
            builder.append("Content-Length: ${stream.length}\r\n")
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