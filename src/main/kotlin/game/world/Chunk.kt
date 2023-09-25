package game.world

import math.datatype.*
import math.datatype.vec.DVec3
import math.datatype.vec.IVec3
import math.datatype.vec.Vec3


class Chunk(val world: World, val position: IVec3) {
    var blocks: Array<Array<ShortArray>>? = null //Array(extent) { Array(extent) { ShortArray(extent) { 0 } } }
    var light: Array<Array<ShortArray>>? = null
    var hasBlocks = false
    var defaultLight = 0b1111_0000_0000_0000.toShort()//0b1111_1111_0111_1111.toShort()
    var dirtyBlocks = false
    var existsOnDisk = false

    var indexInBlockBuffer: Int = -1
    var indexInLightBuffer: Int = -1

    val isGenerated: Boolean; get() = indexInBlockBuffer != -1


    fun createBlocksArray() {
        blocks = Array(extent) { Array(extent) { ShortArray(extent) { 0 } } }
    }

    fun createLightArray() {
        light = Array(extent) { Array(extent) { ShortArray(extent) { defaultLight } } }
    }

    fun setBlocksArray(blocksArray: Array<Array<ShortArray>>?) {
        blocks = blocksArray
        if (blocksArray != null) hasBlocks = true
    }

    fun setLightArray(lightArray: Array<Array<ShortArray>>?) {
        light = lightArray
    }

    fun setBlockAt(pos: IVec3, blockID: Short) {
        if (blocks == null) {
            createLightArray()
            createBlocksArray()
        }
        blocks!![pos.x][pos.y][pos.z] = blockID
        hasBlocks = true
        dirtyBlocks = true
    }

    fun getBlockAt(pos: IVec3): Short {
        return if (blocks == null || !hasBlocks) 0
        else blocks!![pos.x][pos.y][pos.z]
    }

    fun setLightAt(pos: IVec3, lightLevel: Short) {
        if (light == null) {
            createLightArray()
            createBlocksArray()
            hasBlocks = true
        }
        light!![pos.x][pos.y][pos.z] = lightLevel
        dirtyBlocks = true
    }

    fun getLightAt(pos: IVec3): Short {
        return if (light == null) defaultLight
        else light!![pos.x][pos.y][pos.z]
    }

    private fun getBlockFromSurroundingChunks(blockPosInChunk: IVec3, surroundingChunks: MutableMap<IVec3, Chunk>): Short {
        val x = blockPosInChunk.x
        val y = blockPosInChunk.y
        val z = blockPosInChunk.z
        if (x in 0 until extent && y in 0 until extent && z in 0 until extent) {
            return blocks?.get(x)?.get(y)?.get(z) ?: 0.toShort()
        } else {
            val worldPos = position + blockPosInChunk
            val chunkPos = worldPosToChunkPos(worldPos)
            var chunk = surroundingChunks[chunkPos]
            if (chunk == null) {
                chunk = world.getChunkAt(chunkPos)
                if (chunk != null) {
                    surroundingChunks[chunkPos] = chunk
                    return chunk.getBlockAt(worldPosToPosInChunk(blockPosInChunk))
                }
                else {
                    surroundingChunks[chunkPos] = Chunk(world, chunkPos * extent)
                    return 0.toShort()
                }
            }
            else {
                return chunk.getBlockAt(worldPosToPosInChunk(blockPosInChunk))
            }
        }
    }

    operator fun contains(worldPos: IVec3): Boolean {
        val localPos = worldPos - position
        return localPos.x in 0 until extent && localPos.y in 0 until extent && localPos.z in 0 until extent
    }

    companion object {

        /*
        converts a world-space position in blocks to a position in chunks,
        to be used, for example, to get the key of a chunk in the chunk map.
         */
        fun worldPosToChunkPos(worldPos: IVec3): IVec3 {
            return IVec3(worldPos.x/ extent - if (worldPos.x < 0 && worldPos.x % extent != 0) 1 else 0,
                worldPos.y/ extent - if (worldPos.y < 0 && worldPos.y % extent != 0) 1 else 0,
                worldPos.z/ extent - if (worldPos.z < 0 && worldPos.z % extent != 0) 1 else 0)
        }

        fun worldPosToChunkPos(worldPos: DVec3): IVec3 {
            return IVec3(worldPos.x/ extent - if (worldPos.x < 0 || (worldPos.x) % extent == 0.0) 1 else 0,
                    worldPos.y/ extent - if (worldPos.y < 0 || (worldPos.y) % extent == 0.0) 1 else 0,
                    worldPos.z/ extent - if (worldPos.z < 0 || (worldPos.z) % extent == 0.0) 1 else 0)
        }

        fun worldPosToChunkPos(worldPos: Vec3): IVec3 {
            return IVec3(worldPos.x/ extent - if (worldPos.x < 0 || (worldPos.x) % extent == 0f) 1 else 0,
                    worldPos.y/ extent - if (worldPos.y < 0 || (worldPos.y) % extent == 0f) 1 else 0,
                    worldPos.z/ extent - if (worldPos.z < 0 || (worldPos.z) % extent == 0f) 1 else 0)
        }

        /*
        converts a world-space position to a position within a chunk.
        all coordinates of the pos-in-chunk lie between 0 (inclusive) and Chunk.extent (exclusive)
         */
        fun worldPosToPosInChunk(worldPos: IVec3): IVec3 {
            var blockPosX = worldPos.x % extent
            var blockPosY = worldPos.y % extent
            var blockPosZ = worldPos.z % extent
            if (blockPosX < 0) blockPosX += extent
            if (blockPosY < 0) blockPosY += extent
            if (blockPosZ < 0) blockPosZ += extent
            return IVec3(blockPosX, blockPosY, blockPosZ)
        }

        fun worldPosToPosInChunk(worldPos: DVec3): Vec3 {
            var blockPosX = worldPos.x % extent
            var blockPosY = worldPos.y % extent
            var blockPosZ = worldPos.z % extent
            if (blockPosX < 0) blockPosX += extent
            if (blockPosY < 0) blockPosY += extent
            if (blockPosZ < 0) blockPosZ += extent
            return Vec3(blockPosX, blockPosY, blockPosZ)
        }

        const val extent: Int = 16
        const val blocksPerChunk: Int = extent * extent * extent
    }
}