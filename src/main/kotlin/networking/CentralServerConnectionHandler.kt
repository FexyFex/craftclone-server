package networking


class CentralServerConnectionHandler : NetworkServerConnectionHandler {
    override suspend fun clientConnected(client: NetworkClient) {
        // helo this is called when client connect
        ClientConnectionThread(client).start()
    }
}