package networking

interface NetworkServerConnectionHandler {
    suspend fun clientConnected(client: NetworkClient)
}