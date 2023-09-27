package networking.packet

import game.RenderDistance
import io.ktor.utils.io.core.*
import math.datatype.vec.IVec3
import networking.readVarInt
import networking.writeVarInt


data object Packet20RequestSurroundingChunks: Packet() {
    override val signature: Int = 20


    override fun readPacket(packet: ByteReadPacket): HumanReadableData {
        val horizontal = packet.readVarInt()
        val upwards = packet.readVarInt()
        val downwards = packet.readVarInt()
        return HumanReadableData(RenderDistance(horizontal, upwards, downwards))
    }

    override fun <T : Packet.HumanReadableData> writePacket(data: T): ByteReadPacket {
        data as HumanReadableData

        val packet = buildPacket {
            writeVarInt(data.renderDistance.horizontal)
            writeVarInt(data.renderDistance.upwards)
            writeVarInt(data.renderDistance.downwards)
        }

        return packet
    }


    data class HumanReadableData(val renderDistance: RenderDistance): Packet.HumanReadableData()
}