package game.world

import math.datatype.vec.IVec3


class ChunkCache(private val chunkCount: Int, private val world: World) {
    private val cachedChunks = Array<Chunk?>(chunkCount){null}
    private var nextIndex = 0

    fun add(chunk: Chunk) {
        val oldChunk = cachedChunks[nextIndex]
        cachedChunks[nextIndex] = chunk
        if (oldChunk != null)
            world.unloadChunkAt(Chunk.worldPosToChunkPos(oldChunk.position))
        nextIndex = (nextIndex + 1) % cachedChunks.size
    }

    fun getCachedChunkAt(pos: IVec3): Chunk? {
        return cachedChunks.firstOrNull {
            if (it != null) Chunk.worldPosToChunkPos(it.position) == pos
            else false
        }
    }

    fun containsChunkAt(pos: IVec3): Boolean {
        return getCachedChunkAt(pos) != null
    }

    fun getChunksToSave(): MutableList<Chunk> {
        val chunksToSave = mutableListOf<Chunk>()
        for (c in cachedChunks) {
            if (c != null && (c.dirtyBlocks || !c.existsOnDisk)) chunksToSave.add(c)
        }
        return chunksToSave
    }

}