package com.tans.tfiletransfer.net.transferproto.filetransfer.speedcalculator

import com.tans.tfiletransfer.net.transferproto.filetransfer.downloader.IFilesDownloader
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

interface IFilesDownloaderSpeedCalculator : IFilesDownloader, ISpeedCalculator

class FilesDownloaderSpeedDecorator(
    private val filesDownloader: IFilesDownloader,
    private val calculaDurationInMillis: Long = 300L
) : IFilesDownloader by filesDownloader, IFilesDownloaderSpeedCalculator {

    @OptIn(ExperimentalCoroutinesApi::class)
    private val speedCalculator = SpeedCalculator(
        calculaDurationInMillis = calculaDurationInMillis,
        activeFile = filesDownloader.downloadingFileTask().map { it?.toDownloadRemoteFile },
        handledFileSize = filesDownloader.downloadingFileTask().flatMapLatest {
            it?.downloadedSize() ?: flowOf(0L)
        },
        coroutineScope = filesDownloader.coroutineScope,
    )

    override fun speed(): Flow<Speed> = speedCalculator.speed()
}

fun IFilesDownloader.withSpeedCalculator(
    calculaDurationInMillis: Long = 300L
): IFilesDownloaderSpeedCalculator {
    if (this is IFilesDownloaderSpeedCalculator) return this
    return FilesDownloaderSpeedDecorator(
        filesDownloader = this,
        calculaDurationInMillis = calculaDurationInMillis,
    )
}
