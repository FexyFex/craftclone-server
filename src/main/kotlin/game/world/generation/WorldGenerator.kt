package game.world.generation

import game.blocks.BlockRegistry
import game.world.Chunk
import game.world.World
import io.github.overrun.perlinoisej.PerlinNoise
import math.datatype.vec.IVec2
import math.datatype.vec.IVec3
import math.datatype.vec.clamp
import math.datatype.vec.mix
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Predicate
import kotlin.experimental.and
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.round


class WorldGenerator (private val world: World) {
    private val blocksToReplace = ConcurrentHashMap<IVec3, ConcurrentHashMap<IVec3, MutableList<BlockReplacement>>>()
    private val air = BlockRegistry.getIdByName("Air")
    private val stone = BlockRegistry.getIdByName("Stone")
    private val dirt = BlockRegistry.getIdByName("Dirt")
    private val grass = BlockRegistry.getIdByName("Grass")
    private val leaves = BlockRegistry.getIdByName("Leaves")

    private val heightmapMultiplier = 128
    private val heightmapAddition = 10
    private val heightmapNoise = FastNoiseLite()
    private val terrainNoise3D = FastNoiseLite()
    private val mixNoise = FastNoiseLite()

    private val caveTunnelNoise1 = FastNoiseLite()
    private val caveTunnelNoise2 = FastNoiseLite()
    private val caveRoomsNoise = FastNoiseLite()
    private val caveSpeleothermsNoise2D = FastNoiseLite()
    private val caveSpeleothermsNoise3D = FastNoiseLite()

    init {
        heightmapNoise.SetNoiseType(FastNoiseLite.NoiseType.Perlin)
        heightmapNoise.SetSeed(world.seed + 100)
        heightmapNoise.SetFrequency(1f/300f)
        heightmapNoise.SetFractalType(FastNoiseLite.FractalType.FBm)
        heightmapNoise.SetFractalOctaves(5)

        terrainNoise3D.SetNoiseType(FastNoiseLite.NoiseType.Perlin)
        terrainNoise3D.SetSeed(world.seed + 200)
        terrainNoise3D.SetFrequency(1f/32f)
        terrainNoise3D.SetFractalType(FastNoiseLite.FractalType.FBm)
        terrainNoise3D.SetFractalOctaves(3)

        mixNoise.SetNoiseType(FastNoiseLite.NoiseType.OpenSimplex2)
        mixNoise.SetSeed(world.seed + 300)
        mixNoise.SetFrequency(1f/100f)
        mixNoise.SetFractalType(FastNoiseLite.FractalType.FBm)
        mixNoise.SetFractalOctaves(3)

        caveTunnelNoise1.SetNoiseType(FastNoiseLite.NoiseType.OpenSimplex2)
        caveTunnelNoise1.SetSeed(world.seed + 400)
        caveTunnelNoise1.SetFrequency(1f/50f)
        caveTunnelNoise1.SetFractalType(FastNoiseLite.FractalType.FBm)
        caveTunnelNoise1.SetFractalOctaves(3)

        caveTunnelNoise2.SetNoiseType(FastNoiseLite.NoiseType.OpenSimplex2)
        caveTunnelNoise2.SetSeed(world.seed + 500)
        caveTunnelNoise2.SetFrequency(1f/50f)
        caveTunnelNoise2.SetFractalType(FastNoiseLite.FractalType.FBm)
        caveTunnelNoise2.SetFractalOctaves(3)

        caveRoomsNoise.SetNoiseType(FastNoiseLite.NoiseType.OpenSimplex2)
        caveRoomsNoise.SetSeed(world.seed + 600)
        caveRoomsNoise.SetFrequency(1f/184f)
        caveRoomsNoise.SetFractalType(FastNoiseLite.FractalType.FBm)
        caveRoomsNoise.SetFractalOctaves(3)

        caveSpeleothermsNoise3D.SetNoiseType(FastNoiseLite.NoiseType.OpenSimplex2)
        caveSpeleothermsNoise3D.SetSeed(world.seed + 700)
        caveSpeleothermsNoise3D.SetFrequency(1f/64f)
        caveSpeleothermsNoise3D.SetFractalType(FastNoiseLite.FractalType.FBm)
        caveSpeleothermsNoise3D.SetFractalOctaves(3)

        caveSpeleothermsNoise2D.SetNoiseType(FastNoiseLite.NoiseType.OpenSimplex2)
        caveSpeleothermsNoise2D.SetSeed(world.seed + 800)
        caveSpeleothermsNoise2D.SetFrequency(1f/48f)
        caveSpeleothermsNoise2D.SetFractalType(FastNoiseLite.FractalType.FBm)
        caveSpeleothermsNoise2D.SetFractalOctaves(3)
    }


