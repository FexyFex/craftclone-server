package networking.packet

import io.ktor.utils.io.core.*
import math.datatype.vec.Vec2
import math.datatype.vec.Vec3


data object Packet7PlayerRotate: Packet() {
    override val signature: Int = 7


    override fun readPacket(packet: ByteReadPacket): HumanReadableData {
        val rotX = packet.readFloat()
        val rotY = packet.readFloat()

        return HumanReadableData(Vec2(rotX, rotY))
    }

    override fun <T: Packet.HumanReadableData> writePacket(data: T): ByteReadPacket {
        data as HumanReadableData

        val packet = buildPacket {
            writeFloat(data.rotation.x)
            writeFloat(data.rotation.y)
        }

        return packet
    }


    data class HumanReadableData(val rotation: Vec2): Packet.HumanReadableData()
}