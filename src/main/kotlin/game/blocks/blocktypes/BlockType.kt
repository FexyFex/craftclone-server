package game.blocks.blocktypes

sealed class BlockType {
    abstract val name: String
    abstract val solid: Boolean

    var blockID: Short = -1
    var itemID: Int = -1
}