package old.miscscreen.Meditation

import android.media.MediaPlayer
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.technource.android.R
import java.text.SimpleDateFormat
import java.util.*

class MeditateActivity : AppCompatActivity() {

    private var timeRemaining = 600 // 10 minutes in seconds
    private var isActive = false
    private var interruptions = 0
    private var isCompleted = false
    private lateinit var timer: CountDownTimer
    private lateinit var mediaPlayer: MediaPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_meditate)

        // Header setup
        val dateFormat = SimpleDateFormat("EEEE h:mm a", Locale.getDefault())
        val currentDate = dateFormat.format(Date()).split(" ")
        findViewById<TextView>(R.id.tv_day).text = currentDate[0]
        findViewById<TextView>(R.id.tv_time).text = currentDate[1] + " " + currentDate[2]
        findViewById<TextView>(R.id.tv_session).text = "#4"
        findViewById<TextView>(R.id.tv_duration).text = "10 min"

        // Song selection
        val songs = arrayOf("Calm Waters (5:30)", "Forest Ambience (8:45)", "Gentle Rain (6:20)", "Tibetan Bowls (12:10)", "Ocean Waves (9:15)")
        val spinner = findViewById<Spinner>(R.id.spinner_songs)
        ArrayAdapter(this, android.R.layout.simple_spinner_item, songs).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner.adapter = adapter
        }

        // Timer setup
        val tvTimer = findViewById<TextView>(R.id.tv_timer)
        val progressCircle = findViewById<ProgressBar>(R.id.progress_circle)
        val btnToggle = findViewById<Button>(R.id.btn_toggle_timer)

        timer = object : CountDownTimer(timeRemaining * 1000L, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeRemaining = (millisUntilFinished / 1000).toInt()
                tvTimer.text = formatTime(timeRemaining)
                progressCircle.progress = 600 - timeRemaining
            }

            override fun onFinish() {
                isActive = false
                isCompleted = true
                btnToggle.visibility = View.GONE
                findViewById<LinearLayout>(R.id.interruption_layout).visibility = View.GONE
                findViewById<LinearLayout>(R.id.efficacy_layout).visibility = View.VISIBLE
                mediaPlayer.stop()
            }
        }

        btnToggle.setOnClickListener {
            toggleTimer()
        }

        // Interruption logging
        findViewById<Button>(R.id.btn_log_interruption).setOnClickListener {
            interruptions++
            findViewById<TextView>(R.id.tv_interruptions).text = interruptions.toString()
        }

        // Efficacy rating
        val efficacyButtons = listOf(
            R.id.btn_rate_1, R.id.btn_rate_2, R.id.btn_rate_3, R.id.btn_rate_4, R.id.btn_rate_5
        )
        efficacyButtons.forEachIndexed { index, id ->
            findViewById<Button>(id).setOnClickListener {
                handleEfficacyRating(index + 1)
            }
        }

        // Media Player setup
        mediaPlayer = MediaPlayer.create(this, R.raw.good) // Replace with actual audio resource
        mediaPlayer.isLooping = true
    }

    private fun toggleTimer() {
        isActive = !isActive
        val btnToggle = findViewById<Button>(R.id.btn_toggle_timer)
        if (isActive) {
            timer.start()
            btnToggle.text = "Pause"
            mediaPlayer.start()
        } else {
            timer.cancel()
            btnToggle.text = "Resume"
            mediaPlayer.pause()
        }
    }

    private fun formatTime(seconds: Int): String {
        val mins = seconds / 60
        val secs = seconds % 60
        return String.format("%02d:%02d", mins, secs)
    }

    private fun handleEfficacyRating(rating: Int) {
        Toast.makeText(this, "Rated: $rating", Toast.LENGTH_SHORT).show()
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        timer.cancel()
        mediaPlayer.release()
    }
}