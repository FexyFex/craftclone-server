package math.datatype.vec

data class IVec2(override var x: Int, override var y: Int): TVec2<Int>() {
    constructor(s: Int): this(s,s)

    override fun plus(other: TVec2<Int>): TVec2<Int> = IVec2(this.x + other.x, this.y + other.y)
    override fun minus(other: TVec2<Int>): TVec2<Int> = IVec2(this.x - other.x, this.y - other.y)
    override fun times(other: TVec2<Int>): TVec2<Int> = IVec2(this.x * other.x, this.y * other.y)
    override fun divide(other: TVec2<Int>): TVec2<Int> = IVec2(this.x / other.x, this.y / other.y)
}
