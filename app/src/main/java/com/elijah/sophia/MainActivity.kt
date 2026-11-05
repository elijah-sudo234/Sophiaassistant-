
```
package com.elijah.sophia

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity(), RecognitionListener {

    private lateinit var status: TextView
    private var tts: TextToSpeech? = null
    private var speechService: SpeechService? = null
    private var model: Model? = null
    private var awake = false
    private var speaking = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        status = TextView(this).apply {
            text = "Starting Sophia..."
            textSize = 22f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#1B1B2F"))
            setPadding(40, 40, 40, 40)
        }
        setContentView(status)

        tts = TextToSpeech(this) { code ->
            if (code == TextToSpeech.SUCCESS) {
                tts?.language = Locale.ENGLISH
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 1)
        } else {
            loadModel()
        }
    }

    override fun onRequestPermissionsResult(code: Int, perms: Array<String>, results: IntArray) {
        super.onRequestPermissionsResult(code, perms, results)
        if (results.isNotEmpty() && results[0] == PackageManager.PERMISSION_GRANTED) {
            loadModel()
        } else {
            status.text = "Sophia needs the microphone to hear you."
        }
    }

    private fun loadModel() {
        val folder = File(getExternalFilesDir(null), "model")
        if (!folder.exists()) {
            status.text = "Speech model not found.\nPut the model folder in:\n" + folder.absolutePath
            return
        }
        Thread {
            try {
                model = Model(folder.absolutePath)
                runOnUiThread { startListening() }
            } catch (e: Exception) {
                runOnUiThread { status.text = "Could not load model: " + e.message }
            }
        }.start()
    }

    private fun startListening() {
        val recognizer = Recognizer(model, 16000.0f)
        speechService = SpeechService(recognizer, 16000.0f)
        speechService?.startListening(this)
        status.text = "Say \"Sophia\""
    }

    private fun say(text: String) {
        speaking = true
        status.text = text
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "sophia")
        status.postDelayed({
            speaking = false
            if (!awake) status.text = "Say \"Sophia\""
        }, 600L + text.length * 70L)
    }

    private fun handle(heard: String) {
        val text = heard.lowercase(Locale.ROOT).trim()
        if (text.isEmpty() || speaking) return

        if (!awake) {
            if (text.contains("sophia") || text.contains("sofia")) {
                awake = true
                say("Yes?")
            }
            return
        }

        awake = false
        when {
            text.contains("time") -> {
                val now = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
                say("It is $now")
            }
            text.contains("date") || text.contains("day") -> {
                val today = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date())
                say("Today is $today")
            }
            text.contains("your name") || text.contains("who are you") -> {
                say("I am Sophia, your offline assistant.")
            }
            text.contains("hello") || text.contains("hi") -> {
                say("Hello Elijah!")
            }
            else -> say("Sorry, I do not know that yet.")
        }
    }

    override fun onFinalResult(hypothesis: String?) {
        hypothesis ?: return
        handle(JSONObject(hypothesis).optString("text"))
    }

    override fun onResult(hypothesis: String?) {
        hypothesis ?: return
        handle(JSONObject(hypothesis).optString("text"))
    }

    override fun onPartialResult(hypothesis: String?) {}

    override fun onError(e: Exception?) {
        status.text = "Error: " + e?.message
    }

    override fun onTimeout() {}

    override fun onDestroy() {
        super.onDestroy()
        speechService?.stop()
        speechService?.shutdown()
        tts?.shutdown()
    }
}
```
