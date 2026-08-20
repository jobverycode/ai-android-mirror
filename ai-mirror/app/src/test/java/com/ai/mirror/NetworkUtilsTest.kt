package com.ai.mirror

import com.ai.mirror.utils.NetworkUtils
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkUtilsTest {

    @Test
    fun testValidIpAddresses() {
        assertTrue(NetworkUtils.isValidIp("192.168.1.1"))
        assertTrue(NetworkUtils.isValidIp("10.0.0.1"))
        assertTrue(NetworkUtils.isValidIp("172.16.0.100"))
        assertTrue(NetworkUtils.isValidIp("127.0.0.1"))
        assertTrue(NetworkUtils.isValidIp("255.255.255.255"))
        assertTrue(NetworkUtils.isValidIp("0.0.0.0"))
    }

    @Test
    fun testInvalidIpAddresses() {
        assertFalse(NetworkUtils.isValidIp(""))
        assertFalse(NetworkUtils.isValidIp("abc"))
        assertFalse(NetworkUtils.isValidIp("192.168.1"))
        assertFalse(NetworkUtils.isValidIp("192.168.1.256"))
        assertFalse(NetworkUtils.isValidIp("192.168.1.-1"))
        assertFalse(NetworkUtils.isValidIp("192.168.1.1.1"))
        assertFalse(NetworkUtils.isValidIp("192.168.1.foo"))
    }

    @Test
    fun testValidPorts() {
        assertTrue(NetworkUtils.isValidPort(1024))
        assertTrue(NetworkUtils.isValidPort(8888))
        assertTrue(NetworkUtils.isValidPort(65535))
    }

    @Test
    fun testInvalidPorts() {
        assertFalse(NetworkUtils.isValidPort(0))
        assertFalse(NetworkUtils.isValidPort(80))
        assertFalse(NetworkUtils.isValidPort(1023))
        assertFalse(NetworkUtils.isValidPort(65536))
        assertFalse(NetworkUtils.isValidPort(-1))
    }
}
