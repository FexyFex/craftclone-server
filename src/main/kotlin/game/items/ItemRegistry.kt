package game.items

import FileSystem
import game.blocks.BlockRegistry
import game.blocks.blocktypes.BlockType
import game.items.type.BlockItem
import game.items.type.ItemType
import java.io.File


object ItemRegistry {
    private val registeredItems = mutableMapOf<Int, ItemType>()
    private val registeredItemNames = mutableMapOf<String, Int>()
    private var nextId: Int = 0


    init {
        val itemTypes = ItemType::class.sealedSubclasses.minus(BlockItem::class).map { it.objectInstance!! }.toMutableList()
        val blockTypes = BlockRegistry.mapBlocks { it }.toMutableList()
        val worldDirectory = File(FileSystem.worldDir)
        val savedRegistryFile = File(worldDirectory, "item-registry.itr")
        if (savedRegistryFile.exists()) {
            val reader = savedRegistryFile.bufferedReader()
            var line : String? = ""
            while (line != null) {
                line = reader.readLine()
                if (line == null) break
                val split = line.split(":")
                val id =  split[0].toInt()
                val itemName = split[1]
                val itemFromBlock = BlockRegistry.getBlockByName(itemName)
                if (itemFromBlock != null) {
                    blockTypes.remove(itemFromBlock)
                    nextId = id
                    registerBlockItem(itemFromBlock)
                } else {
                    val itemFromType = itemTypes.firstOrNull { it.name == itemName }
                    if (itemFromType != null) {
                        itemTypes.remove(itemFromType)
                        nextId = id
                        registerItem(itemFromType)
                    }
                }
            }
            nextId = registeredItems.size
            reader.close()
        }
        blockTypes.forEach {
            registerBlockItem(it)
        }
        itemTypes.forEach {
            registerItem(it)
        }
        if (itemTypes.size > 0 || blockTypes.size > 0) { // blocks were added. save block registy
            worldDirectory.mkdirs()
            val writer = savedRegistryFile.bufferedWriter()
            registeredItemNames.forEach {
                writer.write(""+it.value+":"+it.key)
                writer.newLine()
            }
            writer.close()
        }
        //BlockRegistry.forEachBlock(::registerBlockItem)
        //ItemTypeFinder.findAllItemsInPackage("game.item.type").forEach(::registerItem)
        //registeredItemNames.forEach { println("Item: ${it.key} ${it.value}") }
    }


    private fun registerBlockItem(blockType: BlockType) {
        val id = nextId++
        if (id != blockType.blockID.toInt())
            println("WARNING: BLOCK ${blockType.name} IS BEING ASSIGNED ITEM ID $id NOT EQUAL TO ITS BLOCK ID ${blockType.blockID}")
        blockType.itemID = id
        val blockItem = BlockItem(blockType.name, blockType.blockID)
        registeredItems[id] = blockItem
        registeredItemNames[blockItem.name] = id
        blockItem.itemID = id
    }


    private fun registerItem(itemType: ItemType) {
        if (registeredItems.containsValue(itemType)) return
        val id = nextId++
        registeredItems[id] = itemType
        registeredItemNames[itemType.name] = id
        itemType.itemID = id
    }


    fun getIdByName(name: String): Int? = registeredItemNames[name]
    fun getItemById(id: Int): ItemType? = registeredItems[id]
    fun getItemByName(name: String): ItemType? {
        val id = registeredItemNames[name] ?: return null
       return registeredItems[id]
    }


    fun forEachItem(action: (ItemType) -> Unit) = registeredItems.forEach { (id, itemType) ->
        action(itemType)
    }
}