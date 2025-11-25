package com.tans.tfiletransfer.net.transferproto.conn.wifip2p.model

import com.tans.tfiletransfer.net.socket.Address

data class WifiP2pHandshake(
    val localAddress: Address,
    val remoteAddress: Address,
    val remoteDeviceName: String
)