package networking.packet

import io.ktor.utils.io.core.*
import networking.PacketIndex
import networking.readVarInt


sealed class Packet {
    abstract val signature: Int

    abstract fun readPacket(packet: ByteReadPacket): HumanReadableData
    abstract fun <T: HumanReadableData>writePacket(data: T): ByteReadPacket

    abstract class HumanReadableData
}