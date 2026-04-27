package com.example.wascheduler.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.wascheduler.R
import com.example.wascheduler.data.MessageDatabase
import com.example.wascheduler.data.ScheduledMessage
import kotlinx.coroutines.launch
import java.util.*

class ScheduleFragment : Fragment() {
    
    private var chatJid: String = ""
    private var chatName: String = ""
    private var isGroup: Boolean = false
    
    private lateinit var recipientSection: LinearLayout
    private lateinit var recipientIcon: ImageView
    private lateinit var recipientName: TextView
    private lateinit var messageInput: EditText
    private lateinit var timeModeGroup: RadioGroup
    private lateinit var specificTimeRadio: RadioButton
    private lateinit var delayRadio: RadioButton
    private lateinit var specificTimeSection: LinearLayout
    private lateinit var dateButton: Button
    private lateinit var timeButton: Button
    private lateinit var delaySection: LinearLayout
    private lateinit var hoursInput: EditText
    private lateinit var minutesInput: EditText
    private lateinit var scheduleButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var errorText: TextView
    private lateinit var successText: TextView
    
    private var selectedDate: Calendar = Calendar.getInstance()
    private var selectedTime: Calendar = Calendar.getInstance().apply {
        add(Calendar.MINUTE, 30)
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_schedule, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initViews(view)
        setupListeners()
        
        arguments?.let {
            chatJid = it.getString("chatJid", "")
            chatName = it.getString("chatName", "")
            isGroup = it.getBoolean("isGroup", false)
        }
        
        updateRecipientDisplay()
        updateDateTimeButtons()
        
        specificTimeSection.visibility = View.VISIBLE
        delaySection.visibility = View.GONE
    }
    
    private fun initViews(view: View) {
        recipientSection = view.findViewById(R.id.recipientSection)
        recipientIcon = view.findViewById(R.id.recipientIcon)
        recipientName = view.findViewById(R.id.recipientName)
        messageInput = view.findViewById(R.id.messageInput)
        timeModeGroup = view.findViewById(R.id.timeModeGroup)
        specificTimeRadio = view.findViewById(R.id.specificTimeRadio)
        delayRadio = view.findViewById(R.id.delayRadio)
        specificTimeSection = view.findViewById(R.id.specificTimeSection)
        dateButton = view.findViewById(R.id.dateButton)
        timeButton = view.findViewById(R.id.timeButton)
        delaySection = view.findViewById(R.id.delaySection)
        hoursInput = view.findViewById(R.id.hoursInput)
        minutesInput = view.findViewById(R.id.minutesInput)
        scheduleButton = view.findViewById(R.id.scheduleButton)
        progressBar = view.findViewById(R.id.progressBar)
        errorText = view.findViewById(R.id.errorText)
        successText = view.findViewById(R.id.successText)
    }
    
    private fun setupListeners() {
        specificTimeRadio.setOnCheckedChangeListener { _, isChecked ->
            specificTimeSection.visibility = if (isChecked) View.VISIBLE else View.GONE
            delaySection.visibility = if (!isChecked) View.VISIBLE else View.GONE
        }
        
        dateButton.setOnClickListener {
            showDatePicker()
        }
        
        timeButton.setOnClickListener {
            showTimePicker()
        }
        
        scheduleButton.setOnClickListener {
            scheduleMessage()
        }
    }
    
    private fun updateRecipientDisplay() {
        if (chatJid.isNotEmpty()) {
            recipientName.text = chatName
            recipientIcon.setImageResource(
                if (isGroup) R.drawable.ic_group else R.drawable.ic_person
            )
        } else {
            recipientName.text = getString(R.string.main_select_chat)
        }
    }
    
    private fun updateDateTimeButtons() {
        dateButton.text = formatDate(selectedDate)
        timeButton.text = formatTime(selectedTime)
    }
    
