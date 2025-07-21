package com.technource.android.utils

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.technource.android.R
import com.technource.android.system_status.SystemStatusActivity

class HeaderComponent @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private var headerTitle: TextView
    private var systemIndicator: View
    private var btnLogs: ImageButton
    private var btnNotifications: ImageButton
    private var notificationClickListener: (() -> Unit)? = null

    enum class SystemStatus {
        NORMAL, WARNING, ERROR
    }

    init {
        LayoutInflater.from(context).inflate(R.layout.component_header, this, true)

        headerTitle = findViewById(R.id.headerTitle)
        systemIndicator = findViewById(R.id.systemIndicator)
        btnLogs = findViewById(R.id.btnLogs)
        btnNotifications = findViewById(R.id.btnNotifications)

        setupClickListeners()
    }

    private fun setupClickListeners() {
        btnLogs.setOnClickListener {
            context.startActivity(Intent(context, SystemStatusActivity::class.java))
        }

        btnNotifications.setOnClickListener {
            notificationClickListener?.invoke()
        }
    }

    fun setTitle(title: String) {
        headerTitle.text = title
        headerTitle.setTextColor(ContextCompat.getColor(context, R.color.header_text))
    }

    fun setSystemStatus(status: SystemStatus) {
        val color = when (status) {
            SystemStatus.NORMAL -> R.color.status_system_normal
            SystemStatus.WARNING -> R.color.status_system_warning
            SystemStatus.ERROR -> R.color.status_system_error
        }
        systemIndicator.backgroundTintList = ContextCompat.getColorStateList(context, color)
    }

    fun setOnNotificationClickListener(listener: () -> Unit) {
        notificationClickListener = listener
    }
}