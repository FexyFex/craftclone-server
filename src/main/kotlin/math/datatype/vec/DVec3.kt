package math.datatype.vec

data class DVec3(override var x: Double, override var y: Double, override var z: Double): TVec3<Double>() {
    constructor(s: Double): this(s,s,s)

    override fun plus(other: TVec3<Double>): TVec3<Double> = DVec3(this.x + other.x, this.y + other.y, this.z + other.z)
    override fun minus(other: TVec3<Double>): TVec3<Double> = DVec3(this.x - other.x, this.y - other.y, this.z - other.z)
    override fun times(other: TVec3<Double>): TVec3<Double> = DVec3(this.x * other.x, this.y * other.y, this.z * other.z)
    override fun div(other: TVec3<Double>): TVec3<Double> = DVec3(this.x / other.x, this.y / other.y, this.z / other.z)

    override operator fun plus(other: Number): DVec3 {
        val num = other.toFloat()
        return DVec3(this.x + num, this.y + num, this.z + num)
    }
    override operator fun minus(other: Number): DVec3 {
        val num = other.toFloat()
        return DVec3(this.x - num, this.y - num, this.z - num)
    }
    override operator fun times(other: Number): DVec3 {
        val num = other.toFloat()
        return DVec3(this.x * num, this.y * num, this.z * num)
    }
    override operator fun div(other: Number): DVec3 {
        val num = other.toFloat()
        return DVec3(this.x / num, this.y / num, this.z / num)
    }

    override operator fun unaryMinus(): DVec3 = DVec3(-x, -y, -z)

    override fun dot(other: TVec3<Double>): Double = this.x * other.x + this.y * other.y + this.z * other.z
}
