package com.tans.tfiletransfer.net.transferproto

object TransferProtoConstant {
    const val VERSION: Int = 20230523

    /**
     * WiFi-P2P Connect
     */
    const val WIFI_P2P_CONN_GROUP_OWNER_PORT = 1996

    /**
     * Broadcast Connection
     */
    const val BROADCAST_CONN_SCANNER_PORT = 1997
    const val BROADCAST_CONN_SERVER_PORT = 1998
    const val BROADCAST_CONN_CLIENT_PORT = 1999

    /**
     * File Explore
     */
    const val FILE_EXPLORE_SERVER_PORT = 2000

    /**
     * File Transfer
     */
    const val FILE_TRANSFER_SERVER_PORT = 2001

    /**
     * QR code scan connection
     */
    const val QR_CODE_CONN_SERVER_PORT = 2002
    const val QR_CODE_CONN_CLIENT_PORT = 2003
}