import kotlinx.coroutines.*
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.net.ServerSocket
import java.net.Socket

class Server(
    private val port: Int = 1234
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
        println("New connection from $client")
        ioScope.launch {
            val inputData = client.readInput()
            println(inputData)
            val request = inputData.extractHeaders()
            val response = generateResponse(request)
            client.sendResponse(response)
            client.closeConnection()
            println("IO completed for $client")
        }
    }

    private suspend fun generateResponse(headers: Map<String, String>): InputStream = withContext(Dispatchers.Default) {
        ByteArrayInputStream("hello".toByteArray())
    }

    private suspend fun String.extractHeaders(): Map<String, String> = withContext(Dispatchers.Default) {
        mapOf<String, String>()
    }

    private suspend fun Socket.closeConnection() = withContext(Dispatchers.IO) {
        close()
    }

    private suspend fun Socket.readInput(): String = withContext(Dispatchers.IO) {
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
    }

    private suspend fun Socket.sendResponse(response: InputStream) = withContext(Dispatchers.IO) {
        val op = getOutputStream()
        response.copyTo(op)
        op.close()
    }
}
