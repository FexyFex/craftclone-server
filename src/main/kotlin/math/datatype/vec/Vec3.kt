package math.datatype.vec

data class Vec3(override var x: Float, override var y: Float, override var z: Float): TVec3<Float>() {
    constructor(s: Float): this(s,s,s)
    constructor(x: Number, y: Number, z: Number): this(x.toFloat(), y.toFloat(), z.toFloat())

    override operator fun plus(other: TVec3<Float>): Vec3 = Vec3(this.x + other.x, this.y + other.y, this.z + other.z)
    override operator fun minus(other: TVec3<Float>): Vec3 = Vec3(this.x - other.x, this.y - other.y, this.z - other.z)
    override operator fun times(other: TVec3<Float>): Vec3 = Vec3(this.x * other.x, this.y * other.y, this.z * other.z)
    override operator fun div(other: TVec3<Float>): Vec3 = Vec3(this.x / other.x, this.y / other.y, this.z / other.z)

    override operator fun plus(other: Number): Vec3 {
        val num = other.toFloat()
        return Vec3(this.x + num, this.y + num, this.z + num)
    }
    override operator fun minus(other: Number): Vec3 {
        val num = other.toFloat()
        return Vec3(this.x - num, this.y - num, this.z - num)
    }
    override operator fun times(other: Number): Vec3 {
        val num = other.toFloat()
        return Vec3(this.x * num, this.y * num, this.z * num)
    }
    override operator fun div(other: Number): Vec3 {
        val num = other.toFloat()
        return Vec3(this.x / num, this.y / num, this.z / num)
    }

    override operator fun unaryMinus(): Vec3 = Vec3(-x, -y, -z)

    override fun dot(other: TVec3<Float>): Float = this.x * other.x + this.y * other.y + this.z * other.z
}
