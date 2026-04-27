package com.example.wascheduler.bridge

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.google.gson.JsonObject
import kotlinx.coroutines.*

class WebViewManager(
    private val context: Context,
    private val onEvent: (type: String, data: JsonObject) -> Unit
) {
    private var webView: WebView? = null
    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(Dispatchers.Main)
    
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
            }
            
            webChromeClient = WebChromeClient()
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    Log.d("WebViewManager", "Page finished: $url")
                    injectBridge()
                }
                
                override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                    Log.e("WebViewManager", "Error: ${error?.description}")
                }
            }
            
            addJavascriptInterface(JsBridge { type, data ->
                handleJsEvent(type, data)
            }, "AndroidBridge")
        }
        
        loadWhatsAppClient()
        return webView!!
    }
    
    private fun loadWhatsAppClient() {
        webView?.loadUrl("file:///android_asset/whatsapp/index.html")
    }
    
    private fun injectBridge() {
        val js = """
            (function() {
                window.AndroidBridge = {
                    onEvent: function(data) {
                        console.log('Event: ' + data);
                    },
                    log: function(msg) {
                        console.log(msg);
                    },
                    getStoredAuth: function() {
                        return '';
                    }
                };
                console.log('Bridge injected');
            })();
        """
        webView?.evaluateJavascript(js, null)
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
                onEvent(type, data)
            }
            else -> {
                onEvent(type, data)
            }
        }
    }
    
    fun initWhatsApp(phoneNumber: String) {
        connectionState = ConnectionState.CONNECTING
        val js = "WhatsApp.init('$phoneNumber')"
        evaluateJs(js)
    }
    
    fun requestPairingCode() {
        evaluateJs("WhatsApp.requestPairingCode()")
    }
    
    fun enterPairingCode(code: String) {
        evaluateJs("WhatsApp.enterPairingCode('$code')")
    }
    
    fun getChats() {
        evaluateJs("WhatsApp.getChats()")
    }
    
    fun sendMessage(jid: String, content: String, callback: ((success: Boolean, messageId: String?) -> Unit)? = null) {
        val escapedContent = content.replace("'", "\\'").replace("\n", "\\n")
        val js = "WhatsApp.sendMessage('$jid', '$escapedContent')"
        
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
        evaluateJs("WhatsApp.disconnect()")
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