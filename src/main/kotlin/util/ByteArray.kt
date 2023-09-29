package util

import org.lwjgl.system.MemoryUtil.memAlloc
import java.nio.ByteBuffer

val Byte.Companion.BYTES: Int
    get() = java.lang.Byte.BYTES
val ByteBuffer.size
    get() = capacity() * Byte.BYTES

fun Double.Companion.longBitsToDouble(bits: Long) = java.lang.Double.longBitsToDouble(bits)
fun Float.Companion.intBitsToFloat(bits: Int) = java.lang.Float.intBitsToFloat(bits)

fun ByteArray.getInt(index: Int, bigEndian: Boolean = true) =
    if (bigEndian) this[index].toInt() and 0xFF or
            (this[index + 1].toInt() and 0xFF shl 8) or
            (this[index + 2].toInt() and 0xFF shl 16) or
            (this[index + 3].toInt() and 0xFF shl 24)
    else this[index + 3].toInt() and 0xFF or
            (this[index + 2].toInt() and 0xFF shl 8) or
            (this[index + 1].toInt() and 0xFF shl 16) or
            (this[index].toInt() and 0xFF shl 24)

fun ByteArray.getDouble(index: Int, bigEndian: Boolean = true) = Double.longBitsToDouble(
    if (bigEndian) this[index].toLong() and 0xFF or
            (this[index + 1].toLong() and 0xFF shl 8) or
            (this[index + 2].toLong() and 0xFF shl 16) or
            (this[index + 3].toLong() and 0xFF shl 24) or
            (this[index + 4].toLong() and 0xFF shl 32) or
            (this[index + 5].toLong() and 0xFF shl 40) or
            (this[index + 6].toLong() and 0xFF shl 48) or
            (this[index + 7].toLong() and 0xFF shl 56)
    else this[index + 7].toLong() and 0xFF or
            (this[index + 6].toLong() and 0xFF shl 8) or
            (this[index + 5].toLong() and 0xFF shl 16) or
            (this[index + 4].toLong() and 0xFF shl 24) or
            (this[index + 3].toLong() and 0xFF shl 32) or
            (this[index + 2].toLong() and 0xFF shl 40) or
            (this[index + 1].toLong() and 0xFF shl 48) or
            (this[index].toLong() and 0xFF shl 56))

fun ByteArray.getFloat(index: Int, bigEndian: Boolean = true) = Float.intBitsToFloat(
    if (bigEndian) this[index].toInt() and 0xFF or
            (this[index + 1].toInt() and 0xFF shl 8) or
            (this[index + 2].toInt() and 0xFF shl 16) or
            (this[index + 3].toInt() and 0xFF shl 24)
    else this[index + 3].toInt() and 0xFF or
            (this[index + 2].toInt() and 0xFF shl 8) or
            (this[index + 1].toInt() and 0xFF shl 16) or
            (this[index].toInt() and 0xFF shl 24)
)

fun ByteBuffer.toByteArray(): ByteArray {
    val array = ByteArray(this.size)
    for (i in 0 until this.size) {
        array[i] = this[i]
    }
    return array
}

fun Int.toByteArray() : ByteArray {
    val buffer = memAlloc(Integer.BYTES)
    buffer.putInt(0,this)
    return buffer.toByteArray()
}
fun Double.toByteArray(): ByteArray {
    return ByteBuffer.allocate(Long.SIZE_BYTES)
        .putLong(this.toBits()).toByteArray()
}