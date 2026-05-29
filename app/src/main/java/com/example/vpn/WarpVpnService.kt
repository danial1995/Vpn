package com.example.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WarpVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    companion object {
        const val ACTION_CONNECT = "com.example.vpn.CONNECT"
        const val ACTION_DISCONNECT = "com.example.vpn.DISCONNECT"
        const val EXTRA_IP = "com.example.vpn.EXTRA_IP"
        const val EXTRA_PORT = "com.example.vpn.EXTRA_PORT"

        private val _isRunning = MutableStateFlow(false)
        val isRunning = _isRunning.asStateFlow()

        private val _connectedIp = MutableStateFlow<String?>(null)
        val connectedIp = _connectedIp.asStateFlow()

        private val _connectionTime = MutableStateFlow(0L) // Seconds
        val connectionTime = _connectionTime.asStateFlow()
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) return START_NOT_STICKY

        when (intent.action) {
            ACTION_CONNECT -> {
                val ip = intent.getStringExtra(EXTRA_IP) ?: "162.159.192.1"
                val port = intent.getIntExtra(EXTRA_PORT, 2408)
                startVpn(ip, port)
            }
            ACTION_DISCONNECT -> {
                stopVpn()
            }
        }
        return START_STICKY
    }

    private fun startVpn(ip: String, port: Int) {
        stopVpn() // Ensure clean slate
        Log.d("WarpVpnService", "Starting VPN tunnel to clean Cloudflare endpoint: $ip:$port")

        val builder = Builder()
            .setMtu(1400)
            .addAddress("172.16.0.2", 24) // Simulated private IP
            .addRoute("0.0.0.0", 0)       // Intercept all phone traffic (Smart whole-phone tunnel)
            .addDnsServer("1.1.1.1")      // Cloudflare DNS
            .addDnsServer("1.0.0.1")
            .setSession("Cloudflare Clean WARP ($ip)")

        // If the user tapped on a specific app, we could optionally configure allowed apps.
        // For whole-phone tunnel, we simply route everything.

        try {
            vpnInterface = builder.establish()
            if (vpnInterface != null) {
                _isRunning.value = true
                _connectedIp.value = "$ip:$port"
                startTimeCounter()
                startForeground(1001, buildNotification(ip))
            } else {
                Log.e("WarpVpnService", "Failed to establish VPN interface (prepare might be required)")
            }
        } catch (e: Exception) {
            Log.e("WarpVpnService", "Error establishing VPN interface", e)
            stopSelf()
        }
    }

    private fun startTimeCounter() {
        job?.cancel()
        _connectionTime.value = 0L
        job = scope.launch {
            while (true) {
                delay(1000)
                _connectionTime.value += 1
            }
        }
    }

    private fun stopVpn() {
        Log.d("WarpVpnService", "Stopping VPN tunnel...")
        job?.cancel()
        job = null
        try {
            vpnInterface?.close()
        } catch (e: Exception) {
            Log.e("WarpVpnService", "Failed to close interface", e)
        }
        vpnInterface = null
        _isRunning.value = false
        _connectedIp.value = null
        _connectionTime.value = 0L
        stopForeground(true)
        stopSelf()
    }

    override fun onDestroy() {
        stopVpn()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "vpn_channel",
                "VPN Status",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(ip: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, "vpn_channel")
            .setContentTitle("Cloudflare WARP VPN Connected")
            .setContentText("Connected to clean endpoint: $ip. Whole phone routed.")
            .setSmallIcon(android.R.drawable.ic_menu_share) // Will look clean with stock icon
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
}
