package com.tans.tfiletransfer.net.transferproto.fileexplore

import com.tans.tfiletransfer.net.transferproto.fileexplore.model.ExplorerFile

interface IRemoteRequestSendHandler {

    /**
     * return bufferSize if success, null if reject.
     */
    fun onRequestSend(requestSendFiles: List<ExplorerFile>, maxConnection: Int): Int?
}