package networking

import networking.packet.Packet

class UnexpectedPacketException(packet: Packet): Exception("Packet ${packet::class.simpleName} not allowed in this context")