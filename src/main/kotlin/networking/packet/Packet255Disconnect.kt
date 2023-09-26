package networking.packet

import io.ktor.utils.io.core.*
import networking.DisconnectReason


data object Packet255Disconnect: Packet() {
    override val signature: Int = 255


    override fun readPacket(packet: ByteReadPacket): HumanReadableData {
        val reasonValue = packet.readByte()
        val reason = DisconnectReason.values().first { it.value == reasonValue }
        return HumanReadableData(reason)
    }

    override fun <T : Packet.HumanReadableData> writePacket(data: T): ByteReadPacket {
        data as HumanReadableData

        val packet = buildPacket {
            writeByte(data.reason.value)
        }

        return packet
    }


    data class HumanReadableData(val reason: DisconnectReason): Packet.HumanReadableData()
}