package com.tans.tfiletranfer.net

import com.tans.tfiletransfer.net.socket.Address
import com.tans.tfiletransfer.net.socket.findLocalAddressV4
import com.tans.tfiletransfer.net.transferproto.fileexplore.model.ExplorerFile
import com.tans.tfiletransfer.net.transferproto.filetransfer.downloader.FilesDownloader
import com.tans.tfiletransfer.net.transferproto.filetransfer.model.SenderFile
import com.tans.tfiletransfer.net.transferproto.filetransfer.sender.FilesSender
import com.tans.tfiletransfer.net.transferproto.filetransfer.speedcalculator.withSpeedCalculator
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.io.File

object FileTransferIntegrationTest {

    suspend fun run() {
        val localAddress: Address = findLocalAddressV4().first()

        val testFile = File("net2/src/jvmTest/testFile/test.jpg")
        println("Test file: ${testFile.absolutePath}, exists=${testFile.exists()}, size=${testFile.length()}")
        if (!testFile.exists()) {
            println("Test file not found, please put test file at src/jvmTest/testFile/test.jpg")
            return
        }

        val testMeta = ExplorerFile(
            name = testFile.name,
            path = testFile.absolutePath,
            size = testFile.length(),
            lastModify = System.currentTimeMillis()
        )

        val sender = FilesSender(
            toSendLocalFiles = listOf(SenderFile(realFilePath = testFile.absolutePath, exploreFile = testMeta)),
            localAddress = localAddress
        ).withSpeedCalculator()

        val downloadDir = File("net2/src/jvmTest/download/").absolutePath
        val downloader = FilesDownloader(
            downloadDir = downloadDir,
            toDownloadRemoteFiles = listOf(testMeta),
            senderAddress = localAddress,
            maxConnection = 8,
        ).withSpeedCalculator()

        coroutineScope {
            sender.startTask()
            downloader.startTask()

            val senderJob = launch {
                val sj = launch {
                    sender.speed()
                        .collect {
                            println("Sender speed: $it")
                        }
                }
                val s = sender.waitTaskFinished()
                sj.cancel()
                println("Sender finished: $s")
            }
            val downloaderJob = launch {
                val sj = launch {
                    downloader.speed()
                        .collect {
                            println("Downloader speed: $it")
                        }
                }
                val s = downloader.waitTaskFinished()
                sj.cancel()
                println("Downloader finished: $s")
            }

            senderJob.join()
            downloaderJob.join()
        }
    }
}

