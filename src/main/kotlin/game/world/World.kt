package game.world

import FileSystem
import game.CraftCloneServer
import game.RemotePlayer
import game.blocks.BlockRegistry
import game.blocks.blocktypes.LightSourceBlockType
import game.world.generation.WorldGenerator
import game.world.saving.PlayerSaver
import game.world.saving.WorldSaver
import game.world.util.ChunkBounds
import math.datatype.vec.IVec2
import math.datatype.vec.IVec3
import math.datatype.vec.IVec4
import util.toByteArray
import util.ushr
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.PriorityBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.experimental.and
import kotlin.math.max
import kotlin.random.Random


class World(var seed : Long) {
    // TODO: shut down, loading and saving methods, list of players, loaded chunks management

    var worldTime: Double = 0.0

    val chunks = ConcurrentHashMap<IVec3, Chunk>()
    private val numberOfChunkGenerationThreads: Int = 1
    private var chunkGenerationThreadShouldBeRunning: AtomicBoolean = AtomicBoolean(true)
    private val currentlyGeneratingChunks = ConcurrentHashMap<IVec3, Thread>()
    private val chunkGenerationQueue = PriorityBlockingQueue<PriorityQueueChunkPosition>()
    private val chunkDestroyQueue = LinkedBlockingQueue<IVec3>()
    private val chunkCache = ChunkCache(64, this)
    private val worldGenerator : WorldGenerator
    private val loadedAreas = mutableMapOf<RemotePlayer, ChunkBounds>()

    init {

        val worldDirectory = File(FileSystem.worldDir)
        val worldInfoFile = File(worldDirectory, "world.info")
        var infoFileNeedsRewrite = false
        if (worldInfoFile.exists()) {
            val reader = worldInfoFile.bufferedReader()
            val lines = reader.readLines().map { it.split(":") }
            reader.close()
            seed = lines.firstOrNull { it[0] == "seed" }?.get(1)?.toLong() ?: run {
                infoFileNeedsRewrite = true
                Random(System.nanoTime()).nextLong()
            }
        } else {
            infoFileNeedsRewrite = true
            seed = Random(System.nanoTime()).nextLong()
        }
        if (infoFileNeedsRewrite) {
            worldDirectory.mkdirs()
            val writer = worldInfoFile.bufferedWriter()
            writer.write("seed:$seed")
            writer.close()
        }

        worldGenerator = WorldGenerator(this)

        repeat(numberOfChunkGenerationThreads) {
            createChunkGenerationThread().start()
        }
    }

    private fun createChunkGenerationThread() = Thread {
        while (chunkGenerationThreadShouldBeRunning.get()) {
            val chunkToLoad = chunkGenerationQueue.poll()
            if (chunkToLoad != null && !currentlyGeneratingChunks.containsKey(chunkToLoad.chunkPos)) {
                currentlyGeneratingChunks[chunkToLoad.chunkPos] = Thread.currentThread()
                val generatedChunk = generateChunkAt(chunkToLoad.chunkPos)
                val pos = Chunk.worldPosToChunkPos(generatedChunk.position)
                val oldChunk = chunks[pos]
                if (oldChunk == null) {
                    if (currentlyGeneratingChunks[chunkToLoad.chunkPos] != Thread.currentThread()) continue
                    setChunkAt(pos, generatedChunk)
                    worldGenerator.replaceBlocksInChunk(generatedChunk, pos)
                    if (generatedChunk.hasBlocks) {
                        val distance = (getClosestPlayerPosition() - chunkToLoad.chunkPos).length
                    }
                }
                currentlyGeneratingChunks.remove(chunkToLoad.chunkPos)
            }
            else Thread.sleep(70)
        }
    }

    private fun getClosestPlayerPosition(): IVec3 {
        return IVec3(0)
    }

