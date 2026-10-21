package com.example.samsungflashtool

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var usbManager: UsbManager
    private var lokeManager: LokeUsbManager? = null
    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = androidx.appcompat.widget.LinearLayoutCompat(this).apply {
            orientation = androidx.appcompat.widget.LinearLayoutCompat.VERTICAL
            setPadding(48, 48, 48, 48)
        }

        statusText = TextView(this).apply { text = "Samsung cihaz bekleniyor..." }
        val btnConnect = Button(this).apply { text = "Cihaza Bağlan (LOKE)" }

        layout.addView(statusText)
        layout.addView(btnConnect)
        setContentView(layout)

        usbManager = getSystemService(Context.USB_SERVICE) as UsbManager
        lokeManager = LokeUsbManager(usbManager)

        btnConnect.setOnClickListener {
            checkAndConnectDevice()
        }
    }

    private fun checkAndConnectDevice() {
        val deviceList = usbManager.deviceList
        val samsungDevice = deviceList.values.firstOrNull { it.vendorId == 0x04E8 }

        if (samsungDevice == null) {
            statusText.text = "Hata: Download modunda Samsung cihaz bulunamadı!"
            return
        }

        if (!usbManager.hasPermission(samsungDevice)) {
            val permissionIntent = PendingIntent.getBroadcast(
                this, 0, Intent("com.example.samsungflashtool.USB_PERMISSION"), PendingIntent.FLAG_IMMUTABLE
            )
            usbManager.requestPermission(samsungDevice, permissionIntent)
        } else {
            startLokeConnection(samsungDevice)
        }
    }

    private fun startLokeConnection(device: UsbDevice) {
        CoroutineScope(Dispatchers.IO).launch {
            val initialized = lokeManager?.initDevice(device) ?: false
            if (!initialized) {
                updateStatus("USB Bağlantısı kurulamadı.")
                return@launch
            }

            updateStatus("LOKE El sıkışması yapılıyor...")
            val success = lokeManager?.performLokeHandshake() ?: false

            if (success) {
                updateStatus("Başarılı! Cihaz Download modunda yanıt verdi.")
            } else {
                updateStatus("LOKE el sıkışması başarısız oldu.")
            }
        }
    }

    private suspend fun updateStatus(msg: String) {
        withContext(Dispatchers.Main) {
            statusText.text = msg
        }
    }
}
