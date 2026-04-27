package com.example.wascheduler

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.wascheduler.bridge.WhatsAppClient
import com.example.wascheduler.service.WhatsAppService
import kotlinx.coroutines.launch

class AuthActivity : AppCompatActivity() {
    
    private lateinit var progressBar: ProgressBar
    private lateinit var statusText: TextView
    private lateinit var errorText: TextView
    
    private lateinit var phoneInputSection: LinearLayout
    private lateinit var phoneInput: EditText
    private lateinit var continueButton: Button
    
    private lateinit var pairingSection: LinearLayout
    private lateinit var pairingCode: TextView
    private lateinit var codeInput: EditText
    private lateinit var verifyButton: Button
    
    private lateinit var connectedSection: LinearLayout
    private lateinit var connectedPhone: TextView
    private lateinit var startAppButton: Button
    
    private lateinit var whatsappClient: WhatsAppClient
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)
        
        initViews()
        setupClient()
        
        val existingAuth = getSharedPreferences("auth_store", MODE_PRIVATE)
            .getString("user_phone", null)
        
        if (existingAuth != null) {
            connectedPhone.text = getString(R.string.auth_phone_label) + " $existingAuth"
            showConnected()
        } else {
            showPhoneInput()
        }
    }
    
    private fun initViews() {
        progressBar = findViewById(R.id.progressBar)
        statusText = findViewById(R.id.statusText)
        errorText = findViewById(R.id.errorText)
        
        phoneInputSection = findViewById(R.id.phoneInputSection)
        phoneInput = findViewById(R.id.phoneInput)
        continueButton = findViewById(R.id.continueButton)
        
        pairingSection = findViewById(R.id.pairingSection)
        pairingCode = findViewById(R.id.pairingCode)
        codeInput = findViewById(R.id.codeInput)
        verifyButton = findViewById(R.id.verifyButton)
        
        connectedSection = findViewById(R.id.connectedSection)
        connectedPhone = findViewById(R.id.connectedPhone)
        startAppButton = findViewById(R.id.startAppButton)
        
        continueButton.setOnClickListener {
            val phone = phoneInput.text.toString().trim()
            if (phone.isNotEmpty() && phone.length >= 10) {
                initWhatsApp(phone)
            }
        }
        
        verifyButton.setOnClickListener {
            val code = codeInput.text.toString().trim()
            if (code.length == 8) {
                enterPairingCode(code)
            }
        }
        
        startAppButton.setOnClickListener {
            startMainActivity()
        }
        
        phoneInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                errorText.visibility = View.GONE
            }
            override fun afterTextChanged(s: Editable?) {}
        })
        
        codeInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                errorText.visibility = View.GONE
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }
    
    private fun setupClient() {
        whatsappClient = WhatsAppClient.getInstance(this)
        
        lifecycleScope.launch {
            whatsappClient.connectionState.collect { state ->
                when (state) {
                    WhatsAppClient.ConnectionState.CONNECTING -> {
                        showLoading(getString(R.string.auth_connecting))
                    }
                    WhatsAppClient.ConnectionState.PAIRING -> {
                        whatsappClient.pairingCode.value?.let { code ->
                            pairingCode.text = code
                            showPairing()
                        }
                    }
                    WhatsAppClient.ConnectionState.CONNECTED -> {
                        whatsappClient.userPhone.value?.let { phone ->
                            connectedPhone.text = getString(R.string.auth_phone_label) + " $phone"
                            saveAuth(phone)
                            showConnected()
                        }
                    }
                    WhatsAppClient.ConnectionState.DISCONNECTED -> {
                        showError(getString(R.string.auth_error_connection))
                    }
                }
            }
        }
        
        lifecycleScope.launch {
            whatsappClient.error.collect { error ->
                if (error != null) {
                    showError(error)
                }
            }
        }
    }
    
    private fun initWhatsApp(phone: String) {
        showLoading(getString(R.string.auth_generating_code))
        whatsappClient.init(phone)
        whatsappClient.requestPairingCode()
    }
    
    private fun enterPairingCode(code: String) {
        showLoading(getString(R.string.auth_connecting))
        whatsappClient.enterPairingCode(code)
    }
    
    private fun saveAuth(phone: String) {
        getSharedPreferences("auth_store", MODE_PRIVATE)
            .edit()
            .putString("user_phone", phone)
            .apply()
    }
    
    private fun startMainActivity() {
        WhatsAppService.start(this)
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
    
    private fun showPhoneInput() {
        progressBar.visibility = View.GONE
        statusText.visibility = View.GONE
        phoneInputSection.visibility = View.VISIBLE
        pairingSection.visibility = View.GONE
        connectedSection.visibility = View.GONE
        errorText.visibility = View.GONE
    }
    
    private fun showPairing() {
        progressBar.visibility = View.GONE
        statusText.visibility = View.GONE
        phoneInputSection.visibility = View.GONE
        pairingSection.visibility = View.VISIBLE
        connectedSection.visibility = View.GONE
        errorText.visibility = View.GONE
    }
    
    private fun showConnected() {
        progressBar.visibility = View.GONE
        statusText.visibility = View.GONE
        phoneInputSection.visibility = View.GONE
        pairingSection.visibility = View.GONE
        connectedSection.visibility = View.VISIBLE
        errorText.visibility = View.GONE
    }
    
    private fun showLoading(message: String) {
        progressBar.visibility = View.VISIBLE
        statusText.text = message
        statusText.visibility = View.VISIBLE
        phoneInputSection.visibility = View.GONE
        pairingSection.visibility = View.GONE
        connectedSection.visibility = View.GONE
        errorText.visibility = View.GONE
    }
    
    private fun showError(message: String) {
        progressBar.visibility = View.GONE
        statusText.visibility = View.GONE
        errorText.text = message
        errorText.visibility = View.VISIBLE
    }
    
    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing && !whatsappClient.isConnected()) {
            whatsappClient.destroy()
        }
    }
}