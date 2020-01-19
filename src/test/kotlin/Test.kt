fun main() {
    val server = Server()
    server.start()
    while (readLine() != "s") continue
    server.stop()
    println("stopped")
}