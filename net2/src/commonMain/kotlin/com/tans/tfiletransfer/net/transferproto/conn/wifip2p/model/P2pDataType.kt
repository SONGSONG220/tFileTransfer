package com.tans.tfiletransfer.net.transferproto.conn.wifip2p.model

enum class P2pDataType(val type: Int) {
    HandshakeReq(0),
    HandshakeRsp(1),
    TransferFileReq(2),
    TransferFileRsp(3),
    CloseConnReq(4),
    CloseConnRsp(5)
}