package game

import kotlinx.coroutines.*
import math.datatype.transform.DTransform3
import math.datatype.vec.DVec3
import networking.*
import networking.packet.Packet
import networking.packet.Packet1LogIn
import java.util.concurrent.Executors


object CraftCloneServer {
    private var serverShouldRun: Boolean = true

    private val connections = mutableListOf<ClientConnectionThread>()
    private val players = mutableListOf<RemotePlayer>()


    fun bindAndLaunch() = runBlocking {
        val server = NetworkServer.bind("localhost", 25567, CentralServerConnectionHandler())
        startCentralServerLoop()
    }

    private fun startCentralServerLoop() {

    }


    class CentralServerConnectionHandler: NetworkServerConnectionHandler {
        override suspend fun clientConnected(client: NetworkClient) {
            // This is called when a client connects
            val connection = ClientConnectionThread(client)
            connections.add(connection)
            connection.start()
        }
    }

    class ClientConnectionThread(private val client: NetworkClient) {
        private lateinit var playerName: String
        var isRunning: Boolean = true

        suspend fun start() {
            val scope = CoroutineScope(dispatcher + CoroutineName("Client Connection"))
            scope.launch {
                val loggedIn = awaitLoginPacket()
                if (loggedIn) beginAcceptingPackets()
                isRunning = false
            }
        }

        private suspend fun awaitLoginPacket(): Boolean {
            // TODO: Timeouts!
            while (true) {
                val writtenPacket = client.receivePacket()
                val packetType = PacketIndex.getPacketBySignature(writtenPacket.signature) ?: continue
                if (packetType != Packet1LogIn) continue

                val data = packetType.readPacket(writtenPacket.packetData) as Packet1LogIn.HumanReadableData

                this.playerName = data.username
                println("Player ${data.username} logged on!")
                // TODO: load player transform
                val transform = DTransform3(DVec3(0.0), DVec3(0.0))
                players.add(RemotePlayer(playerName).placeInWorld(transform))
                return true
            }
            return false
        }

        private suspend fun beginAcceptingPackets() {
            println("successful login. now accepting packets...")
            while (true) {
                val writtenPacket = client.receivePacket()
                val packetType = PacketIndex.getPacketBySignature(writtenPacket.signature) ?: continue
                val data = packetType.readPacket(writtenPacket.packetData)
                handlePacket(packetType, data)
            }
        }

        private fun handlePacket(packetType: Packet, packetData: Packet.HumanReadableData) {

        }


        companion object {
            private val dispatcher = Executors.newCachedThreadPool().asCoroutineDispatcher()
        }
    }
}