    private fun showDatePicker() {
        val year = selectedDate.get(Calendar.YEAR)
        val month = selectedDate.get(Calendar.MONTH)
        val day = selectedDate.get(Calendar.DAY_OF_MONTH)
        
        DatePickerDialog(requireContext(), { _, y, m, d ->
            selectedDate.set(Calendar.YEAR, y)
            selectedDate.set(Calendar.MONTH, m)
            selectedDate.set(Calendar.DAY_OF_MONTH, d)
            updateDateTimeButtons()
        }, year, month, day).apply {
            datePicker.minDate = System.currentTimeMillis()
            show()
        }
    }
    
    private fun showTimePicker() {
        val hour = selectedTime.get(Calendar.HOUR_OF_DAY)
        val minute = selectedTime.get(Calendar.MINUTE)
        
        TimePickerDialog(requireContext(), { _, h, m ->
            selectedTime.set(Calendar.HOUR_OF_DAY, h)
            selectedTime.set(Calendar.MINUTE, m)
            updateDateTimeButtons()
        }, hour, minute, true).show()
    }
    
    private fun getScheduledTime(): Long {
        return if (specificTimeRadio.isChecked) {
            val calendar = Calendar.getInstance().apply {
                set(Calendar.YEAR, selectedDate.get(Calendar.YEAR))
                set(Calendar.MONTH, selectedDate.get(Calendar.MONTH))
                set(Calendar.DAY_OF_MONTH, selectedDate.get(Calendar.DAY_OF_MONTH))
                set(Calendar.HOUR_OF_DAY, selectedTime.get(Calendar.HOUR_OF_DAY))
                set(Calendar.MINUTE, selectedTime.get(Calendar.MINUTE))
                set(Calendar.SECOND, 0)
            }
            calendar.timeInMillis
        } else {
            val hours = hoursInput.text.toString().toIntOrNull() ?: 0
            val minutes = minutesInput.text.toString().toIntOrNull() ?: 30
            System.currentTimeMillis() + (hours * 3600 + minutes * 60) * 1000
        }
    }
    
    private fun scheduleMessage() {
        val content = messageInput.text.toString().trim()
        
        if (chatJid.isEmpty()) {
            showError(getString(R.string.main_error_no_chat))
            return
        }
        
        if (content.isEmpty()) {
            showError(getString(R.string.main_error_empty_message))
            return
        }
        
        val scheduledTime = getScheduledTime()
        if (scheduledTime <= System.currentTimeMillis()) {
            showError(getString(R.string.main_error_invalid_time))
            return
        }
        
        showLoading()
        
        lifecycleScope.launch {
            val db = MessageDatabase.getInstance(requireContext())
            val message = ScheduledMessage(
                chatJid = chatJid,
                chatName = chatName,
                isGroup = isGroup,
                content = content,
                scheduledFor = scheduledTime
            )
            db.messageDao().insert(message)
            showSuccess()
            messageInput.text.clear()
        }
    }
    
    private fun showLoading() {
        progressBar.visibility = View.VISIBLE
        scheduleButton.isEnabled = false
        scheduleButton.text = getString(R.string.main_scheduling)
        errorText.visibility = View.GONE
        successText.visibility = View.GONE
    }
    
    private fun showSuccess() {
        progressBar.visibility = View.GONE
        scheduleButton.isEnabled = true
        scheduleButton.text = getString(R.string.main_schedule_button)
        successText.visibility = View.VISIBLE
        errorText.visibility = View.GONE
        
        Handler().postDelayed({
            successText.visibility = View.GONE
        }, 2000)
    }
    
    private fun showError(message: String) {
        progressBar.visibility = View.GONE
        scheduleButton.isEnabled = true
        scheduleButton.text = getString(R.string.main_schedule_button)
        errorText.text = message
        errorText.visibility = View.VISIBLE
        successText.visibility = View.GONE
    }
    
    private fun formatDate(calendar: Calendar): String {
        return "${calendar.get(Calendar.YEAR)}-${calendar.get(Calendar.MONTH) + 1}-${calendar.get(Calendar.DAY_OF_MONTH)}"
    }
    
    private fun formatTime(calendar: Calendar): String {
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        return "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
    }
}