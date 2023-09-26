package util


fun Short.toUByte() = toByte()
fun Short.toUInt() = toInt() and 0xffff
fun Short.toULong() = toUInt().toLong()

infix fun Short.ushr(b: Int): Short = (this.toUInt() ushr b).toShort()