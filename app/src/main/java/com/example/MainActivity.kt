package com.example

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.CleanIp
import com.example.ui.theme.*
import com.example.viewmodel.VpnViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets.safeDrawing
                ) { innerPadding ->
                    VpnDashboardScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VpnDashboardScreen(
    modifier: Modifier = Modifier,
    viewModel: VpnViewModel = viewModel()
) {
    val context = LocalContext.current

    val isVpnRunning by viewModel.isVpnRunning.collectAsStateWithLifecycle()
    val connectedIp by viewModel.connectedIp.collectAsStateWithLifecycle()
    val connectionDuration by viewModel.connectionDuration.collectAsStateWithLifecycle()

    val allCleanIps by viewModel.allCleanIps.collectAsStateWithLifecycle()
    val topCleanIps by viewModel.topCleanIps.collectAsStateWithLifecycle()

    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    val scanProgress by viewModel.scanProgress.collectAsStateWithLifecycle()
    val scanStatusMessage by viewModel.scanStatusMessage.collectAsStateWithLifecycle()

    val isCloudSynced by viewModel.isCloudSynced.collectAsStateWithLifecycle()
    val isCloudSaving by viewModel.isCloudSaving.collectAsStateWithLifecycle()
    val syncStatusMessage by viewModel.syncMessage.collectAsStateWithLifecycle()

    var selectedIpForConnect by remember { mutableStateOf<CleanIp?>(null) }

    // Launcher to capture VPN system authorization dialog result
    val vpnPrepareLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Permission granted, trigger actual service start
            viewModel.toggleVpnConnection(context, selectedIpForConnect)
        } else {
            Toast.makeText(context, "VPN Permission is required to route traffic", Toast.LENGTH_SHORT).show()
        }
    }

    // Helper lambda to initiate VPN connection with permission checking
    val requestVpnConnection: (CleanIp?) -> Unit = { targetIp ->
        selectedIpForConnect = targetIp
        val vpnIntent = VpnService.prepare(context)
        if (vpnIntent != null) {
            vpnPrepareLauncher.launch(vpnIntent)
        } else {
            // Already authorized
            viewModel.toggleVpnConnection(context, targetIp)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        DeepCosmicSlate,
                        Color(0xFF0F172A)
                    )
                )
            )
    ) {
        // App Bar Title / Elegant Header
        CenterAlignedTopAppBar(
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Cloudflare secure logo",
                        tint = WarmOrange,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "WARP CLEAN VPN",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        letterSpacing = 1.5.sp,
                        color = Color.White
                    )
                }
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = Color.Transparent
            )
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Item 1: Glowing Circular Connection Controller (The Pulse)
            item {
                ConnectionRadarCard(
                    isRunning = isVpnRunning,
                    connectedIp = connectedIp ?: "Searching Clean Endpoint...",
                    duration = connectionDuration,
                    onToggleClick = { requestVpnConnection(null) }
                )
            }

            // Item 2: Cloud Sync Controller ("Cloud Save" capability)
            item {
                CloudSyncCard(
                    isSynced = isCloudSynced,
                    isSaving = isCloudSaving,
                    syncMessage = syncStatusMessage,
                    onSyncClick = { viewModel.triggerCloudBackup() }
                )
            }

            // Item 3: Multi-threaded IP Optimizer Scanner
            item {
                SmartSpeedScannerCard(
                    isScanning = isScanning,
                    progress = scanProgress,
                    statusText = scanStatusMessage,
                    onStartScan = { viewModel.startSmartCleanIpScan() }
                )
            }

            // Item 4: Connection Performance & Endpoint Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Verified WARP Endpoints (${allCleanIps.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (allCleanIps.isNotEmpty()) {
                        Text(
                            text = "Clear All",
                            style = MaterialTheme.typography.bodySmall,
                            color = WarmOrange,
                            modifier = Modifier
                                .clickable { viewModel.clearEndpoints() }
                                .padding(4.dp)
                        )
                    }
                }
            }

            // Item 5: List of Cleaned Cloudflare IPs
            if (allCleanIps.isEmpty()) {
                item {
                    NoIpsPlaceholder(onScanClick = { viewModel.startSmartCleanIpScan() })
                }
            } else {
                items(allCleanIps) { cleanIp ->
                    val isCheapestOrCleanest = allCleanIps.firstOrNull()?.ip == cleanIp.ip
                    IpPerformanceRow(
                        cleanIp = cleanIp,
                        isCleanest = isCheapestOrCleanest,
                        isConnectingToThis = isVpnRunning && connectedIp?.contains(cleanIp.ip) == true,
                        onConnectClick = { requestVpnConnection(cleanIp) },
                        onDeleteClick = { viewModel.deleteEndpoint(cleanIp.ip) }
                    )
                }
            }
        }

        // Integrated M3 Bottom Connection Banner for context feedback
        AnimatedVisibility(
            visible = isVpnRunning,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceSlate),
                border = BorderStroke(1.dp, WarmOrange.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(BrightTeal, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Whole-Phone Smart Tunnel Connected",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Routes all network interfaces safely via Cloudflare WARP.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedTextSlate
                        )
                    }
                    Button(
                        onClick = { viewModel.toggleVpnConnection(context) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text("Stop", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ConnectionRadarCard(
    isRunning: Boolean,
    connectedIp: String,
    duration: Long,
    onToggleClick: () -> Unit
) {
    // Rotation animation for active connection radar glow pulse
    val infiniteTransition = rememberInfiniteTransition(label = "RadarPulse")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Angle"
    )

    val scaleAmount by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "RadarScale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, shape = RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceSlate),
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.linearGradient(
                colors = if (isRunning) listOf(WarmOrange, ElectricBlue) else listOf(BorderSlate, BorderSlate)
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Radial Radar Display Ring
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(180.dp)
                    .padding(8.dp)
            ) {
                // Outer Pulse Ring (Animated only when running)
                if (isRunning) {
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .border(
                                width = 1.5.dp,
                                color = WarmOrange.copy(alpha = 0.15f * (2f - scaleAmount)),
                                shape = CircleShape
                            )
                    )
                }

                Canvas(
                    modifier = Modifier
                        .size(140.dp)
                        .rotate(angle)
                ) {
                    val brush = Brush.sweepGradient(
                        colors = if (isRunning) {
                            listOf(WarmOrange, ElectricBlue, Color.Transparent, WarmOrange)
                        } else {
                            listOf(BorderSlate, BorderSlate.copy(alpha = 0.2f), BorderSlate)
                        }
                    )
                    drawCircle(
                        brush = brush,
                        radius = size.minDimension / 2,
                        style = Stroke(width = 4.dp.toPx())
                    )
                }

                // Internal Central Power Control Button Action
                Surface(
                    onClick = onToggleClick,
                    shape = CircleShape,
                    color = if (isRunning) WarmOrange else BorderSlate,
                    modifier = Modifier
                        .size(105.dp)
                        .shadow(8.dp, CircleShape)
                        .testTag("vpn_toggle_button")
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (isRunning) Icons.Default.PowerSettingsNew else Icons.Default.PowerOff,
                                contentDescription = "VPN Switch Toggle Button",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isRunning) "CONNECTED" else "DISCONNECTED",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White.copy(alpha = 0.9f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // State indicators
            Text(
                text = if (isRunning) "SECURITY ENFORCED" else "UNAUTHORIZED ROUTING",
                style = MaterialTheme.typography.labelSmall,
                color = if (isRunning) BrightTeal else MutedTextSlate,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (isRunning) connectedIp else "Whole-phone traffic unencrypted",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (isRunning) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = "Session Timer",
                        tint = ElectricBlue,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = formatTimer(duration),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
fun CloudSyncCard(
    isSynced: Boolean,
    isSaving: Boolean,
    syncMessage: String,
    onSyncClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceSlate),
        border = BorderStroke(1.dp, BorderSlate)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Color.White.copy(alpha = 0.06f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isSynced) Icons.Default.CloudDone else Icons.Default.CloudUpload,
                    contentDescription = "Cloud backup sync status icon",
                    tint = if (isSynced) BrightTeal else ElectricBlue,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Cloud Backup Wallet",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (syncMessage.isNotEmpty()) syncMessage else "Cloud save endpoints for seamless transition across devices.",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSynced) BrightTeal.copy(alpha = 0.9f) else MutedTextSlate,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onSyncClick,
                modifier = Modifier
                    .background(if (isSaving) Color.Transparent else WarmOrange.copy(alpha = 0.15f), CircleShape)
                    .testTag("cloud_sync_button"),
                enabled = !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = WarmOrange,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = "Trigger backup",
                        tint = WarmOrange
                    )
                }
            }
        }
    }
}

