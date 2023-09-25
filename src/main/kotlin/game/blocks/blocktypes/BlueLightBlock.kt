package game.blocks.blocktypes

import math.datatype.vec.IVec3


data object BlueLightBlock: LightSourceBlockType() {
    override val lightLevels: IVec3 = IVec3(0,0,15)
    override val name: String = "Blue Light Block"
    override val solid: Boolean = true
}