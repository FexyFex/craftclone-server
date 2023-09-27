package game.properties

class ServerPropertyWorldSeed(override val value: String): ServerProperty() {
    constructor(): this((Math.random() * Long.MAX_VALUE).toLong().toString())
    override val name: String = "world_seed"
}