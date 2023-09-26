package game

import game.world.World
import kotlinx.coroutines.*
import math.datatype.transform.Transform
import math.datatype.vec.DVec3
import math.datatype.vec.IVec3
import networking.*
import networking.packet.*
import java.util.concurrent.Executors


object CraftCloneServer {
    private var serverShouldRun: Boolean = true

    private val connections = mutableListOf<ClientConnectionThread>()
    private val players = mutableListOf<RemotePlayer>()

    private val world = World((((Math.random() * 2.0) - 1.0) * Int.MAX_VALUE).toInt())


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
        private var shouldRun: Boolean = true
        var isRunning: Boolean = true

        suspend fun start() {
            val scope = CoroutineScope(dispatcher + CoroutineName("Client Connection"))
            scope.launch {
                val loggedIn = awaitLoginPacket()
                if (loggedIn) {
                    println("Player $playerName logged on!")
                    sendServerInfo()
                    beginAcceptingPackets()
                }
                client.close()
                isRunning = false
                println("Goodbye $playerName")
            }
        }

        private suspend fun awaitLoginPacket(): Boolean {
            // TODO: Timeouts!
            while (shouldRun) {
                val writtenPacket = client.receivePacket()
                val packetType = PacketIndex.getPacketBySignature(writtenPacket.signature) ?: continue
                if (packetType != Packet1LogIn) continue

                val data = packetType.readPacket(writtenPacket.packetData) as Packet1LogIn.HumanReadableData

                this.playerName = data.username
                if (players.firstOrNull { it.name == playerName } != null) {
                    println("Player with that name already online!")
                    return false
                }
                val okPacket = Packet2AllowLogIn.writePacket()
                client.sendPacket(WrittenPacket(Packet2AllowLogIn.signature, okPacket))
                // TODO: load player transform
                val transform = Transform(DVec3(0.0), DVec3(0.0))
                players.add(RemotePlayer(playerName).placeInWorld(transform))
                return true
            }
            return false
        }

        private suspend fun sendServerInfo() {
            // Send all online players
            players.forEach {
                val playerOnlinePacketData = Packet4PlayerOnlineInfo.HumanReadableData(it.name, 420)
                val packet = Packet4PlayerOnlineInfo.writePacket(playerOnlinePacketData)
                client.sendPacket(WrittenPacket(Packet4PlayerOnlineInfo.signature, packet))
            }
            // TODO: Send chunks
        }

        private suspend fun beginAcceptingPackets() {
            while (shouldRun) {
                val writtenPacket = client.receivePacket()
                val packetType = PacketIndex.getPacketBySignature(writtenPacket.signature) ?: continue
                val data = packetType.readPacket(writtenPacket.packetData)
                handlePacket(packetType, data)
            }
        }

        private fun handlePacket(packetType: Packet, packetData: Packet.HumanReadableData) {
            when (packetType) {
                Packet255Disconnect -> shouldRun = false
                Packet20RequestSurroundingChunks -> requestChunk((packetData as Packet20RequestSurroundingChunks.HumanReadableData).chunkPos)
                else -> throw IllegalArgumentException("huh")
            }
        }

        private fun requestChunk(chunkPos: IVec3) {

        }


        companion object {
            private val dispatcher = Executors.newCachedThreadPool().asCoroutineDispatcher()
        }
    }
}