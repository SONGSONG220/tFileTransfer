package com.tans.tfiletransfer.net.transferproto.filetransfer.speedcalculator

import com.tans.tfiletransfer.net.transferproto.fileexplore.model.ExplorerFile
import com.tans.tfiletransfer.net.transferproto.filetransfer.toHumanReadableSpeed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class SpeedCalculator(
    val activeFile: Flow<ExplorerFile?>,
    val handledFileSize: Flow<Long>,
    val coroutineScope: CoroutineScope,
    val calculaDurationInMillis: Long = 300L
) : ISpeedCalculator {
    private val speedFlow = MutableSharedFlow<Speed>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    init {
        coroutineScope.launch {
            try {
                var lastFile: ExplorerFile? = null
                var lastHandledSize: Long = 0
                while (true) {
                    val currentFile = activeFile.firstOrNull()
                    val currentHandledSize = handledFileSize.first()
                    if (currentFile == lastFile && currentFile != null) {
                        val bytesDiff = currentHandledSize - lastHandledSize
                        val bytesPerSecond = (bytesDiff * 1000L) / calculaDurationInMillis
                        val speedInHumanreadable = bytesPerSecond.toHumanReadableSpeed()
                        val progressInPercents = currentHandledSize.toDouble() / currentFile.size.coerceAtLeast(1L)
                        speedFlow.emit(
                            Speed(
                                bytesPerSecond = bytesPerSecond,
                                speedInHumanreadable = speedInHumanreadable,
                                progressInPercents = progressInPercents,
                                handledSizeInBytes = currentHandledSize,
                                file = currentFile
                            )
                        )
                    }
                    lastHandledSize = currentHandledSize
                    lastFile = currentFile
                    delay(calculaDurationInMillis)
                }
            } catch (_: Throwable) { // Canceled.
            }
        }
    }

    override fun speed(): Flow<Speed> = speedFlow
}
