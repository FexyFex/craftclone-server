package networking.packet

import io.ktor.utils.io.core.*
import networking.writeVarInt


data object PacketLogIn: Packet() {
    override val signature: Int = 1
    override val layout = Layout


    override fun readPacket(packet: ByteReadPacket): HumanReadableData {
        val usernameLength = packet.readByte()
        var username = ""
        for (i in 0 until usernameLength) {
            username += packet.readShort().toInt().toChar()
        }
        return HumanReadableData(usernameLength, username)
    }

    override fun <T: Packet.HumanReadableData> writePacket(data: T): ByteReadPacket {
        data as HumanReadableData

        val packet = buildPacket {
            writeVarInt(signature)
            writeByte(data.usernameLength)

            for (i in 0 until data.usernameLength) {
                val char = data.username[i]
                val charCode = char.code.toShort()
                writeShort(charCode)
            }
        }

        return packet
    }


    data class HumanReadableData(val usernameLength: Byte, val username: String): Packet.HumanReadableData()

    object Layout: Packet.Layout() {
        override val fields: Array<Field> = arrayOf(
            FieldStatic("signature", 0, Short.SIZE_BYTES),
            FieldStatic("usernameLength", Short.SIZE_BYTES, Byte.SIZE_BYTES),
            FieldVariableLength("username", Char.SIZE_BYTES * 1, Char.SIZE_BYTES * 20)
        )
    }
}