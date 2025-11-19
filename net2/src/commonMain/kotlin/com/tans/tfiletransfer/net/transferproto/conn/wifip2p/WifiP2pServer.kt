package com.tans.tfiletransfer.net.transferproto.conn.wifip2p

import com.tans.tfiletransfer.net.NetLog
import com.tans.tfiletransfer.net.TaskState
import com.tans.tfiletransfer.net.socket.Address
import com.tans.tfiletransfer.net.socket.AddressWithPort
import com.tans.tfiletransfer.net.socket.ext.client.ITcpClientManager
import com.tans.tfiletransfer.net.socket.ext.defaultClientManager
import com.tans.tfiletransfer.net.socket.ext.server.defaultServerManager
import com.tans.tfiletransfer.net.socket.ext.server.server
import com.tans.tfiletransfer.net.socket.tcp.ITcpClientTask
import com.tans.tfiletransfer.net.socket.tcp.ITcpServerTask
import com.tans.tfiletransfer.net.socket.tcp.TcpServerTask
import com.tans.tfiletransfer.net.transferproto.TransferException
import com.tans.tfiletransfer.net.transferproto.TransferProtoConstant
import com.tans.tfiletransfer.net.transferproto.conn.wifip2p.model.WifiP2pDataType
import com.tans.tfiletransfer.net.transferproto.conn.wifip2p.model.WifiP2pHandshake
import com.tans.tfiletransfer.net.transferproto.conn.wifip2p.model.WifiP2pHandshakeReq
import com.tans.tfiletransfer.net.transferproto.conn.wifip2p.model.WifiP2pHandshakeRsp
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class WifiP2pServer(
    private val localDeviceName: String,
    private val localAddress: Address
) : BaseWifiP2pConnection() {

    override val tag: String = TAG

    private var serverClientManager: ITcpClientManager? = null
    private var serverTask: ITcpServerTask? = null
    private var clientTask: ITcpClientTask? = null

    override suspend fun onStartTask() {
        val serverTask = TcpServerTask(
            bindAddress = AddressWithPort(localAddress, TransferProtoConstant.WIFI_P2P_CONN_GROUP_OWNER_PORT)
        )
        serverTask.startTask()
        serverTask.waitTaskConnectedOrError().apply {
            if (this is TaskState.Error) {
                error(TransferException("Failed to start server task. Cause: ${this.throwable?.message}", this.throwable))
                return
            }
        }
        val clientTask = try {
            serverTask.clientChannel().receive()
        } catch (e: Throwable) {
            serverTask.stopTask()
            error(TransferException("Failed to receive client channel. Cause: ${e.message}", e))
            return
        }
        val serverClientManager = clientTask.defaultServerManager().defaultClientManager()
        val handshakeServer = server<WifiP2pHandshakeReq, WifiP2pHandshakeRsp>(
            requestType = WifiP2pDataType.HandshakeReq.type,
            responseType = WifiP2pDataType.HandshakeRsp.type
        ) { localAddress, remoteAddress, r, isNew ->
            if (r.version != TransferProtoConstant.VERSION) {
                error(TransferException("Version not match. Expect: ${TransferProtoConstant.VERSION}, but got: ${r.version}"))
                serverTask.stopTask()
                clientTask.stopTask()
                null
            } else {
                if (isNew) {
                    wifiHandshakeFlow.value = WifiP2pHandshake(
                        localAddress = localAddress.address,
                        remoteAddress = remoteAddress.address,
                        remoteDeviceName = r.deviceName
                    )
                }
                WifiP2pHandshakeRsp(
                    deviceName = localDeviceName,
                )
            }
        }
        serverClientManager.registerServer(handshakeServer)
        val handshake = try {
            wifiHandshakeFlow.filterNotNull().first()
        } catch (e: Throwable) {
            serverTask.stopTask()
            clientTask.stopTask()
            error(TransferException("Failed to receive handshake. Cause: ${e.message}", e))
            return
        }
        serverClientManager.unregisterServer(handshakeServer)
        NetLog.d(TAG, "Handshake: $handshake")

        updateStateExpect(
            expect = TaskState.Connecting,
            update = TaskState.Connected,
            fail = {
                serverTask.stopTask()
                clientTask.stopTask()
                error(TransferException("Fail to update connected state."))
            }
        ) {
            NetLog.d(TAG, "Task connected.")
            this.serverTask = serverTask
            this.clientTask = clientTask
            serverClientManager.registerServer(createConnServer)
            serverClientManager.registerServer(closeP2pServer)
            this.serverClientManager = serverClientManager
            onConnectionCreated(serverTask, clientTask)
        }
    }

    override fun clientManager(): ITcpClientManager? = serverClientManager

    override fun release() {
        clientTask?.stopTask()
        serverTask?.stopTask()
        serverClientManager = null
        coroutineScope.cancel()
    }

    private fun onConnectionCreated(
        serverTask: ITcpServerTask,
        clientTask: ITcpClientTask,
    ) {
        coroutineScope.launch {
            runCatching {
                serverTask.waitTaskFinished()
                error(TransferException("Server task finished."))
            }
        }
        coroutineScope.launch {
            runCatching {
                clientTask.waitTaskFinished()
                error(TransferException("Client task finished."))
            }
        }
    }

    companion object {
        const val TAG = "WifiP2pServer"
    }
}