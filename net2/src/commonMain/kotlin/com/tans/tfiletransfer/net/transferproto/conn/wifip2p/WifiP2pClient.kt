package com.tans.tfiletransfer.net.transferproto.conn.wifip2p

import com.tans.tfiletransfer.net.NetLog
import com.tans.tfiletransfer.net.TaskState
import com.tans.tfiletransfer.net.socket.Address
import com.tans.tfiletransfer.net.socket.AddressWithPort
import com.tans.tfiletransfer.net.socket.ext.client.ITcpClientManager
import com.tans.tfiletransfer.net.socket.ext.client.defaultClientManager
import com.tans.tfiletransfer.net.socket.ext.client.requestSimplify
import com.tans.tfiletransfer.net.socket.ext.defaultServerManager
import com.tans.tfiletransfer.net.socket.tcp.ITcpClientTask
import com.tans.tfiletransfer.net.socket.tcp.TcpClientTask
import com.tans.tfiletransfer.net.socket.toAddress
import com.tans.tfiletransfer.net.transferproto.TransferException
import com.tans.tfiletransfer.net.transferproto.TransferProtoConstant
import com.tans.tfiletransfer.net.transferproto.conn.wifip2p.model.WifiP2pDataType
import com.tans.tfiletransfer.net.transferproto.conn.wifip2p.model.WifiP2pHandshake
import com.tans.tfiletransfer.net.transferproto.conn.wifip2p.model.WifiP2pHandshakeReq
import com.tans.tfiletransfer.net.transferproto.conn.wifip2p.model.WifiP2pHandshakeRsp
import io.ktor.network.sockets.InetSocketAddress
import kotlinx.coroutines.launch

class WifiP2pClient(
    private val localDeviceName: String,
    private val serverAddress: Address
) : BaseWifiP2pConnection() {

    override val tag: String = TAG

    private var clientManager: ITcpClientManager? = null
    private var clientTask: ITcpClientTask? = null

    override suspend fun onStartTask() {
        val clientTask = TcpClientTask(
            serverAddress = AddressWithPort(serverAddress, TransferProtoConstant.WIFI_P2P_CONN_GROUP_OWNER_PORT)
        )
        clientTask.startTask()
        clientTask.waitTaskConnectedOrError().apply {
            if (this is TaskState.Error) {
                error(TransferException("Failed to start client task. Cause: ${this.throwable?.message}", this.throwable))
                return
            }
        }
        val clientManager = clientTask.defaultClientManager().defaultServerManager()
        val handshakeRsp = try {
            clientManager.requestSimplify<WifiP2pHandshakeReq, WifiP2pHandshakeRsp>(
                requestType = WifiP2pDataType.HandshakeReq.type,
                responseType = WifiP2pDataType.HandshakeRsp.type,
                request = WifiP2pHandshakeReq(
                    version = TransferProtoConstant.VERSION,
                    deviceName = localDeviceName
                )
            )
        } catch (e: Throwable) {
            clientTask.stopTask()
            error(TransferException("Handshake fail. ${e.message}", e))
            return
        }
        val localAddress = (clientTask.socket()?.localAddress as? InetSocketAddress)?.toAddress()
        if (localAddress == null) {
            clientTask.stopTask()
            error(TransferException("Can't get local address."))
            return
        }
        val handshake = WifiP2pHandshake(
            localAddress = localAddress,
            remoteAddress = serverAddress,
            remoteDeviceName = handshakeRsp.deviceName
        )
        wifiHandshakeFlow.value = handshake
        NetLog.d(TAG, "Handshake: $handshake")

        this.clientTask = clientTask
        this.clientManager = clientManager
        updateStateExpect(
            expect = TaskState.Connecting,
            update = TaskState.Connected,
            fail = {
                this.clientTask = null
                this.clientManager = null
                clientTask.stopTask()
                error(TransferException("Fail to update connected state."))
            }
        ) {
            NetLog.d(TAG, "Task connected.")
            clientManager.registerServer(createConnServer)
            clientManager.registerServer(closeP2pServer)
            onConnectionCreated(clientTask)
        }
    }

    override fun clientManager(): ITcpClientManager? = clientManager

    override fun release() {
        clientTask?.stopTask()
        clientManager = null
    }

    private fun onConnectionCreated(
        clientTask: ITcpClientTask,
    ) {
        coroutineScope.launch {
            runCatching {
                clientTask.waitTaskFinished()
                error(TransferException("Client task finished."))
            }
        }
    }

    companion object {
        private const val TAG = "WifiP2pClient"
    }
}