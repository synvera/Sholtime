package com.sholtime.sholtime.ui.settings

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.sholtime.sholtime.data.local.SholtimePreferences
import com.sholtime.sholtime.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefs: SholtimePreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(top = bars.top, bottom = bars.bottom)
            insets
        }

        prefs = SholtimePreferences(this)
        muatPengaturanTersimpan()
        setupListeners()
    }

    private fun muatPengaturanTersimpan() {
        when (prefs.getThemeMode()) {
            AppCompatDelegate.MODE_NIGHT_NO -> binding.rbTemaLight.isChecked = true
            AppCompatDelegate.MODE_NIGHT_YES -> binding.rbTemaDark.isChecked = true
            else -> binding.rbTemaSystem.isChecked = true
        }

        binding.tvNilaiJumlahSnooze.text = "${prefs.getSnoozeMaxCount()} Kali"
        binding.tvNilaiIntervalSnooze.text = "${prefs.getSnoozeIntervalMinutes()} Menit"

        val volume = prefs.getAlarmVolumePercent().toFloat().coerceIn(10f, 100f)
        binding.sliderVolumeAlarm.value = volume
        binding.tvNilaiVolume.text = "${volume.toInt()}%"

        binding.switchGetarAlarm.isChecked = prefs.isAlarmVibrate()
    }

    private fun setupListeners() {
        binding.btnBackSettings.setOnClickListener {
            finish()
        }

        binding.rgTemaAplikasi.setOnCheckedChangeListener { _, checkedId ->
            val modeBaru = when (checkedId) {
                binding.rbTemaLight.id -> AppCompatDelegate.MODE_NIGHT_NO
                binding.rbTemaDark.id -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
            if (modeBaru != prefs.getThemeMode()) {
                prefs.setThemeMode(modeBaru)
            }
        }

        binding.btnAturJumlahSnooze.setOnClickListener {
            val opsi = arrayOf(1, 2, 3, 5, 10)
            val labels = opsi.map { "$it Kali" }.toTypedArray()
            val currentIdx = opsi.indexOf(prefs.getSnoozeMaxCount()).coerceAtLeast(0)

            AlertDialog.Builder(this)
                .setTitle("Maksimal Pengulangan Tunda")
                .setSingleChoiceItems(labels, currentIdx) { dialog, which ->
                    val dipilih = opsi[which]
                    prefs.setSnoozeMaxCount(dipilih)
                    binding.tvNilaiJumlahSnooze.text = "$dipilih Kali"
                    dialog.dismiss()
                }
                .setNegativeButton("Batal", null)
                .show()
        }

        binding.btnAturIntervalSnooze.setOnClickListener {
            val opsiMenit = arrayOf(1, 3, 5, 10, 15)
            val labels = opsiMenit.map { "$it Menit" }.toTypedArray()
            val currentIdx = opsiMenit.indexOf(prefs.getSnoozeIntervalMinutes()).coerceAtLeast(0)

            AlertDialog.Builder(this)
                .setTitle("Interval Jeda Tunda")
                .setSingleChoiceItems(labels, currentIdx) { dialog, which ->
                    val dipilih = opsiMenit[which]
                    prefs.setSnoozeIntervalMinutes(dipilih)
                    binding.tvNilaiIntervalSnooze.text = "$dipilih Menit"
                    dialog.dismiss()
                }
                .setNegativeButton("Batal", null)
                .show()
        }

        binding.btnAturNadaSemua.setOnClickListener {
            val keys = arrayOf("adzan_subuh", "adzan_reguler", "nada_alarm_1")
            val labels = arrayOf("Adzan Subuh", "Adzan Reguler", "Nada Alarm Lembut")

            AlertDialog.Builder(this)
                .setTitle("Terapkan Nada ke Semua Sholat")
                .setItems(labels) { _, which ->
                    prefs.setAllAlarmToneKey(keys[which])
                    Toast.makeText(this, "Nada ${labels[which]} diterapkan untuk semua waktu sholat", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Batal", null)
                .show()
        }

        binding.sliderVolumeAlarm.addOnChangeListener { _, value, _ ->
            val persen = value.toInt()
            prefs.setAlarmVolumePercent(persen)
            binding.tvNilaiVolume.text = "$persen%"
        }

        binding.switchGetarAlarm.setOnCheckedChangeListener { _, isChecked ->
            prefs.setAlarmVibrate(isChecked)
        }
    }
}