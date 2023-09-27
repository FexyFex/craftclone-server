package networking

class UnknownPacketException(signature: Int): Exception("Unexpected Packet with signature $signature")