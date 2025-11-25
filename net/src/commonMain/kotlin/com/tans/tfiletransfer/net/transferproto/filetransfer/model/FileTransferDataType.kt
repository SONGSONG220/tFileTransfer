package com.tans.tfiletransfer.net.transferproto.filetransfer.model

enum class FileTransferDataType(val type: Int) {
    DownloadFileSegmentReq(0),
    DownloadFileSegmentRsp(1),
    SendFileBufferReq(2),
    SendFileBufferRsp(3),
    DownloadFileSegmentEndReq(4),
    DownloadFileSegmentEndRsp(6),
    ErrorReq(7),
    ErrorRsp(8)
}