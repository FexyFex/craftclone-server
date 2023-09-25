package math.datatype.vec

import kotlin.math.sqrt

abstract class TVec3<T: Number>: Vec<T> {
    abstract val x: T
    abstract val y: T
    abstract val z: T

    override val length: Float; get() = sqrt(this.dot(this).toFloat())

    abstract fun plus(other: TVec3<T>): TVec3<T>
    abstract fun minus(other: TVec3<T>): TVec3<T>
    abstract fun times(other: TVec3<T>): TVec3<T>
    abstract fun div(other: TVec3<T>): TVec3<T>

    abstract fun plus(other: Number): TVec3<T>
    abstract fun minus(other: Number): TVec3<T>
    abstract fun times(other: Number): TVec3<T>
    abstract fun div(other: Number): TVec3<T>

    abstract fun dot(other: TVec3<T>): T
}