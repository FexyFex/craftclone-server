package game.world.util

import math.datatype.vec.IVec2
import math.datatype.vec.IVec3
import util.for3D
import util.size


class ChunkBounds(private val xRange: IntRange, private val yRange: IntRange, private val zRange: IntRange) {
    constructor(minChunk: IVec3, maxChunk: IVec3): this(
        IntRange(minChunk.x, maxChunk.x),
        IntRange(minChunk.y, maxChunk.y),
        IntRange(minChunk.z, maxChunk.z)
    )

    fun getMinChunk(): IVec3 {
        return IVec3(xRange.first, yRange.first, zRange.first)
    }

    fun getMaxChunk(): IVec3 {
        return IVec3(xRange.last, yRange.last, zRange.last)
    }

    operator fun contains(chunkPos: IVec3): Boolean =
        chunkPos.x in xRange && chunkPos.y in yRange && chunkPos.z in zRange

    operator fun contains(chunkPos: IVec2): Boolean =
        chunkPos.x in xRange && chunkPos.y in zRange

    fun forEachChunk(action: (IVec3) -> Unit) {
        for3D(xRange, yRange, zRange) { x,y,z ->
            action(IVec3(x,y,z))
        }
    }

    override operator fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (this === other) return true
        if (other !is ChunkBounds) return false
        return this.xRange == other.xRange && this.yRange == other.yRange && this.zRange == other.zRange
    }

    operator fun compareTo(other: ChunkBounds): Int {
        val xDiff = this.xRange.size - other.xRange.size
        val yDiff = this.yRange.size - other.yRange.size
        val zDiff = this.zRange.size - other.zRange.size

        // All dimensions of this must be greater or lesser than those of other.
        if (xDiff > 0 && yDiff > 0 && zDiff > 0) return 1
        if (xDiff < 0 && yDiff < 0 && zDiff < 0) return -1
        throw Exception("Please do not compare bounds like that. All dimensions of this must be greater or lesser than those of other")
    }


    override fun hashCode(): Int {
        var result = xRange.hashCode()
        result = 31 * result + yRange.hashCode()
        result = 31 * result + zRange.hashCode()
        return result
    }


    companion object {
        fun maxOf(bounds1: ChunkBounds, vararg bounds: ChunkBounds): ChunkBounds {
            var maxB: ChunkBounds = bounds1
            for (b in bounds) if (maxB < b) maxB = b
            return maxB
        }
    }
}