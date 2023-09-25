package game.blocks.blocktypes

import math.datatype.vec.IVec3

data object GreenLightBlock: LightSourceBlockType() {
    override val lightLevels: IVec3 = IVec3(0,15,0)
    override val name: String = "Green Light Block"
    override val solid: Boolean = true
}