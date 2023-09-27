package game.properties

sealed class ServerProperty {
    abstract val name: String
    abstract val value: String
}