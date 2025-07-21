package com.technource.android.module.miscModule.miscscreen.Notes.utils

import com.technource.android.R


object JournalTemplates {

    data class JournalTemplate(
        val id: String,
        val name: String,
        val title: String,
        val content: String,
        val description: String,
        val iconRes: Int
    )

    fun getAllTemplates(): List<JournalTemplate> {
        return listOf(
            JournalTemplate(
                id = "daily",
                name = "Daily Journal",
                title = "Daily Reflection - ${java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(java.util.Date())}",
                content = """
                    **Today's Date:** ${java.text.SimpleDateFormat("MMMM dd, yyyy", java.util.Locale.getDefault()).format(java.util.Date())}
                    
                    **How I'm feeling today:**
                    
                    **Three things I'm grateful for:**
                    1. 
                    2. 
                    3. 
                    
                    **Today's highlights:**
                    
                    **Challenges I faced:**
                    
                    **What I learned:**
                    
                    **Tomorrow's goals:**
                    
                    **Additional thoughts:**
                """.trimIndent(),
                description = "Track your daily activities, mood, and reflections",
                iconRes = R.drawable.ic_daily_journal
            ),

            JournalTemplate(
                id = "gratitude",
                name = "Gratitude Journal",
                title = "Gratitude Practice",
                content = """
                    **Date:** ${java.text.SimpleDateFormat("MMMM dd, yyyy", java.util.Locale.getDefault()).format(java.util.Date())}
                    
                    **I am grateful for:**
                    
                    **People who made my day better:**
                    
                    **Small moments that brought me joy:**
                    
                    **Accomplishments I'm proud of:**
                    
                    **Lessons I learned:**
                    
                    **Looking forward to:**
                """.trimIndent(),
                description = "Focus on gratitude and positive experiences",
                iconRes = R.drawable.ic_gratitude
            ),

            JournalTemplate(
                id = "goal_tracking",
                name = "Goal Tracker",
                title = "Goal Progress",
                content = """
                    **Date:** ${java.text.SimpleDateFormat("MMMM dd, yyyy", java.util.Locale.getDefault()).format(java.util.Date())}
                    
                    **Current Goals:**
                    
                    **Progress made today:**
                    
                    **Obstacles encountered:**
                    
                    **Solutions and next steps:**
                    
                    **Motivation and inspiration:**
                    
                    **Weekly/Monthly review:**
                """.trimIndent(),
                description = "Track progress towards your goals",
                iconRes = R.drawable.ic_goal
            ),

            JournalTemplate(
                id = "mood_tracker",
                name = "Mood Tracker",
                title = "Mood & Emotions",
                content = """
                    **Date:** ${java.text.SimpleDateFormat("MMMM dd, yyyy", java.util.Locale.getDefault()).format(java.util.Date())}
                    
                    **Overall mood:** (1-10)
                    
                    **Emotions I felt today:**
                    
                    **What triggered these emotions:**
                    
                    **How I handled difficult emotions:**
                    
                    **What made me happy:**
                    
                    **Energy level:** (1-10)
                    
                    **Sleep quality:** (1-10)
                    
                    **Notes:**
                """.trimIndent(),
                description = "Track your emotional well-being",
                iconRes = R.drawable.ic_mood
            ),

            JournalTemplate(
                id = "creative",
                name = "Creative Journal",
                title = "Creative Thoughts",
                content = """
                    **Date:** ${java.text.SimpleDateFormat("MMMM dd, yyyy", java.util.Locale.getDefault()).format(java.util.Date())}
                    
                    **Creative ideas:**
                    
                    **Inspiration sources:**
                    
                    **Projects I'm working on:**
                    
                    **Artistic experiments:**
                    
                    **Creative challenges:**
                    
                    **Future creative goals:**
                """.trimIndent(),
                description = "Capture your creative thoughts and ideas",
                iconRes = R.drawable.ic_creative
            )
        )
    }

    fun getTemplateById(id: String): JournalTemplate? {
        return getAllTemplates().find { it.id == id }
    }
}