    private class PriorityQueueChunkPosition(val chunkPos: IVec3, val distance: Float): Comparable<PriorityQueueChunkPosition> {
        override fun compareTo(other: PriorityQueueChunkPosition): Int {
            return (distance - other.distance).toInt()
        }
    }
    private class PriorityQueueChunk(val chunk: Chunk, var distance: Float): Comparable<PriorityQueueChunk> {
        override fun compareTo(other: PriorityQueueChunk): Int {
            return (distance - other.distance).toInt()
        }
    }

    fun process(delta: Float) {
        worldTime += delta

        // ----- deal with queues related to chunk loading -----
        val extraQueueHandlingTime = 2_000_000L
        var destroyRemainingTime = extraQueueHandlingTime + 0
        while (chunkDestroyQueue.isNotEmpty() && destroyRemainingTime > 0){
            val startTime = System.nanoTime()
            val chunkPosToDestroy = chunkDestroyQueue.peek()
            val chunk = chunks[chunkPosToDestroy]
            if (chunk != null) {
                chunkGenerationQueue.removeIf { it.chunkPos == chunkPosToDestroy }

                chunks.remove(chunkPosToDestroy)
            }
            chunkDestroyQueue.poll()
            destroyRemainingTime -= (System.nanoTime()-startTime)
        }
    }

    fun setChunkAt(pos: IVec3, chunk: Chunk) {
        if (chunks.containsKey(pos)) return
        chunks[pos] = chunk
        //TODO: send chunk to players that have it in their loaded area (loadedAreas)
    }

    fun getChunkAt(chunkPos: IVec3): Chunk {
        return Chunk(this, chunkPos)
    }

    private fun queueChunkGenerationAt(chunkPos: IVec3, distance: Float){
        val queueElement = PriorityQueueChunkPosition(chunkPos, distance)
        //chunkGenerationQueue.removeIf { it.chunkPos == chunkPos && it != queueElement }
        if ( chunkGenerationQueue.none { it.chunkPos == chunkPos} )
            chunkGenerationQueue.put(queueElement)
    }
    private fun generateChunkAt(pos: IVec3): Chunk {
        return worldGenerator.generateChunk(pos)
    }

    private fun cacheChunkAt(pos: IVec3) {
        val targetChunk = chunks[pos]
        if (targetChunk != null) chunkCache.add(targetChunk)
    }

    fun unloadChunkAt(chunkPos: IVec3) {
        removeChunkAt(chunkPos)
    }
    private fun removeChunkAt(pos: IVec3) {
        chunkDestroyQueue.put(pos)
    }

    fun isPositionInLoadedArea(pos: IVec3): Boolean {
        val chunkPos = Chunk.worldPosToChunkPos(pos)
        for (area in loadedAreas.values) {
            if (chunkPos in area) return true
        }
        return false
    }

    private fun updateAreaLoadedByPlayer(player: RemotePlayer, newArea: ChunkBounds) {
        val pos = Chunk.worldPosToChunkPos(player.pos)
        val remainLoadedBounds = ChunkBounds(newArea.getMinChunk() + IVec3(-1), newArea.getMaxChunk() + IVec3(1))
        val oldBounds = loadedAreas[player]
        chunkGenerationQueue.removeIf { it.chunkPos !in newArea }
        val chunkPositionsToLoad = mutableListOf<IVec3>()
        newArea.forEachChunk { chunkPos ->
            val oldChunk = chunks[chunkPos]
            // queue new chunks to be loaded (only if not in chunk cache
            if (oldChunk == null) {
                val cachedChunk = chunkCache.getCachedChunkAt(chunkPos)
                if (cachedChunk == null) chunkPositionsToLoad.add(chunkPos)
            } else {
                worldGenerator.replaceBlocksInChunk(oldChunk, chunkPos)
            }
        }
        val loadedChunks = WorldSaver.loadChunks(this, chunkPositionsToLoad)
        loadedChunks.forEach {
            val chunk = it.second
            val chunkPos = it.first
            if (chunk != null) { // load chunk from file
                setChunkAt(chunkPos, chunk)
                worldGenerator.replaceBlocksInChunk(chunk, chunkPos)
            } else { // generate chunk
                val dif = pos - chunkPos
                val horizontalDistance = IVec2(dif.x, dif.z).length
                queueChunkGenerationAt(chunkPos, horizontalDistance - (chunkPos.y * 2))
            }
        }
        // before unloading, first save all chunks that need it
        saveChunks()
        PlayerSaver.savePlayer(player)
        //unload chunks that are in the old range but not in the new range (put into chunk cache)
        chunkDestroyQueue.removeIf { it in newArea }
        oldBounds?.forEachChunk { chunkPos ->
            if (chunkPos !in remainLoadedBounds)
                cacheChunkAt(chunkPos)
        }
        loadedAreas[player] = newArea
    }

