package networking

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import java.util.concurrent.Executors


class ClientConnectionThread(private val client: NetworkClient) {



    suspend fun start() {
        val scope = CoroutineScope(dispatcher + CoroutineName("Connection Coroutine"))
        scope.launch {
            beginLoop()
        }
    }

    private suspend fun beginLoop() {
        while (true) {
            val writtenPacket = client.receivePacket()
            val packetType = PacketIndex.getPacketBySignature(writtenPacket.signature) ?: continue
            val data = packetType.readPacket(writtenPacket.packetData)
            println(data.toString())
        }
    }


    companion object {
        private val dispatcher = Executors.newCachedThreadPool().asCoroutineDispatcher()
    }
}