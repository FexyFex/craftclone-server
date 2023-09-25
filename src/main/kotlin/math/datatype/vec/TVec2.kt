package math.datatype.vec

import kotlin.math.sqrt

abstract class TVec2<T: Number>: Vec<T> {
    abstract val x: T
    abstract val y: T

    override val length: Float; get() = sqrt(this.dot(this).toFloat())

    abstract fun plus(other: TVec2<T>): TVec2<T>
    abstract fun minus(other: TVec2<T>): TVec2<T>
    abstract fun times(other: TVec2<T>): TVec2<T>
    abstract fun div(other: TVec2<T>): TVec2<T>

    abstract fun plus(other: Number): TVec2<T>
    abstract fun minus(other: Number): TVec2<T>
    abstract fun times(other: Number): TVec2<T>
    abstract fun div(other: Number): TVec2<T>

    abstract fun dot(other: TVec2<T>): T
}