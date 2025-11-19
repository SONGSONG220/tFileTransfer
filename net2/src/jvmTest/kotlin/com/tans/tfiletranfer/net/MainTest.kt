package com.tans.tfiletranfer.net

import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

object MainTest {

    @JvmStatic
    fun main(args: Array<String>) {
        runBlocking {
            launch {
                WifiP2pServerTest.run()
            }
            launch {
                WifiP2pClientTest.run()
            }
        }
    }
}