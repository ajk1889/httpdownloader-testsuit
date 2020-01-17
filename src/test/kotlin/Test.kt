fun main() {
//    val op = FileOutputStream("/Users/ajk/Desktop/abc.txt")
//    val ip = Generator(0, 1024 * 1024 * 50)
    print("'" + Generator[0L to 0L] + "'")
}

fun print2(vararg items: Any) {
    if (items.isEmpty()) {
        println(); return
    }
    for (i in 0 until items.size - 1)
        print("${items[i]} ")
    println(items.last())
}