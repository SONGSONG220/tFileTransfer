package com.tans.tfiletransfer.net.transferproto.filetransfer.speedcalculator

import com.tans.tfiletransfer.net.transferproto.fileexplore.model.ExplorerFile

data class Speed(
    val bytesPerSecond: Long,
    val speedInHumanreadable: String,
    val progressInPercents: Double,
    val handledSizeInBytes: Long,
    val file: ExplorerFile
)
