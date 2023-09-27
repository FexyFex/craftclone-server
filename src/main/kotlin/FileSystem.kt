import java.io.File

object FileSystem {
    val workingDir = System.getProperty("user.dir")
    val worldDir = "$workingDir/world/"
    val playerDir = "$workingDir/players/"
    val propertiesFile = File("$workingDir/game.properties.txt")
}