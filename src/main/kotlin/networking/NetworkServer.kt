package networking

import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import kotlinx.coroutines.*
import java.nio.channels.ClosedChannelException
import java.util.concurrent.Executors

class NetworkServer(
    private val dispatcher: CoroutineDispatcher,
    private val socket: ServerSocket,
    private val handler: NetworkServerConnectionHandler,
) {
    private fun launchAcceptLoop(dispatchScope: CoroutineScope) {
        dispatchScope.launch {
            while (!socket.isClosed) {
                val clientSocket = try {
                    socket.accept()
                } catch (exception: ClosedChannelException) {
                    break
                }

                acceptClient(clientSocket)
            }
        }
    }

    private suspend fun acceptClient(socket: Socket) {
        val client = NetworkClient.create(socket)

        handler.clientConnected(client)
    }

    suspend fun close() {
        withContext(Dispatchers.IO) {
            socket.close()
        }
        socket.awaitClosed()
        dispatcher.job.cancelAndJoin()
    }

    companion object {
        suspend fun bind(hostname: String, port: Int, handler: NetworkServerConnectionHandler): NetworkServer {
            val dispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
            val dispatchScope = CoroutineScope(dispatcher + CoroutineName("Server network IO"))

            val selectorManager = ActorSelectorManager(Dispatchers.IO)
            val serverSocket = aSocket(selectorManager).tcp().bind(hostname, port)

            val server = NetworkServer(dispatcher, serverSocket, handler)

            server.launchAcceptLoop(dispatchScope)

            return server
        }
    }
}