package com.example.wascheduler.ui

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.wascheduler.R
import com.example.wascheduler.data.MessageDatabase
import com.example.wascheduler.data.ScheduledMessage
import kotlinx.coroutines.launch

class QueueFragment : Fragment() {
    
    private lateinit var pendingList: RecyclerView
    private lateinit var sentList: RecyclerView
    private lateinit var failedList: RecyclerView
    private lateinit var pendingHeader: TextView
    private lateinit var sentHeader: TextView
    private lateinit var failedHeader: TextView
    private lateinit var emptyText: TextView
    
    private lateinit var pendingAdapter: MessageAdapter
    private lateinit var sentAdapter: MessageAdapter
    private lateinit var failedAdapter: MessageAdapter
    
    private lateinit var messageDatabase: MessageDatabase
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_queue, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initViews(view)
        setupAdapters()
        
        messageDatabase = MessageDatabase.getInstance(requireContext())
        
        loadMessages()
    }
    
    private fun initViews(view: View) {
        pendingList = view.findViewById(R.id.pendingList)
        sentList = view.findViewById(R.id.sentList)
        failedList = view.findViewById(R.id.failedList)
        pendingHeader = view.findViewById(R.id.pendingHeader)
        sentHeader = view.findViewById(R.id.sentHeader)
        failedHeader = view.findViewById(R.id.failedHeader)
        emptyText = view.findViewById(R.id.emptyText)
        
        pendingList.layoutManager = LinearLayoutManager(requireContext())
        sentList.layoutManager = LinearLayoutManager(requireContext())
        failedList.layoutManager = LinearLayoutManager(requireContext())
    }
    
    private fun setupAdapters() {
        pendingAdapter = MessageAdapter(true) { message ->
            showCancelDialog(message)
        }
        sentAdapter = MessageAdapter(false) { }
        failedAdapter = MessageAdapter(false) { }
        
        pendingList.adapter = pendingAdapter
        sentList.adapter = sentAdapter
        failedList.adapter = failedAdapter
    }
    
    private fun loadMessages() {
        lifecycleScope.launch {
            messageDatabase.messageDao().getAll().collect { messages ->
                val pending = messages.filter { it.status == ScheduledMessage.STATUS_PENDING }
                val sent = messages.filter { it.status == ScheduledMessage.STATUS_SENT }
                val failed = messages.filter { it.status == ScheduledMessage.STATUS_FAILED }
                
                if (messages.isEmpty()) {
                    emptyText.visibility = View.VISIBLE
                    pendingHeader.visibility = View.GONE
                    sentHeader.visibility = View.GONE
                    failedHeader.visibility = View.GONE
                    pendingList.visibility = View.GONE
                    sentList.visibility = View.GONE
                    failedList.visibility = View.GONE
                } else {
                    emptyText.visibility = View.GONE
                    
                    if (pending.isNotEmpty()) {
                        pendingAdapter.submitList(pending)
                        pendingHeader.visibility = View.VISIBLE
                        pendingList.visibility = View.VISIBLE
                    } else {
                        pendingHeader.visibility = View.GONE
                        pendingList.visibility = View.GONE
                    }
                    
                    if (sent.isNotEmpty()) {
                        sentAdapter.submitList(sent)
                        sentHeader.visibility = View.VISIBLE
                        sentList.visibility = View.VISIBLE
                    } else {
                        sentHeader.visibility = View.GONE
                        sentList.visibility = View.GONE
                    }
                    
                    if (failed.isNotEmpty()) {
                        failedAdapter.submitList(failed)
                        failedHeader.visibility = View.VISIBLE
                        failedList.visibility = View.VISIBLE
                    } else {
                        failedHeader.visibility = View.GONE
                        failedList.visibility = View.GONE
                    }
                }
            }
        }
    }
    
    private fun showCancelDialog(message: ScheduledMessage) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.queue_cancel_confirm)
            .setPositiveButton(R.string.queue_yes) { _, _ ->
                cancelMessage(message)
            }
            .setNegativeButton(R.string.queue_no, null)
            .show()
    }
    
    private fun cancelMessage(message: ScheduledMessage) {
        lifecycleScope.launch {
            messageDatabase.messageDao().delete(message)
        }
    }
}