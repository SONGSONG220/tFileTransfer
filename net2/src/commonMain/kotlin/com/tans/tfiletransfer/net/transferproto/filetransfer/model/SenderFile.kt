package com.tans.tfiletransfer.net.transferproto.filetransfer.model

import com.tans.tfiletransfer.net.transferproto.fileexplore.model.FileExploreFile

data class SenderFile(
    val realFile: String,
    val exploreFile: FileExploreFile
)