package networking

enum class DisconnectReason(val value: Byte) {
    UNKNOWN(0),
    LOGOUT(1),
    TIMEOUT(2),
    KICKED(3),
    BANNED(4),
    SERVER_CLOSED(5)
}