package networking

import io.ktor.utils.io.core.*


data class WrittenPacket(val signature: Int, val packetData: ByteReadPacket)