    fun generateChunk(chunkPos: IVec3): Chunk {
        val chunkWorldPos = chunkPos * Chunk.extent
        val chunk = Chunk(world, chunkWorldPos)
        val blocks: Array<Array<ShortArray>> = Array(Chunk.extent) { Array(Chunk.extent) { ShortArray(Chunk.extent) { 0 } } }
        val light: Array<Array<ShortArray>> = Array(Chunk.extent) { Array(Chunk.extent) { ShortArray(Chunk.extent) { 0b1111_0000_0000_0000.toShort() } } }
        val skylightQueue = LinkedList<IVec3>()
        var hasBlocks = false
        var minHeight = Float.MAX_VALUE
        for (x in 0 until Chunk.extent) {
            for (z in 0 until Chunk.extent) {
                val posInChunk = IVec3(x,0,z)
                val worldPos = posInChunk + chunkWorldPos

                val terrainHeight: Float = heightmapNoise.GetNoise(worldPos.x.toFloat(),worldPos.z.toFloat()) * heightmapMultiplier + heightmapAddition
                if (terrainHeight < minHeight) minHeight = terrainHeight

                /*if (chunkWorldPos.y > 2*Chunk.extent + terrainHeight) {
                    for (y in Chunk.extent-1 downTo  0) {
                        light[x][y][z] = 0b1111_0000_0000_0000.toShort()
                    }
                    continue
                }*/

                for (y in Chunk.extent -1 downTo  0) {
                    posInChunk.y = y
                    worldPos.y = posInChunk.y + chunkWorldPos.y

                    if (getBlockToPlaceAt(worldPos) != 0.toShort() && getCaveAt(worldPos) != 0.toShort()) {
                        var airAbove = false
                        var caveAbove = false
                        if (y == Chunk.extent -1) {
                            if (getBlockToPlaceAt(worldPos + IVec3(0, 1, 0)) == 0.toShort()) airAbove = true
                            else if (getCaveAt(worldPos + IVec3(0, 1, 0)) == 0.toShort()) caveAbove = true
                        }
                        else {
                            if (blocks[x][y + 1][z] == 0.toShort()) {
                                airAbove = true
                                if (getBlockToPlaceAt(worldPos + IVec3(0, 1, 0)) != 0.toShort()) caveAbove = true
                            }
                        }

                        if (airAbove && !caveAbove) {
                            blocks[x][y][z] = grass ?: 2
                        }
                        else blocks[x][y][z] = stone ?: 1
                        light[x][y][z] = 0b0000_0000_0000_0000.toShort()

                        //if grass
                        if (airAbove && !caveAbove) {
                            val r0 = (PerlinNoise.fbmNoise3(worldPos.x/16f, worldPos.y/16f, worldPos.z/16f, 2f,0.5f,3) + 1f) * 2
                            val r = round(r0).toInt()
                            for (i in 1..r) {
                                val blockToReplacePos = IVec3(x,y-i,z)
                                val newY = y-i
                                if (newY < 0){
                                    replaceBlockAt(chunkPos - IVec3(0,1,0), IVec3(x, Chunk.extent + newY, z),
                                        { b -> b == stone }, dirt ?: 1)
                                }
                                else {
                                    addToReplaceBlocks(chunkPos, blockToReplacePos, { b -> b == stone }, dirt ?: 1)
                                }
                            }

                            if ( r0.hashCode() % 1000 == 0) {
                                generateStructure(GeneratedStructure.testStructure1, worldPos + IVec3(0,1,0))
                            }

                            if ( r0.hashCode() % 14745 == 0) {
                                generateStructure(GeneratedStructure.generateDungeon(r0.hashCode().toLong()), worldPos)
                            }
                        }
                        else {
                            val r = worldPos.hashCode()
                            if (r % 8000 == 0) generateStructure(GeneratedStructure.generateCopperVein(r.toLong()), worldPos)
                        }

                        val replaceChunk = blocksToReplace[chunkPos]
                        var replaceList: List<BlockReplacement>? = null
                        if (replaceChunk != null) replaceList = replaceChunk[posInChunk]
                        replaceList?.forEach { replace ->
                            if (replace.from.test(blocks[x][y][z])) blocks[x][y][z] = replace.to
                            replaceChunk!!.remove(posInChunk)
                            if (replaceChunk.isEmpty()) blocksToReplace.remove(chunkPos)
                        }
                        hasBlocks = true
                    }
                    else {
                        blocks[x][y][z] = 0

                        val skyLightAbove: Short
                        if (y < Chunk.extent -1) {
                            skyLightAbove = light[x][y+1][z] and 0b1111_0000_0000_0000.toShort()
                        }
                        else {
                            val aboveChunk = world.getChunkAt(chunkPos + IVec3(0,1,0))
                            if (aboveChunk != null) {
                                skyLightAbove = aboveChunk.getLightAt(IVec3(x,0,z)) and 0b1111_0000_0000_0000.toShort()
                            }
                            else {
                                if (worldPos.y + Chunk.extent > terrainHeight) skyLightAbove = 0b1111_0000_0000_0000.toShort()
                                else skyLightAbove = 0b0000_0000_0000_0000.toShort()
                            }
                        }
                        if (skyLightAbove == 0b1111_0000_0000_0000.toShort()) light[x][y][z] = skyLightAbove
                        else {
                            light[x][y][z] = 0b0000_0000_0000_0000.toShort()
                            skylightQueue.add(IVec3(x,y,z))
                        }
                    }
                    //if (light[x][y][z] == 0b0000_0000_0000_0000.toShort()) light[x][y][z] = 0b0000_0100_0110_1000.toShort()//debug
                }
            }
        }
        if (hasBlocks) chunk.setBlocksArray(blocks) else chunk.setBlocksArray(null)
        chunk.setLightArray(light)
        val neighborLightUpdateQueue = LinkedList<IVec3>()
        updateSkyLightQueue(skylightQueue, chunk, neighborLightUpdateQueue)
        world.updateSkyLightQueue(neighborLightUpdateQueue, false)
        if ((chunkWorldPos* Chunk.extent).y + Chunk.extent >= minHeight) {
            chunk.defaultLight = 0b1111_0000_0000_0000.toShort()
        }
        else chunk.defaultLight = 0
        return chunk
    }

