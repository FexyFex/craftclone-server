package game.blocks

import FileSystem
import game.blocks.blocktypes.AirBlock
import game.blocks.blocktypes.BlockType
import game.items.ItemRegistry
import java.io.File


object BlockRegistry {
    private val registeredBlocks = mutableMapOf<Short, BlockType>()
    private val registeredBlockNames = mutableMapOf<String, Short>()
    private var nextId: Short = 0.toShort()
    val blockCount: Int; get() = registeredBlocks.size

    init {
        registerBlock(AirBlock)

        val blockTypes = BlockType::class.sealedSubclasses
            .flatMap { it.sealedSubclasses + it }
            .mapNotNull { it.objectInstance }
            .minus(AirBlock)
            .toMutableList()

        val worldDirectory = File(FileSystem.worldDir)
        val savedRegistryFile = File(worldDirectory, "block-registry.blr")
        if (savedRegistryFile.exists()) {
            val reader = savedRegistryFile.bufferedReader()
            var line : String? = ""
            while (line != null) {
                line = reader.readLine()
                if (line == null) break
                val split = line.split(":")
                val id =  split[0].toShort()
                val blockName = split[1]
                val block = blockTypes.firstOrNull { it.name == blockName }
                if (block != null) {
                    nextId = id
                    registerBlock(block)
                    blockTypes.remove(block)
                }
            }
            nextId = registeredBlocks.size.toShort()
            reader.close()
        }

        blockTypes.forEach {
            registerBlock(it)
        }

        if (blockTypes.isNotEmpty()) { // blocks were added. save block registy
            worldDirectory.mkdirs()
            val writer = savedRegistryFile.bufferedWriter()
            registeredBlockNames.forEach {
                writer.write("${it.value}:${it.key}")
                writer.newLine()
            }
            writer.close()
        }

        ItemRegistry
    }

    private fun registerBlock(blockType: BlockType) {
        if (nextId < 0) throw Exception("Things can only go so far")
        if (registeredBlocks.containsValue(blockType)) return
        val id = nextId++
        registeredBlocks[id] = blockType
        registeredBlockNames[blockType.name] = id
        blockType.blockID = id
    }

    fun getBlockById(id: Short): BlockType? = registeredBlocks[id]
    fun getBlockByName(name: String): BlockType? {
        val id = registeredBlockNames[name]
        return if (id != null) registeredBlocks[id]
        else null
    }

    fun getIdByName(name: String): Short? {
        return registeredBlockNames[name]
    }

    fun forEachBlock(action: (BlockType) -> Unit) = registeredBlocks.forEach { (_, blockType) ->
        action(blockType)
    }

    fun <T> mapBlocks(mapAction: (BlockType) -> T) = registeredBlocks.values.map(mapAction)
    fun <T> flatMapBlocks(mapAction: (BlockType) -> Iterable<T>) = registeredBlocks.values.flatMap(mapAction)
}