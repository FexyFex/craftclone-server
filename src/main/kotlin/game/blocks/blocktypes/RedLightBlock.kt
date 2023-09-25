package game.blocks.blocktypes

import math.datatype.vec.IVec3

data object RedLightBlock: LightSourceBlockType() {
    override val lightLevels: IVec3 = IVec3(15,0,0)
    override val name: String = "Red Light Block"
    override val solid: Boolean = true
}