    fun getBlockToPlaceAt(pos: IVec3): Short{
        val heightmap = heightmapNoise.GetNoise(pos.x.toFloat(), pos.z.toFloat()) * heightmapMultiplier + heightmapAddition
        val terrainMultiplier = 96
        val terrainValue = terrainNoise3D.GetNoise(pos.x.toFloat(), pos.y.toFloat(), pos.z.toFloat()) * terrainMultiplier
        val mixValue = mixNoise.GetNoise(pos.x.toFloat(), pos.z.toFloat())
        val mixedValue = mix(heightmap, terrainValue, clamp(mixValue+1f/2f, 0f, 1f))
        val scaledY = if (pos.y < 0) pos.y * 3 else pos.y
        if (mixedValue - scaledY > 0) {
            return stone ?: 1
        }
        return 0
    }

    fun getCaveAt(pos: IVec3): Short {
        return if (getCaveTunnelsAt(pos) || getCaveRoomsAt(pos)) 0 else stone ?:1
    }
    fun getCaveTunnelsAt(pos: IVec3): Boolean {
        val mid = 0.2f
        val dist = 0.1f
        val start = mid-2*dist
        val end = mid+2*dist
        var cave1 = caveTunnelNoise1.GetNoise(pos.x.toFloat(), pos.y.toFloat(), pos.z.toFloat())
        cave1 = if (cave1 > start && cave1 < end) (dist - (cave1-mid).absoluteValue) / dist else 0f
        cave1 = clamp(cave1, 0f, 1f)
        var cave2 = caveTunnelNoise2.GetNoise(pos.x.toFloat(), pos.y.toFloat(), pos.z.toFloat())
        cave2 = if (cave2 > start && cave2 < end) (dist - (cave2-mid).absoluteValue) / dist else 0f
        cave2 = clamp(cave2, 0f, 1f)
        val cave = cave1 * cave2
        return cave >= 0.01
    }

