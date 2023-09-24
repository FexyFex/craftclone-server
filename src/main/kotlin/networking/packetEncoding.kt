package networking

import io.ktor.utils.io.*
import io.ktor.utils.io.core.*

private const val VAR_INT_VALUE_MASK = 0b01111111
private const val VAR_INT_CONTINUE_FLAG = 0b10000000

class VarIntOutOfBoundsException : Exception("VarInt out of bounds")

suspend fun ByteReadChannel.readVarInt(): Int {
    return readVarIntTemplate {
        readByte()
    }
}

suspend fun ByteWriteChannel.writeVarInt(value: Int) {
    writeVarIntTemplate(value) {
        writeByte(it)
    }
}

fun ByteReadPacket.readVarInt(): Int {
    return readVarIntTemplate {
        readByte()
    }
}

fun BytePacketBuilder.writeVarInt(value: Int) {
    writeVarIntTemplate(value) {
        writeByte(it)
    }
}

private inline fun readVarIntTemplate(readByte: () -> Byte): Int {
    var value = 0
    var position = 0

    do {
        val read = readByte()
        value = value or ((read.toInt() and VAR_INT_VALUE_MASK) shl position)
        position += 7

        if (position >= 32) {
            throw VarIntOutOfBoundsException()
        }
    } while (read.toInt() and VAR_INT_CONTINUE_FLAG != 0)

    return value
}

private inline fun writeVarIntTemplate(value: Int, writeByte: (value: Byte) -> Unit) {
    var remaining = value

    while (remaining and VAR_INT_VALUE_MASK.inv() != 0) {
        writeByte(((remaining and VAR_INT_VALUE_MASK) or VAR_INT_CONTINUE_FLAG).toByte())

        remaining = remaining ushr 7
    }

    writeByte(remaining.toByte())
}