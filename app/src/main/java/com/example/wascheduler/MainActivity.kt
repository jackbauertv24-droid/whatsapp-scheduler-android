package com.example.wascheduler

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.wascheduler.bridge.WhatsAppClient
import com.example.wascheduler.service.WhatsAppService
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
    
    private lateinit var toolbar: Toolbar
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var whatsappClient: WhatsAppClient
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        toolbar = findViewById(R.id.toolbar)
        bottomNav = findViewById(R.id.bottomNav)
        
        setSupportActionBar(toolbar)
        
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
        val navController = navHostFragment.navController
        
        bottomNav.setupWithNavController(navController)
        
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.logout -> {
                    showLogoutDialog()
                    false
                }
                else -> {
                    navController.navigate(item.itemId)
                    true
                }
            }
        }
        
        whatsappClient = WhatsAppClient.getInstance(this)
        
        WhatsAppService.start(this)
    }
    
    private fun showLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.logout_title)
            .setMessage(R.string.logout_message)
            .setPositiveButton(R.string.logout_confirm) { _, _ ->
                logout()
            }
            .setNegativeButton(R.string.logout_cancel, null)
            .show()
    }
    
    private fun logout() {
        WhatsAppService.stop(this)
        whatsappClient.disconnect()
        
        getSharedPreferences("auth_store", MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
        
        val intent = Intent(this, AuthActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
    
    override fun onDestroy() {
        super.onDestroy()
    }
}