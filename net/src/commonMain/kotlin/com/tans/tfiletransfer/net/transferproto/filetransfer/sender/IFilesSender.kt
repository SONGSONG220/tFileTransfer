package com.tans.tfiletransfer.net.transferproto.filetransfer.sender

import com.tans.tfiletransfer.net.ITask
import kotlinx.coroutines.flow.Flow

interface IFilesSender : ITask {
    fun sendingFileTask(): Flow<FileSender?>
}