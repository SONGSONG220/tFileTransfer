package com.tans.tfiletransfer.net.socket.ext.client

import com.tans.tfiletransfer.net.NetLog
import com.tans.tfiletransfer.net.socket.AddressWithPort
import com.tans.tfiletransfer.net.socket.PackageData
import com.tans.tfiletransfer.net.socket.PackageDataWithAddress
import com.tans.tfiletransfer.net.socket.SocketRuntimeException
import com.tans.tfiletransfer.net.socket.ext.Connection
import com.tans.tfiletransfer.net.socket.ext.IConnectionManager
import com.tans.tfiletransfer.net.socket.tcp.ITcpClientTask
import com.tans.tfiletransfer.net.socket.udp.IUdpTask
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.reflect.KClass

const val DEFAULT_RETRY_DELAY = 100L
const val DEFAULT_RETRY_TIMEOUT = 1000L
const val DEFAULT_RETRY_TIMES = 2

internal abstract class BaseClientManager(
    connection: Connection
) : IConnectionManager {

    abstract val tag: String

    private val waitingResponseTasksLock = Mutex()
    private val waitingResponseTasks = mutableSetOf<Task<*, *>>()
    private val messageId = atomic(0L)

    init {
        connection.connectionTask.coroutineScope.launch {
            try {
                when (connection) {
                    is Connection.TcpConnection -> {
                        connection.connectionTask.pktReadChannel()
                            .collect {
                                onResponseData(it)
                            }
                    }
                    is Connection.UdpConnection -> {
                        connection.connectionTask.pktReadChannel()
                            .collect {
                                onResponseData(it.pkt)
                            }
                    }
                }
            } catch (e: Throwable) {
                NetLog.e(tag, "Read pkt fail: ${e.message}", e)
            }
        }
    }

    protected fun generateMessageId(): Long = messageId.addAndGet(1)

    // 收到来自 server 的回复消息
    private fun onResponseData(msg: PackageData) {
        // 通知正在等待 server 回复消息的 Task
        connectionTask.coroutineScope.launch {
            waitingResponseTasksLock.withLock {
                val iterator = waitingResponseTasks.iterator()
                while (iterator.hasNext()) {
                    if (iterator.next().onResponseData(msg)) {
                        iterator.remove()
                        break
                    }
                }
            }
        }
    }

    inner class Task<Request : Any, Response : Any>(
        private val type: Int,
        private val messageId: Long,
        private val udpTargetAddress: AddressWithPort?,
        private val request: Request,
        private val requestClass: KClass<Request>,
        private val responseClass: KClass<Response>,
        private val retryTimes: Int,
        private val retryTimeoutInMillis: Long,
        val delay: Long = 0L,
        val callback: Continuation<Response>
    ) {

        private val taskIsDone = atomic(false)

        private val timeoutTask = atomic<Job?>(null)

        // 收到来自 Server 的回复消息
        fun onResponseData(
            responsePkt: PackageData
        ) : Boolean {
            return if (responsePkt.messageId == this.messageId) { // 是当前的任务的回复消息
                connectionTask.coroutineScope.launch {
                    try {
                        // 移除超时信息
                        timeoutTask.getAndSet(null)?.cancel()

                        // 找到回复的消息的转换器
                        val responseConverter = converterFactory.findTypeConverter(responsePkt.type, responseClass)
                        if (responseConverter != null) {
                            val response = responseConverter.convert(
                                responsePkt.type,
                                responseClass,
                                responsePkt,
                                connectionTask.bufferPool
                            )

                            if (taskIsDone.compareAndSet(expect = false, update = true)) {
                                callback.resume(response)
                            }
                        } else {
                            val errorMsg = "Didn't find converter for: $type, $responseClass"
                            handleError(errorMsg)
                        }
                    } catch (e: Throwable) {
                        handleError(e.message ?: "")
                    }
                }
                true
            } else {
                false
            }
        }

        // 发送信息到 Server
        fun run() {
            connectionTask.coroutineScope.launch {
                try {
                    if (delay > 0) {
                        delay(delay)
                    }
                    // 将当前任务添加到等待回复的队列
                    waitingResponseTasksLock.withLock {
                        waitingResponseTasks.add(this@Task)
                    }
                    val requestConverter = converterFactory.findPackageConverter(
                        type = type,
                        dataTypeClass = requestClass
                    )
                    if (requestConverter == null) {
                        val errorMsg = "Didn't find converter for: $type, $requestClass"
                        handleError(errorMsg)
                    } else {
                        val requestPkt = requestConverter.convert(
                            type = type,
                            messageId = messageId,
                            dataTypeClass = requestClass,
                            data = request,
                            bufferPool = connectionTask.bufferPool
                        )
                        val isSendSuccess = when(connection) {
                            is Connection.TcpConnection -> {
                                (connection.connectionTask as ITcpClientTask).writePktData(requestPkt)
                            }
                            is Connection.UdpConnection -> {
                                (connection.connectionTask as IUdpTask).writePktData(PackageDataWithAddress(requestPkt, udpTargetAddress!!))
                            }
                        }
                        if (!isSendSuccess) {
                            val errorMsg = "Request $type fail, connection task not active."
                            handleError(errorMsg)
                        } else {
                            val timeoutTask = connectionTask.coroutineScope.launch {
                                try {
                                    delay(retryTimeoutInMillis)
                                    handleError("Waiting server response timeout: type=${type}")
                                } catch (_: Throwable) {
                                }
                            }
                            val old = this@Task.timeoutTask.getAndSet(timeoutTask)
                            old?.cancel()
                        }
                    }
                } catch (e: Throwable) {
                    handleError(e.message ?: "Unknown error.")
                }
            }
        }

        // 发送失败，处理异常
        private fun handleError(e: String) {
            connectionTask.coroutineScope.launch {
                NetLog.e(tag, "Send request error: msgId=$messageId, cmdType=$type, error=$e")
                // 从等待队列中移除当前任务
                waitingResponseTasksLock.withLock {
                    waitingResponseTasks.remove(this@Task)
                }
                if (taskIsDone.compareAndSet(expect = false, update = true)) {
                    // 判断是否需要重试，如果需要重试，构建一个新的任务继续请求，反之直接回调异常.
                    if (retryTimes > 0) {
                        NetLog.e(tag, "Retry request")
                        Task(
                            type = type,
                            messageId = messageId,
                            request = request,
                            requestClass = requestClass,
                            responseClass = responseClass,
                            callback = callback,
                            retryTimes = retryTimes - 1,
                            delay = DEFAULT_RETRY_DELAY,
                            retryTimeoutInMillis = retryTimeoutInMillis,
                            udpTargetAddress = udpTargetAddress
                        ).run()
                    } else {
                        callback.resumeWithException(SocketRuntimeException(msg = e))
                    }
                }

            }
        }

    }
}