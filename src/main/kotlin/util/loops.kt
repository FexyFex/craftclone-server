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