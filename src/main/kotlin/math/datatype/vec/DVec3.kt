package math.datatype.vec

data class DVec3(override var x: Double, override var y: Double, override var z: Double): TVec3<Double>() {
    constructor(s: Double): this(s,s,s)

    override fun plus(other: TVec3<Double>): TVec3<Double> = DVec3(this.x + other.x, this.y + other.y, this.z + other.z)
    override fun minus(other: TVec3<Double>): TVec3<Double> = DVec3(this.x - other.x, this.y - other.y, this.z - other.z)
    override fun times(other: TVec3<Double>): TVec3<Double> = DVec3(this.x * other.x, this.y * other.y, this.z * other.z)
    override fun div(other: TVec3<Double>): TVec3<Double> = DVec3(this.x / other.x, this.y / other.y, this.z / other.z)
}
