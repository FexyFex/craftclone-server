import kotlinx.coroutines.runBlocking
import networking.DisconnectReason
import networking.NetworkClient
import networking.WrittenPacket
import networking.packet.PacketDisconnect
import networking.packet.PacketLogIn


fun main() {
    clientMain()
}

fun clientMain() = runBlocking {
    val server = NetworkClient.connectToServer("localhost", 25567)

    // helo this is client
    val packetData = PacketLogIn.writePacket(PacketLogIn.HumanReadableData("Fexus"))
    val sPacket = WrittenPacket(PacketLogIn.signature, packetData)
    server.sendPacket(sPacket)

    val disconnectPacketData = PacketDisconnect.HumanReadableData(DisconnectReason.LOGOUT)
    val writtenPacket = WrittenPacket(PacketDisconnect.signature, PacketDisconnect.writePacket(disconnectPacketData))
    server.sendPacket(writtenPacket)

    // sometime later:
    server.close()
}