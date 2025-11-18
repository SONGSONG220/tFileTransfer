package com.tans.tfiletransfer.net.transferproto.conn.broadcast.model

enum class BroadcastDataType(val type: Int) {
    BroadcastMsg(0),
    CreateConnReq(1),
    CreateConnRsp(2)
}