package com.tans.tfiletranfer.net

import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File

object MainTest {

    @JvmStatic
    fun main(args: Array<String>) {
        runBlocking {
//            launch {
//                TcpServerTest.run()
//            }
//            launch {
//                TcpClientTest.run()
//            }
//            FileExploreIntegrationTest.run()
            FileTransferIntegrationTest.run()
        }
    }
}