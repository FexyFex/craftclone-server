import kotlinx.coroutines.runBlocking
import networking.*
import networking.packet.Packet255Disconnect
import networking.packet.Packet1LogIn
import networking.packet.Packet2AllowLogIn
import networking.packet.Packet3RejectLogIn


fun main() {
    clientMain()
}

fun clientMain() = runBlocking {
    val server = NetworkClient.connectToServer("localhost", 25567)

    val packetData = Packet1LogIn.writePacket(Packet1LogIn.HumanReadableData("Fexus"))
    val sPacket = WrittenPacket(Packet1LogIn.signature, packetData)
    server.sendPacket(sPacket)

    if (!serverAllowsLogin(server)) {
        server.close()
        return@runBlocking
    }

    val disconnectPacketData = Packet255Disconnect.HumanReadableData(DisconnectReason.LOGOUT)
    val writtenPacket = WrittenPacket(Packet255Disconnect.signature, Packet255Disconnect.writePacket(disconnectPacketData))
    server.sendPacket(writtenPacket)

    server.close()
}


suspend fun serverAllowsLogin(server: NetworkClient): Boolean {
    // Await allow or rejection packet
    val writtenPacket = server.receivePacket()
    val packet = PacketIndex.getPacketBySignature(writtenPacket.signature) ?: throw UnknownPacketException(writtenPacket.signature)

    if (packet == Packet3RejectLogIn) {
        val data = Packet3RejectLogIn.readPacket(writtenPacket.packetData)
        println("Login rejected. Reason: ${data.reason.name}")
        return false
    }

    if (packet == Packet2AllowLogIn) {
        println("Login accepted!")
        return true
    }

    throw UnexpectedPacketException(packet)
}