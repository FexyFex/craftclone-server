package networking.packet

import io.ktor.utils.io.core.*
import networking.PacketIndex
import networking.readVarInt


sealed class Packet {
    abstract val signature: Int
    abstract val layout: Layout

    abstract fun readPacket(packet: ByteReadPacket): HumanReadableData
    abstract fun <T: HumanReadableData>writePacket(data: T): ByteReadPacket

    abstract class HumanReadableData
    abstract class Layout {
        abstract val fields: Array<Field>
        val size: Int
            get() = fields.sumOf {
                when (it) {
                    is FieldStatic -> it.size
                    is FieldVariableLength -> it.maxSize
                    else -> 0
                }
            }

        interface Field { val name: String }
        class FieldStatic(override val name: String, val offset: Int, val size: Int): Field
        class FieldVariableLength(override val name: String, val minSize: Int, val maxSize: Int): Field
    }

    companion object {
        fun detect(packet: ByteReadPacket): Packet? {
            val signature: Int = packet.readVarInt()
            return PacketIndex.getPacketBySignature(signature)
        }
    }
}