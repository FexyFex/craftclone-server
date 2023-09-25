package math.datatype.vec

data class IVec4(override var x: Int, override var y: Int, override var z: Int, override val w: Int): TVec4<Int>() {
    constructor(s: Int): this(s,s,s,s)

    override fun plus(other: TVec4<Int>): TVec4<Int> = IVec4(this.x + other.x, this.y + other.y, this.z + other.z, this.w + other.w)
    override fun minus(other: TVec4<Int>): TVec4<Int> = IVec4(this.x - other.x, this.y - other.y, this.z - other.z, this.w - other.w)
    override fun times(other: TVec4<Int>): TVec4<Int> = IVec4(this.x * other.x, this.y * other.y, this.z * other.z, this.w * other.w)
    override fun div(other: TVec4<Int>): TVec4<Int> = IVec4(this.x / other.x, this.y / other.y, this.z / other.z, this.w / other.w)
}
