package com.sholtime.sholtime.ui.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.VolumeProvider
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.sholtime.sholtime.R
import com.sholtime.sholtime.data.local.SholtimePreferences
import com.sholtime.sholtime.ui.ringing.AlarmRingingActivity

class AlarmService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var previousAlarmVolume: Int = -1
    private var mediaSession: MediaSession? = null

    private val mainHandler = Handler(Looper.getMainLooper())
    private var pendingSoundRunnable: Runnable? = null

    private var currentNamaSholat: String = "Subuh"
    private var lastVolumeDownTime: Long = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "ACTION_STOP_ALARM") {
            matikanAlarmSepenuhnya(jadwalkanBesok = false)
            return START_NOT_STICKY
        }

        if (intent?.action == "ACTION_STOP_AND_RESCHEDULE") {
            matikanAlarmSepenuhnya(jadwalkanBesok = true)
            return START_NOT_STICKY
        }

        currentNamaSholat = intent?.getStringExtra("EXTRA_NAMA_SHOLAT") ?: "Subuh"
        val snoozeCount = intent?.getIntExtra("EXTRA_SNOOZE_COUNT", 0) ?: 0

        // 1. Aktifkan penangkap tombol volume fisik di background/lockscreen
        setupVolumeShortcutSession()

        // 2. Tampilkan notifikasi layar penuh terlebih dahulu
        createNotificationChannel()
        val notification = buildFullScreenNotification(currentNamaSholat, snoozeCount)
        startForeground(777, notification)

        // 3. Paksa buka UI layar penuh (jalur cadangan untuk semua ROM HP)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || Settings.canDrawOverlays(this)) {
            val directIntent = Intent(this, AlarmRingingActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
                )
                putExtra("EXTRA_NAMA_SHOLAT", currentNamaSholat)
                putExtra("EXTRA_SNOOZE_COUNT", snoozeCount)
            }
            try {
                startActivity(directIntent)
            } catch (_: Exception) {
            }
        }

        // 4. Mulai suara adzan setelah jeda 400ms agar UI tampil lebih dulu
        pendingSoundRunnable?.let { mainHandler.removeCallbacks(it) }
        val soundRunnable = Runnable {
            startAlarmSoundAndVibration(currentNamaSholat)
        }
        pendingSoundRunnable = soundRunnable
        mainHandler.postDelayed(soundRunnable, 400L)

        return START_NOT_STICKY
    }

    private fun setupVolumeShortcutSession() {
        releaseMediaSession()
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        val curVol = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)

        val volumeProvider = object : VolumeProvider(VOLUME_CONTROL_RELATIVE, maxVol, curVol) {
            override fun onAdjustVolume(direction: Int) {
                if (direction == AudioManager.ADJUST_LOWER) {
                    val now = System.currentTimeMillis()
                    // Deteksi klik tombol Volume Turun 2x dalam 700ms
                    if (now - lastVolumeDownTime <= 700L) {
                        lastVolumeDownTime = 0L
                        mainHandler.post {
                            Toast.makeText(
                                applicationContext,
                                "Alarm $currentNamaSholat dimatikan (Volume Turun 2x)",
                                Toast.LENGTH_SHORT
                            ).show()
                            matikanAlarmSepenuhnya(jadwalkanBesok = true)
                        }
                        return
                    }
                    lastVolumeDownTime = now

                    // Tetap kecilkan volume suara alarm seperti biasa
                    audioManager.adjustStreamVolume(
                        AudioManager.STREAM_ALARM,
                        AudioManager.ADJUST_LOWER,
                        AudioManager.FLAG_SHOW_UI
                    )
                    currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
                } else if (direction == AudioManager.ADJUST_RAISE) {
                    audioManager.adjustStreamVolume(
                        AudioManager.STREAM_ALARM,
                        AudioManager.ADJUST_RAISE,
                        AudioManager.FLAG_SHOW_UI
                    )
                    currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
                }
            }
        }

        mediaSession = MediaSession(this, "SholtimeAlarmSession").apply {
            setPlaybackState(
                PlaybackState.Builder()
                    .setState(PlaybackState.STATE_PLAYING, 0L, 1.0f)
                    .build()
            )
            setPlaybackToRemote(volumeProvider)
            isActive = true
        }
    }

    private fun releaseMediaSession() {
        try {
            mediaSession?.isActive = false
            mediaSession?.release()
        } catch (_: Exception) {
        }
        mediaSession = null
    }

    private fun matikanAlarmSepenuhnya(jadwalkanBesok: Boolean) {
        pendingSoundRunnable?.let { mainHandler.removeCallbacks(it) }
        stopAlarmSoundAndVibration()
        releaseMediaSession()

        if (jadwalkanBesok) {
            AlarmScheduler(this).jadwalkanAlarmSholat(currentNamaSholat)
        }

        // Kirim sinyal agar AlarmRingingActivity ikut tertutup jika sedang terbuka
        sendBroadcast(Intent("ACTION_CLOSE_RINGING_UI").setPackage(packageName))
        stopSelf()
    }

    private fun startAlarmSoundAndVibration(namaSholat: String) {
        stopAlarmSoundAndVibration()

        val prefs = SholtimePreferences(this)
        val toneKey = prefs.getAlarmToneKey(namaSholat)
        val resId = prefs.getToneResId(toneKey)

        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        previousAlarmVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        val targetVol = ((prefs.getAlarmVolumePercent() / 100.0) * maxVol).toInt().coerceIn(1, maxVol)
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, targetVol, 0)

        try {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                val afd = resources.openRawResourceFd(resId)
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
                isLooping = true
                prepare()
                start()
            }
        } catch (_: Exception) {
        }

        if (prefs.isAlarmVibrate()) {
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                manager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            val pattern = longArrayOf(0, 800, 500, 800, 500)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }
        }
    }

    private fun stopAlarmSoundAndVibration() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {
        }
        mediaPlayer = null

        try {
            vibrator?.cancel()
        } catch (_: Exception) {
        }
        vibrator = null

        if (previousAlarmVolume >= 0) {
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, previousAlarmVolume, 0)
            previousAlarmVolume = -1
        }
    }

    private fun buildFullScreenNotification(namaSholat: String, snoozeCount: Int): Notification {
        val fullScreenIntent = Intent(this, AlarmRingingActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_NO_USER_ACTION
            )
            putExtra("EXTRA_NAMA_SHOLAT", namaSholat)
            putExtra("EXTRA_SNOOZE_COUNT", snoozeCount)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, AlarmService::class.java).apply {
            action = "ACTION_STOP_AND_RESCHEDULE"
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            888,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val judul = if (snoozeCount > 0) {
            "Pengingat Sholat $namaSholat (Tunda ke-$snoozeCount)"
        } else {
            "Waktu Sholat $namaSholat Telah Tiba"
        }

        return NotificationCompat.Builder(this, "CHANNEL_ALARM_SHOLAT_V4")
            .setSmallIcon(R.drawable.ic_nav_alarm)
            .setContentTitle(judul)
            .setContentText("Tekan Volume Turun 2x atau usap layar untuk mematikan")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(fullScreenPendingIntent)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .addAction(R.drawable.ic_nav_alarm, "Matikan", stopPendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "CHANNEL_ALARM_SHOLAT_V4",
                "Alarm Waktu Sholat",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifikasi layar penuh saat waktu sholat tiba"
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setBypassDnd(true)
                setSound(null, null)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        pendingSoundRunnable?.let { mainHandler.removeCallbacks(it) }
        releaseMediaSession()
        stopAlarmSoundAndVibration()
        super.onDestroy()
    }
}