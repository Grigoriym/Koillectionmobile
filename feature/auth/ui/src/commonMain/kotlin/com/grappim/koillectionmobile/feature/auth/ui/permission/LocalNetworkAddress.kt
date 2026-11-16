package com.grappim.koillectionmobile.feature.auth.ui.permission

/**
 * Whether a resolved address (4 or 16 bytes, network order) is on the local network: RFC 1918,
 * link-local (`169.254.0.0/16`, `fe80::/10`), or a unique local IPv6 address (`fc00::/7`). The
 * retired `fec0::/10` counts too, as Java's `isSiteLocalAddress` counts it in wallosmobile.
 * Loopback is not local network.
 */
internal fun isLocalNetworkAddress(address: ByteArray): Boolean {
    val first = address.getOrNull(0)?.toInt()?.and(BYTE_MASK) ?: return false
    val second = address.getOrNull(1)?.toInt()?.and(BYTE_MASK) ?: return false
    return when (address.size) {
        IPV4_SIZE ->
            first == 10 ||
                (first == 172 && second and 0xF0 == 16) ||
                (first == 192 && second == 168) ||
                (first == 169 && second == 254)

        IPV6_SIZE ->
            first and 0xFE == 0xFC ||
                (first == 0xFE && second and 0xC0 == 0x80) ||
                (first == 0xFE && second and 0xC0 == 0xC0)

        else -> false
    }
}

private const val BYTE_MASK = 0xFF
private const val IPV4_SIZE = 4
private const val IPV6_SIZE = 16
