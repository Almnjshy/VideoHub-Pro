package com.videohub.pro.ui.screens.diagnostics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.videohub.pro.resolver.ResolverManager
import com.videohub.pro.ui.theme.AmberPrimary
import com.videohub.pro.ui.theme.DarkBgCard
import com.videohub.pro.ui.theme.DarkBgPrimary
import com.videohub.pro.ui.theme.DarkBorder
import com.videohub.pro.ui.theme.DarkTextPrimary
import com.videohub.pro.ui.theme.DarkTextSecondary
import com.videohub.pro.ui.theme.EmeraldSuccess
import com.videohub.pro.ui.theme.RedError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class DiagnosticResult(
    val testName: String,
    val status: DiagnosticStatus,
    val message: String,
    val durationMs: Long,
)

enum class DiagnosticStatus { PASS, FAIL, RUNNING, PENDING }

@HiltViewModel
class DiagnosticsViewModel @Inject constructor(
    private val resolverManager: ResolverManager,
) : ViewModel() {
    var results by mutableStateOf<List<DiagnosticResult>>(emptyList())
    var isRunning by mutableStateOf(false)

    fun runDiagnostics() {
        isRunning = true
        results = listOf(
            DiagnosticResult("Python Engine", DiagnosticStatus.RUNNING, "Checking...", 0),
            DiagnosticResult("yt-dlp Version", DiagnosticStatus.PENDING, "Waiting", 0),
            DiagnosticResult("YouTube Resolve", DiagnosticStatus.PENDING, "Waiting", 0),
            DiagnosticResult("Network Connectivity", DiagnosticStatus.PENDING, "Waiting", 0),
        )
        viewModelScope.launch {
            // Test 1: Python Engine
            val t1 = System.currentTimeMillis()
            val ready = withContext(Dispatchers.IO) { resolverManager.isReady() }
            results = results.toMutableList().also {
                it[0] = DiagnosticResult("Python Engine", if (ready) DiagnosticStatus.PASS else DiagnosticStatus.FAIL, if (ready) "Python + yt-dlp running" else "Failed to start", System.currentTimeMillis() - t1)
            }

            // Test 2: yt-dlp Version
            val t2 = System.currentTimeMillis()
            val version = withContext(Dispatchers.IO) { resolverManager.getVersion() }
            results = results.toMutableList().also {
                it[1] = DiagnosticResult("yt-dlp Version", if (version != "unknown") DiagnosticStatus.PASS else DiagnosticStatus.FAIL, "yt-dlp $version", System.currentTimeMillis() - t2)
            }

            // Test 3: YouTube Resolve
            val t3 = System.currentTimeMillis()
            results = results.toMutableList().also { it[2] = DiagnosticResult("YouTube Resolve", DiagnosticStatus.RUNNING, "Testing...", 0) }
            try {
                val resolveResult = withContext(Dispatchers.IO) {
                    resolverManager.resolve("https://www.youtube.com/watch?v=dQw4w9WgXcQ", "diag-test")
                }
                val ok = resolveResult.status == com.videohub.pro.resolver.ResolveStatus.RESOLVED
                results = results.toMutableList().also {
                    it[2] = DiagnosticResult("YouTube Resolve", if (ok) DiagnosticStatus.PASS else DiagnosticStatus.FAIL, if (ok) "${resolveResult.formats.size} formats found" else resolveResult.errorMessage ?: "Failed", System.currentTimeMillis() - t3)
                }
            } catch (e: Exception) {
                results = results.toMutableList().also {
                    it[2] = DiagnosticResult("YouTube Resolve", DiagnosticStatus.FAIL, e.message ?: "Exception", System.currentTimeMillis() - t3)
                }
            }

            // Test 4: Network
            val t4 = System.currentTimeMillis()
            results = results.toMutableList().also { it[3] = DiagnosticResult("Network Connectivity", DiagnosticStatus.RUNNING, "Checking...", 0) }
            try {
                val netOk = withContext(Dispatchers.IO) {
                    try {
                        java.net.URL("https://www.google.com").openConnection().connect()
                        true
                    } catch (e: Exception) { false }
                }
                results = results.toMutableList().also {
                    it[3] = DiagnosticResult("Network Connectivity", if (netOk) DiagnosticStatus.PASS else DiagnosticStatus.FAIL, if (netOk) "Connected" else "No internet", System.currentTimeMillis() - t4)
                }
            } catch (e: Exception) {
                results = results.toMutableList().also {
                    it[3] = DiagnosticResult("Network Connectivity", DiagnosticStatus.FAIL, e.message ?: "Failed", System.currentTimeMillis() - t4)
                }
            }

            isRunning = false
        }
    }
}

@Composable
fun DiagnosticsScreen(
    viewModel: DiagnosticsViewModel = hiltViewModel(),
) {
    Column(
        modifier = Modifier.fillMaxSize().background(DarkBgPrimary).verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.BugReport, contentDescription = null, tint = AmberPrimary, modifier = Modifier.size(22.dp))
                Text("التشخيص", color = DarkTextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = { viewModel.runDiagnostics() },
                enabled = !viewModel.isRunning,
                colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary),
            ) { Text("تشغيل الفحص", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
        }

        if (viewModel.isRunning && viewModel.results.isEmpty()) {
            Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = AmberPrimary, modifier = Modifier.size(32.dp)) }
        }

        viewModel.results.forEach { result ->
            DiagnosticCard(result)
        }

        if (viewModel.results.isNotEmpty()) {
            val passed = viewModel.results.count { it.status == DiagnosticStatus.PASS }
            val failed = viewModel.results.count { it.status == DiagnosticStatus.FAIL }
            Surface(color = DarkBgCard, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("النتيجة الإجمالية", color = DarkTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text("✓ نجح: $passed", color = EmeraldSuccess, fontSize = 12.sp)
                    if (failed > 0) Text("✗ فشل: $failed", color = RedError, fontSize = 12.sp)
                }
            }
        }

        Spacer(Modifier.height(80.dp))
    }
}

@Composable
private fun DiagnosticCard(result: DiagnosticResult) {
    val statusColor = when (result.status) {
        DiagnosticStatus.PASS -> EmeraldSuccess
        DiagnosticStatus.FAIL -> RedError
        DiagnosticStatus.RUNNING -> AmberPrimary
        DiagnosticStatus.PENDING -> DarkBorder
    }
    val statusIcon = when (result.status) {
        DiagnosticStatus.PASS -> Icons.Default.CheckCircle
        DiagnosticStatus.FAIL -> Icons.Default.Error
        DiagnosticStatus.RUNNING -> Icons.Default.Speed
        DiagnosticStatus.PENDING -> Icons.Default.BugReport
    }
    val statusLabel = when (result.status) {
        DiagnosticStatus.PASS -> "نجح"
        DiagnosticStatus.FAIL -> "فشل"
        DiagnosticStatus.RUNNING -> "جارٍ"
        DiagnosticStatus.PENDING -> "بانتظار"
    }

    Surface(color = DarkBgCard, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(color = statusColor.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp), modifier = Modifier.size(36.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    if (result.status == DiagnosticStatus.RUNNING) {
                        CircularProgressIndicator(color = statusColor, modifier = Modifier.size(16.dp))
                    } else {
                        Icon(statusIcon, contentDescription = null, tint = statusColor, modifier = Modifier.size(18.dp))
                    }
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(result.testName, color = DarkTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text(result.message, color = DarkTextSecondary, fontSize = 10.sp)
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(statusLabel, color = statusColor, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                if (result.durationMs > 0) Text("${result.durationMs}ms", color = DarkBorder, fontSize = 9.sp)
            }
        }
    }
}
