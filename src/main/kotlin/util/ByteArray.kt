package util

import org.lwjgl.system.MemoryUtil.memAlloc
import java.nio.ByteBuffer

val Byte.Companion.BYTES: Int
    get() = java.lang.Byte.BYTES
val ByteBuffer.size
    get() = capacity() * Byte.BYTES
fun ByteArray.getInt(index: Int, bigEndian: Boolean = true) =
    if (bigEndian) this[index].toInt() and 0xFF or
            (this[index + 1].toInt() and 0xFF shl 8) or
            (this[index + 2].toInt() and 0xFF shl 16) or
            (this[index + 3].toInt() and 0xFF shl 24)
    else this[index + 3].toInt() and 0xFF or
            (this[index + 2].toInt() and 0xFF shl 8) or
            (this[index + 1].toInt() and 0xFF shl 16) or
            (this[index].toInt() and 0xFF shl 24)

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