    fun getBlockAt(pos: IVec3): Short {
        val chunkPos = Chunk.worldPosToChunkPos(pos)
        val blockPosInChunk = Chunk.worldPosToPosInChunk(pos)
        val chunk = chunks[chunkPos]
        return chunk?.getBlockAt(blockPosInChunk) ?: 0.toShort()
    }

    fun setBlockAt(pos: IVec3, blockID: Short) {
        val chunkPos = Chunk.worldPosToChunkPos(pos)
        val blockPosInChunk = Chunk.worldPosToPosInChunk(pos)
        var chunk = chunks[chunkPos]
        if(chunk == null){
            chunk = Chunk(this, IVec3(chunkPos.x*Chunk.extent, chunkPos.y*Chunk.extent, chunkPos.z*Chunk.extent))
            chunks[chunkPos] = chunk
        }
        chunk.setBlockAt(blockPosInChunk, blockID)
        for (playerArea in loadedAreas) {
            if (pos in playerArea.value) {
                //TODO: send update to player playerArea.key
            }
        }
    }

    fun placeBlockAt(pos: IVec3, blockID: Short) {
        val previousBlockID = getBlockAt(pos)
        val blockType = BlockRegistry.getBlockById(blockID)
        val previousBlockType = BlockRegistry.getBlockById(previousBlockID)
        setBlockAt(pos, blockID)
        if ((blockID == 0.toShort() || blockType?.solid == false)
            && previousBlockType?.solid == true
        ) {
            for (i in 0..2) {
                if (previousBlockType is LightSourceBlockType && previousBlockType.lightLevels[i] > 0) removeLight(
                    pos,
                    i
                )
                else updateLight(pos, i)
            }
            updateSkylight(pos)
        } else if (blockType?.solid == true
            && (previousBlockID == 0.toShort() || previousBlockType?.solid == false)
        ) {
            removeLight(pos, 0)
            removeLight(pos, 1)
            removeLight(pos, 2)
            removeSkyLight(pos)
        }
        if (previousBlockType is LightSourceBlockType) {
            for (i in 0..2) {
                if (previousBlockType.lightLevels[i] > 0) removeLight(pos, i)
            }
        }
        if (blockType is LightSourceBlockType) {
            for (i in 0..2) {
                if (blockType.lightLevels[i] > 0) addLight(pos, blockType.lightLevels[i], i)
            }
        }
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

    private fun addLight(pos: IVec3, lightLevel: Int, lightIndex: Int) {
        val lightQueue = LinkedList<IVec3>()
        val currentLight = getLightAt(pos)
        currentLight[lightIndex] = lightLevel
        setLightAt(pos, currentLight, true)
        lightQueue.add(pos)
        updateLightQueue(lightQueue, lightIndex)
    }

    private fun updateLight(pos: IVec3, lightIndex: Int) {
        var maxLvl = 0
        for (nextDirection in listOf(
            IVec3(-1, 0, 0), IVec3(1, 0, 0), IVec3(0, -1, 0), IVec3(0, 1, 0), IVec3(0, 0, -1), IVec3(0, 0, 1)
        )) {
            maxLvl = max(maxLvl, getLightAt(pos + nextDirection)[lightIndex])
        }
        val lightLevel = if (maxLvl == 0) 0 else maxLvl - 1
        addLight(pos, lightLevel, lightIndex)
    }

    private fun updateLightQueue(lightQueue: LinkedList<IVec3>, lightIndex: Int) {
        while (!lightQueue.isEmpty()) {
            val currentPos = lightQueue.pop()
            val currentLight = getLightAt(currentPos)
            for (nextDirection in listOf(
                IVec3(-1, 0, 0), IVec3(1, 0, 0), IVec3(0, -1, 0), IVec3(0, 1, 0), IVec3(0, 0, -1), IVec3(0, 0, 1)
            )) {
                val nextPos = currentPos + nextDirection
                val nextBlock = getBlockAt(nextPos)
                val nextLight = getLightAt(nextPos)
                if (nextBlock == 0.toShort() || BlockRegistry.getBlockById(nextBlock)?.solid == false) {
                    if (nextLight[lightIndex] + 2 <= currentLight[lightIndex]) {
                        nextLight[lightIndex] = currentLight[lightIndex] - 1
                        setLightAt(nextPos, nextLight, true)
                        lightQueue.add(nextPos)
                    }
                }
            }
        }
    }

    private fun removeLight(pos: IVec3, lightIndex: Int) {
        val lightRemovalQueue = LinkedList<Pair<IVec3, IVec4>>()
        val lightQueue = LinkedList<IVec3>()

        lightRemovalQueue.add(Pair(pos, getLightAt(pos)))
        var currentLight = getLightAt(pos)
        currentLight[lightIndex] = 0
        setLightAt(pos, currentLight, true)
        while (!lightRemovalQueue.isEmpty()) {
            val currentNode = lightRemovalQueue.pop()
            currentLight = currentNode.second
            for (nextDirection in listOf(
                IVec3(-1,0,0), IVec3(1,0,0), IVec3(0,-1,0), IVec3(0,1,0), IVec3(0,0,-1), IVec3(0,0,1)
            ) ) {
                val nextPos = currentNode.first + nextDirection
                val nextLight = getLightAt(nextPos)
                if (nextLight[lightIndex] != 0 && nextLight[lightIndex] < currentLight[lightIndex]) {
                    val newNextLight = IVec4(nextLight)
                    newNextLight[lightIndex] = 0
                    setLightAt(nextPos, newNextLight, true)
                    lightRemovalQueue.add(Pair(nextPos, nextLight))
                }
                else if (nextLight[lightIndex] >= currentLight[lightIndex]) {
                    lightQueue.add(nextPos)
                }
            }
        }
        updateLightQueue(lightQueue, lightIndex)
    }

    private fun updateSkylight(pos: IVec3) {
        val lightQueue = LinkedList<IVec3>()
        lightQueue.add(pos)
        updateSkyLightQueue(lightQueue)
    }

    fun updateSkyLightQueue(lightQueue: LinkedList<IVec3>, instantRemesh: Boolean = true) {
        while (!lightQueue.isEmpty()) {
            val currentPos = lightQueue.pop()

            var currentLight = getLightAt(currentPos)
            val lightLevel = if (getLightAt(currentPos + IVec3(0, 1, 0))[3] == 15) 15 else {
                var maxLvl = 0
                for (nextDirection in listOf(
                    IVec3(-1, 0, 0), IVec3(1, 0, 0), IVec3(0, -1, 0), IVec3(0, 1, 0), IVec3(0, 0, -1), IVec3(0, 0, 1)
                )) {
                    maxLvl = max(maxLvl, getLightAt(currentPos + nextDirection)[3])
                }
                if (maxLvl == 0) 0 else maxLvl - 1
            }
            currentLight[3] = lightLevel
            setLightAt(currentPos, currentLight, instantRemesh)

            currentLight = getLightAt(currentPos)
            for (nextDirection in listOf(
                IVec3(-1, 0, 0), IVec3(1, 0, 0), IVec3(0, 1, 0), IVec3(0, 0, -1), IVec3(0, 0, 1)
            )) {
                val nextPos = currentPos + nextDirection
                val nextBlock = getBlockAt(nextPos)
                val nextLight = getLightAt(nextPos)
                if (nextBlock == 0.toShort() || BlockRegistry.getBlockById(nextBlock)?.solid == false) {
                    if (nextLight[3] + 2 <= currentLight[3]) {
                        nextLight[3] = currentLight[3] - 1
                        setLightAt(nextPos, nextLight, instantRemesh)
                        lightQueue.add(nextPos)
                    }
                }
            }
            val nextPos = currentPos + IVec3(0, -1, 0)
            val nextBlock = getBlockAt(nextPos)
            val nextLight = getLightAt(nextPos)
            if (nextBlock == 0.toShort() || BlockRegistry.getBlockById(nextBlock)?.solid == false) {
                if (nextLight[3] < currentLight[3]) {
                    nextLight[3] = currentLight[3]
                    setLightAt(nextPos, nextLight, instantRemesh)
                    lightQueue.add(nextPos)
                }
            }
        }
    }

    private fun removeSkyLight(pos: IVec3) {
        val lightRemovalQueue = LinkedList<Pair<IVec3, IVec4>>()
        val lightQueue = LinkedList<IVec3>()

        lightRemovalQueue.add(Pair(pos, getLightAt(pos)))
        var currentLight = getLightAt(pos)
        currentLight[3] = 0
        setLightAt(pos, currentLight, true)
        while (!lightRemovalQueue.isEmpty()) {
            val currentNode = lightRemovalQueue.pop()
            currentLight = currentNode.second
            for (nextDirection in listOf(
                IVec3(-1,0,0), IVec3(1,0,0), IVec3(0,1,0), IVec3(0,0,-1), IVec3(0,0,1)
            ) ) {
                val nextPos = currentNode.first + nextDirection
                val nextLight = getLightAt(nextPos)
                if (nextLight[3] != 0 && nextLight[3] < currentLight[3]) {
                    val newNextLight = IVec4(nextLight)
                    newNextLight[3] = 0
                    setLightAt(nextPos, newNextLight, true)
                    lightRemovalQueue.add(Pair(nextPos, nextLight))
                }
                else if (nextLight[3] >= currentLight[3]) {
                    lightQueue.add(nextPos)
                }
            }
            val nextPos = currentNode.first + IVec3(0,-1,0)
            val nextLight = getLightAt(nextPos)
            if (nextLight[3] != 0 && (nextLight[3] < currentLight[3] || nextLight[3] == 15)) {
                val newNextLight = IVec4(nextLight)
                newNextLight[3] = 0
                setLightAt(nextPos, newNextLight, true)
                lightRemovalQueue.add(Pair(nextPos, nextLight))
            }
            else if (nextLight[3] >= currentLight[3]) {
                lightQueue.add(nextPos)
            }
        }
        updateSkyLightQueue(lightQueue)
    }

    private fun setLightAt(pos: IVec3, light: IVec4, addChunkToInstantRemeshSet: Boolean = false) {
        val chunkPos = Chunk.worldPosToChunkPos(pos)
        val chunk = getChunkAt(chunkPos)
        if(chunk != null) {
            chunk.setLightAt(
                Chunk.worldPosToPosInChunk(pos),
                lightFromIVec4ToShort(light)
            )
        }
        for (playerArea in loadedAreas) {
            if (pos in playerArea.value) {
                //TODO: send update to player playerArea.key
            }
        }
    }

    fun saveChunks() {
        val chunksToSave = mutableListOf<Chunk>()
        for (c in chunks) {
            if (c.value.dirtyBlocks || !c.value.existsOnDisk) chunksToSave.add(c.value)
        }
        chunksToSave.addAll(chunkCache.getChunksToSave())
        WorldSaver.saveChunks(this, chunksToSave)
    }

    fun shutDown() {
        chunkGenerationThreadShouldBeRunning.set(false)
        Thread.sleep(10)
        saveChunks()
    }
}