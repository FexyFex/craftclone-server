package networking.packet

import io.ktor.utils.io.core.*
import networking.ConnectionRejectedReason


data object Packet3RejectLogIn: Packet() {
    override val signature: Int = 3


    override fun readPacket(packet: ByteReadPacket): HumanReadableData {
        val reasonByte = packet.readByte()
        val reason = ConnectionRejectedReason.values().firstOrNull { it.byte == reasonByte } ?: ConnectionRejectedReason.UNKNOWN
        return HumanReadableData(reason)
    }

    override fun <T: Packet.HumanReadableData> writePacket(data: T): ByteReadPacket {
        data as HumanReadableData

        val packet = buildPacket {
            writeByte(data.reason.byte)
        }

        return packet
    }


    data class HumanReadableData(val reason: ConnectionRejectedReason): Packet.HumanReadableData()
}