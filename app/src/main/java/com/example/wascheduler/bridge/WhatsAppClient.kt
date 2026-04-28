package com.example.wascheduler.bridge

import android.content.Context
import android.util.Log
import com.google.gson.JsonObject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class WhatsAppClient private constructor(
    private val context: Context
) {
    private val webViewManager = WebViewManager(context, ::handleEvent)
    private val scope = CoroutineScope(Dispatchers.Main)
    
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState
    
    private val _userPhone = MutableStateFlow<String?>(null)
    val userPhone: StateFlow<String?> = _userPhone
    
    private val _pairingCode = MutableStateFlow<String?>(null)
    val pairingCode: StateFlow<String?> = _pairingCode
    
    private val _chats = MutableStateFlow<List<Chat>>(emptyList())
    val chats: StateFlow<List<Chat>> = _chats
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    
    private var lastMessageCallback: ((Boolean, String?) -> Unit)? = null
    private var webViewInitialized = false
    
    data class Chat(
        val jid: String,
        val name: String,
        val isGroup: Boolean,
        val timestamp: Long? = null
    )
    
    enum class ConnectionState {
        DISCONNECTED,
        CONNECTING,
        PAIRING,
        CONNECTED
    }
    
    companion object {
        @Volatile
        private var instance: WhatsAppClient? = null
        
        fun getInstance(context: Context): WhatsAppClient {
            return instance ?: synchronized(this) {
                instance ?: WhatsAppClient(context).also { instance = it }
            }
        }
    }
    
    private fun handleEvent(type: String, data: JsonObject) {
        Log.d("WhatsAppClient", "Event: $type")
        when (type) {
            "connecting" -> {
                _connectionState.value = ConnectionState.CONNECTING
            }
            "pairing_code" -> {
                _pairingCode.value = data.get("code")?.asString
                _connectionState.value = ConnectionState.PAIRING
                Log.d("WhatsAppClient", "Pairing code received: ${_pairingCode.value}")
            }
            "pairing_code_entered" -> {
                _connectionState.value = ConnectionState.CONNECTING
            }
            "connected" -> {
                _connectionState.value = ConnectionState.CONNECTED
                _userPhone.value = data.get("phone")?.asString
                _pairingCode.value = null
                Log.d("WhatsAppClient", "Connected: ${_userPhone.value}")
            }
            "disconnected" -> {
                val shouldReconnect = data.get("shouldReconnect")?.asBoolean ?: false
                if (!shouldReconnect) {
                    _connectionState.value = ConnectionState.DISCONNECTED
                    _userPhone.value = null
                }
            }
            "chats" -> {
                val chatsArray = data.getAsJsonArray("chats")
                val chatList = chatsArray.mapNotNull { chatJson ->
                    val chatObj = chatJson.asJsonObject
                    Chat(
                        jid = chatObj.get("jid")?.asString ?: "",
                        name = chatObj.get("name")?.asString ?: "",
                        isGroup = chatObj.get("isGroup")?.asBoolean ?: false,
                        timestamp = chatObj.get("timestamp")?.asLong
                    )
                }.filter { it.jid.isNotEmpty() }
                _chats.value = chatList
            }
            "message_sent" -> {
                lastMessageCallback?.invoke(true, data.get("messageId")?.asString)
                lastMessageCallback = null
            }
            "message_failed" -> {
                lastMessageCallback?.invoke(false, null)
                lastMessageCallback = null
            }
            "error" -> {
                _error.value = data.get("message")?.asString ?: "Unknown error"
                Log.e("WhatsAppClient", "Error: ${_error.value}")
            }
        }
    }
    
    fun init(phoneNumber: String) {
        Log.d("WhatsAppClient", "Init called for: $phoneNumber")
        webViewManager.initWebView()
        webViewInitialized = true
        
        scope.launch {
            delay(2000)
            Log.d("WhatsAppClient", "Calling initWhatsApp after delay")
            webViewManager.initWhatsApp(phoneNumber)
            
            delay(1000)
            Log.d("WhatsAppClient", "Calling requestPairingCode after delay")
            webViewManager.requestPairingCode()
        }
    }
    
    fun requestPairingCode() {
        webViewManager.requestPairingCode()
    }
    
    fun enterPairingCode(code: String) {
        webViewManager.enterPairingCode(code)
    }
    
    fun getChats() {
        webViewManager.getChats()
    }
    
    fun sendMessage(jid: String, content: String, callback: ((Boolean, String?) -> Unit)? = null) {
        lastMessageCallback = callback
        webViewManager.sendMessage(jid, content, callback)
    }
    
    fun disconnect() {
        webViewManager.disconnect()
        _connectionState.value = ConnectionState.DISCONNECTED
        _userPhone.value = null
        _pairingCode.value = null
        _chats.value = emptyList()
    }
    
    fun isConnected(): Boolean {
        return _connectionState.value == ConnectionState.CONNECTED
    }
    
    fun destroy() {
        webViewManager.destroy()
        scope.cancel()
        instance = null
    }
}