package com.tans.tfiletranfer.net

import com.tans.tfiletransfer.net.socket.findLocalAddressV4
import com.tans.tfiletransfer.net.transferproto.fileexplore.FileExploreServer
import com.tans.tfiletransfer.net.transferproto.fileexplore.IDeviceExplorer
import com.tans.tfiletransfer.net.transferproto.fileexplore.IRemoteRequestDownloadHandler
import com.tans.tfiletransfer.net.transferproto.fileexplore.IRemoteRequestSendHandler
import com.tans.tfiletransfer.net.transferproto.fileexplore.model.ExplorerDir
import com.tans.tfiletransfer.net.transferproto.fileexplore.model.ExplorerFile
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

object FileExploreServerTest {

    private class TestDeviceExplorer : IDeviceExplorer {
        override fun explore(path: String): Pair<List<ExplorerDir>, List<ExplorerFile>> {
            val d = ExplorerDir(name = "home", path = "/home", childrenCount = 1, lastModify = System.currentTimeMillis())
            val f = ExplorerFile(name = "readme.txt", path = "/home/readme.txt", size = 12, lastModify = System.currentTimeMillis())
            return listOf(d) to listOf(f)
        }
    }

    private class TestSendHandler : IRemoteRequestSendHandler {
        override fun onRequestSend(requestSendFiles: List<ExplorerFile>, maxConnection: Int): Int? {
            return if (requestSendFiles.isNotEmpty()) 8192 else null
        }
    }

    private class TestDownloadHandler : IRemoteRequestDownloadHandler {
        override fun onRequestDownload(requestDownloadFiles: List<ExplorerFile>, bufferSize: Int): Int? {
            return if (bufferSize > 0) 2 else null
        }
    }

    suspend fun run() {
        val localAddress = findLocalAddressV4().first()
        val server = FileExploreServer(
            localAddress = localAddress,
            localFileSeparator = "/",
            deviceExplorer = TestDeviceExplorer(),
            remoteRequestSendHandler = TestSendHandler(),
            remoteRequestDownloadHandler = TestDownloadHandler()
        )
        coroutineScope {
            server.startTask()
            launch {
                val hs = server.waitHandshakeOrNull()
                println("Server handshake: $hs")
                server.stopTask()
            }
            server.waitTaskFinished()
        }
    }
}