    fun getCaveRoomsAt(pos: IVec3): Boolean {
        var speleo2D = caveSpeleothermsNoise2D.GetNoise(pos.x.toFloat(), pos.z.toFloat())
        speleo2D = if (speleo2D < 0.5) 0f else 8*speleo2D-6
        val speleo3D = caveSpeleothermsNoise3D.GetNoise(pos.x.toFloat(), 2 * pos.y.toFloat(), pos.z.toFloat())
        val speleo = if (speleo2D+speleo3D < 0) 1f else 0f
        val rooms = caveRoomsNoise.GetNoise(pos.x.toFloat(), pos.y.toFloat(), pos.z.toFloat())
        val transformedY = clamp((pos.y+500)/1000f, 0f, 0.087f) +0.25f
        val t = mix(transformedY, speleo, rooms)
        return t < 0
    }

    private fun updateSkyLightQueue(lightQueue: LinkedList<IVec3>, chunk: Chunk, neighborLightUpdateQueue: LinkedList<IVec3>) {
        while (!lightQueue.isEmpty()) {
            val currentPos = lightQueue.pop()
            if (currentPos.x < 0 || currentPos.x >= Chunk.extent ||
                currentPos.y < 0 || currentPos.y >= Chunk.extent ||
                currentPos.z < 0 || currentPos.z >= Chunk.extent
            ) {
                neighborLightUpdateQueue.add(chunk.position+currentPos)
                continue
            }

            var currentLight = world.lightFromShortToVec4i(chunk.getLightAt(currentPos))
            val abovePos = currentPos + IVec3(0, 1, 0)
            val aboveLightLevel = if (abovePos.y >= Chunk.extent)
                                    world.getLightAt(chunk.position+abovePos)[3]
                                    else world.lightFromShortToVec4i(chunk.getLightAt(abovePos))[3]
            val lightLevel = if (aboveLightLevel == 15) 15 else {
                var maxLvl = 0
                for (nextDirection in listOf(
                    IVec3(-1, 0, 0), IVec3(1, 0, 0), IVec3(0, -1, 0), IVec3(0, 1, 0), IVec3(0, 0, -1), IVec3(0, 0, 1)
                )) {
                    val nextPos = currentPos + nextDirection
                    val nextLight = if (nextPos.x < 0 || nextPos.x >= Chunk.extent ||
                                        nextPos.y < 0 || nextPos.y >= Chunk.extent ||
                                        nextPos.z < 0 || nextPos.z >= Chunk.extent
                    )
                                        world.getLightAt(chunk.position+nextPos)
                                    else
                                        world.lightFromShortToVec4i(chunk.getLightAt(nextPos))
                    maxLvl = max(maxLvl, nextLight[3])
                }
                if (maxLvl == 0) 0 else maxLvl - 1
            }
            currentLight[3] = lightLevel
            chunk.setLightAt(currentPos, world.lightFromVec4iToShort(currentLight))

            currentLight = world.lightFromShortToVec4i(chunk.getLightAt(currentPos))
            for (nextDirection in listOf(
                IVec3(-1, 0, 0), IVec3(1, 0, 0), IVec3(0, 1, 0), IVec3(0, 0, -1), IVec3(0, 0, 1)
            )) {
                val nextPos = currentPos + nextDirection
                if (nextPos.x < 0 || nextPos.x >= Chunk.extent ||
                    nextPos.y < 0 || nextPos.y >= Chunk.extent ||
                    nextPos.z < 0 || nextPos.z >= Chunk.extent
                ) {
                    neighborLightUpdateQueue.add(chunk.position+nextPos)
                    continue
                }

                val nextBlock = chunk.getBlockAt(nextPos)
                val nextLight = world.lightFromShortToVec4i(chunk.getLightAt(nextPos))
                if (nextBlock == 0.toShort() || BlockRegistry.getBlockById(nextBlock)?.solid == false) {
                    if (nextLight[3] + 2 <= currentLight[3]) {
                        nextLight[3] = currentLight[3] - 1
                        chunk.setLightAt(nextPos, world.lightFromVec4iToShort(nextLight))
                        lightQueue.add(nextPos)
                    }
                }
            }
            val nextPos = currentPos + IVec3(0, -1, 0)
            if (!(nextPos.x < 0 || nextPos.x >= Chunk.extent ||
                nextPos.y < 0 || nextPos.y >= Chunk.extent ||
                nextPos.z < 0 || nextPos.z >= Chunk.extent)) {
                val nextBlock = chunk.getBlockAt(nextPos)
                val nextLight = world.lightFromShortToVec4i(chunk.getLightAt(nextPos))
                if (nextBlock == 0.toShort() || BlockRegistry.getBlockById(nextBlock)?.solid == false) {
                    if (nextLight[3] < currentLight[3]) {
                        nextLight[3] = currentLight[3]
                        chunk.setLightAt(nextPos, world.lightFromVec4iToShort(nextLight))
                        lightQueue.add(nextPos)
                    }
                }
            }
            else {
                neighborLightUpdateQueue.add(chunk.position+nextPos)
            }
        }
    }


