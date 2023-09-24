package networking

import networking.packet.Packet


class ClientConnectionThread(private val client: NetworkClient) {
    suspend fun run() {
        while (true) {
            val writtenPacket = client.receivePacket()
            val packet = Packet.detect(writtenPacket.packetData) ?: return
            val data = packet.readPacket(writtenPacket.packetData)
            println(data.toString())
        }
    }
}