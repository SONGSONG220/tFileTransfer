package com.tans.tfiletranfer.net

import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

object MainTest {

    @JvmStatic
    fun main(args: Array<String>) {
        runBlocking {
            launch {
                UdpServerTest.run()
            }
            launch {
                UdpClientTest.run()
            }
        }
    }
}