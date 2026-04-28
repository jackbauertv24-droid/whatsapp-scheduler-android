package com.example.wascheduler.bridge

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.*

class WebViewManager(
    private val context: Context,
    private val onEvent: (type: String, data: JsonObject) -> Unit
) {
    private var webView: WebView? = null
    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(Dispatchers.Main)
    private val gson = Gson()
    
    private var connectionState = ConnectionState.DISCONNECTED
    private var pairingCode: String? = null
    private var userPhone: String? = null
    
    enum class ConnectionState {
        DISCONNECTED,
        CONNECTING,
        PAIRING,
        CONNECTED
    }
    
    fun initWebView(): WebView {
        if (webView != null) return webView!!
        
        webView = WebView(context).apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                allowFileAccess = true
                allowContentAccess = true
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                blockNetworkImage = false
                loadsImagesAutomatically = true
                mediaPlaybackRequiresUserGesture = false
            }
            
            webChromeClient = object : WebChromeClient() {
                override fun onConsoleMessage(message: android.webkit.ConsoleMessage?): Boolean {
                    Log.d("WebViewConsole", "${message?.message()} (${message?.sourceId()}:${message?.lineNumber()})")
                    return true
                }
            }
            
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    Log.d("WebViewManager", "Page finished: $url")
                    checkWhatsAppReady()
                }
                
                override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                    Log.e("WebViewManager", "Error loading ${request?.url}: ${error?.description}")
                }
            }
            
            addJavascriptInterface(JsBridge { type, data ->
                handler.post {
                    handleJsEvent(type, data)
                }
            }, "AndroidBridge")
        }
        
        loadWhatsAppClient()
        return webView!!
    }
    
    private fun loadWhatsAppClient() {
        Log.d("WebViewManager", "Loading WhatsApp client")
        webView?.loadUrl("file:///android_asset/whatsapp/index.html")
    }
    
    private fun checkWhatsAppReady() {
        handler.postDelayed({
            webView?.evaluateJavascript(
                "(function() { return typeof WhatsApp !== 'undefined' && typeof WhatsApp.init === 'function'; })();",
                { result ->
                    Log.d("WebViewManager", "WhatsApp ready: $result")
                    if (result == "true") {
                        Log.d("WebViewManager", "WhatsApp client loaded successfully")
                    } else {
                        Log.e("WebViewManager", "WhatsApp client not loaded, retrying...")
                        handler.postDelayed({ checkWhatsAppReady() }, 500)
                    }
                }
            )
        }, 1000)
    }
    
    private fun handleJsEvent(type: String, data: JsonObject) {
        Log.d("WebViewManager", "Event: $type, data: $data")
        
        when (type) {
            "connecting" -> {
                connectionState = ConnectionState.CONNECTING
                onEvent(type, data)
            }
            "qr_generated" -> {
                onEvent(type, data)
            }
            "pairing_code" -> {
                pairingCode = data.get("code")?.asString
                connectionState = ConnectionState.PAIRING
                onEvent(type, data)
            }
            "pairing_code_entered" -> {
                connectionState = ConnectionState.CONNECTING
                onEvent(type, data)
            }
            "connected" -> {
                connectionState = ConnectionState.CONNECTED
                userPhone = data.get("phone")?.asString
                onEvent(type, data)
            }
            "disconnected" -> {
                connectionState = ConnectionState.DISCONNECTED
                val shouldReconnect = data.get("shouldReconnect")?.asBoolean ?: false
                onEvent(type, data.apply {
                    addProperty("shouldReconnect", shouldReconnect)
                })
            }
            "chats" -> {
                onEvent(type, data)
            }
            "message_sent" -> {
                onEvent(type, data)
            }
            "message_failed" -> {
                onEvent(type, data)
            }
            "error" -> {
                Log.e("WebViewManager", "JS Error: ${data.get("message")?.asString}")
                onEvent(type, data)
            }
            else -> {
                onEvent(type, data)
            }
        }
    }
    
    fun initWhatsApp(phoneNumber: String) {
        connectionState = ConnectionState.CONNECTING
        Log.d("WebViewManager", "Init WhatsApp for: $phoneNumber")
        val js = "if (typeof WhatsApp !== 'undefined') { WhatsApp.init('$phoneNumber'); } else { console.error('WhatsApp not loaded'); }"
        evaluateJs(js)
    }
    
    fun requestPairingCode() {
        Log.d("WebViewManager", "Requesting pairing code")
        evaluateJs("if (typeof WhatsApp !== 'undefined') { WhatsApp.requestPairingCode(); }")
    }
    
    fun enterPairingCode(code: String) {
        Log.d("WebViewManager", "Entering pairing code: $code")
        evaluateJs("if (typeof WhatsApp !== 'undefined') { WhatsApp.enterPairingCode('$code'); }")
    }
    
    fun getChats() {
        Log.d("WebViewManager", "Getting chats")
        evaluateJs("if (typeof WhatsApp !== 'undefined') { WhatsApp.getChats(); }")
    }
    
    fun sendMessage(jid: String, content: String, callback: ((success: Boolean, messageId: String?) -> Unit)? = null) {
        val escapedContent = content.replace("'", "\\'").replace("\n", "\\n")
        val js = "if (typeof WhatsApp !== 'undefined') { WhatsApp.sendMessage('$jid', '$escapedContent'); }"
        
        if (callback != null) {
            scope.launch {
                evaluateJs(js)
                delay(1000)
                callback(true, null)
            }
        } else {
            evaluateJs(js)
        }
    }
    
    fun disconnect() {
        evaluateJs("if (typeof WhatsApp !== 'undefined') { WhatsApp.disconnect(); }")
        connectionState = ConnectionState.DISCONNECTED
    }
    
    fun isConnected(): Boolean {
        return connectionState == ConnectionState.CONNECTED
    }
    
    fun getConnectionState(): ConnectionState {
        return connectionState
    }
    
    fun getPairingCode(): String? {
        return pairingCode
    }
    
    fun getUserPhone(): String? {
        return userPhone
    }
    
    private fun evaluateJs(js: String) {
        handler.post {
            Log.d("WebViewManager", "Executing JS: $js")
            webView?.evaluateJavascript(js, { result ->
                Log.d("WebViewManager", "JS result: $result")
            })
        }
    }
    
    fun destroy() {
        webView?.destroy()
        webView = null
        scope.cancel()
    }
}