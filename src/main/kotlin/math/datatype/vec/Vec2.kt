package math.datatype.vec

data class Vec2(override var x: Float, override var y: Float): TVec2<Float>() {
    constructor(s: Float): this(s,s)

    override fun plus(other: TVec2<Float>): TVec2<Float> = Vec2(this.x + other.x, this.y + other.y)
    override fun minus(other: TVec2<Float>): TVec2<Float> = Vec2(this.x - other.x, this.y - other.y)
    override fun times(other: TVec2<Float>): TVec2<Float> = Vec2(this.x * other.x, this.y * other.y)
    override fun divide(other: TVec2<Float>): TVec2<Float> = Vec2(this.x / other.x, this.y / other.y)
}
