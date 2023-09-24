package networking.packet

import io.ktor.utils.io.core.*
import networking.DisconnectReason
import networking.writeVarInt


data object PacketDisconnect: Packet() {
    override val signature: Int = 101
    override val layout = Layout


    override fun readPacket(packet: ByteReadPacket): HumanReadableData {
        val reasonValue = packet.readByte()
        val reason = DisconnectReason.values().first { it.value == reasonValue }
        return HumanReadableData(reason)
    }

    override fun <T : Packet.HumanReadableData> writePacket(data: T): ByteReadPacket {
        data as HumanReadableData

        val packet = buildPacket {
            writeVarInt(signature)
            writeByte(data.reason.value)
        }

        return packet
    }


    data class HumanReadableData(val reason: DisconnectReason): Packet.HumanReadableData()

    object Layout: Packet.Layout() {
        override val fields: Array<Field> = arrayOf(
            FieldStatic("signature", 0, Short.SIZE_BYTES),
            FieldStatic("reason", Short.SIZE_BYTES, Byte.SIZE_BYTES)
        )
    }
}