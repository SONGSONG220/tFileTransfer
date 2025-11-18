package com.tans.tfiletransfer.net.socket

import com.tans.tfiletransfer.net.ITask
import com.tans.tfiletransfer.net.socket.buffer.BufferPool

interface IConnectionTask : ITask {
    val bufferPool: BufferPool
}