package com.sholtime.sholtime.ui.alarm

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.sholtime.sholtime.MainActivity
import com.sholtime.sholtime.data.local.SholtimePreferences
import com.sholtime.sholtime.databinding.FragmentAlarmBinding
import com.sholtime.sholtime.databinding.ItemAlarmSholatBinding
import com.sholtime.sholtime.ui.home.JadwalViewModel

class AlarmFragment : Fragment() {

    private var _binding: FragmentAlarmBinding? = null
    private val binding get() = _binding!!

    private val viewModel: JadwalViewModel by activityViewModels()
    private lateinit var prefs: SholtimePreferences
    private lateinit var scheduler: AlarmScheduler

    private val notifPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        cekIzinSistemAlarm()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAlarmBinding.inflate(inflater, container, false)
        prefs = SholtimePreferences(requireContext())
        scheduler = AlarmScheduler(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Render daftar 5 alarm sholat dengan mengambil jadwal tersimpan
        muatDanRenderDaftarAlarm()

        // 2. Cek izin universal untuk semua merek HP & tombol kartu peringatan
        AlarmPermissionHelper.periksaSemuaIzin(requireContext())
        binding.btnBukaIzinAlarm.setOnClickListener {
            mintaIzinNotifikasiDanExactAlarm()
        }

        // 3. Tombol tes alarm 10 detik (kunci layar HP setelah menekan tombol ini)
        binding.btnTesAlarmSekarang.setOnClickListener {
            Toast.makeText(
                requireContext(),
                "Alarm akan berbunyi dalam 10 detik. Kunci layar HP sekarang!",
                Toast.LENGTH_LONG
            ).show()

            val triggerAt = System.currentTimeMillis() + 10_000L
            val testIntent = Intent(requireContext(), AlarmReceiver::class.java).apply {
                putExtra("EXTRA_NAMA_SHOLAT", "Subuh")
                putExtra("EXTRA_SNOOZE_COUNT", 0)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                requireContext(),
                9991,
                testIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val showIntent = PendingIntent.getActivity(
                requireContext(),
                0,
                Intent(requireContext(), MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(triggerAt, showIntent),
                pendingIntent
            )
        }
    }

    override fun onResume() {
        super.onResume()
        cekIzinSistemAlarm()
        if (_binding != null) {
            muatDanRenderDaftarAlarm()
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden && _binding != null) {
            muatDanRenderDaftarAlarm()
        }
    }

    private fun muatDanRenderDaftarAlarm() {
        val jadwalMap = ambilJadwalTersimpan()
        val daftarSholat = listOf(
            "Subuh" to (jadwalMap["Subuh"] ?: "--:--"),
            "Dzuhur" to (jadwalMap["Dzuhur"] ?: "--:--"),
            "Ashar" to (jadwalMap["Ashar"] ?: "--:--"),
            "Maghrib" to (jadwalMap["Maghrib"] ?: "--:--"),
            "Isya" to (jadwalMap["Isya"] ?: "--:--")
        )

        val container = binding.containerDaftarAlarm
        container.removeAllViews()

        for ((nama, jam) in daftarSholat) {
            val itemBinding = ItemAlarmSholatBinding.inflate(layoutInflater, container, false)
            itemBinding.tvAlarmNamaSholat.text = nama
            itemBinding.tvAlarmJamSholat.text = jam

            val isAktif = prefs.isAlarmEnabled(nama)
            itemBinding.switchAlarm.isChecked = isAktif

            val toneKey = prefs.getAlarmToneKey(nama)
            itemBinding.tvNadaTerpilih.text = prefs.getToneDisplayName(toneKey)

            itemBinding.switchAlarm.setOnCheckedChangeListener { _, checked ->
                prefs.setAlarmEnabled(nama, checked)
                if (checked) {
                    scheduler.jadwalkanAlarmSholat(nama)
                    Toast.makeText(requireContext(), "Alarm $nama diaktifkan ($jam)", Toast.LENGTH_SHORT).show()
                } else {
                    scheduler.batalkanAlarm(nama)
                    Toast.makeText(requireContext(), "Alarm $nama dinonaktifkan", Toast.LENGTH_SHORT).show()
                }
            }

            itemBinding.btnPilihNadaAlarm.setOnClickListener {
                tampilkanDialogPilihNada(nama) { namaNadaBaru ->
                    itemBinding.tvNadaTerpilih.text = namaNadaBaru
                }
            }

            container.addView(itemBinding.root)
        }
    }

    private fun ambilJadwalTersimpan(): Map<String, String> {
        val hasil = mutableMapOf<String, String>()
        val namaWaktu = listOf("Subuh", "Dzuhur", "Ashar", "Maghrib", "Isya")

        // 1. Cek di semua file SharedPreferences aplikasi (termasuk milik SholtimePreferences)
        val namaPrefsKandidat = listOf(
            "sholtime_prefs",
            "sholtime_preferences",
            "${requireContext().packageName}_preferences"
        )

        for (prefName in namaPrefsKandidat) {
            val sp = requireContext().getSharedPreferences(prefName, Context.MODE_PRIVATE)
            val allEntries = sp.all
            if (allEntries.isEmpty()) continue

            for (waktu in namaWaktu) {
                if (hasil.containsKey(waktu)) continue
                val keyCocok = allEntries.keys.firstOrNull { key ->
                    key.contains(waktu, ignoreCase = true) &&
                            !key.contains("enabled", ignoreCase = true) &&
                            !key.contains("tone", ignoreCase = true) &&
                            !key.contains("active", ignoreCase = true)
                }
                val nilai = keyCocok?.let { allEntries[it]?.toString() }
                if (!nilai.isNullOrBlank() && nilai.contains(":")) {
                    hasil[waktu] = nilai
                }
            }
        }

        // 2. Fallback: Baca lewat refleksi dari JadwalViewModel yang sedang aktif di HomeFragment
        if (hasil.size < 5) {
            try {
                for (field in viewModel.javaClass.declaredFields) {
                    field.isAccessible = true
                    val fieldValue = field.get(viewModel) ?: continue
                    val liveValue = try {
                        fieldValue.javaClass.getMethod("getValue").invoke(fieldValue)
                    } catch (_: Exception) {
                        null
                    } ?: continue

                    for (waktu in namaWaktu) {
                        if (hasil.containsKey(waktu)) continue
                        val getterName = "get$waktu"
                        val jam = try {
                            liveValue.javaClass.getMethod(getterName).invoke(liveValue)?.toString()
                        } catch (_: Exception) {
                            null
                        }
                        if (!jam.isNullOrBlank() && jam.contains(":")) {
                            hasil[waktu] = jam
                        }
                    }
                }
            } catch (_: Exception) {
            }
        }

        return hasil
    }

    private fun cekIzinSistemAlarm() {
        var butuhIzinNotif = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            butuhIzinNotif = ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        }

        var butuhIzinExact = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
            butuhIzinExact = !alarmManager.canScheduleExactAlarms()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val notifManager = requireContext().getSystemService(NotificationManager::class.java)
            if (!notifManager.canUseFullScreenIntent()) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                    Uri.parse("package:${requireContext().packageName}")
                )
                startActivity(intent)
            }
        }

        binding.cardPeringatanIzin.visibility = if (butuhIzinNotif || butuhIzinExact) View.VISIBLE else View.GONE
    }

    private fun mintaIzinNotifikasiDanExactAlarm() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:${requireContext().packageName}")
                }
                startActivity(intent)
            }
        }
    }

    private fun tampilkanDialogPilihNada(namaSholat: String, onSelected: (String) -> Unit) {
        val keys = arrayOf("adzan_subuh", "adzan_reguler", "nada_alarm_1")
        val labels = arrayOf("Adzan Subuh", "Adzan Reguler", "Nada Alarm Lembut")
        val currentKey = prefs.getAlarmToneKey(namaSholat)
        val checkedIndex = keys.indexOf(currentKey).coerceAtLeast(0)

        AlertDialog.Builder(requireContext())
            .setTitle("Pilih Nada Alarm $namaSholat")
            .setSingleChoiceItems(labels, checkedIndex) { dialog, which ->
                val chosenKey = keys[which]
                prefs.setAlarmToneKey(namaSholat, chosenKey)
                onSelected(labels[which])
                dialog.dismiss()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}