import java.io.File

fun main(args: Array<String>) {
    var index: Int = args.indexOf("-port")
    val server = if(index!=-1) Server(args[index+1].toInt())
    else Server()

    index = args.indexOf("-htdocs")
    if (index != -1) Server.htdocs = File(args[index + 1])
    index = args.indexOf("-path123")
    if (index != -1) Server.path123 = args[index + 1]
    index = args.indexOf("-cookies")
    if (index != -1) Server.cookies = args[index + 1]
    index = args.indexOf("-size123")
    if (index != -1) Server.size123 = args[index + 1].toLong()

    server.start()
    println("Server started")
    processInputs(server)
    server.stop()
    println("stopped")
}

fun processInputs(server: Server){
    var cmd: String?
    loop@ while (true) {
        cmd = readLine()
        when {
            cmd == null -> continue@loop
            cmd == "stop" -> break@loop
            cmd.startsWith("cookie=") -> { }
            cmd.startsWith("path123=") -> {
                Server.path123 = cmd.substring("path123=".length)
                println("path123 changed to ${Server.path123}")
            }
            cmd.startsWith("size123=") -> {
                Server.size123 = cmd.substring("size123=".length).toLong()
                println("size123 changed to ${Server.size123}")
            }
            cmd.startsWith("htdocs=") -> {
                val htdocs = cmd.substring("htdocs=".length)
                if (htdocs.isBlank()) Server.htdocs = null
                else Server.htdocs = File(htdocs)
                println("htdocs changed to ${Server.htdocs}")
            }
            cmd.startsWith("ping=") -> {
                Server.ping = cmd.substring("ping=".length).toLong()
                println("ping changed to ${Server.ping}")
            }
            cmd.startsWith("sleep=") -> {
                Server.sleep = cmd.substring("sleep=".length).toLong()
                println("sleep changed to ${Server.sleep}")
            }
            cmd.startsWith("bfrsize=") -> {
                Server.bufferSize = cmd.substring("bfrsize=".length).toInt()
                println("bufferSize changed to ${Server.bufferSize}")
            }
            else -> println(
                "Invalid command\n" +
                        "Available commands: cookie, path123, size123, htdocs, ping, sleep, bfrsize\n" +
                        "Usage: `command=value`"
            )
        }
    }
}