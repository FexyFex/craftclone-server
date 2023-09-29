package game.world.saving

import FileSystem
import game.world.Chunk
import game.world.World
import math.datatype.vec.IVec3
import net.jpountz.lz4.LZ4Factory
import util.for3D
import util.getInt
import util.toByteArray
import util.ushr
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.lang.Exception
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.SortedMap
import java.util.SortedSet


object WorldSaver {
    val factory = LZ4Factory.fastestInstance()
    val compressor = factory.fastCompressor()
    val decompressor = factory.fastDecompressor()

    private object PosInRegionComparator: Comparator<IVec3> {
        override fun compare(o1: IVec3?, o2: IVec3?): Int {
            if (o1 == null || o2 == null) return 0
            val x = o1.x - o2.x
            if (x != 0) return x
            else {
                val y = o1.y - o2.y
                if (y != 0) return y
                else return o1.z - o2.z
            }
        }

    }

    fun saveChunks(world: World, chunksToSave: List<Chunk>) {
        val chunksInRegions = mutableMapOf<IVec3, SortedMap<IVec3, Chunk>>()
        for (chunk in chunksToSave) { // sort chunks into their respective regions (sorted by position within the regions)
            val chunkRegionPos = Region.worldPosToRegionPos(chunk.position)
            val chunkPos = Chunk.worldPosToChunkPos(chunk.position)
            val chunkPosInRegion = Region.chunkPosToChunkPosInRegion(chunkPos)
            if (chunksInRegions[chunkRegionPos] == null) chunksInRegions[chunkRegionPos] =
                sortedMapOf(PosInRegionComparator, Pair(chunkPosInRegion, chunk))
            else chunksInRegions[chunkRegionPos]!![chunkPosInRegion] = chunk
        }
        for (region in chunksInRegions) {
            val regionPos = region.key
            val chunks = region.value
            val worldDirectory = File(FileSystem.worldDir, "\\regions")
            worldDirectory.mkdirs()
            val oldFile = File(FileSystem.worldDir, ""+regionPos.x+"."+regionPos.y+"."+regionPos.z+".voxl")
            val newFile = File(FileSystem.worldDir, ""+regionPos.x+"."+regionPos.y+"."+regionPos.z+".voxl_new")
            if (newFile.createNewFile()) { // good, we can create the file
                val writer = FileOutputStream(newFile, true)
                // write header
                writer.write("VOXL".toByteArray()) // signature
                val version = ByteBuffer.allocate(4).putInt(1).array()
                writer.write(version) // file format version
                if (oldFile.exists()) {
                    // read old file
                    val reader = FileInputStream(oldFile)
                    if (!(reader.read().toChar()=='V' && reader.read().toChar()=='O' && reader.read().toChar()=='X' && reader.read().toChar()=='L')) {
                        println("Wrong signature!")
                    }
                    val oldVersionArray = ByteArray(4)
                    reader.read(oldVersionArray, 0, 4)
                    val oldVersion = oldVersionArray.getInt(0)
                    if (oldVersion > 1) println("Unknown voxl file format version: $oldVersion")
                    // write new file with interspersed new and old chunks
                    for3D(0 until Region.extent, 0 until Region.extent, 0 until Region.extent) { x,y,z ->
                        val chunk = chunks[IVec3(x,y,z)]
                        if (chunk == null) {
                            // read chunk length and chunk data from old file
                            val saved = reader.read()
                            if (saved == 1 || oldVersion == 0) {
                                val lengthArray = ByteArray(4)
                                reader.read(lengthArray, 0, 4)
                                val chunkLength = lengthArray.getInt(0)
                                val oldData = ByteArray(chunkLength)
                                reader.read(oldData, 0, chunkLength)
                                // and write it
                                writer.write(saved)
                                writer.write(lengthArray)
                                writer.write(oldData)
                            }
                            else writer.write(saved)
                        }
                        else {
                            // compress chunk and write data
                            val chunkData = compressChunk(chunk)
                            writer.write(1) // chunk exists
                            writer.write(chunkData.size.toByteArray())
                            writer.write(chunkData)
                            // skip the chunk in oldFile
                            val saved = reader.read() // saved/chunk exists
                            if (saved == 1 || oldVersion == 0) {
                                val lengthArray = ByteArray(4)
                                reader.read(lengthArray, 0, 4)
                                val chunkLength = lengthArray.getInt(0)
                                reader.skip(chunkLength.toLong())
                            }
                            chunk.existsOnDisk = true
                            chunk.dirtyBlocks = false
                        }
                    }
                    reader.close()
                }
                else {
                    // write new file with new chunks
                    for3D(0 until Region.extent, 0 until Region.extent, 0 until Region.extent) { x,y,z ->
                        val chunk = chunks[IVec3(x,y,z)]
                        if (chunk == null) {
                            writer.write(0)
                        }
                        else {
                            val chunkData = compressChunk(chunk)
                            writer.write(1) // chunk exists
                            writer.write(ByteBuffer.allocate(4).putInt(chunkData.size).array())
                            writer.write(chunkData)
                        }
                    }
                }
                writer.close()
                // move newFile to overwrite oldFile
                while (true) {
                    try {
                        Files.move(newFile.toPath(), oldFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
                        break
                    } catch (e: Exception) {
                        continue
                    }
                }

            }
            else { // file already exists
                println("why exists????")
            }
        }
    }

    fun loadChunks(world: World, chunksToLoad: List<IVec3>): List<Pair<IVec3, Chunk?>> {
        val chunksInRegions = mutableMapOf<IVec3, SortedSet<IVec3>>()
        for (pos in chunksToLoad) { // sort chunk pos into their respective regions (sorted by position within the regions)
            val chunkRegionPos = Region.chunkPosToRegionPos(pos)
            val chunkPosInRegion = Region.chunkPosToChunkPosInRegion(pos)
            if (chunksInRegions[chunkRegionPos] == null) chunksInRegions[chunkRegionPos] =
                sortedSetOf(PosInRegionComparator, chunkPosInRegion)
            else chunksInRegions[chunkRegionPos]!!.add(chunkPosInRegion)
        }
        val loadedChunks = mutableListOf<Pair<IVec3, Chunk?>>()
        for (region in chunksInRegions) {
            val regionPos = region.key
            val chunkPositions = region.value
            val worldDirectory = File(FileSystem.worldDir, "\\regions")
            val file = File(worldDirectory, ""+regionPos.x+"."+regionPos.y+"."+regionPos.z+".voxl")
            if (file.exists()) {
                val reader = FileInputStream(file)
                if (!(reader.read().toChar()=='V' && reader.read().toChar()=='O' && reader.read().toChar()=='X' && reader.read().toChar()=='L')) {
                    println("Wrong signature!")
                }
                val oldVersionArray = ByteArray(4)
                reader.read(oldVersionArray, 0, 4)
                val oldVersion = oldVersionArray.getInt(0)
                if (oldVersion > 1) println("Unknown voxl file format version: $oldVersion")
                var chunksRead = 0
                for (pos in chunkPositions) {
                    val chunkPos = regionPos*Region.extent+pos
                    val posIndex = pos.z + (pos.y + pos.x * Region.extent) * Region.extent
                    while (chunksRead < posIndex) { // skip until the wanted chunk is reached
                        val saved = reader.read()
                        if (saved == 1 || oldVersion == 0) {
                            val lengthArray = ByteArray(4)
                            reader.read(lengthArray, 0, 4)
                            val chunkLength = lengthArray.getInt(0)
                            reader.skip(chunkLength.toLong())
                        }
                        chunksRead++
                    }
                    // this is the chunk we want
                    val saved = reader.read()
                    val chunkLength: Int
                    var chunkData : ByteArray
                    if (saved == 1 || oldVersion == 0) {
                        val lengthArray = ByteArray(4)
                        reader.read(lengthArray, 0, 4)
                        chunkLength = lengthArray.getInt(0)
                        chunkData = ByteArray(chunkLength)
                        reader.read(chunkData, 0, chunkLength)
                    }
                    else {
                        chunkLength = 0
                        chunkData = ByteArray(0)
                    }
                    chunksRead++
                    if (saved == 0 || chunkLength == 0)
                        loadedChunks.add(Pair(chunkPos, null))
                    else {
                        val chunk = decompressChunk(world, chunkData, chunkPos * Chunk.extent)
                        chunk.existsOnDisk = true
                        loadedChunks.add(Pair(chunkPos, chunk))
                    }
                }
            }
            else {
                for (pos in chunkPositions) {
                    val chunkPos = regionPos*Region.extent+pos
                    loadedChunks.add(Pair(chunkPos, null))
                }
            }
        }

        return loadedChunks
    }

    fun compressChunk(chunk: Chunk): ByteArray {
        val data = getChunkData(chunk)
        val decompressedLength = data.size
        val maxCompressedLength = compressor.maxCompressedLength(decompressedLength)
        val compressed = ByteArray(maxCompressedLength)
        val compressedLength = compressor.compress(data, 0, decompressedLength, compressed, 0, maxCompressedLength)
//        println("Decompressed length: $decompressedLength")
//        println("Compressed length:   $compressedLength")
//        println("Ratio:               " + decompressedLength.toFloat()/compressedLength.toFloat())
        val res = ByteArray(compressedLength)
        for (i in 0 until compressedLength) {
            res[i] = compressed[i]
        }
        return res
    }

    fun decompressChunk(world: World, data: ByteArray, chunkPosition: IVec3): Chunk {
        val compressedLength = data.size
        val expectedDecompressedLength = if (data.size > 4) Chunk.extent * Chunk.extent * Chunk.extent *4 + 3 else 3
        val restored = ByteArray(expectedDecompressedLength)
        try {
            val compressedLength2 = decompressor.decompress(data, 0, restored, 0, expectedDecompressedLength)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return createChunkFromByteArray(world, restored, chunkPosition)
    }

    fun getChunkData(chunk: Chunk): ByteArray {
        if (chunk.hasBlocks) {
            val data = ByteArray(Chunk.extent * Chunk.extent * Chunk.extent *4 + 3) {0.toByte()}
            data[0] = 1
            data[1] = chunk.defaultLight.toByte()
            data[2] = (chunk.defaultLight ushr 8).toByte()
            var index = 3
            chunk.blocks!!.forEachIndexed { x, xArrays ->
                xArrays.forEachIndexed { y, blocksY ->
                    blocksY.forEachIndexed { z, block ->
                        //val index = (z + Chunk.extent*(y + x * Chunk.extent)) * 2
                        data[index] = block.toByte()
                        data[index+1] = (block ushr 8).toByte()
                        index += 2
                    }
                }
            }
            chunk.light!!.forEachIndexed { x, xArrays ->
                xArrays.forEachIndexed { y, lightsY ->
                    lightsY.forEachIndexed { z, light ->
                        //val index = (z + Chunk.extent*(y + x * Chunk.extent)) * 2
                        data[index] = light.toByte()
                        data[index + 1] = (light ushr 8).toByte()
                        index += 2
                    }
                }
            }
            return data
        }
        else return byteArrayOf(0.toByte(), chunk.defaultLight.toByte(), (chunk.defaultLight ushr 8).toByte())
    }

    fun createChunkFromByteArray(world: World, data: ByteArray, chunkPosition: IVec3): Chunk {
        val chunk = Chunk(world, chunkPosition)
        if (data[0] == 0.toByte()) {
            chunk.hasBlocks = false
            chunk.defaultLight = ((0+data[1]) or (0+data[2]) shl 8).toShort()
        }
        else {
            chunk.hasBlocks = true
            chunk.defaultLight = ((0+data[1]) or (0+data[2]) shl 8).toShort()
            chunk.createBlocksArray()
            chunk.createLightArray()
            var index = 3
            chunk.blocks!!.forEachIndexed { x, xArrays ->
                xArrays.forEachIndexed { y, blocksY ->
                    blocksY.forEachIndexed { z, _ ->
                        chunk.blocks!![x][y][z] = ((0+data[index]) or ((0+data[index+1]) shl 8)).toShort()
                        index += 2
                    }
                }
            }
            chunk.light!!.forEachIndexed { x, xArrays ->
                xArrays.forEachIndexed { y, lightsY ->
                    lightsY.forEachIndexed { z, _ ->
                        chunk.light!![x][y][z] = ((0+data[index]) or ((0+data[index+1]) shl 8)).toShort()
                        index += 2
                    }
                }
            }
        }
        return chunk
    }

}
