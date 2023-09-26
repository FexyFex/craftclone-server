package game.items.type

sealed class ItemType {
    abstract val name: String

    var itemID: Int = -1
}