package com.technource.android.eTMS

import android.os.Bundle
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import android.webkit.JavascriptInterface
import com.google.gson.Gson
import com.technource.android.R
import com.technource.android.system_status.SystemStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DashboardActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private val gson = Gson()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        webView = findViewById(R.id.web_view)
        webView.settings.javaScriptEnabled = true
        webView.addJavascriptInterface(WebAppInterface(), "Android")

        // Load the dashboard HTML
        webView.loadUrl("file:///android_asset/DashBoard.html")

        // Make webView accessible globally
        webViewInstance = webView

        // Flush any queued logs
        flushLogQueue()
    }

    companion object {
        var webViewInstance: WebView? = null
        private val logQueue = mutableListOf<LogEntry>()

        data class LogEntry(
            val component: String,
            val message: String,
            val status: String,
            val timestamp: String,
            val metadata: Map<String, Any>?
        )

        fun logEventToWebView(
            component: String,
            message: String,
            status: String,
            timestamp: String,
            metadata: Map<String, Any>? = null
        ) {
            if (webViewInstance != null) {
                webViewInstance?.post {
                    val jsonMetadata = metadata?.let { Gson().toJson(it) } ?: "{}"
                    webViewInstance?.evaluateJavascript(
                        "window.logEvent('$component', '$message', '$status', '$timestamp', $jsonMetadata)",
                        null
                    )
                }
                // Flush queued logs
                flushLogQueue()
            } else {
                logQueue.add(LogEntry(component, message, status, timestamp, metadata))
                // Fallback to SystemStatus for debugging
                SystemStatus.logEvent(component, "Queued log: $message")
            }
        }

        private fun flushLogQueue() {
            webViewInstance?.post {
                logQueue.forEach { log ->
                    val jsonMetadata = log.metadata?.let { Gson().toJson(it) } ?: "{}"
                    webViewInstance?.evaluateJavascript(
                        "window.logEvent('${log.component}', '${log.message}', '${log.status}', '${log.timestamp}', $jsonMetadata)",
                        null
                    )
                }
                logQueue.clear()
            }
        }
    }

    // JavaScript interface for potential JavaScript-to-Kotlin communication
    inner class WebAppInterface {
        @JavascriptInterface
        fun logEvent(component: String, message: String, status: String, timestamp: String, metadata: String) {
            // Handle JavaScript-to-Kotlin logging if needed
        }
    }
}