    private fun replaceBlockAt(chunkPos: IVec3, posInChunk: IVec3, from: Predicate<Short>, to: Short) {
        val chunk = world.getChunkAt(chunkPos)
        if (chunk != null) {
            if (from.test(chunk.getBlockAt(posInChunk)))
                chunk.setBlockAt(posInChunk, to)
        }
        else {
            addToReplaceBlocks(chunkPos, posInChunk, from, to)
        }
    }

    //this is for if you don't want to check the world's chunk (you already know the chunk (like currently generating chunk)
    private fun addToReplaceBlocks(chunkPos: IVec3, posInChunk: IVec3, from: Predicate<Short>, to: Short) {
        val oldMap = blocksToReplace[chunkPos]
        if (oldMap == null) {
            val newMap = ConcurrentHashMap<IVec3, MutableList<BlockReplacement>>()
            blocksToReplace[chunkPos] = newMap
            newMap[posInChunk] = mutableListOf(BlockReplacement(from, to))
        }
       else {
            val oldList = oldMap[posInChunk]
            if (oldList != null) oldList.add(BlockReplacement(from, to))
            else oldMap[posInChunk] = mutableListOf(BlockReplacement(from, to))
        }
    }

    fun replaceBlocksInChunk(chunk: Chunk, chunkPosition: IVec3? = null): Boolean {
        var blocksReplaced = false
        val chunkPos: IVec3 = chunkPosition ?: Chunk.worldPosToChunkPos(chunk.position)
        var replaceBlocks = blocksToReplace[chunkPos]
        while (replaceBlocks != null) {
            blocksToReplace.remove(chunkPos)
            replaceBlocks.forEach { it ->
                it.value.forEach { replacement ->
                    if (replacement.from.test(chunk.getBlockAt(it.key))) {
                        blocksReplaced = true
                        chunk.setBlockAt(it.key, replacement.to)
                    }
                }
            }
            replaceBlocks = blocksToReplace[chunkPos]
        }
        return blocksReplaced
    }

    fun generateStructure(structure: GeneratedStructure, worldPos: IVec3) {
        structure.blockReplacements.forEach { (offset, replace) ->
            val blockWorldPos = worldPos + offset
            val blockChunkPos = Chunk.worldPosToChunkPos(blockWorldPos)
            val blockPosInChunk = Chunk.worldPosToPosInChunk(blockWorldPos)
            replaceBlockAt(blockChunkPos, blockPosInChunk, replace.from, replace.to)
        }
    }
}