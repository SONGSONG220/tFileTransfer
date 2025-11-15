package com.tans.tfiletransfer.net.transferproto.fileexplore.model

enum class FileExploreDataType(val type: Int) {
    HandshakeReq(0),
    HandshakeRsp(1),
    ScanDirReq(2),
    ScanDirRsp(3),
    SendFilesReq(4),
    SendFilesRsp(5),
    DownloadFilesReq(6),
    DownloadFilesRsp(7),
    SendMsgReq(8),
    SendMsgRsp(9),
    HeartbeatReq(10),
    HeartbeatRsp(11)
}