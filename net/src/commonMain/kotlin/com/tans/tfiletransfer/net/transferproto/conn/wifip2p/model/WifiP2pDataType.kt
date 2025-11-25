package com.tans.tfiletransfer.net.transferproto.conn.wifip2p.model

enum class WifiP2pDataType(val type: Int) {
    HandshakeReq(0),
    HandshakeRsp(1),
    CreateConnReq(2),
    CreateConnRsp(3),
    CloseP2pReq(4),
    CloseP2pRsp(5)
}