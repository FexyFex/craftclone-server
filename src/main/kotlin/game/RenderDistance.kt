package game

import math.datatype.vec.IVec3
import math.datatype.vec.Vec3


data class RenderDistance(var horizontal: Int, var upwards: Int, var downwards: Int) {
    fun toVec3() = Vec3(horizontal, upwards, downwards)
    fun toVec3i() = IVec3(horizontal, upwards, downwards)
}
