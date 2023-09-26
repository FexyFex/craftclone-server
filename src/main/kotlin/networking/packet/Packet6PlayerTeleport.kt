package networking.packet

import io.ktor.utils.io.core.*
import math.datatype.vec.Vec3


data object Packet6PlayerTeleport: Packet() {
    override val signature: Int = 5


    override fun readPacket(packet: ByteReadPacket): HumanReadableData {
        val velX = packet.readFloat()
        val velY = packet.readFloat()
        val velZ = packet.readFloat()

        return HumanReadableData(Vec3(velX, velY, velZ))
    }

    override fun <T: Packet.HumanReadableData> writePacket(data: T): ByteReadPacket {
        data as HumanReadableData

        val packet = buildPacket {
            writeFloat(data.position.x)
            writeFloat(data.position.y)
            writeFloat(data.position.z)
        }

        return packet
    }


    data class HumanReadableData(val position: Vec3): Packet.HumanReadableData()
}