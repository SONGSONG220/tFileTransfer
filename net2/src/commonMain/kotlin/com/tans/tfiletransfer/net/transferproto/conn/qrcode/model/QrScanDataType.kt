package com.tans.tfiletransfer.net.transferproto.conn.qrcode.model

enum class QrScanDataType(val type: Int) {
    TransferFileReq(0),
    TransferFileRsp(1)
}