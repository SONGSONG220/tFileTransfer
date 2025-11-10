package com.tans.tfiletranfer.net

import kotlinx.coroutines.runBlocking

object MainTest {

    @JvmStatic
    fun main(args: Array<String>) {
        runBlocking {
            UdpBroadcastReceiverTest.run()
        }
    }
}