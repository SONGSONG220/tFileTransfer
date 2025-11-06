package com.tans.tfiletranfer.net

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

object MainTest {

    @JvmStatic
    fun main(args: Array<String>) {
        runBlocking {

            val serverJob = launch { args
                UdpServerTest.run()
            }

            launch { args
                delay(200)
                UdpClientTest.run()
            }

            serverJob.join()
        }
    }
}