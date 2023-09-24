package networking

import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import java.util.concurrent.Executors


class NetworkClient(
    private val dispatcher: CoroutineDispatcher,
    private val socket: Socket,
    private val send: SendChannel<WrittenPacket>,
    private val receive: ReceiveChannel<WrittenPacket>,
) {
    suspend fun sendPacket(packet: WrittenPacket) {
        send.send(packet)
    }

    suspend fun receivePacket(): WrittenPacket {
        return receive.receive()
    }

    suspend fun close() {
        withContext(Dispatchers.IO) {
            socket.close()
        }
        socket.awaitClosed()
        dispatcher.job.cancelAndJoin()
    }

    companion object {
        suspend fun connectToServer(hostname: String, port: Int): NetworkClient {
            val selectorManager = ActorSelectorManager(Dispatchers.IO)
            val clientSocket = aSocket(selectorManager).tcp().connect(hostname, port)

            return create(clientSocket)
        }

        fun create(clientSocket: Socket): NetworkClient {
            val dispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
            val dispatchScope = CoroutineScope(dispatcher + CoroutineName("Client network IO"))

            val sendPackets = Channel<WrittenPacket>()
            val receivePackets = Channel<WrittenPacket>()

            clientSocket.launchSendLoop(dispatchScope, sendPackets)
            clientSocket.launchReceiveLoop(dispatchScope, receivePackets)

            return NetworkClient(dispatcher, clientSocket, sendPackets, receivePackets)
        }

        private fun Socket.launchSendLoop(
            scope: CoroutineScope,
            source: ReceiveChannel<WrittenPacket>,
        ) {
            val target = openWriteChannel()

            scope.launch {
                while (!source.isClosedForReceive && !target.isClosedForWrite) {
                    val packet = try {
                        source.receive()
                    } catch (exception: ClosedReceiveChannelException) {
                        break
                    }

                    target.writeVarInt(packet.signature)
                    target.writeVarInt(packet.packetData.remaining.toInt())
                    target.writePacket(packet.packetData)
                }
            }
        }

        private fun Socket.launchReceiveLoop(
            scope: CoroutineScope,
            target: SendChannel<WrittenPacket>,
        ) {
            val source = openReadChannel()

            scope.launch {
                while (!source.isClosedForRead && !target.isClosedForSend) {
                    val id = source.readVarInt()
                    val size = source.readVarInt()
                    val packet = source.readPacket(size)

                    target.send(WrittenPacket(id, packet))
                }
            }
        }
    }
}
