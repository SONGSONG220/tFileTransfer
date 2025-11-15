package com.tans.tfiletransfer.net.transferproto.filetransfer.model

enum class FileTransferDataType(val type: Int) {
    DownloadReq(0),
    DownloadRsp(1),
    SendReq(2),
    SendRsp(3),
    FinishedReq(4),
    FinishedRsp(6),
    ErrorReq(7),
    ErrorRsp(8)
}