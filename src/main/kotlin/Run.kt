import java.io.File

fun main(args: Array<String>) {
    var index: Int = args.indexOf("-port")
    val server = if(index!=-1) Server(args[index+1].toInt())
    else Server()

    index = args.indexOf("-htdocs")
    if (index != -1) Server.htdocs = File(args[index + 1])
    index = args.indexOf("-path123")
    if (index != -1) Server.path123 = args[index + 1]
    index = args.indexOf("-cookie")
    if (index != -1) Server.cookies = args[index + 1]
    index = args.indexOf("-size123")
    if (index != -1) Server.size123 = args[index + 1].toLong()
    index = args.indexOf("-ping")
    if (index != -1) Server.ping = args[index + 1].toLong()
    index = args.indexOf("-sleep")
    if (index != -1) Server.sleep = args[index + 1].toLong()
    index = args.indexOf("-bfrsize")
    if (index != -1) Server.bufferSize = args[index + 1].toInt()
    index = args.indexOf("-lengthonly")
    if (index != -1) Server.contentLengthMode = args[index + 1].toBoolean()
    index = args.indexOf("-nolength")
    if (index != -1) Server.noLengthMode = args[index + 1].toBoolean()
    index = args.indexOf("-logging")
    if (index != -1) Server.loggingAllowed = args[index + 1].toBoolean()

    server.start()
    println("Server started")
    processInputs()
    server.stop()
    println("stopped")
}

fun processInputs() {
    var cmd: String?
    loop@ while (true) {
        cmd = readLine()?.trim()
        when {
            cmd == null -> continue@loop
            cmd == "stop" -> break@loop
            cmd.startsWith("cookie=") -> {
                val cookie = cmd.substring("cookie=".length)
                if (cookie.isBlank()) Server.cookies = null
                else Server.cookies = cookie
                println("Cookie changed to ${Server.cookies}")
            }
            cmd.startsWith("path123=") -> {
                Server.path123 = cmd.substring("path123=".length)
                println("path123 changed to ${Server.path123}")
            }
            cmd.startsWith("size123=") -> {
                Server.size123 = cmd.substring("size123=".length).toLong()
                println("size123 changed to ${Server.size123} bytes = " + Server.size123.formatted())
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
                println("bufferSize changed to ${Server.bufferSize} bytes = " + Server.bufferSize.toLong().formatted())
            }
            cmd.startsWith("lengthonly=") -> {
                Server.contentLengthMode = cmd.substring("lengthonly=".length).toBoolean()
                if (Server.contentLengthMode) {
                    Server.noLengthMode = false
                    println("no-length mode changed to false")
                }
                println("length-only mode changed to ${Server.contentLengthMode}")
            }
            cmd.startsWith("nolength=") -> {
                Server.noLengthMode = cmd.substring("nolength=".length).toBoolean()
                if (Server.noLengthMode) {
                    Server.contentLengthMode = false
                    println("length-only mode changed to false")
                }
                println("no-length mode changed to ${Server.noLengthMode}")
            }
            cmd.startsWith("logging=") -> {
                Server.loggingAllowed = cmd.substring("logging=".length).toBoolean()
                println("loggingAllowed changed to ${Server.loggingAllowed}")
            }
            else -> println(
                "Invalid command\n" +
                        "Available commands: cookie, path123, size123, htdocs, ping, " +
                        "sleep, bfrsize, stop, lengthonly, nolength, logging\n" +
                        "Usage: `command=value`"
            )
        }
    }
}