package game.world
/*
import FileSystem
import game.blocks.BlockRegistry
import game.items.ItemStack
import game.world.generation.WorldGenerator
import game.world.saving.WorldSaver
import math.datatype.vec.DVec3
import math.datatype.vec.IVec3
import math.datatype.vec.Vec3
import util.for3D
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.LinkedList
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.PriorityBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max
import kotlin.math.sign
import kotlin.random.Random


class WorldOldClientThingyICopied {
    val seed: Int

    var worldTime: Double = 0.0

    private val chunks = ConcurrentHashMap<IVec3, Chunk>()
    private val numberOfChunkGenerationThreads: Int = 1
    private var chunkGenerationThreadShouldBeRunning: AtomicBoolean = AtomicBoolean(true)
    private var chunkLoadingThreadShouldBeRunning: AtomicBoolean = AtomicBoolean(true)
    private val currentlyGeneratingChunks = ConcurrentHashMap<IVec3, Thread>()
    private val chunkGenerationQueue = PriorityBlockingQueue<PriorityQueueChunkPosition>()
    private val chunkDestroyQueue = LinkedBlockingQueue<IVec3>()
    private var minChunk = IVec3(0,0,0)
    private var maxChunk = IVec3(0,0,0)
    private val worldGenerator: WorldGenerator
    private var lastLoadPosition = DVec3(100.0)
    private var lastLoadTime: Double = 0.0
    private val reloadInterval: Double = 30.0


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
                    generatedChunk.setLoaded(true)
                    if (generatedChunk.hasBlocks) {
                        val distance = Vec3(RuntimeGlobals.playerGlobalPos - chunkToLoad.chunkPos).length()
                        if (areNeighborsLoaded(chunkToLoad.chunkPos)) chunkMeshingQueue.put(PriorityQueueChunk(generatedChunk, distance))
                    }
                }
                currentlyGeneratingChunks.remove(chunkToLoad.chunkPos)
            }
            else Thread.sleep(70)
        }
    }

    private val chunkLoadingThread = Thread {
        fun load() {
            loadChunksAroundPlayerPosition()
            lastLoadPosition.x = RuntimeGlobals.playerGlobalPos.x
            lastLoadPosition.y = RuntimeGlobals.playerGlobalPos.y
            lastLoadPosition.z = RuntimeGlobals.playerGlobalPos.z
            lastLoadTime = worldTime
        }

        load()
        Thread.sleep(5000)
        load()
        while (chunkLoadingThreadShouldBeRunning.get()) {
            if ((RuntimeGlobals.playerGlobalPos-lastLoadPosition).length() > Chunk.extent) {
                load()
            }
            else {
                if (worldTime-lastLoadTime > reloadInterval) {
                    load()
                }
                Thread.sleep(500)
            }
        }
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

    init {
        val worldDirectory = File(FileSystem.worldDir)
        val worldInfoFile = File(worldDirectory, "world.info")
        var infoFileNeedsRewrite = false
        if (worldInfoFile.exists()) {
            val reader = worldInfoFile.bufferedReader()
            val lines = reader.readLines().map { it.split(":") }
            reader.close()
            seed = lines.firstOrNull { it[0] == "seed" }?.get(1)?.toInt() ?: run {
                infoFileNeedsRewrite = true
                Random(System.nanoTime()).nextLong().toInt()
            }
        }
        else {
            infoFileNeedsRewrite = true
            seed = Random(System.nanoTime()).nextLong().toInt()
        }
        if (infoFileNeedsRewrite) {
            worldDirectory.mkdirs()
            val writer = worldInfoFile.bufferedWriter()
            writer.write("seed:$seed")
            writer.close()
        }

        loadPlayer()

        worldGenerator = WorldGenerator(this)

        repeat(numberOfChunkGenerationThreads){
            createChunkGenerationThread().start()
        }
        chunkLoadingThread.start()
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
            if (chunk != null && chunk.flags.contains(ChunkFlag.PENDING_DESTRUCTION)) {
                chunkGenerationQueue.removeIf { it.chunkPos == chunkPosToDestroy }
                chunkMeshingQueue.removeIf { it.chunk == chunk }
                //chunkMeshingOutputQueue.removeIf { it.first == chunk }

                chunk.destroy()
                chunks.remove(chunkPosToDestroy)
            }
            chunkDestroyQueue.poll()
            destroyRemainingTime -= (System.nanoTime()-startTime)
        }
        var meshingOutputRemainingTime = max(destroyRemainingTime,0L) + 50_000
        while (chunkMeshingOutputQueue.isNotEmpty() && meshingOutputRemainingTime > 0) {
            val startTime = System.nanoTime()
            val meshedChunk = chunkMeshingOutputQueue.poll()
            if (!meshedChunk.first.flags.contains(ChunkFlag.PENDING_DESTRUCTION)) {
                meshedChunk.first.setMesh(meshedChunk.second)
                meshedChunk.first.display()
            }
            meshingOutputRemainingTime -= (System.nanoTime()-startTime)
        }
        // ----- dealt with queues related to chunk loading -----
        /*if (System.currentTimeMillis() % 1000 <= 10.toLong()) {
            println("------------------------------------------")
            println("Generate: ${chunkGenerationQueue.size}")
            println("Meshing : ${chunkMeshingQueue.size}")
            println("Mesh Out: ${chunkMeshingOutputQueue.size}")
            println("Destroy : ${chunkDestroyQueue.size}")
        }*/
    }

    /*
    queues chunk positions within render distance with no loaded chunk to be loaded.
    also updates the actively loaded area's "bounding box" (minChunk and maxChunk)
    and puts the chunks from the old box that are not in the new one into the chunk cache.
     */
    private fun loadChunksAroundPlayerPosition() {
        val pos = Chunk.worldPosToChunkPos(IVec3(RuntimeGlobals.playerGlobalPos))
        val renderDistBounds = ChunkBounds(pos, RuntimeGlobals.chunkRenderDistance)
        val remainLoadedBounds = ChunkBounds(pos, RuntimeGlobals.chunkRemainLoadedDistance)
        val oldBounds = ChunkBounds(minChunk, maxChunk)
        chunkGenerationQueue.removeIf { it.chunkPos !in renderDistBounds }
        val chunkPositionsToLoad = mutableListOf<IVec3>()
        renderDistBounds.forEachChunk { chunkPos ->
            val oldChunk = chunks[chunkPos]
            // queue new chunks to be loaded (only if not in chunk cache
            if (oldChunk == null) {
                val cachedChunk = chunkCache.getCachedChunkAt(chunkPos)
                if (cachedChunk == null) chunkPositionsToLoad.add(chunkPos)
            }
            // update meshing priority for loaded chunks with no mesh (so there are no gaps near the player)
            else if (ChunkFlag.MESHED !in oldChunk.flags && chunkMeshingOutputQueue.none { it.first == oldChunk })
            {
                worldGenerator.replaceBlocksInChunk(oldChunk, chunkPos)
                if (oldChunk.hasBlocks) {
                    val distance = Vec3(pos - chunkPos).length()
                    val queueElement = PriorityQueueChunk(oldChunk, distance)
                    if (areNeighborsLoaded(chunkPos)) {
                        chunkMeshingQueue.removeIf { it.chunk == oldChunk }
                        chunkMeshingQueue.put(queueElement)
                    }
                }
            }
            else {
                val needsRemeshing = worldGenerator.replaceBlocksInChunk(oldChunk, chunkPos)
                if (needsRemeshing) {
                    val distance = Vec3(pos - chunkPos).length()
                    val queueElement = PriorityQueueChunk(oldChunk, distance)
                    chunkMeshingQueue.removeIf { it.chunk == oldChunk }
                    chunkMeshingQueue.put(queueElement)
                }
            }
        }
        // TODO: fetching chunk from server
        val loadedChunks = WorldSaver.loadChunks(chunkPositionsToLoad)
        loadedChunks.forEach {
            val chunk = it.second
            val chunkPos = it.first
            if (chunk != null) { // load chunk from file
                setChunkAt(chunkPos, chunk)
                worldGenerator.replaceBlocksInChunk(chunk, chunkPos)
                chunk.setLoaded(true)
                if (chunk.hasBlocks && areNeighborsLoaded(chunkPos)) {
                    val distance = Vec3(RuntimeGlobals.playerGlobalPos - chunk.position).length()
                    if (areNeighborsLoaded(chunkPos)) chunkMeshingQueue.put(PriorityQueueChunk(chunk, distance))
                }
            }
            else { // generate chunk
                val dif = Vec3(pos-chunkPos)
                val horizontalDistance = Vec2(dif.x, dif.z).length()
                queueChunkGenerationAt(chunkPos, horizontalDistance - (chunkPos.y*2))
            }
        }
        // before unloading, first save all chunks that need it
        saveChunks()
        savePlayer()
        //unload chunks that are in the old range but not in the new range (put into chunk cache)
        chunkDestroyQueue.removeIf { it in renderDistBounds }
        oldBounds.forEachChunk { chunkPos ->
            if (chunkPos !in remainLoadedBounds)
                cacheChunkAt(chunkPos)
        }
        worldGenerator.cleanCachedData(remainLoadedBounds)
        minChunk = remainLoadedBounds.getMinChunk()
        maxChunk = remainLoadedBounds.getMaxChunk()
    }

    fun areNeighborsLoaded(pos: IVec3): Boolean {
        return (chunks[pos+IVec3(0,0,1)]?.flags?.contains(ChunkFlag.LOADED) == true) &&
                (chunks[pos+IVec3(0,0,-1)]?.flags?.contains(ChunkFlag.LOADED) == true) &&
                (chunks[pos+IVec3(0,1,0)]?.flags?.contains(ChunkFlag.LOADED) == true) &&
                (chunks[pos+IVec3(0,-1,0)]?.flags?.contains(ChunkFlag.LOADED) == true) &&
                (chunks[pos+IVec3(1,0,0)]?.flags?.contains(ChunkFlag.LOADED) == true) &&
                (chunks[pos+IVec3(-1,0,0)]?.flags?.contains(ChunkFlag.LOADED) == true)
    }

    fun isPositionInLoadedArea(pos: IVec3): Boolean {
        val chunkPos = Chunk.worldPosToChunkPos(pos)
        return chunkPos in ChunkBounds(minChunk, maxChunk)
    }

    fun setBlockAt(pos: IVec3, blockID: Short) {
        val chunkPos = Chunk.worldPosToChunkPos(pos)
        val blockPosInChunk = Chunk.worldPosToPosInChunk(pos)
        var chunk = chunks[chunkPos]
        if(chunk == null){
            chunk = Chunk(IVec3(chunkPos.x*Chunk.extent, chunkPos.y*Chunk.extent, chunkPos.z*Chunk.extent))
            chunks[chunkPos] = chunk
        }
        chunk.setBlockAt(blockPosInChunk, blockID)
    }

    fun placeBlockAt(pos: IVec3, blockID: Short) {
        val previousBlockID = getBlockAt(pos)
        val blockType = BlockRegistry.getBlockById(blockID)
        val previousBlockType = BlockRegistry.getBlockById(previousBlockID)
        setBlockAt(pos, blockID)
        if ((blockID == 0.toShort() || blockType?.solid==false)
            && previousBlockType?.solid==true) {
            for (i in 0..2) {
                if (previousBlockType is LightSourceBlockType && previousBlockType.lightLevels[i] > 0) removeLight(pos, i)
                else updateLight(pos, i)
            }
            updateSkylight(pos)
        }
        else if (blockType?.solid==true
            && (previousBlockID == 0.toShort() || previousBlockType?.solid==false)) {
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

    fun getBlockAt(pos: IVec3): Short {
        val chunkPos = Chunk.worldPosToChunkPos(pos)
        val blockPosInChunk = Chunk.worldPosToPosInChunk(pos)
        val chunk = chunks[chunkPos]
        if(chunk == null) return 0.toShort()
        else return chunk.getBlockAt(blockPosInChunk)
    }

    fun addChunkForInstantRemeshing(chunkPos: IVec3) {
        val chunk = getChunkAt(chunkPos)
        if (chunk != null) chunksForInstantRemeshing.add(chunk)
    }

    fun addChunkForInstantRemeshing(chunk: Chunk) {
        chunksForInstantRemeshing.add(chunk)
    }

    fun remeshChunksQueuedForInstantRemeshing() {
        chunksForInstantRemeshing.forEach {
            //println("Meshed in " + measureNanoTime { it.remesh() }/1000000.0 + ".")
            it.remesh()
            it.display()
        }
        chunksForInstantRemeshing.clear()
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
        val lightRemovalQueue = LinkedList<Pair<IVec3, Vec4i>>()
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
                    val newNextLight = Vec4i(nextLight)
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
        val lightRemovalQueue = LinkedList<Pair<IVec3, Vec4i>>()
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
                    val newNextLight = Vec4i(nextLight)
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
                val newNextLight = Vec4i(nextLight)
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

    private fun setLightAt(pos: IVec3, light: Vec4i, addChunkToInstantRemeshSet: Boolean = false) {
        val chunkPos = Chunk.worldPosToChunkPos(pos)
        val chunk = getChunkAt(chunkPos)
        if(chunk != null) {
            chunk.setLightAt(
                Chunk.worldPosToPosInChunk(pos),
                lightFromVec4iToShort(light)
            )
            if(addChunkToInstantRemeshSet) {
                chunksForInstantRemeshing.add(chunk)
                val blockPosInChunk = Chunk.worldPosToPosInChunk(pos)

                val remeshDirections = IVec3()

                if (blockPosInChunk.x == 0) remeshDirections.x = -1
                else if (blockPosInChunk.x == Chunk.extent - 1) remeshDirections.x = 1
                if (blockPosInChunk.y == 0) remeshDirections.y = -1
                else if (blockPosInChunk.y == Chunk.extent - 1) remeshDirections.y = 1
                if (blockPosInChunk.z == 0) remeshDirections.z = -1
                else if (blockPosInChunk.z == Chunk.extent - 1) remeshDirections.z = 1

                for3D(0..remeshDirections.x.abs, 0..remeshDirections.y.abs, 0..remeshDirections.z.abs) { x, y, z ->
                    if ((remeshDirections.x == x*remeshDirections.x.sign && x != 0) ||
                        (remeshDirections.y == y*remeshDirections.y.sign && y != 0) ||
                        (remeshDirections.z == z*remeshDirections.z.sign && z != 0)) {
                        addChunkForInstantRemeshing(chunkPos + IVec3(x*remeshDirections.x.sign,y*remeshDirections.y.sign,z*remeshDirections.z.sign))
                    }
                }
            }
        }
    }

    fun getLightShortAt(pos: IVec3): Short {
        val chunk = chunks[Chunk.worldPosToChunkPos(pos)]
        if (chunk != null) return chunk.getLightAt(Chunk.worldPosToPosInChunk(pos))
        else return 0
    }

    fun getLightAt(pos: IVec3): Vec4i {
        //return IVec3((Math.random()*15).toInt(),(Math.random()*15).toInt(),(Math.random()*15).toInt())
        //return IVec3(abs(pos.x%16), abs(pos.x%16), abs(pos.x%16))
        //return IVec3((8+8*sin(0.1*pos.x)).toInt()%16, (8+8*sin(0.05*pos.y)).toInt()%16, (8+8*sin(0.11*pos.z)).toInt()%16)
        //return IVec3(15,15,15)
        val chunk = chunks[Chunk.worldPosToChunkPos(pos)]
        if (chunk != null) return lightFromShortToVec4i(chunk.getLightAt(Chunk.worldPosToPosInChunk(pos)))
        else return lightFromShortToVec4i(0)
    }

    fun lightFromShortToVec4i(light: Short): Vec4i {
        val r = (light and 0b0000_0000_0000_1111)
        val g = (light and 0b0000_0000_1111_0000) ushr 4
        val b = (light and 0b0000_1111_0000_0000) ushr 8
        val s = (light and 0b1111_0000_0000_0000) ushr 12
        return Vec4i(r, g, b, s)
    }

    fun lightFromVec4iToShort(light: Vec4i): Short {
        return (light.r or (light.g shl 4) or (light.b shl 8) or (light.a shl 12)).toShort()
    }

    /*
    sets the specified chunk at the specified chunk position (in chunks) if there is no chunk at that position
     */
    fun setChunkAt(pos: IVec3, chunk: Chunk) {
        if (chunks.containsKey(pos)) return
        chunks[pos] = chunk
    }

    private fun removeChunkAt(pos: IVec3) {
        chunks[pos]?.setPendingDestruction(true)
        chunkDestroyQueue.put(pos)
    }

    /*
    puts the chunk at the specified position (if it exists) into the chunk cache (where it may be destroyed later)
     */
    private fun cacheChunkAt(pos: IVec3) {
        val targetChunk = chunks[pos]
        if (targetChunk != null) chunkCache.add(targetChunk)
    }

    /*
    unloads the chunk at pos by calling removeChunkAt.
    should also do stuff like save to disk if modified or new (to-do)
     */
    fun unloadChunkAt(pos: IVec3) {
        removeChunkAt(pos)
    }

    private fun queueChunkGenerationAt(chunkPos: IVec3, distance: Float){
        val queueElement = PriorityQueueChunkPosition(chunkPos, distance)
        //chunkGenerationQueue.removeIf { it.chunkPos == chunkPos && it != queueElement }
        if ( chunkGenerationQueue.none { it.chunkPos == chunkPos} )
            chunkGenerationQueue.put(queueElement)
    }

    /*
    creates a new chunk with blocks filled by the world generator
     */
    private fun generateChunkAt(pos: IVec3): Chunk {
        return worldGenerator.generateChunk(pos)
    }

    fun getChunkAt(chunkPos: IVec3): Chunk? {
        return chunks[chunkPos]
    }


    // Used for collision. Returns a list of all non-zero block IDs surrounding the center
    fun getBlocksInArea(blockArea3D: BlockArea3D): List<IVec3> {
        val blocks = mutableListOf<IVec3>()
        val air = 0.toShort()

        blockArea3D.forEachBlock {
            val block = getBlockAt(it)
            if (block != air) blocks.add(it)
        }

        return blocks
    }

    fun shutDown() {
        chunkGenerationThreadShouldBeRunning.set(false)
        chunkLoadingThreadShouldBeRunning.set(false)
        Thread.sleep(10)
        saveChunks()
        savePlayer()
    }

    fun saveChunks() {
        val chunksToSave = mutableListOf<Chunk>()
        for (c in chunks) {
            if (c.value.dirtyBlocks || !c.value.existsOnDisk) chunksToSave.add(c.value)
        }
        chunksToSave.addAll(chunkCache.getChunksToSave())
        WorldSaver.saveChunks(chunksToSave)
    }

    fun savePlayer() {
        val playerDirectory = File(FileSystem.playerDir)
        val playersDirectory = File(playerDirectory, "players")
        val oldPlayerFile = File(playersDirectory, "player0.player")
        val playerFile = File(playersDirectory, "player0.player_new")
        playersDirectory.mkdirs()
        val writer = FileOutputStream(playerFile, true)
        writer.write("PLYR".toByteArray())
        writer.write(0.toByteBuffer().toByteArray()) // player file format version
        writer.write(LocalPlayer.globalPosition.x.toByteArray())
        writer.write(LocalPlayer.globalPosition.y.toByteArray())
        writer.write(LocalPlayer.globalPosition.z.toByteArray())
        writer.write(LocalPlayer.viewRotation.x.toByteArray())
        writer.write(LocalPlayer.viewRotation.y.toByteArray())
        writer.write(LocalPlayer.viewRotation.z.toByteArray())
        writer.write((LocalPlayer.getComponent<Health>()?.healthPoints ?: 20).toByteBuffer().toByteArray())
        LocalPlayer.inventory.forEachSlot {
            if (it != null) {
                writer.write(it.itemID.toByteBuffer().toByteArray())
                writer.write(it.amount.toByteBuffer().toByteArray())
            }
            else {
                writer.write(0.toByteBuffer().toByteArray())
                writer.write(0.toByteBuffer().toByteArray())
            }
        }
        writer.close()
        Files.move(playerFile.toPath(), oldPlayerFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
    }

    fun loadPlayer() {
        val worldDirectory = File(FileSystem.worldDir)
        val playersDirectory = File(worldDirectory, "players")
        val playerFile = File(playersDirectory, "player0.player")
        if (playerFile.exists()) {
            val reader = FileInputStream(playerFile)
            val signatureAndVersion = ByteArray(8)
            reader.read(signatureAndVersion)
            if (signatureAndVersion.decodeToString(0,4) != "PLYR") println("Wrong player file signature!")
            if (signatureAndVersion.getInt(4) != 0) println("Unknown player file format version!")
            val coordinate = ByteArray(Double.SIZE_BYTES)
            reader.read(coordinate)
            LocalPlayer.globalPosition.x = coordinate.getDouble(0, false)
            reader.read(coordinate)
            LocalPlayer.globalPosition.y = coordinate.getDouble(0, false)
            reader.read(coordinate)
            LocalPlayer.globalPosition.z = coordinate.getDouble(0, false)
            RuntimeGlobals.playerGlobalPos = LocalPlayer.globalPosition
            val rotationValue = ByteArray(Float.SIZE_BYTES)
            reader.read(rotationValue)
            LocalPlayer.viewRotation.x = rotationValue.getFloat(0, false)
            reader.read(rotationValue)
            LocalPlayer.viewRotation.y = rotationValue.getFloat(0, false)
            reader.read(rotationValue)
            LocalPlayer.viewRotation.z = rotationValue.getFloat(0, false)
            val arrayForInts = ByteArray(Int.SIZE_BYTES)
            reader.read(arrayForInts)
            LocalPlayer.getComponent<Health>()?.healthPoints = arrayForInts.getInt(0)
            LocalPlayer.inventory.forEachSlotIndexed { index, _ ->
                reader.read(arrayForInts)
                val id = arrayForInts.getInt(0)
                reader.read(arrayForInts)
                val amount = arrayForInts.getInt(0)
                LocalPlayer.inventory.emptySlot(index)
                if( id != 0) LocalPlayer.inventory[index] = ItemStack(id, amount)
            }
            reader.close()
        }
        else {
            savePlayer()
        }
    }

}
 */
