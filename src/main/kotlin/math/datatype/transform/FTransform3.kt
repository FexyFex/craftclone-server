package math.datatype.transform

import math.datatype.vec.TVec3


data class FTransform3(override var position: TVec3<Float>, override var rotation: TVec3<Float>): TTransform3<Float>()