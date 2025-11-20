package com.tans.tfiletransfer.net.socket.ext.client

import com.tans.tfiletransfer.net.NetLog
import com.tans.tfiletransfer.net.socket.PackageData
import com.tans.tfiletransfer.net.socket.SocketException
import com.tans.tfiletransfer.net.socket.ext.IConnectionManager
import com.tans.tfiletransfer.net.collections.AtomicSet
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.reflect.KClass

const val DEFAULT_RETRY_DELAY = 100L
const val DEFAULT_RETRY_TIMEOUT = 1000L
const val DEFAULT_RETRY_TIMES = 2

internal abstract class BaseClientManager() : IConnectionManager {

    abstract val tag: String

    private val waitingResponseTasks = AtomicSet<Task<*, *>>()
    private val messageId = atomic(0L)

    protected fun generateMessageId(): Long = messageId.addAndGet(1)

    // 收到来自 server 的回复消息
    protected fun onResponseData(
        responsePkt: PackageData,
        remoteAddress: String,
        remotePort: Int
    ) {
        // 通知正在等待 server 回复消息的 Task
        connectionTask.coroutineScope.launch {
            val snapshot = waitingResponseTasks.snapshot
            var matched: Task<*, *>? = null
            for (t in snapshot) {
                if (t.onResponseData(responsePkt, remoteAddress, remotePort)) {
                    matched = t
                    break
                }
            }
            if (matched != null) {
                waitingResponseTasks.remove(matched)
            }
        }
    }

    protected suspend fun <R : Any> safeTask(contCallback: (cont: CancellableContinuation<R>) -> Unit): R {
        return withContext(connectionTask.coroutineScope.coroutineContext) {
            suspendCancellableCoroutine { cont ->
                contCallback(cont)
            }
        }
    }

    abstract inner class Task<Request : Any, Response : Any>() {

        abstract val requestType: Int
        abstract val messageId: Long
        abstract val request: Request
        abstract val requestClass: KClass<Request>
        abstract val responseType: Int
        abstract val responseClass: KClass<Response>
        abstract val retryTimes: Int
        abstract val retryTimeoutInMillis: Long
        abstract val callback: CancellableContinuation<Response>
        abstract val delay: Long

        private val taskIsDone = atomic(false)

        private val timeoutTask = atomic<Job?>(null)

        abstract fun handleResponseData(responsePkt: PackageData, remoteAddress: String, remotePort: Int) : Boolean

        // 收到来自 Server 的回复消息
        fun onResponseData(
            responsePkt: PackageData,
            remoteAddress: String,
            remotePort: Int
        ) : Boolean {
            return if (handleResponseData(responsePkt, remoteAddress, remotePort)) { // 是当前的任务的回复消息
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
                                if (callback.isActive) {
                                    callback.resume(response)
                                }
                            }
                        } else {
                            val errorMsg = "Didn't find converter for: $requestType, $responseClass"
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
                    waitingResponseTasks.add(this@Task)
                    val requestConverter = converterFactory.findPackageConverter(
                        type = requestType,
                        dataTypeClass = requestClass
                    )
                    if (requestConverter == null) {
                        val errorMsg = "Didn't find converter for: $requestType, $requestClass"
                        handleError(errorMsg)
                    } else {
                        val requestPkt = requestConverter.convert(
                            type = requestType,
                            messageId = messageId,
                            dataTypeClass = requestClass,
                            data = request,
                            bufferPool = connectionTask.bufferPool
                        )
                        val isSendSuccess = writeRequestPktData(pktData = requestPkt)
                        if (!isSendSuccess) {
                            val errorMsg = "Request $requestType fail, connection task not active."
                            handleError(errorMsg)
                        } else {
                            val timeoutTask = connectionTask.coroutineScope.launch {
                                try {
                                    delay(retryTimeoutInMillis)
                                    handleError("Waiting server response timeout: type=${requestType}")
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

        abstract suspend fun writeRequestPktData(pktData: PackageData): Boolean

        abstract fun retry()

        // 发送失败，处理异常
        private fun handleError(e: String) {
            timeoutTask.getAndSet(null)?.cancel()
            connectionTask.coroutineScope.launch {
                NetLog.e(tag, "Send request error: msgId=$messageId, cmdType=$requestType, error=$e")
                // 从等待队列中移除当前任务
                waitingResponseTasks.remove(this@Task)
                if (taskIsDone.compareAndSet(expect = false, update = true)) {
                    // 判断是否需要重试，如果需要重试，构建一个新的任务继续请求，反之直接回调异常.
                    if (retryTimes > 0) {
                        NetLog.e(tag, "Retry request")
                        retry()
                    } else {
                        if (callback.isActive) {
                            callback.resumeWithException(SocketException(msg = e))
                        }
                    }
                }
            }
        }

        fun removeTaskForce() {
            timeoutTask.getAndSet(null)?.cancel()
            waitingResponseTasks.remove(this@Task)
            if (taskIsDone.compareAndSet(expect = false, update = true)) {
                NetLog.w(tag, "Remove task force: requestType=$requestType, responseType=$responseType, messageId=$messageId")
                if (callback.isActive) {
                    callback.resumeWithException(SocketException(msg = "Task force removed."))
                }
            }
        }

    }
}