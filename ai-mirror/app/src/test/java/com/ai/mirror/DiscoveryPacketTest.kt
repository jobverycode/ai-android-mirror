package com.ai.mirror

import com.ai.mirror.data.discovery.UdpBeaconPayload
import com.ai.mirror.data.model.DeviceRole
import com.ai.mirror.data.model.DiscoveredDevice
import com.ai.mirror.data.protocol.PairRequestPayload
import com.ai.mirror.data.protocol.PairResponsePayload
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiscoveryPacketTest {

    private val gson = Gson()

    @Test
    fun testUdpBeaconSerialization() {
        val payload = UdpBeaconPayload(
            id = "dev_1234",
            name = "Pixel 8 Pro",
            ip = "192.168.1.50",
            port = 8888,
            role = "SENDER",
            timestamp = 1000000L
        )

        val json = gson.toJson(payload)
        val deserialized = gson.fromJson(json, UdpBeaconPayload::class.java)

        assertEquals("dev_1234", deserialized.id)
        assertEquals("Pixel 8 Pro", deserialized.name)
        assertEquals("192.168.1.50", deserialized.ip)
        assertEquals(8888, deserialized.port)
        assertEquals("SENDER", deserialized.role)
        assertEquals(1000000L, deserialized.timestamp)
    }

    @Test
    fun testPairRequestAndResponsePayload() {
        val req = PairRequestPayload(
            deviceId = "receiver_01",
            deviceName = "Xiaomi 14",
            role = "RECEIVER",
            clientTimestamp = 2000000L
        )
        val reqJson = gson.toJson(req)
        val deserializedReq = gson.fromJson(reqJson, PairRequestPayload::class.java)
        assertEquals("receiver_01", deserializedReq.deviceId)
        assertEquals("Xiaomi 14", deserializedReq.deviceName)

        val resp = PairResponsePayload(
            accepted = true,
            serverDeviceId = "sender_01",
            serverDeviceName = "Galaxy S24",
            message = "OK"
        )
        val respJson = gson.toJson(resp)
        val deserializedResp = gson.fromJson(respJson, PairResponsePayload::class.java)
        assertTrue(deserializedResp.accepted)
        assertEquals("sender_01", deserializedResp.serverDeviceId)
        assertEquals("Galaxy S24", deserializedResp.serverDeviceName)
    }

    @Test
    fun testDiscoveredDeviceHelpers() {
        val senderDevice = DiscoveredDevice(
            id = "s1",
            name = "Sender Phone",
            ip = "192.168.1.20",
            port = 8888,
            role = DeviceRole.SENDER
        )
        assertEquals("192.168.1.20:8888", senderDevice.endpoint)
        assertTrue(senderDevice.isSender)
        assertFalse(senderDevice.isReceiver)

        val receiverDevice = DiscoveredDevice(
            id = "r1",
            name = "Receiver Tablet",
            ip = "192.168.1.25",
            port = 8888,
            role = DeviceRole.RECEIVER
        )
        assertEquals("192.168.1.25:8888", receiverDevice.endpoint)
        assertFalse(receiverDevice.isSender)
        assertTrue(receiverDevice.isReceiver)
    }
}
