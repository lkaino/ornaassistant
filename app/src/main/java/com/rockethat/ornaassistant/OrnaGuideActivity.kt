package com.rockethat.ornaassistant

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.rockethat.ornaassistant.R

class OrnaGuideActivity : AppCompatActivity() {

    companion object {
        private const val ORNA_HUB_URL = "https://orna.guide"
    }

    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ornahub)
        webView = findViewById(R.id.webview)
        setupWebView()
        webView.loadUrl(ORNA_HUB_URL)
    }

    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = false
        }

        webView.webViewClient = getWebViewClient()
    }

    private fun getWebViewClient() = object : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            val url = request.url.toString()
            if (Uri.parse(url).host == "www.orna.guide") {
                return false
            }
            Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                startActivity(this)
            }
            return true
        }
    }
}
