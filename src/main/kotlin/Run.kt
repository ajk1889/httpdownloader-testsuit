import java.io.File

fun main(args: Array<String>) {
    var index: Int = args.indexOf("-port")
    val server = if(index!=-1) Server(args[index+1].toInt())
    else Server()

    index = args.indexOf("-htdocs")
    if(index!=-1) server.htdocs = File(args[index+1])
    index = args.indexOf("-path123")
    if(index!=-1) server.path123 = args[index+1]
    index = args.indexOf("-cookies")
    if(index!=-1) server.cookies = args[index+1]
    index = args.indexOf("-size123")
    if(index!=-1) server.size123 = args[index+1].toLong()

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
                server.path123 = cmd.substring("path123=".length)
                println("path123 changed to ${server.path123}")
            }
            cmd.startsWith("size123=") -> {
                server.size123 = cmd.substring("size123=".length).toLong()
                println("size123 changed to ${server.size123}")
            }
            cmd.startsWith("htdocs=") -> {
                val htdocs = cmd.substring("htdocs=".length)
                if(htdocs.isBlank()) server.htdocs = null
                else server.htdocs = File(htdocs)
                println("htdocs changed to ${server.htdocs}")
            }
            else -> println("Invalid command")
        }
    }
}