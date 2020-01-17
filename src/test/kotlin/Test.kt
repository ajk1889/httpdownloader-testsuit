fun main() {
    val server = Server()
    server.start()
    while (readLine() != "s")
        continue
    server.stop()
}

fun print2(vararg items: Any) {
    if (items.isEmpty()) {
        println(); return
    }
    for (i in 0 until items.size - 1)
        print("${items[i]} ")
    println(items.last())
}