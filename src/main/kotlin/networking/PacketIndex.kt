package networking

import networking.packet.Packet

object PacketIndex {
    private val packets = mutableMapOf<Int, Packet>()

    init {
        Packet::class.sealedSubclasses.map { it.objectInstance!! }.forEach {
            packets[it.signature] = it
        }
    }


    fun getPacketBySignature(signature: Int) = packets[signature]
}