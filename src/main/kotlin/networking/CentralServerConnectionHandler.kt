package networking


class CentralServerConnectionHandler : NetworkServerConnectionHandler {
    override suspend fun clientConnected(client: NetworkClient) {
        // helo this is called when client connect
        // do not block this scope long yes good
        ClientConnectionThread(client).run()
    }
}