package com.tans.tfiletranfer.net

import com.tans.tfiletransfer.net.socket.findLocalAddressV4
import com.tans.tfiletransfer.net.socket.getBroadcastAddressV4
import kotlinx.coroutines.runBlocking

object MainTest {

    @JvmStatic
    fun main(args: Array<String>) {
        runBlocking {
            val localAddress = findLocalAddressV4()
            for (a in localAddress) {
                println("LocalAddress: $a, Broadcast: ${a.getBroadcastAddressV4()}")
            }
        }
    }
}