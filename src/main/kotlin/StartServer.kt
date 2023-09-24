import kotlinx.coroutines.runBlocking
import networking.CentralServerConnectionHandler
import networking.NetworkServer


fun main() {
    serverMain()
}

fun serverMain() = runBlocking {
    val server = NetworkServer.bind("localhost", 25567, CentralServerConnectionHandler())

    // sometime later:
    //server.close()
}
