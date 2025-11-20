package com.tans.tfiletransfer.net.transferproto.fileexplore

import com.tans.tfiletransfer.net.transferproto.fileexplore.model.ExplorerFile

interface IRemoteRequestDownloadHandler {

    /**
     * return maxConnection if success, null if reject.
     */
    fun onRequestDownload(requestDownloadFiles: List<ExplorerFile>, bufferSize: Int): Int?
}