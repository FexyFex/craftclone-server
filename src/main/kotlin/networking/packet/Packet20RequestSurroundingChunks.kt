package networking.packet

import io.ktor.utils.io.core.*
import math.datatype.vec.IVec3
import networking.readVarInt
import networking.writeVarInt


data object Packet20RequestSurroundingChunks: Packet() {
    override val signature: Int = 20


    override fun readPacket(packet: ByteReadPacket): HumanReadableData {
        val x = packet.readVarInt()
        val y = packet.readVarInt()
        val z = packet.readVarInt()
        val viewDistance = packet.readVarInt()
        return HumanReadableData(IVec3(x,y,z), viewDistance)
    }

    override fun <T : Packet.HumanReadableData> writePacket(data: T): ByteReadPacket {
        data as HumanReadableData

        val packet = buildPacket {
            writeVarInt(data.chunkPos.x)
            writeVarInt(data.chunkPos.y)
            writeVarInt(data.chunkPos.z)
            writeVarInt(data.viewDistance)
        }

        return packet
    }


    data class HumanReadableData(val chunkPos: IVec3, val viewDistance: Int): Packet.HumanReadableData()
}