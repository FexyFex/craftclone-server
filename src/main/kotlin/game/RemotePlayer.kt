package game

import math.datatype.transform.DTransform3


class RemotePlayer(val name: String) {
    private lateinit var worldPlayer: WorldPlayer

    fun placeInWorld(transform: DTransform3): RemotePlayer {
        if (!this::worldPlayer.isInitialized) worldPlayer = WorldPlayer(transform)
        return this
    }


    class WorldPlayer(val transform: DTransform3)
}