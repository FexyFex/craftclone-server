package networking

enum class ConnectionRejectedReason(val byte: Byte) {
    UNKNOWN(0),
    WHITELIST_ENABLED(1),
    BANNED(2),
    PLAYER_NAME_ALREADY_ONLINE(3),
    AUTHENTICATION_FAILED(4)
}