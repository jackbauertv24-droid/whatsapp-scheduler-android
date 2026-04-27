package com.example.wascheduler.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.wascheduler.bridge.WhatsAppClient
import com.example.wascheduler.R
import kotlinx.coroutines.launch

class ChatListFragment : Fragment() {
    
    private lateinit var chatList: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var loadingText: TextView
    private lateinit var emptyText: TextView
    private lateinit var chatAdapter: ChatAdapter
    
    private lateinit var whatsappClient: WhatsAppClient
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_chat_list, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        chatList = view.findViewById(R.id.chatList)
        progressBar = view.findViewById(R.id.progressBar)
        loadingText = view.findViewById(R.id.loadingText)
        emptyText = view.findViewById(R.id.emptyText)
        
        chatList.layoutManager = LinearLayoutManager(requireContext())
        chatAdapter = ChatAdapter { chat ->
            val action = ChatListFragmentDirections.actionChatListFragmentToScheduleFragment(
                chatJid = chat.jid,
                chatName = chat.name,
                isGroup = chat.isGroup
            )
            findNavController().navigate(action)
        }
        chatList.adapter = chatAdapter
        
        whatsappClient = WhatsAppClient.getInstance(requireContext())
        
        showLoading()
        loadChats()
        
        lifecycleScope.launch {
            whatsappClient.chats.collect { chats ->
                if (chats.isNotEmpty()) {
                    chatAdapter.submitList(chats)
                    showChats()
                } else if (!whatsappClient.isConnected()) {
                    showLoading()
                } else {
                    showEmpty()
                }
            }
        }
        
        lifecycleScope.launch {
            whatsappClient.connectionState.collect { state ->
                if (state == WhatsAppClient.ConnectionState.CONNECTED) {
                    whatsappClient.getChats()
                }
            }
        }
    }
    
    private fun loadChats() {
        lifecycleScope.launch {
            whatsappClient.getChats()
        }
    }
    
    private fun showLoading() {
        progressBar.visibility = View.VISIBLE
        loadingText.visibility = View.VISIBLE
        emptyText.visibility = View.GONE
        chatList.visibility = View.GONE
    }
    
    private fun showChats() {
        progressBar.visibility = View.GONE
        loadingText.visibility = View.GONE
        emptyText.visibility = View.GONE
        chatList.visibility = View.VISIBLE
    }
    
    private fun showEmpty() {
        progressBar.visibility = View.GONE
        loadingText.visibility = View.GONE
        emptyText.visibility = View.VISIBLE
        chatList.visibility = View.GONE
    }
}