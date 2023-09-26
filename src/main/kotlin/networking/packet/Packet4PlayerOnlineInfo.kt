package networking.packet

import io.ktor.utils.io.core.*
import networking.readVarInt
import networking.writeVarInt


data object Packet4PlayerOnlineInfo: Packet() {
    override val signature: Int = 7


    override fun readPacket(packet: ByteReadPacket): HumanReadableData {
        val playerNameLength = packet.readVarInt()
        var playerName = ""
        for (i in 0 until playerNameLength) {
            playerName += packet.readShort().toInt().toChar()
        }
        val ping = packet.readVarInt()

        return HumanReadableData(playerName, ping)
    }

    override fun <T: Packet.HumanReadableData> writePacket(data: T): ByteReadPacket {
        data as HumanReadableData

        val packet = buildPacket {
            writeVarInt(data.playerNameLength)

            for (i in 0 until data.playerNameLength) {
                val char = data.playerName[i]
                val charCode = char.code.toShort()
                writeShort(charCode)
            }

            writeVarInt(data.ping)
        }

        return packet
    }


    data class HumanReadableData(val playerName: String, val ping: Int, val playerNameLength: Int = playerName.length): Packet.HumanReadableData()
}