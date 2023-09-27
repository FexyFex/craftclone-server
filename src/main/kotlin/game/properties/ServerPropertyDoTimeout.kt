package game.properties

class ServerPropertyDoTimeout(override val value: String): ServerProperty() {
    constructor(): this("true")
    override val name: String = "do_timeout"
}