@Composable
fun SmartSpeedScannerCard(
    isScanning: Boolean,
    progress: Float,
    statusText: String,
    onStartScan: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceSlate),
        border = BorderStroke(1.dp, BorderSlate)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = "Speed check",
                        tint = ElectricBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Smart CDN Clean IP Scanner",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Button(
                    onClick = onStartScan,
                    enabled = !isScanning,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WarmOrange,
                        disabledContainerColor = WarmOrange.copy(alpha = 0.3f)
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text(
                        text = if (isScanning) "SCANNING" else "SCAN NOW",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = statusText,
                style = MaterialTheme.typography.bodySmall,
                color = MutedTextSlate
            )

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = if (isScanning) ElectricBlue else BorderSlate,
                trackColor = Color.White.copy(alpha = 0.05f)
            )
        }
    }
}

@Composable
fun IpPerformanceRow(
    cleanIp: CleanIp,
    isCleanest: Boolean,
    isConnectingToThis: Boolean,
    onConnectClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onConnectClick() }
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isConnectingToThis) DarkSurfaceSlate else DarkSurfaceSlate.copy(alpha = 0.8f)
        ),
        border = BorderStroke(
            width = 1.dp,
            color = when {
                isConnectingToThis -> WarmOrange
                isCleanest -> ElectricBlue.copy(alpha = 0.6f)
                else -> BorderSlate
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Status Pillar Indicator
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(Color.White.copy(alpha = 0.05f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isConnectingToThis) Icons.Default.VpnLock else Icons.Default.NetworkCheck,
                    contentDescription = "Active VPN check status indicator",
                    tint = when {
                        isConnectingToThis -> WarmOrange
                        isCleanest -> BrightTeal
                        else -> MutedTextSlate
                    },
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = cleanIp.ip,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = ":${cleanIp.port}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedTextSlate
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Latency Badge
                    Text(
                        text = "${cleanIp.latency} ms",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (cleanIp.latency < 50) BrightTeal else WarmOrange,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    // Packet loss check
                    Text(
                        text = if (cleanIp.packetLoss == 0.0) "0% Loss" else "${String.format("%.1f", cleanIp.packetLoss)}% Loss",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedTextSlate,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    // Subnet tag type
                    Text(
                        text = cleanIp.subnet,
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedTextSlate.copy(alpha = 0.7f),
                        fontSize = 10.sp
                    )
                }
            }

            // Quick badging & delete options
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isCleanest && !isConnectingToThis) {
                    Box(
                        modifier = Modifier
                            .background(BrightTeal.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "SMART FOCUS",
                            color = BrightTeal,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remove this node",
                        tint = Color.Red.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun NoIpsPlaceholder(onScanClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceSlate.copy(alpha = 0.4f)),
        border = BorderStroke(1.dp, BorderSlate.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.FilterListOff,
                contentDescription = "Blank status indicator icon",
                tint = MutedTextSlate,
                modifier = Modifier.size(34.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "No custom clean channels",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Tap Scan to populate high performance secure Cloudflare Warp pings.",
                style = MaterialTheme.typography.bodySmall,
                color = MutedTextSlate,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }
    }
}

private fun formatTimer(seconds: Long): String {
    val hrs = seconds / 3600
    val mins = (seconds % 3600) / 60
    val secs = seconds % 60
    return if (hrs > 0) {
        String.format("%02d:%02d:%02d", hrs, mins, secs)
    } else {
        String.format("%02d:%02d", mins, secs)
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name! Secure Cloudflare Vpn Ready.",
        modifier = modifier,
        color = Color.White
    )
}
