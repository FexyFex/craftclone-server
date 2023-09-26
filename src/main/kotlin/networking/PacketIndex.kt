package networking

import networking.packet.Packet

object PacketIndex {
    private val packets = mutableMapOf<Int, Packet>()

    init {
        Packet::class.sealedSubclasses.map { it.objectInstance!! }.forEach {
            if (packets.containsKey(it.signature))
                throw IllegalArgumentException("Key ${it.signature} already exists!")
            packets[it.signature] = it
        }
    }


    fun getPacketBySignature(signature: Int) = packets[signature]
}