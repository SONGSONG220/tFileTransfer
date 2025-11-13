package com.tans.tfiletransfer.net.socket.ext.client

import com.tans.tfiletransfer.net.NetLog
import com.tans.tfiletransfer.net.socket.AddressWithPort
import com.tans.tfiletransfer.net.socket.IConnectionTask
import com.tans.tfiletransfer.net.socket.PackageData
import com.tans.tfiletransfer.net.socket.PackageDataWithAddress
import com.tans.tfiletransfer.net.socket.SocketRuntimeException
import com.tans.tfiletransfer.net.socket.ext.converter.DefaultConverterFactory
import com.tans.tfiletransfer.net.socket.ext.converter.IConverterFactory
import com.tans.tfiletransfer.net.socket.tcp.BaseTcpClientTask
import com.tans.tfiletransfer.net.socket.udp.UdpTask
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.reflect.KClass

class DefaultClientManager(
    override val connectionTask: IConnectionTask,
    override val converterFactory: IConverterFactory = DefaultConverterFactory(),
) : IClientManager {

    private val waitingResponseTasksLock = Mutex()
    private val waitingResponseTasks = mutableSetOf<Task<*, *>>()

    private val messageId = atomic(0L)

    init {
        connectionTask.coroutineScope.launch {
            try {
                if (connectionTask is BaseTcpClientTask) {
                    for (pkt in connectionTask.pktReadChannel()) {
                        onResponseData(pkt)
                    }
                }
                if (connectionTask is UdpTask) {
                    for (pkt in connectionTask.pktReadChannel()) {
                        onResponseData(pkt.pkt)
                    }
                }
            } catch (e: Throwable) {
                NetLog.e(TAG, "Read pkt fail: ${e.message}", e)
            }
        }
    }

    // 收到来自 server 的回复消息
    private fun onResponseData(msg: PackageData) {
        // 通知正在等待 server 回复消息的 Task
        connectionTask.coroutineScope.launch {
            waitingResponseTasksLock.withLock {
                val iterator = waitingResponseTasks.iterator()
                var handled = false
                while (iterator.hasNext()) {
                    handled = iterator.next().onResponseData(msg)
                    if (handled) {
                        iterator.remove()
                        break
                    }
                }
                if (!handled) {
                    NetLog.w(TAG, "No waiting task to handle msg type=${msg.type}, messageId=${msg.messageId}")
                }
            }
        }
    }

    override suspend fun <Request : Any, Response : Any> request(
        type: Int,
        request: Request,
        requestClass: KClass<Request>,
        responseClass: KClass<Response>,
        retryTimes: Int,
        retryTimeoutInMillis: Long
    ): Response = suspendCancellableCoroutine { cont ->
        Task(
            type = type,
            messageId = messageId.addAndGet(1),
            udpTargetAddress = null,
            request = request,
            requestClass = requestClass,
            responseClass = responseClass,
            retryTimes = retryTimes,
            retryTimeoutInMillis = retryTimeoutInMillis,
            callback = cont
        ).run()
    }

    override suspend fun <Request : Any> request(
        type: Int,
        request: Request,
        requestClass: KClass<Request>
    ) {
        val converter = converterFactory.findPackageConverter(
            type = type,
            dataTypeClass = requestClass
        )
        if (converter == null) {
            throw SocketRuntimeException("Don't find converter for $type")
        }
        val pkt = converter.convert(
            type = type,
            messageId = messageId.addAndGet(1),
            data = request,
            dataTypeClass = requestClass,
            bufferPool = connectionTask.bufferPool
        )
        val ret = (connectionTask as BaseTcpClientTask).writePktData(pkt)
        if (!ret) {
            throw SocketRuntimeException("Request $type fail, connection task not active.")
        }
    }

    override suspend fun <Request : Any, Response : Any> request(
        type: Int,
        request: Request,
        requestClass: KClass<Request>,
        responseClass: KClass<Response>,
        targetAddress: AddressWithPort,
        retryTimes: Int,
        retryTimeoutInMillis: Long
    ): Response = suspendCancellableCoroutine { cont ->
        Task(
            type = type,
            messageId = messageId.addAndGet(1),
            udpTargetAddress = targetAddress,
            request = request,
            requestClass = requestClass,
            responseClass = responseClass,
            retryTimes = retryTimes,
            retryTimeoutInMillis = retryTimeoutInMillis,
            callback = cont
        ).run()
    }

    override suspend fun <Request : Any> request(
        type: Int,
        request: Request,
        requestClass: KClass<Request>,
        targetAddress: AddressWithPort
    ) {
        val converter = converterFactory.findPackageConverter(
            type = type,
            dataTypeClass = requestClass
        )
        if (converter == null) {
            throw SocketRuntimeException("Don't find converter for $type")
        }
        val pkt = converter.convert(
            type = type,
            messageId = messageId.addAndGet(1),
            data = request,
            dataTypeClass = requestClass,
            bufferPool = connectionTask.bufferPool
        )
        val ret = (connectionTask as UdpTask).writePktData(
            pktDataWithAddress = PackageDataWithAddress(
                pkt = pkt,
                address = targetAddress
            )
        )
        if (!ret) {
            throw SocketRuntimeException("Request $type fail, connection task not active.")
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
                        val isSendSuccess = if (udpTargetAddress == null) { // Tcp
                            (connectionTask as BaseTcpClientTask).writePktData(requestPkt)
                        } else { // udp
                            (connectionTask as UdpTask).writePktData(PackageDataWithAddress(requestPkt, udpTargetAddress))
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
                NetLog.e(TAG, "Send request error: msgId=$messageId, cmdType=$type, error=$e")
                // 从等待队列中移除当前任务
                waitingResponseTasksLock.withLock {
                    waitingResponseTasks.remove(this@Task)
                }
                if (taskIsDone.compareAndSet(expect = false, update = true)) {
                    // 判断是否需要重试，如果需要重试，构建一个新的任务继续请求，反之直接回调异常.
                    if (retryTimes > 0) {
                        NetLog.e(TAG, "Retry request")
                        Task(
                            type = type,
                            messageId = messageId,
                            request = request,
                            requestClass = requestClass,
                            responseClass = responseClass,
                            callback = callback,
                            retryTimes = retryTimes - 1,
                            delay = RETRY_DELAY,
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

    companion object {

        private const val RETRY_DELAY = 100L
        private const val TAG = "DefaultClientManager"
    }
}

fun IConnectionTask.defaultClientManager(converterFactory: IConverterFactory = DefaultConverterFactory()): IClientManager {
    return DefaultClientManager(this, converterFactory)
}