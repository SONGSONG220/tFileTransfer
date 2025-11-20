package com.tans.tfiletransfer.net.transferproto.fileexplore

import com.tans.tfiletransfer.net.transferproto.fileexplore.model.ExplorerDir
import com.tans.tfiletransfer.net.transferproto.fileexplore.model.ExplorerFile

interface IDeviceExplorer {
    fun explore(path: String): Pair<List<ExplorerDir>, List<ExplorerFile>>
}