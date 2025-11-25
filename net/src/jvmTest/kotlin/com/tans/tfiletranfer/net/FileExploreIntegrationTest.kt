package com.tans.tfiletranfer.net

import com.tans.tfiletransfer.net.socket.findLocalAddressV4
import com.tans.tfiletransfer.net.transferproto.fileexplore.FileExploreClient
import com.tans.tfiletransfer.net.transferproto.fileexplore.FileExploreServer
import com.tans.tfiletransfer.net.transferproto.fileexplore.IDeviceExplorer
import com.tans.tfiletransfer.net.transferproto.fileexplore.IRemoteRequestDownloadHandler
import com.tans.tfiletransfer.net.transferproto.fileexplore.IRemoteRequestSendHandler
import com.tans.tfiletransfer.net.transferproto.fileexplore.model.ExplorerDir
import com.tans.tfiletransfer.net.transferproto.fileexplore.model.ExplorerFile
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

object FileExploreIntegrationTest {

    private class ServerDeviceExplorer : IDeviceExplorer {
        override fun explore(path: String): Pair<List<ExplorerDir>, List<ExplorerFile>> {
            val d = ExplorerDir(name = "srv", path = "/srv", childrenCount = 1, lastModify = System.currentTimeMillis())
            val f = ExplorerFile(name = "srv.txt", path = "/srv/srv.txt", size = 10, lastModify = System.currentTimeMillis())
            return listOf(d) to listOf(f)
        }
    }

    private class ClientDeviceExplorer : IDeviceExplorer {
        override fun explore(path: String): Pair<List<ExplorerDir>, List<ExplorerFile>> {
            val d = ExplorerDir(name = "cli", path = "/cli", childrenCount = 0, lastModify = System.currentTimeMillis())
            val f = ExplorerFile(name = "cli.txt", path = "/cli/cli.txt", size = 5, lastModify = System.currentTimeMillis())
            return listOf(d) to listOf(f)
        }
    }

    private class ServerSendHandler : IRemoteRequestSendHandler {
        override fun onRequestSend(requestSendFiles: List<ExplorerFile>, maxConnection: Int): Int? {
            return if (requestSendFiles.isNotEmpty()) 8192 else null
        }
    }

    private class ServerDownloadHandler : IRemoteRequestDownloadHandler {
        override fun onRequestDownload(requestDownloadFiles: List<ExplorerFile>, bufferSize: Int): Int? {
            return if (bufferSize > 0) 2 else null
        }
    }

    private class ClientSendHandler : IRemoteRequestSendHandler {
        override fun onRequestSend(requestSendFiles: List<ExplorerFile>, maxConnection: Int): Int? {
            return if (requestSendFiles.isNotEmpty()) 4096 else null
        }
    }

    private class ClientDownloadHandler : IRemoteRequestDownloadHandler {
        override fun onRequestDownload(requestDownloadFiles: List<ExplorerFile>, bufferSize: Int): Int? {
            return if (bufferSize > 0) 1 else null
        }
    }

    suspend fun run() {
        val localAddress = findLocalAddressV4().first()
        val server = FileExploreServer(
            localAddress = localAddress,
            localFileSeparator = "/",
            deviceExplorer = ServerDeviceExplorer(),
            remoteRequestSendHandler = ServerSendHandler(),
            remoteRequestDownloadHandler = ServerDownloadHandler()
        )
        val client = FileExploreClient(
            serverAddress = localAddress,
            localFileSeparator = "/",
            deviceExplorer = ClientDeviceExplorer(),
            remoteRequestSendHandler = ClientSendHandler(),
            remoteRequestDownloadHandler = ClientDownloadHandler()
        )
        coroutineScope {
            launch { server.startTask() }
            launch { client.startTask() }

            val hsServer = launch { println("Server handshake: ${server.waitHandshakeOrNull()}") }
            val hsClient = launch { println("Client handshake: ${client.waitHandshakeOrNull()}") }
            hsServer.join()
            hsClient.join()

            val dir = client.requestExploreRemoteDir("/")
            println("Explore dir: $dir")

            val msgJob = launch { println("Server remote msg: ${server.remoteMessage().first()}") }
            val sent = client.requestSendMessage("hello", System.currentTimeMillis())
            println("Client send msg ret: $sent")
            msgJob.join()

            val sendBuf = client.requestSendFiles(listOf(ExplorerFile("a.txt", "/a.txt", 1L, System.currentTimeMillis())), 1)
            println("Client request send files buffer: $sendBuf")

            val downloadMaxConn = client.requestDownloadFiles(listOf(ExplorerFile("b.txt", "/b.txt", 2L, System.currentTimeMillis())), (sendBuf ?: 4096))
            println("Client request download files maxConn: $downloadMaxConn")

            client.stopTask()
            server.stopTask()
            server.waitTaskFinished()
            client.waitTaskFinished()
        }
    }
}
