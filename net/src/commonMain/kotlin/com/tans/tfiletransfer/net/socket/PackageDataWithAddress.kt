package com.tans.tfiletransfer.net.socket

data class PackageDataWithAddress(
    val pkt: PackageData,
    val address: AddressWithPort
)