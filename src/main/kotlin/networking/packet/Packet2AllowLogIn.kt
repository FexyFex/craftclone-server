package networking.packet

import io.ktor.utils.io.core.*


data object Packet2AllowLogIn: Packet() {
    override val signature: Int = 2


    override fun readPacket(packet: ByteReadPacket): HumanReadableData {
        val ok = packet.readByte()
        return HumanReadableData(ok)
    }

    fun writePacket() = buildPacket { writeByte(1) }
    override fun <T: Packet.HumanReadableData> writePacket(data: T): ByteReadPacket {
        data as HumanReadableData

        val packet = buildPacket {
            writeByte(1)
        }

        return packet
    }


    data class HumanReadableData(val ok: Byte): Packet.HumanReadableData()
}