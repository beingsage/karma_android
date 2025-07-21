package com.technource.android.module.miscModule.miscscreen.Gym.fragments

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.technource.android.R
import com.technource.android.module.miscModule.miscscreen.Gym.models.WorkoutSection

class WorkoutSectionAdapter(
    private val sections: List<WorkoutSection>,
    private val onExerciseClick: (sectionId: Int, exerciseId: Int) -> Unit
) : RecyclerView.Adapter<WorkoutSectionAdapter.SectionViewHolder>() {

    class SectionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val sectionName: TextView = itemView.findViewById(R.id.sectionName)
        // Add more views as needed
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SectionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_workout_section, parent, false)
        return SectionViewHolder(view)
    }

    override fun onBindViewHolder(holder: SectionViewHolder, position: Int) {
        val section = sections[position]
        holder.sectionName.text = section.name
        // Set up click listeners and bind exercises as needed
    }

    override fun getItemCount(): Int = sections.size
}