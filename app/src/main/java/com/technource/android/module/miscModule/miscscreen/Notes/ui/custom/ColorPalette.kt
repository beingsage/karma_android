package com.technource.android.module.miscModule.miscscreen.Notes.ui.custom

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.technource.android.R

class ColorPalette @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
    
    private var onColorSelectedListener: ((Int) -> Unit)? = null
    private var selectedColor: Int = Color.parseColor("#FFFFF9C4")
    
    private val colors = listOf(
        Color.parseColor("#FFFFF9C4"), // Yellow
        Color.parseColor("#FFE8F5E9"), // Green
        Color.parseColor("#FFE3F2FD"), // Blue
        Color.parseColor("#FFF3E5F5"), // Purple
        Color.parseColor("#FFFCE4EC"), // Pink
        Color.parseColor("#FFFFF3E0"), // Orange
        Color.parseColor("#FFFFFFFF"), // White
        Color.parseColor("#FFE0E0E0")  // Gray
    )
    
    init {
        orientation = HORIZONTAL
        setupColorButtons()
    }
    
    private fun setupColorButtons() {
        colors.forEach { color ->
            val button = MaterialButton(context).apply {
                layoutParams = LayoutParams(80, 80).apply {
                    setMargins(8, 8, 8, 8)
                }
                setBackgroundColor(color)
                strokeColor = ContextCompat.getColorStateList(context, R.color.card_stroke)
                strokeWidth = 2
                cornerRadius = 40
                
                setOnClickListener {
                    selectedColor = color
                    onColorSelectedListener?.invoke(color)
                    updateSelection()
                }
            }
            addView(button)
        }
        updateSelection()
    }
    
    private fun updateSelection() {
        for (i in 0 until childCount) {
            val button = getChildAt(i) as MaterialButton
            val color = colors[i]
            button.strokeWidth = if (color == selectedColor) 6 else 2
        }
    }
    
    fun setOnColorSelectedListener(listener: (Int) -> Unit) {
        onColorSelectedListener = listener
    }
    
    fun setSelectedColor(color: Int) {
        selectedColor = color
        updateSelection()
    }
}