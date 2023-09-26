package game.world.saving

import game.world.Chunk
import math.datatype.vec.IVec2
import math.datatype.vec.IVec3


class Region (val regionPosition: IVec3) {
    val chunkPosition = regionPosition * extent
    val worldPosition = chunkPosition * Chunk.extent

    /*
    is the given chunk position within the bounds of this Region?
     */
    private operator fun contains(chunkPos: IVec3): Boolean {
        val chunkPosInRegion = chunkPosToChunkPosInRegion(chunkPos)
        return chunkPosInRegion.x in 0 until extent && chunkPosInRegion.y in 0 until extent && chunkPosInRegion.z in 0 until extent
    }
    private operator fun contains(chunkPos: IVec2): Boolean {
        val chunkPosInRegion = chunkPosToChunkPosInRegion(IVec3(chunkPos.x, 0, chunkPos.y))
        return chunkPosInRegion.x in 0 until extent && chunkPosInRegion.z in 0 until extent
    }

    companion object {
        const val extent: Int = 16
        const val chunksPerRegion: Int = extent * extent * extent

        fun chunkPosToRegionPos(chunkPos: IVec3): IVec3 {
            return IVec3(chunkPos.x/extent - if (chunkPos.x < 0 && chunkPos.x % extent != 0) 1 else 0,
                chunkPos.y/extent - if (chunkPos.y < 0 && chunkPos.y % extent != 0) 1 else 0,
                chunkPos.z/extent - if (chunkPos.z < 0 && chunkPos.z % extent != 0) 1 else 0)
        }
        fun worldPosToRegionPos(worldPos: IVec3): IVec3 {
            return chunkPosToRegionPos(Chunk.worldPosToChunkPos(worldPos))
        }

        fun chunkPosToChunkPosInRegion(chunkPos: IVec3): IVec3 {
            var inRegionPosX = chunkPos.x % extent
            var inRegionPosY = chunkPos.y % extent
            var inRegionPosZ = chunkPos.z % extent
            if (inRegionPosX < 0) inRegionPosX += extent
            if (inRegionPosY < 0) inRegionPosY += extent
            if (inRegionPosZ < 0) inRegionPosZ += extent
            return IVec3(inRegionPosX, inRegionPosY, inRegionPosZ)
        }

        fun worldPosToChunkPosInRegion(worldPos: IVec3): IVec3 {
            return chunkPosToChunkPosInRegion(Chunk.worldPosToChunkPos(worldPos))
        }

        fun pos3Dto2D(pos: IVec3): IVec2 {
            return IVec2(pos.x, pos.z)
        }

        fun pos2Dto3D(pos: IVec2): IVec3 {
            return IVec3(pos.x, 0, pos.y)
        }
    }
}