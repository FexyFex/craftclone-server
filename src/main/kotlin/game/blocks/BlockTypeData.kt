package game.blocks


data class BlockTypeData(val modelID: Int, val textureIndex: Int, val transparency: Boolean) {
    fun toPackedInt(): Int {
        val transparencyInt = if (transparency) 1 else 0
        return modelID or (textureIndex shl 10) or (transparencyInt shl 22)
    }

    companion object {
        const val SIZE_BYTES: Long = Int.SIZE_BYTES.toLong()
    }
}
