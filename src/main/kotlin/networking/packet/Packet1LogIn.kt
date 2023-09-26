package networking.packet

import io.ktor.utils.io.core.*


data object Packet1LogIn: Packet() {
    override val signature: Int = 1


    override fun readPacket(packet: ByteReadPacket): HumanReadableData {
        val usernameLength = packet.readByte()
        var username = ""
        for (i in 0 until usernameLength) {
            username += packet.readShort().toInt().toChar()
        }
        return HumanReadableData(username)
    }

    override fun <T: Packet.HumanReadableData> writePacket(data: T): ByteReadPacket {
        data as HumanReadableData

        val packet = buildPacket {
            writeByte(data.usernameLength)

            for (i in 0 until data.usernameLength) {
                val char = data.username[i]
                val charCode = char.code.toShort()
                writeShort(charCode)
            }
        }

        return packet
    }


    data class HumanReadableData(val username: String, val usernameLength: Byte = username.length.toByte()): Packet.HumanReadableData()
}