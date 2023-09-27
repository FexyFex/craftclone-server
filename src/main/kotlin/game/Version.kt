package game

data class Version(val major: Int, val minor: Int = 0, val patch: Int = 0) {
    override fun toString(): String {
        return "$major.$minor.$patch"
    }
}