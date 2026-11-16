package com.grappim.koillectionmobile.feature.auth.ui.permission

import java.net.InetAddress
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** The JVM parses the literals, so the bytes are the ones `InetAddress.getAllByName` gives on Android. */
class LocalNetworkAddressTest {

    private fun isLocal(literal: String) = isLocalNetworkAddress(InetAddress.getByName(literal).address)

    @Test
    fun `private and link-local addresses are local`() {
        listOf(
            "10.0.2.2", "10.255.255.255", "172.16.0.1", "172.31.255.255", "192.168.0.241",
            "169.254.1.1", "fe80::1", "febf::1", "fec0::1", "fd12:3456::1", "fc00::1"
        ).forEach { assertTrue(isLocal(it), it) }
    }

    @Test
    fun `public and loopback addresses are not local`() {
        listOf(
            "1.1.1.1", "9.255.255.255", "11.0.0.1", "172.15.255.255", "172.32.0.1", "192.167.1.1",
            "192.169.1.1", "169.253.1.1", "127.0.0.1", "::1", "2606:4700:4700::1111", "fbff::1", "fe00::1"
        ).forEach { assertFalse(isLocal(it), it) }
    }

    @Test
    fun `an address of the wrong size is not local`() {
        assertFalse(isLocalNetworkAddress(byteArrayOf()))
        assertFalse(isLocalNetworkAddress(byteArrayOf(10, 0, 0)))
    }
}
