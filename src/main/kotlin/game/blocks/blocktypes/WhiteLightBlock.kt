package game.blocks.blocktypes

import math.datatype.vec.IVec3

data object WhiteLightBlock: LightSourceBlockType() {
    override val lightLevels: IVec3 = IVec3(15,15,15)
    override val name: String = "White Light Block"
    override val solid: Boolean = true
}