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

    protected open fun nextRetryDelay(retryTimesLeft: Int, maxRetryTimes: Int): Long? {
        if (retryTimesLeft <= 0) return null
        val attemptIndex = maxRetryTimes - retryTimesLeft
        val exp = DEFAULT_RETRY_DELAY * (1L shl attemptIndex.coerceAtLeast(0))
        return exp.coerceAtMost(DEFAULT_RETRY_TIMEOUT)
    }

    protected fun onResponseData(
        responsePkt: PackageData,
        remoteAddress: String,
        remotePort: Int
    ) {
        connectionTask.coroutineScope.launch {
            val snapshot = waitingResponseTasks.snapshot
            for (t in snapshot) {
                if (t.onResponseData(responsePkt, remoteAddress, remotePort)) {
                    break
                }
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
        abstract val maxRetryTimes: Int
        abstract val retryTimeoutInMillis: Long
        abstract val callback: CancellableContinuation<Response>
        abstract val delay: Long

        private val taskIsDone = atomic(false)

        private val timeoutTask = atomic<Job?>(null)

        abstract fun handleResponseData(responsePkt: PackageData, remoteAddress: String, remotePort: Int) : Boolean

        fun onResponseData(
            responsePkt: PackageData,
            remoteAddress: String,
            remotePort: Int
        ) : Boolean {
            return if (handleResponseData(responsePkt, remoteAddress, remotePort)) {
                connectionTask.coroutineScope.launch {
                    try {
                        timeoutTask.getAndSet(null)?.cancel()
                        val responseConverter = converterFactory.findTypeConverter(responsePkt.type, responseClass)
                        if (responseConverter != null) {
                            val response = responseConverter.convert(
                                responsePkt.type,
                                responseClass,
                                responsePkt,
                                connectionTask.bufferPool
                            )
                            completeSuccess(response)
                        } else {
                            val errorMsg = "Didn't find converter for: $requestType, $responseClass"
                            completeFailure(errorMsg, retryable = false)
                        }
                    } catch (e: Throwable) {
                        completeFailure(e.message ?: "", retryable = true)
                    }
                }
                true
            } else {
                false
            }
        }

        fun run() {
            connectionTask.coroutineScope.launch {
                try {
                    if (delay > 0) {
                        delay(delay)
                    }
                    waitingResponseTasks.add(this@Task)
                    val requestConverter = converterFactory.findPackageConverter(
                        type = requestType,
                        dataTypeClass = requestClass
                    )
                    if (requestConverter == null) {
                        val errorMsg = "Didn't find converter for: $requestType, $requestClass"
                        completeFailure(errorMsg, retryable = false)
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
                            completeFailure(errorMsg, retryable = true)
                        } else {
                            val t = connectionTask.coroutineScope.launch {
                                try {
                                    delay(retryTimeoutInMillis)
                                    completeFailure("Waiting server response timeout: type=${requestType}", retryable = true)
                                } catch (_: Throwable) {
                                }
                            }
                            val old = this@Task.timeoutTask.getAndSet(t)
                            old?.cancel()
                        }
                    }
                } catch (e: Throwable) {
                    completeFailure(e.message ?: "Unknown error.", retryable = true)
                }
            }
        }

        abstract suspend fun writeRequestPktData(pktData: PackageData): Boolean

        abstract fun retry(delay: Long)

        private fun completeSuccess(response: Response) {
            waitingResponseTasks.remove(this@Task)
            if (taskIsDone.compareAndSet(expect = false, update = true)) {
                if (callback.isActive) {
                    callback.resume(response)
                }
            }
        }

        private fun completeFailure(e: String, retryable: Boolean) {
            timeoutTask.getAndSet(null)?.cancel()
            connectionTask.coroutineScope.launch {
                NetLog.e(tag, "Send request error: msgId=$messageId, cmdType=$requestType, error=$e")
                waitingResponseTasks.remove(this@Task)
                if (taskIsDone.compareAndSet(expect = false, update = true)) {
                    val d = if (retryable) nextRetryDelay(retryTimes, maxRetryTimes) else null
                    if (d != null) {
                        NetLog.e(tag, "Retry request")
                        retry(d)
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