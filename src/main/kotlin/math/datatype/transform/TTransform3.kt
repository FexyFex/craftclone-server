package math.datatype.transform

import math.datatype.vec.TVec3

abstract class TTransform3<T: Number> {
    abstract var position: TVec3<T>
    abstract var rotation: TVec3<T>
}