package game.blocks.blocktypes

import math.datatype.vec.IVec3


sealed class LightSourceBlockType: BlockType() {
    abstract val lightLevels: IVec3
}