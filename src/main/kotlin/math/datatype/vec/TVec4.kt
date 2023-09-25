package math.datatype.vec

import kotlin.math.sqrt


abstract class TVec4<T: Number>: Vec<T> {
    abstract val x: T
    abstract val y: T
    abstract val z: T
    abstract val w: T

    override val length: Float; get() = sqrt(this.dot(this).toFloat())

    abstract fun plus(other: TVec4<T>): TVec4<T>
    abstract fun minus(other: TVec4<T>): TVec4<T>
    abstract fun times(other: TVec4<T>): TVec4<T>
    abstract fun div(other: TVec4<T>): TVec4<T>

    abstract fun plus(other: Number): TVec4<T>
    abstract fun minus(other: Number): TVec4<T>
    abstract fun times(other: Number): TVec4<T>
    abstract fun div(other: Number): TVec4<T>

    abstract fun dot(other: TVec4<T>): T
}