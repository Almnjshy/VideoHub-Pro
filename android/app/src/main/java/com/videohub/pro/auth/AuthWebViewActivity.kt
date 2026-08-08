package com.videohub.pro.auth

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.videohub.pro.ui.theme.DarkBgPrimary
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Authentication WebView Activity — lets the user log in to a platform.
 *
 * The user performs login themselves. We do NOT bypass CAPTCHA, MFA, or any security.
 * After login, we extract cookies from the WebView and save them securely.
 */
@AndroidEntryPoint
class AuthWebViewActivity : ComponentActivity() {

    @Inject lateinit var authManager: AuthenticationManager

    companion object {
        const val EXTRA_PLATFORM_ID = "platform_id"
        const val EXTRA_SUCCESS = "success"

        fun start(context: android.content.Context, platformId: String) {
            val intent = Intent(context, AuthWebViewActivity::class.java).apply {
                putExtra(EXTRA_PLATFORM_ID, platformId)
            }
            context.startActivity(intent)
        }
    }

    private var platformId: String = ""
    private var isLoggedIn = false

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        platformId = intent.getStringExtra(EXTRA_PLATFORM_ID) ?: return finish()

        val loginUrl = authManager.getLoginUrl(platformId) ?: return finish()

        setContent {
            AuthWebViewScreen(
                loginUrl = loginUrl,
                platformId = platformId,
                onLoginDetected = { cookies ->
                    // Save session and close
                    authManager.saveSession(platformId, cookies)
                    isLoggedIn = true
                    val resultIntent = Intent().putExtra(EXTRA_SUCCESS, true)
                    setResult(Activity.RESULT_OK, resultIntent)
                    finish()
                },
                onCancel = {
                    setResult(Activity.RESULT_CANCELED)
                    finish()
                },
            )
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun AuthWebViewScreen(
    loginUrl: String,
    platformId: String,
    onLoginDetected: (Map<String, String>) -> Unit,
    onCancel: () -> Unit,
) {
    var isLoading by remember { mutableStateOf(true) }
    var statusText by remember { mutableStateOf("جاري تحميل صفحة تسجيل الدخول...") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBgPrimary),
    ) {
        // Status bar
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(0.08f)
                .padding(8.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (isLoading) {
                androidx.compose.foundation.layout.Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
                ) {
                    CircularProgressIndicator(modifier = Modifier.padding(4.dp), strokeWidth = 2.dp)
                    Text(statusText, color = Color.White, fontSize = 12.sp)
                }
            } else {
                Text("سجّل الدخول إلى $platformId", color = Color.White, fontSize = 14.sp)
            }
        }

        // WebView
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.userAgentString = "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36"
                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                            isLoading = true
                            statusText = "جاري التحميل..."
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            isLoading = false
                            // Check if login was successful by examining cookies
                            val cookieManager = CookieManager.getInstance()
                            val cookies = cookieManager.getCookie(url ?: "")
                            if (cookies != null && isLoggedInSuccess(platformId, url, cookies)) {
                                // Extract cookies as map
                                val cookieMap = parseCookies(cookies)
                                onLoginDetected(cookieMap)
                            }
                        }

                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                            return false
                        }
                    }
                    webChromeClient = WebChromeClient()
                    loadUrl(loginUrl)
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .weight(0.92f),
        )
    }
}

/**
 * Check if login was successful based on URL and cookies.
 * Platform-specific heuristics.
 */
private fun isLoggedInSuccess(platformId: String, url: String?, cookies: String): Boolean {
    return when (platformId) {
        "youtube" -> cookies.contains("SID=") || cookies.contains("SAPISID=")
        "facebook" -> cookies.contains("c_user=") || cookies.contains("xs=")
        "tiktok" -> cookies.contains("sessionid=") || cookies.contains("sid_tt=")
        "instagram" -> cookies.contains("sessionid=") || cookies.contains("ds_user_id=")
        "x" -> cookies.contains("auth_token=") || cookies.contains("ct0=")
        "reddit" -> cookies.contains("reddit_session=") || cookies.contains("token=")
        else -> false
    }
}

/**
 * Parse cookie string into a map.
 */
private fun parseCookies(cookieString: String): Map<String, String> {
    return cookieString.split(";")
        .mapNotNull { cookie ->
            val parts = cookie.trim().split("=", limit = 2)
            if (parts.size == 2) parts[0].trim() to parts[1].trim() else null
        }
        .toMap()
}
