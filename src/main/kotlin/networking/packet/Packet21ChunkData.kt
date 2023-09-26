package networking.packet

import game.world.Chunk
import io.ktor.utils.io.core.*
import math.datatype.vec.IVec3
import networking.readVarInt
import networking.writeVarInt
import util.repeatCubic3D


data object Packet21ChunkData: Packet() {
    override val signature: Int = 21


    override fun readPacket(packet: ByteReadPacket): HumanReadableData {
        val chunkPosX = packet.readVarInt()
        val chunkPosY = packet.readVarInt()
        val chunkPosZ = packet.readVarInt()

        val blocks = Array(Chunk.extent) { Array(Chunk.extent) { ShortArray(Chunk.extent) { 0 } } }
        val light = Array(Chunk.extent) { Array(Chunk.extent) { ShortArray(Chunk.extent) { 0 } } }

        repeatCubic3D(Chunk.extent) { x, y, z ->
            blocks[x][y][z] = packet.readShort()
        }

        repeatCubic3D(Chunk.extent) { x, y, z ->
            light[x][y][z] = packet.readShort()
        }

        return HumanReadableData(IVec3(chunkPosX, chunkPosY, chunkPosZ), blocks, light)
    }

    override fun <T : Packet.HumanReadableData> writePacket(data: T): ByteReadPacket {
        data as HumanReadableData

        val packet = buildPacket {
            writeVarInt(data.chunkPos.x)
            writeVarInt(data.chunkPos.y)
            writeVarInt(data.chunkPos.z)

            repeatCubic3D(Chunk.extent) { x, y, z ->
                writeShort(data.blocks[x][y][z])
            }
            repeatCubic3D(Chunk.extent) { x, y, z ->
                writeShort(data.light[x][y][z])
            }
        }

        return packet
    }


    data class HumanReadableData(
        val chunkPos: IVec3,
        val blocks: Array<Array<ShortArray>>,
        val light: Array<Array<ShortArray>>
    ): Packet.HumanReadableData() {
        init {
            if (blocks.size != arrLength || light.size != arrLength)
                throw IllegalArgumentException("Arrays must have size $arrLength but are ${blocks.size} and ${light.size}")
        }

        companion object {
            const val arrLength = Chunk.blocksPerChunk
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as HumanReadableData

            if (chunkPos != other.chunkPos) return false
            if (!blocks.contentDeepEquals(other.blocks)) return false
            return light.contentDeepEquals(other.light)
        }

        override fun hashCode(): Int {
            var result = chunkPos.hashCode()
            result = 31 * result + blocks.contentDeepHashCode()
            result = 31 * result + light.contentDeepHashCode()
            return result
        }
    }
}