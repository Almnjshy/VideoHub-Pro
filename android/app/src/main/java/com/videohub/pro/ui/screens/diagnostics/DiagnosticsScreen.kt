package com.videohub.pro.ui.screens.diagnostics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.videohub.pro.diagnostics.ResolverVerifier
import com.videohub.pro.ui.theme.AmberPrimary
import com.videohub.pro.ui.theme.DarkBgCard
import com.videohub.pro.ui.theme.DarkBgPrimary
import com.videohub.pro.ui.theme.DarkBorder
import com.videohub.pro.ui.theme.DarkTextPrimary
import com.videohub.pro.ui.theme.DarkTextSecondary
import com.videohub.pro.ui.theme.EmeraldSuccess
import com.videohub.pro.ui.theme.RedError
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class DiagnosticsViewModel @Inject constructor(
    private val resolverVerifier: ResolverVerifier,
    @ApplicationContext private val context: android.content.Context,
) : ViewModel() {

    private val _result = MutableStateFlow<ResolverVerifier.VerificationResult?>(null)
    val result: StateFlow<ResolverVerifier.VerificationResult?> = _result.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    fun runVerification(url: String) {
        if (url.isBlank()) return
        _isRunning.value = true
        _result.value = null

        viewModelScope.launch {
            try {
                val outputDir = File(context.getExternalFilesDir(null), "VideoHub Pro/diagnostics").apply {
                    if (!exists()) mkdirs()
                }
                val result = resolverVerifier.verify(url, outputDir)
                _result.value = result
            } catch (e: Exception) {
                _result.value = ResolverVerifier.VerificationResult(
                    url = url, platform = "error",
                    stages = listOf(ResolverVerifier.StageResult("EXCEPTION", ResolverVerifier.StageStatus.FAIL, e.message ?: "Unknown error")),
                    overallStatus = ResolverVerifier.VerificationStatus.FAILED,
                )
            } finally {
                _isRunning.value = false
            }
        }
    }
}

@Composable
fun DiagnosticsScreen(viewModel: DiagnosticsViewModel = hiltViewModel()) {
    val result by viewModel.result.collectAsState()
    val isRunning by viewModel.isRunning.collectAsState()
    var url by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().background(DarkBgPrimary).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Resolver Diagnostics", color = DarkTextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("Enter a real media URL to test the complete resolver pipeline.", color = DarkTextSecondary, fontSize = 12.sp)

        OutlinedTextField(
            value = url, onValueChange = { url = it },
            label = { Text("Media URL", color = DarkTextSecondary) },
            modifier = Modifier.fillMaxWidth(), singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(color = DarkTextPrimary, fontFamily = FontFamily.Monospace, fontSize = 12.sp),
        )

        Button(
            onClick = { viewModel.runVerification(url) },
            enabled = !isRunning && url.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary),
            shape = RoundedCornerShape(12.dp),
        ) {
            if (isRunning) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.Black)
                Spacer(Modifier.size(8.dp))
                Text("Running...", color = Color.Black, fontSize = 14.sp)
            } else {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(20.dp))
                Spacer(Modifier.size(8.dp))
                Text("Run Verification", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        result?.let { res ->
            val statusColor = when (res.overallStatus) {
                ResolverVerifier.VerificationStatus.PASSED -> EmeraldSuccess
                ResolverVerifier.VerificationStatus.FAILED -> RedError
                ResolverVerifier.VerificationStatus.PARTIAL -> AmberPrimary
            }
            Surface(color = statusColor.copy(alpha = 0.15f), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Overall:", color = DarkTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text(res.overallStatus.name, color = statusColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.fillMaxWidth())
                    Text("Platform: ${res.platform}", color = DarkTextSecondary, fontSize = 11.sp)
                }
            }

            LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(res.stages) { stage -> StageResultCard(stage) }
            }
        }
    }
}

@Composable
private fun StageResultCard(stage: ResolverVerifier.StageResult) {
    val statusColor = when (stage.status) {
        ResolverVerifier.StageStatus.PASS -> EmeraldSuccess
        ResolverVerifier.StageStatus.FAIL -> RedError
        ResolverVerifier.StageStatus.SKIPPED -> DarkTextSecondary
        ResolverVerifier.StageStatus.NOT_VERIFIED -> AmberPrimary
    }
    val statusIcon = when (stage.status) {
        ResolverVerifier.StageStatus.PASS -> Icons.Default.Check
        ResolverVerifier.StageStatus.FAIL -> Icons.Default.Close
        else -> null
    }

    Surface(color = DarkBgCard, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (statusIcon != null) Icon(statusIcon, contentDescription = null, tint = statusColor, modifier = Modifier.size(16.dp))
                Text(stage.stage, color = statusColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.fillMaxWidth())
                Text(stage.status.name, color = statusColor, fontSize = 10.sp, fontWeight = FontWeight.Medium)
            }
            Text(stage.message, color = DarkTextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            if (stage.details.isNotEmpty()) {
                stage.details.forEach { (key, value) ->
                    Text("  $key: $value", color = DarkBorder, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}
