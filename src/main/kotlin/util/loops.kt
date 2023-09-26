package util


fun for3D(rangeX: IntRange, rangeY: IntRange, rangeZ: IntRange, action: (x: Int, y: Int, z: Int) -> Unit) {
    for (x in rangeX) {
        for (y in rangeY) {
            for (z in rangeZ) {
                action(x,y,z)
            }
        }
    }
}

fun repeat3D(xTimes: Int, yTimes: Int, zTimes: Int, action: (x: Int, y: Int, z: Int) -> Unit) {
    repeat(xTimes) { x ->
        repeat(yTimes) { y ->
            repeat(zTimes) { z ->
                action(x,y,z)
            }
        }
    }
}

fun repeatCubic3D(times: Int, action: (x: Int, y: Int, z: Int) -> Unit) {
    repeat(times) { x ->
        repeat(times) { y ->
            repeat(times) { z ->
                action(x,y,z)
            }
        }
    }
}