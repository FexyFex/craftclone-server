package math.datatype.vec

data class DVec2(override var x: Double, override var y: Double): TVec2<Double>() {
    constructor(s: Double): this(s,s)

    override fun plus(other: TVec2<Double>): TVec2<Double> = DVec2(this.x + other.x, this.y + other.y)
    override fun minus(other: TVec2<Double>): TVec2<Double> = DVec2(this.x - other.x, this.y - other.y)
    override fun times(other: TVec2<Double>): TVec2<Double> = DVec2(this.x * other.x, this.y * other.y)
    override fun div(other: TVec2<Double>): TVec2<Double> = DVec2(this.x / other.x, this.y / other.y)
}
