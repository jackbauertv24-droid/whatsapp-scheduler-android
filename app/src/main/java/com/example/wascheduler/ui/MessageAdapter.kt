package com.example.wascheduler.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.wascheduler.R
import com.example.wascheduler.data.ScheduledMessage
import java.text.SimpleDateFormat
import java.util.*

class MessageAdapter(
    private val showCancelButton: Boolean,
    private val onCancel: (ScheduledMessage) -> Unit
) : ListAdapter<ScheduledMessage, MessageAdapter.MessageViewHolder>(MessageDiffCallback()) {
    
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_message, parent, false)
        return MessageViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = getItem(position)
        holder.bind(message, showCancelButton, onCancel, dateFormat)
    }
    
    class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val chatIcon: ImageView = itemView.findViewById(R.id.chatIcon)
        private val chatName: TextView = itemView.findViewById(R.id.chatName)
        private val messageContent: TextView = itemView.findViewById(R.id.messageContent)
        private val scheduledTime: TextView = itemView.findViewById(R.id.scheduledTime)
        private val statusText: TextView = itemView.findViewById(R.id.statusText)
        private val errorText: TextView = itemView.findViewById(R.id.errorText)
        private val cancelButton: Button = itemView.findViewById(R.id.cancelButton)
        
        fun bind(
            message: ScheduledMessage,
            showCancel: Boolean,
            onCancel: (ScheduledMessage) -> Unit,
            dateFormat: SimpleDateFormat
        ) {
            chatName.text = message.chatName
            
            if (message.isGroup) {
                chatIcon.setImageResource(R.drawable.ic_group)
            } else {
                chatIcon.setImageResource(R.drawable.ic_person)
            }
            
            val displayContent = if (message.content.length > 50) {
                message.content.take(50) + "..."
            } else {
                message.content
            }
            messageContent.text = displayContent
            
            scheduledTime.text = dateFormat.format(Date(message.scheduledFor))
            
            val context = itemView.context
            when (message.status) {
                ScheduledMessage.STATUS_PENDING -> {
                    statusText.text = context.getString(R.string.queue_pending)
                    statusText.setTextColor(context.getColor(R.color.pending))
                }
                ScheduledMessage.STATUS_SENT -> {
                    statusText.text = context.getString(R.string.queue_sent)
                    statusText.setTextColor(context.getColor(R.color.sent))
                }
                ScheduledMessage.STATUS_FAILED -> {
                    statusText.text = context.getString(R.string.queue_failed)
                    statusText.setTextColor(context.getColor(R.color.failed))
                }
            }
            
            if (message.error != null && message.status == ScheduledMessage.STATUS_FAILED) {
                errorText.text = message.error
                errorText.visibility = View.VISIBLE
            } else {
                errorText.visibility = View.GONE
            }
            
            if (showCancel && message.status == ScheduledMessage.STATUS_PENDING) {
                cancelButton.visibility = View.VISIBLE
                cancelButton.setOnClickListener {
                    onCancel(message)
                }
            } else {
                cancelButton.visibility = View.GONE
            }
        }
    }
    
    class MessageDiffCallback : DiffUtil.ItemCallback<ScheduledMessage>() {
        override fun areItemsTheSame(oldItem: ScheduledMessage, newItem: ScheduledMessage): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: ScheduledMessage, newItem: ScheduledMessage): Boolean {
            return oldItem == newItem
        }
    }
}