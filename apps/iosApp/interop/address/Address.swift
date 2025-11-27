import Foundation
import Darwin

@objc(Address)
public class Address: NSObject {

    @objc public static func findLocalIPv4Addresses() -> [String] {
        var result: [String] = []
        var ifaddr: UnsafeMutablePointer<ifaddrs>? = nil
        guard getifaddrs(&ifaddr) == 0, let first = ifaddr else { return result }
        defer { freeifaddrs(ifaddr) }
        var ptr = first
        while true {
            let ifa = ptr.pointee
            if let addr = ifa.ifa_addr, addr.pointee.sa_family == sa_family_t(AF_INET) {
                let sin = UnsafeMutablePointer<sockaddr_in>(OpaquePointer(addr))!.pointee
                let s = sin.sin_addr
                if let cStr = inet_ntoa(s) {
                    let ip = String(cString: cStr)
                    if !ip.hasPrefix("127.") && !ip.hasPrefix("169.254.") {
                        result.append(ip)
                    }
                }
            }
            if let next = ifa.ifa_next { ptr = next } else { break }
        }
        return result
    }

    @objc public static func broadcastAddress(for ip: String) -> String? {
        var ifaddr: UnsafeMutablePointer<ifaddrs>? = nil
        guard getifaddrs(&ifaddr) == 0, let first = ifaddr else { return nil }
        defer { freeifaddrs(ifaddr) }
        var ptr = first
        while true {
            let ifa = ptr.pointee
            if let addr = ifa.ifa_addr, addr.pointee.sa_family == sa_family_t(AF_INET) {
                let sin = UnsafeMutablePointer<sockaddr_in>(OpaquePointer(addr))!.pointee
                let s = sin.sin_addr
                if let cStr = inet_ntoa(s) {
                    let ipStr = String(cString: cStr)
                    if ipStr == ip {
                        let flags = ifa.ifa_flags
                        if (flags & UInt32(IFF_BROADCAST)) != 0, let dst = ifa.ifa_dstaddr, dst.pointee.sa_family == sa_family_t(AF_INET) {
                            let bsin = UnsafeMutablePointer<sockaddr_in>(OpaquePointer(dst))!.pointee
                            let bs = bsin.sin_addr
                            if let bcStr = inet_ntoa(bs) { return String(cString: bcStr) }
                        }
                        if let netmask = ifa.ifa_netmask {
                            let msin = UnsafeMutablePointer<sockaddr_in>(OpaquePointer(netmask))!.pointee
                            let ipNet = sin.sin_addr.s_addr
                            let maskNet = msin.sin_addr.s_addr
                            let ipHost = UInt32(bigEndian: ipNet)
                            let maskHost = UInt32(bigEndian: maskNet)
                            let bcHost = ipHost | ~maskHost
                            let bcNet = UInt32(bigEndian: bcHost)
                            let bcAddr = in_addr(s_addr: bcNet)
                            if let bcStr = inet_ntoa(bcAddr) { return String(cString: bcStr) }
                        }
                        return "255.255.255.255"
                    }
                }
            }
            if let next = ifa.ifa_next { ptr = next } else { break }
        }
        return nil
    }
}
