package game.world.saving

import game.RemotePlayer
import util.getDouble
import util.getFloat
import util.getInt
import util.toByteArray
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.StandardCopyOption

object PlayerSaver {
    fun savePlayer(player: RemotePlayer) {
        val intBuf = ByteBuffer.allocate(4)

        val playersDirectory = File(FileSystem.playerDir)
        val oldPlayerFile = File(playersDirectory, "${player.name}.player")
        val playerFile = File(playersDirectory, "${player.name}.player_new")
        playersDirectory.mkdirs()
        val writer = FileOutputStream(playerFile, true)
        writer.write("PLYR".toByteArray())
        intBuf.putInt(0, 0)
        writer.write(intBuf.toByteArray()) // player file format version
        writer.write(player.pos.x.toByteArray())
        writer.write(player.pos.y.toByteArray())
        writer.write(player.pos.z.toByteArray())
        writer.write(player.rotation.x.toByteArray())
        writer.write(player.rotation.y.toByteArray())
        writer.write(player.rotation.z.toByteArray())
        intBuf.putInt(0, 20) //TODO: save health
        writer.write(intBuf.toByteArray())
//        player.inventory.forEachSlot { //TODO: save inventory
//            if (it != null) {
//                intBuf.putInt(0, it.itemID)
//                writer.write(intBuf.toByteArray())
//                intBuf.putInt(0, it.amount)
//                writer.write(intBuf.toByteArray())
//            } else {
//                intBuf.putInt(0, 0)
//                writer.write(intBuf.toByteArray())
//                writer.write(intBuf.toByteArray())
//            }
//        }
        writer.close()
        Files.move(playerFile.toPath(), oldPlayerFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
    }

    fun loadPlayer(playerName: String) : RemotePlayer {
        val playersDirectory = File(FileSystem.playerDir)
        val playerFile = File(playersDirectory, "$playerName.player")
        val player = RemotePlayer(playerName)
        if (playerFile.exists()) {
            val reader = FileInputStream(playerFile)
            val signatureAndVersion = ByteArray(8)
            reader.read(signatureAndVersion)
            if (signatureAndVersion.decodeToString(0,4) != "PLYR") println("Wrong player file signature!")
            if (signatureAndVersion.getInt(4) != 0) println("Unknown player file format version!")
            val coordinate = ByteArray(Double.SIZE_BYTES)
            reader.read(coordinate)
            player.pos.x = coordinate.getDouble(0, false)
            reader.read(coordinate)
            player.pos.y = coordinate.getDouble(0, false)
            reader.read(coordinate)
            player.pos.z = coordinate.getDouble(0, false)
            val rotationValue = ByteArray(Float.SIZE_BYTES)
            reader.read(rotationValue)
            player.rotation.x = rotationValue.getFloat(0, false).toDouble()
            reader.read(rotationValue)
            player.rotation.y = rotationValue.getFloat(0, false).toDouble()
            reader.read(rotationValue)
            player.rotation.z = rotationValue.getFloat(0, false).toDouble()
            val arrayForInts = ByteArray(Int.SIZE_BYTES)
            reader.read(arrayForInts)
            //TODO: health and inventory
            //player.getComponent<Health>()?.healthPoints = arrayForInts.getInt(0)
//            player.inventory.forEachSlotIndexed { index, _ ->
//                reader.read(arrayForInts)
//                val id = arrayForInts.getInt(0)
//                reader.read(arrayForInts)
//                val amount = arrayForInts.getInt(0)
//                player.inventory.emptySlot(index)
//                if( id != 0) player.inventory[index] = ItemStack(id, amount)
//            }
            reader.close()
        }
        else {
            savePlayer(player)
        }
        return player
    }

}