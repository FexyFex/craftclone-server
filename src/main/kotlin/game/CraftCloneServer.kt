package game

import kotlinx.coroutines.runBlocking
import networking.CentralServerConnectionHandler
import networking.NetworkServer

class CraftCloneServer {
    fun start() = runBlocking {
        val server = NetworkServer.bind("localhost", 25567, CentralServerConnectionHandler())
    }
}