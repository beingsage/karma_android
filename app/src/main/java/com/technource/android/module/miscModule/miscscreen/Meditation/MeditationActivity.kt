//package old.miscscreen.Meditation
//
//import android.annotation.SuppressLint
//import android.content.Intent
//import android.os.Bundle
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.Button
//import android.widget.TextView
//import androidx.recyclerview.widget.LinearLayoutManager
//import androidx.recyclerview.widget.RecyclerView
//import com.technource.android.R
//import java.text.SimpleDateFormat
//import java.util.*
//
//class MeditationActivity : BaseActivity() {
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_base) // Set the base layout
//
//        // Inflate activity_meditation into content_container and get the root view
//        val contentView = layoutInflater.inflate(R.layout.activity_meditation, findViewById(R.id.content_container), true)
//        setSelectedItem(R.id.nav_misc) // Keep Misc selected
//
//        // Initialize RecyclerView from the inflated layout
//        val recyclerView = contentView.findViewById<RecyclerView>(R.id.rv_sessions)
//        recyclerView.layoutManager = LinearLayoutManager(this)
//        recyclerView.adapter = SessionAdapter(getScheduledSessions())
//
//        // Start Session Button
//        contentView.findViewById<Button>(R.id.btn_start_session).setOnClickListener {
//            if (isNextSessionTime()) {
//                startActivity(Intent(this, MeditationActivity::class.java)) // Note: This creates a loop; consider a different activity
//            }
//        }
//
//        // Update time until next session
//        updateTimeUntilNext(contentView)
//    }
//
//    private fun getScheduledSessions(): List<Session> {
//        return listOf(
//            Session("Morning Mindfulness", "06:00 AM", "15 min", true),
//            Session("Lunch Break Breathing", "12:30 PM", "10 min", true),
//            Session("Afternoon Reset", "03:00 PM", "5 min", true),
//            Session("Evening Relaxation", "06:30 PM", "15 min", false),
//            Session("Sleep Preparation", "09:00 PM", "20 min", false)
//        )
//    }
//
//    private fun isNextSessionTime(): Boolean {
//        val nextSession = getScheduledSessions().find { !it.completed } ?: return false
//        return try {
//            val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
//            val sessionTime = timeFormat.parse(nextSession.time)
//            val currentTime = Calendar.getInstance().time
//            currentTime.after(sessionTime)
//        } catch (e: Exception) {
//            false
//        }
//    }
//
//    @SuppressLint("SetTextI18n")
//    private fun updateTimeUntilNext(contentView: View) {
//        val tvTimeUntil = contentView.findViewById<TextView>(R.id.tv_time_until_next)
//        val nextSession = getScheduledSessions().find { !it.completed } ?: return
//        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
//        val sessionTime = timeFormat.parse(nextSession.time)
//        val currentTime = Calendar.getInstance().time
//        val diff = sessionTime.time - currentTime.time
//        if (diff > 0) {
//            val hours = diff / (1000 * 60 * 60)
//            val minutes = (diff / (1000 * 60)) % 60
//            tvTimeUntil.text = "Next in: ${hours}h ${minutes}m"
//        }
//    }
//}
//
//data class Session(val name: String, val time: String, val duration: String, val completed: Boolean)
//
//class SessionAdapter(private val sessions: List<Session>) : RecyclerView.Adapter<SessionAdapter.ViewHolder>() {
//    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
//        val tvName: TextView = itemView.findViewById(android.R.id.text1)
//        val tvDetails: TextView = itemView.findViewById(android.R.id.text2)
//    }
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
//        val view = LayoutInflater.from(parent.context)
//            .inflate(android.R.layout.simple_list_item_2, parent, false)
//        return ViewHolder(view)
//    }
//
//    @SuppressLint("SetTextI18n")
//    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
//        val session = sessions[position]
//        holder.tvName.text = session.name
//        holder.tvDetails.text = "${session.time} • ${session.duration}"
//    }
//
//    override fun getItemCount() = sessions.size
//}