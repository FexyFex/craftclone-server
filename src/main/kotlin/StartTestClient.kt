import kotlinx.coroutines.runBlocking
import networking.DisconnectReason
import networking.NetworkClient
import networking.WrittenPacket
import networking.packet.Packet255Disconnect
import networking.packet.Packet1LogIn


fun main() {
    clientMain()
}

fun clientMain() = runBlocking {
    val server = NetworkClient.connectToServer("localhost", 25567)

    Thread.sleep(1000)

    // helo this is client
    val packetData = Packet1LogIn.writePacket(Packet1LogIn.HumanReadableData("Fexus"))
    val sPacket = WrittenPacket(Packet1LogIn.signature, packetData)
    server.sendPacket(sPacket)

    Thread.sleep(1000)

    val disconnectPacketData = Packet255Disconnect.HumanReadableData(DisconnectReason.LOGOUT)
    val writtenPacket = WrittenPacket(Packet255Disconnect.signature, Packet255Disconnect.writePacket(disconnectPacketData))
    server.sendPacket(writtenPacket)

    // sometime later:
    //server.close()
}