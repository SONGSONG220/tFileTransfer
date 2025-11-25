package com.tans.tfiletransfer.net.transferproto.filetransfer.speedcalculator

import com.tans.tfiletransfer.net.transferproto.filetransfer.sender.IFilesSender
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

interface IFilesSenderSpeedCalculator : IFilesSender, ISpeedCalculator

class FilesSenderSpeedDecorator(
    private val filesSender: IFilesSender,
    private val calculaDurationInMillis: Long = 300L
) : IFilesSender by filesSender, IFilesSenderSpeedCalculator {

    @OptIn(ExperimentalCoroutinesApi::class)
    private val speedCalculator = SpeedCalculator(
        calculaDurationInMillis = calculaDurationInMillis,
        activeFile = filesSender.sendingFileTask().map { it?.toSendFile?.exploreFile },
        handledFileSize = filesSender.sendingFileTask().flatMapLatest {
            it?.sendSize() ?: flowOf(0L)
        },
        coroutineScope = filesSender.coroutineScope,
    )

    override fun speed(): Flow<Speed> = speedCalculator.speed()
}

fun IFilesSender.withSpeedCalculator(
    calculaDurationInMillis: Long = 300L
): IFilesSenderSpeedCalculator {
    if (this is IFilesSenderSpeedCalculator) return this
    return FilesSenderSpeedDecorator(
        filesSender = this,
        calculaDurationInMillis = calculaDurationInMillis,
    )
}

