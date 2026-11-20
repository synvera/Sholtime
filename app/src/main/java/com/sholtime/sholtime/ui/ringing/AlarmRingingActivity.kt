package com.sholtime.sholtime.ui.ringing

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.sholtime.sholtime.data.local.SholtimePreferences
import com.sholtime.sholtime.databinding.ActivityAlarmRingingBinding
import com.sholtime.sholtime.ui.alarm.AlarmScheduler
import com.sholtime.sholtime.ui.alarm.AlarmService
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AlarmRingingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAlarmRingingBinding
    private lateinit var prefs: SholtimePreferences
    private lateinit var scheduler: AlarmScheduler

    private var namaSholat: String = "Subuh"
    private var snoozeCount: Int = 0
    private var floatHintAnimator: ObjectAnimator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        setupLockScreenFlags()
        super.onCreate(savedInstanceState)

        // Kunci tombol volume fisik HP agar mengatur volume ALARM (bukan media/dering)
        volumeControlStream = AudioManager.STREAM_ALARM

        binding = ActivityAlarmRingingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(top = bars.top, bottom = bars.bottom)
            insets
        }

        prefs = SholtimePreferences(this)
        scheduler = AlarmScheduler(this)

        namaSholat = intent.getStringExtra("EXTRA_NAMA_SHOLAT") ?: "Subuh"
        snoozeCount = intent.getIntExtra("EXTRA_SNOOZE_COUNT", 0)

        tampilkanInfoAlarm()
        setupListeners()
        setupFreeBottomSwipeUp()
        mulaiAnimasiHintMengambang()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                val now = System.currentTimeMillis()
                if (now - lastVolumeDownTime <= 700L) {
                    lastVolumeDownTime = 0L
                    matikanAlarmDanJadwalkanBesok()
                    return true
                }
                lastVolumeDownTime = now

                audioManager.adjustStreamVolume(
                    AudioManager.STREAM_ALARM,
                    AudioManager.ADJUST_LOWER,
                    AudioManager.FLAG_SHOW_UI
                )
                true
            }
            KeyEvent.KEYCODE_VOLUME_UP -> {
                audioManager.adjustStreamVolume(
                    AudioManager.STREAM_ALARM,
                    AudioManager.ADJUST_RAISE,
                    AudioManager.FLAG_SHOW_UI
                )
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    private fun setupLockScreenFlags() {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            // JANGAN panggil requestDismissKeyguard() agar layar alarm langsung tampil di atas lockscreen
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
    }

    private fun tampilkanInfoAlarm() {
        val namaRapi = namaSholat.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        }
        binding.tvNamaSholatRinging.text = "Sholat $namaRapi"
        binding.tvJamSekarangRinging.text = SimpleDateFormat("HH:mm", Locale.US).format(Date())

        val maxSnooze = prefs.getSnoozeMaxCount()
        val jedaMenit = prefs.getSnoozeIntervalMinutes()

        if (snoozeCount >= maxSnooze) {
            binding.btnSnoozeAlarm.visibility = View.GONE
            binding.tvInfoSnooze.text = "Pengingat ulang telah selesai ($maxSnooze kali)"
        } else {
            binding.btnSnoozeAlarm.visibility = View.VISIBLE
            binding.btnSnoozeAlarm.text = "Ingatkan $jedaMenit menit lagi"
            binding.tvInfoSnooze.text = "Pengingat ke-${snoozeCount + 1} dari $maxSnooze kali"
        }
    }

    private fun mulaiAnimasiHintMengambang() {
        val jarakFloatPx = -10f * resources.displayMetrics.density
        floatHintAnimator = ObjectAnimator.ofFloat(
            binding.swipeHintContainer,
            View.TRANSLATION_Y,
            0f,
            jarakFloatPx
        ).apply {
            duration = 1000L
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }

    private fun setupListeners() {
        binding.btnSnoozeAlarm.setOnClickListener {
            hentikanServiceAlarm()
            val nextSnooze = snoozeCount + 1
            scheduler.jadwalkanSnooze(namaSholat, nextSnooze)
            val jeda = prefs.getSnoozeIntervalMinutes()
            Toast.makeText(this, "Akan diingatkan $jeda menit lagi", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupFreeBottomSwipeUp() {
        var initialRawY = 0f
        val thresholdPx = 120f * resources.displayMetrics.density

        binding.bottomSwipeZone.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    floatHintAnimator?.cancel()
                    initialRawY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaY = event.rawY - initialRawY
                    val upwardMove = deltaY.coerceAtMost(0f)

                    binding.mainRingingContent.translationY = upwardMove * 0.65f
                    binding.swipeHintContainer.translationY = upwardMove * 0.85f

                    val progress = (-upwardMove / thresholdPx).coerceIn(0f, 1f)
                    binding.mainRingingContent.alpha = 1f - (progress * 0.55f)
                    binding.swipeHintContainer.alpha = 1f - (progress * 0.75f)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    val deltaY = event.rawY - initialRawY
                    if (-deltaY >= thresholdPx) {
                        binding.mainRingingContent.animate()
                            .translationY(-400f)
                            .alpha(0f)
                            .setDuration(180L)
                            .withEndAction {
                                matikanAlarmDanJadwalkanBesok()
                            }
                            .start()
                    } else {
                        binding.mainRingingContent.animate()
                            .translationY(0f)
                            .alpha(1f)
                            .setDuration(250L)
                            .start()
                        binding.swipeHintContainer.animate()
                            .translationY(0f)
                            .alpha(1f)
                            .setDuration(250L)
                            .withEndAction {
                                mulaiAnimasiHintMengambang()
                            }
                            .start()
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun matikanAlarmDanJadwalkanBesok() {
        hentikanServiceAlarm()
        scheduler.jadwalkanAlarmSholat(namaSholat)
        Toast.makeText(this, "Alarm $namaSholat dimatikan & dijadwalkan untuk besok", Toast.LENGTH_SHORT).show()
        finish()
    }

    private var lastVolumeDownTime: Long = 0L

    private val closeUiReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            finish()
        }
    }

    private fun hentikanServiceAlarm() {
        val stopIntent = Intent(this, AlarmService::class.java).apply {
            action = "ACTION_STOP_ALARM"
        }
        startService(stopIntent)
    }

    override fun onDestroy() {
        androidx.core.content.ContextCompat.registerReceiver(
            this,
            closeUiReceiver,
            android.content.IntentFilter("ACTION_CLOSE_RINGING_UI"),
            androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED
        )
        floatHintAnimator?.cancel()
        super.onDestroy()
    }
}