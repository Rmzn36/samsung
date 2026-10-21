package com.example.samsungflashtool

import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager

class LokeUsbManager(private val usbManager: UsbManager) {

    private var connection: UsbDeviceConnection? = null
    private var endpointOut: UsbEndpoint? = null
    private var endpointIn: UsbEndpoint? = null

    fun initDevice(device: UsbDevice): Boolean {
        val intf: UsbInterface = device.getInterface(0)
        for (i in 0 until intf.endpointCount) {
            val ep = intf.getEndpoint(i)
            if (ep.type == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                if (ep.direction == UsbConstants.USB_DIR_OUT) endpointOut = ep
                else endpointIn = ep
            }
        }
        connection = usbManager.openDevice(device)
        return connection?.claimInterface(intf, true) ?: false
    }

    fun performLokeHandshake(): Boolean {
        if (connection == null || endpointOut == null) return false
        val handshakePacket = "LOKE".toByteArray(Charsets.US_ASCII)
        val sentBytes = connection!!.bulkTransfer(endpointOut, handshakePacket, handshakePacket.size, 5000)
        if (sentBytes < 0) return false

        val responseBuffer = ByteArray(64)
        val receivedBytes = connection!!.bulkTransfer(endpointIn, responseBuffer, responseBuffer.size, 5000)
        return receivedBytes > 0
    }

    fun close() {
        connection?.close()
        connection = null
    }
}
