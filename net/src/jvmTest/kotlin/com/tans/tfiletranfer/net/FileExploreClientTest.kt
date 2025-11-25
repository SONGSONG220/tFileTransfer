package com.tans.tfiletranfer.net

import com.tans.tfiletransfer.net.socket.AddressWithPort
import com.tans.tfiletransfer.net.socket.findLocalAddressV4
import com.tans.tfiletransfer.net.transferproto.TransferProtoConstant
import com.tans.tfiletransfer.net.transferproto.fileexplore.FileExploreClient
import com.tans.tfiletransfer.net.transferproto.fileexplore.IDeviceExplorer
import com.tans.tfiletransfer.net.transferproto.fileexplore.IRemoteRequestDownloadHandler
import com.tans.tfiletransfer.net.transferproto.fileexplore.IRemoteRequestSendHandler
import com.tans.tfiletransfer.net.transferproto.fileexplore.model.ExplorerDir
import com.tans.tfiletransfer.net.transferproto.fileexplore.model.ExplorerFile
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

object FileExploreClientTest {

    private class TestDeviceExplorer : IDeviceExplorer {
        override fun explore(path: String): Pair<List<ExplorerDir>, List<ExplorerFile>> {
            val d = ExplorerDir(name = "client", path = "/client", childrenCount = 0, lastModify = System.currentTimeMillis())
            val f = ExplorerFile(name = "client.txt", path = "/client/client.txt", size = 1, lastModify = System.currentTimeMillis())
            return listOf(d) to listOf(f)
        }
    }

    private class TestSendHandler : IRemoteRequestSendHandler {
        override fun onRequestSend(requestSendFiles: List<ExplorerFile>, maxConnection: Int): Int? {
            return if (requestSendFiles.isNotEmpty()) 4096 else null
        }
    }

    private class TestDownloadHandler : IRemoteRequestDownloadHandler {
        override fun onRequestDownload(requestDownloadFiles: List<ExplorerFile>, bufferSize: Int): Int? {
            return if (bufferSize > 0) 1 else null
        }
    }

    suspend fun run() {
        val localAddress = findLocalAddressV4().first()
        val serverAddress = AddressWithPort(localAddress, TransferProtoConstant.FILE_EXPLORE_SERVER_PORT).address
        val client = FileExploreClient(
            serverAddress = serverAddress,
            localFileSeparator = "/",
            deviceExplorer = TestDeviceExplorer(),
            remoteRequestSendHandler = TestSendHandler(),
            remoteRequestDownloadHandler = TestDownloadHandler()
        )
        coroutineScope {
            client.startTask()
            launch {
                val hs = client.waitHandshakeOrNull()
                println("Client handshake: $hs")
                client.stopTask()
            }
            client.waitTaskFinished()
        }
    }
}

