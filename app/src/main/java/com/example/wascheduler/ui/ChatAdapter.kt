package com.example.wascheduler.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.wascheduler.R
import com.example.wascheduler.bridge.WhatsAppClient.Chat

class ChatAdapter(
    private val onChatClick: (Chat) -> Unit
) : ListAdapter<Chat, ChatAdapter.ChatViewHolder>(ChatDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat, parent, false)
        return ChatViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val chat = getItem(position)
        holder.bind(chat, onChatClick)
    }
    
    class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val chatIcon: ImageView = itemView.findViewById(R.id.chatIcon)
        private val chatName: TextView = itemView.findViewById(R.id.chatName)
        private val chatType: TextView = itemView.findViewById(R.id.chatType)
        
        fun bind(chat: Chat, onClick: (Chat) -> Unit) {
            chatName.text = chat.name
            
            if (chat.isGroup) {
                chatIcon.setImageResource(R.drawable.ic_group)
                chatType.text = itemView.context.getString(R.string.group_chat)
            } else {
                chatIcon.setImageResource(R.drawable.ic_person)
                chatType.text = itemView.context.getString(R.string.individual_chat)
            }
            
            itemView.setOnClickListener {
                onClick(chat)
            }
        }
    }
    
    class ChatDiffCallback : DiffUtil.ItemCallback<Chat>() {
        override fun areItemsTheSame(oldItem: Chat, newItem: Chat): Boolean {
            return oldItem.jid == newItem.jid
        }
        
        override fun areContentsTheSame(oldItem: Chat, newItem: Chat): Boolean {
            return oldItem == newItem
        }
    }
}