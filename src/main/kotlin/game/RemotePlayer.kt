package game

import math.datatype.transform.Transform
import math.datatype.vec.DVec3


class RemotePlayer(val name: String) {
    private lateinit var worldPlayer: WorldPlayer
    val pos: DVec3; get() = worldPlayer.transform.position
    val rotation: DVec3; get() = worldPlayer.transform.rotation


    fun placeInWorld(transform: Transform): RemotePlayer {
        if (!this::worldPlayer.isInitialized) worldPlayer = WorldPlayer(transform)
        return this
    }

    fun moveBy(velocity: DVec3) {
        // TODO: Collision
        worldPlayer.transform.position += velocity
    }

    fun rotateBy(rotation: DVec3) {
        worldPlayer.transform.rotation += rotation
    }


    class WorldPlayer(val transform: Transform)
}