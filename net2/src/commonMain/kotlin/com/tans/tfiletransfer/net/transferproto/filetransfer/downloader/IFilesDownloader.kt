package com.tans.tfiletransfer.net.transferproto.filetransfer.downloader

import com.tans.tfiletransfer.net.ITask
import kotlinx.coroutines.flow.Flow

interface IFilesDownloader : ITask {

    fun downloadingFileTask(): Flow<FileDownloader?>
}