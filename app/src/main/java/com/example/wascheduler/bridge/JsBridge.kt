package com.example.wascheduler.bridge

import android.webkit.JavascriptInterface
import com.google.gson.Gson
import com.google.gson.JsonObject

class JsBridge(
    private val onEventCallback: (type: String, data: JsonObject) -> Unit
) {
    private val gson = Gson()
    
    @JavascriptInterface
    fun onEvent(jsonData: String) {
        try {
            val json = gson.fromJson(jsonData, JsonObject::class.java)
            val type = json.get("type")?.asString ?: "unknown"
            val data = json.getAsJsonObject("data") ?: JsonObject()
            onEventCallback(type, data)
        } catch (e: Exception) {
            onEventCallback("error", JsonObject().apply {
                addProperty("message", "Failed to parse event: ${e.message}")
            })
        }
    }
    
    @JavascriptInterface
    fun log(message: String) {
        android.util.Log.d("JsBridge", message)
    }
    
    @JavascriptInterface
    fun getStoredAuth(): String {
        return ""
    }
}