package math.datatype.transform

import math.datatype.vec.TVec3

class DTransform3(override var position: TVec3<Double>, override var rotation: TVec3<Double>): TTransform3<Double>()