package game.world

import math.datatype.vec.IVec3
import math.datatype.vec.IVec4
import util.ushr
import kotlin.experimental.and


class World(val seed: Int) {
    // TODO: alles

    fun getChunkAt(chunkPos: IVec3): Chunk {
        return Chunk(this, chunkPos)
    }

    fun getLightAt(pos: IVec3): IVec4 {
        return IVec4(0)
    }

    fun lightFromShortToIVec4(light: Short): IVec4 {
        val r = (light and 0b0000_0000_0000_1111)
        val g = (light and 0b0000_0000_1111_0000) ushr 4
        val b = (light and 0b0000_1111_0000_0000) ushr 8
        val s = (light and 0b1111_0000_0000_0000.toShort()) ushr 12
        return IVec4(r, g, b, s)
    }

    fun lightFromIVec4ToShort(light: IVec4): Short {
        return (light.r or (light.g shl 4) or (light.b shl 8) or (light.a shl 12)).toShort()
    }

    fun updateSkyLightQueue(neighbors: List<IVec3>, thing: Boolean) {

    }
}