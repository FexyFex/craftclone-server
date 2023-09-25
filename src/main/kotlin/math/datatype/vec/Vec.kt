package math.datatype.vec

interface Vec<T: Number> {
    val length: Float

    fun unaryMinus(): Vec<T>
}