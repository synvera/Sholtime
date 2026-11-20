package com.sholtime.sholtime.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import android.content.Context
import android.content.Intent
import com.sholtime.sholtime.MainActivity
import com.sholtime.sholtime.R
import com.sholtime.sholtime.data.model.ItemWaktuSholat
import com.sholtime.sholtime.data.model.JadwalSholatHarian
import com.sholtime.sholtime.data.model.KotaItem
import com.sholtime.sholtime.data.model.StatusWaktuSholat
import com.sholtime.sholtime.databinding.DialogPilihKotaBinding
import com.sholtime.sholtime.databinding.FragmentHomeBinding
import com.sholtime.sholtime.databinding.ItemJadwalHomeBinding
import com.sholtime.sholtime.utils.LocationHelper
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: JadwalViewModel by activityViewModels()
    private lateinit var locationHelper: LocationHelper

    private val clockHandler = Handler(Looper.getMainLooper())
    private var jadwalAktif: JadwalSholatHarian? = null

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            jalankanDeteksiGps()
        } else {
            Toast.makeText(requireContext(), "Izin lokasi ditolak. Ketuk nama kota untuk memilih manual.", Toast.LENGTH_SHORT).show()
        }
    }

    private val clockRunnable = object : Runnable {
        override fun run() {
            if (_binding != null) {
                updateJamDanCountdown()
                clockHandler.postDelayed(this, 1000L)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        locationHelper = LocationHelper(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupObservers()
        setupListeners()
        cekDanMintaIzinLokasiAwal()
    }

    override fun onResume() {
        super.onResume()
        clockHandler.post(clockRunnable)
    }

    override fun onPause() {
        super.onPause()
        clockHandler.removeCallbacks(clockRunnable)
    }

    private fun setupObservers() {
        viewModel.namaKotaAktif.observe(viewLifecycleOwner) { kota ->
            binding.tvKotaAktif.text = kota
            if (!kota.isNullOrBlank()) {
                requireContext().getSharedPreferences("sholtime_prefs", Context.MODE_PRIVATE)
                    .edit()
                    .putString("nama_kota", kota)
                    .apply()
                kirimSinyalUpdateWidget()
            }
        }

        viewModel.isOfflineMode.observe(viewLifecycleOwner) { offline ->
            binding.tvBadgeOffline.visibility = if (offline) View.VISIBLE else View.GONE
        }

        viewModel.jadwalHariIni.observe(viewLifecycleOwner) { jadwal ->
            jadwalAktif = jadwal
            if (jadwal != null) {
                binding.tvTanggalHariIni.text = jadwal.tanggalTeks

                // Simpan jadwal terbaru ke SharedPreferences agar Widget 2x2 & AlarmFragment selalu sinkron
                requireContext().getSharedPreferences("sholtime_prefs", Context.MODE_PRIVATE)
                    .edit()
                    .putString("jadwal_subuh", jadwal.subuh)
                    .putString("jadwal_dzuhur", jadwal.dzuhur)
                    .putString("jadwal_ashar", jadwal.ashar)
                    .putString("jadwal_maghrib", jadwal.maghrib)
                    .putString("jadwal_isya", jadwal.isya)
                    .apply()

                kirimSinyalUpdateWidget()
                updateJamDanCountdown()
            }
        }

        viewModel.statusInfo.observe(viewLifecycleOwner) { pesan ->
            if (!pesan.isNullOrBlank()) {
                Toast.makeText(requireContext(), pesan, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun kirimSinyalUpdateWidget() {
        val ctx = requireContext()

        // Update Widget 2x2 (Hitung Mundur Sholat Terdekat)
        val updateSmallIntent = Intent(ctx, com.sholtime.sholtime.SholtimeSmallWidgetProvider::class.java).apply {
            action = com.sholtime.sholtime.SholtimeSmallWidgetProvider.ACTION_REFRESH_WIDGET
        }
        ctx.sendBroadcast(updateSmallIntent)

        // Update Widget 4x2 (Jadwal Lengkap)
        val manager = android.appwidget.AppWidgetManager.getInstance(ctx)
        val ids4x2 = manager.getAppWidgetIds(
            android.content.ComponentName(ctx, com.sholtime.sholtime.SholtimeWidgetProvider::class.java)
        )
        if (ids4x2.isNotEmpty()) {
            val update4x2Intent = Intent(ctx, com.sholtime.sholtime.SholtimeWidgetProvider::class.java).apply {
                action = android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS, ids4x2)
            }
            ctx.sendBroadcast(update4x2Intent)
        }
    }
    private fun setupListeners() {
        binding.btnPilihLokasi.setOnClickListener {
            tampilkanDialogPilihKota()
        }

        binding.btnShortcutAlarm.setOnClickListener {
            (activity as? MainActivity)?.navigateToTab(R.id.nav_alarm)
        }

        binding.btnShortcutJadwal.setOnClickListener {
            (activity as? MainActivity)?.navigateToTab(R.id.nav_jadwal)
        }
    }

    private fun cekDanMintaIzinLokasiAwal() {
        val prefs = requireContext().getSharedPreferences("sholtime_prefs", 0)
        val sudahPernahCek = prefs.getBoolean("initial_gps_checked", false)
        if (!sudahPernahCek) {
            prefs.edit().putBoolean("initial_gps_checked", true).apply()
            mintaIzinDanDeteksiGps()
        }
    }

    private fun mintaIzinDanDeteksiGps() {
        val fine = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
        if (fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED) {
            jalankanDeteksiGps()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun jalankanDeteksiGps() {
        Toast.makeText(requireContext(), "Mendeteksi kota via GPS...", Toast.LENGTH_SHORT).show()
        locationHelper.detectCityName { namaKota ->
            activity?.runOnUiThread {
                if (!namaKota.isNullOrBlank()) {
                    viewModel.cariDanPilihKotaOtomatis(namaKota)
                } else {
                    Toast.makeText(requireContext(), "Gagal mendapatkan lokasi GPS. Silakan pilih kota manual.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun tampilkanDialogPilihKota() {
        val dialogBinding = DialogPilihKotaBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()

        var daftarKotaSaatIni: List<KotaItem> = emptyList()
        val adapter = ArrayAdapter<String>(requireContext(), android.R.layout.simple_list_item_1, mutableListOf())
        dialogBinding.lvHasilKota.adapter = adapter

        viewModel.hasilCariKota.observe(viewLifecycleOwner) { listKota ->
            daftarKotaSaatIni = listKota
            adapter.clear()
            adapter.addAll(listKota.map { it.lokasi })
            adapter.notifyDataSetChanged()
        }

        dialogBinding.btnDeteksiGpsDialog.setOnClickListener {
            dialog.dismiss()
            mintaIzinDanDeteksiGps()
        }

        dialogBinding.etCariKota.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString()?.trim() ?: ""
                if (query.length >= 3) {
                    viewModel.cariKotaManual(query)
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        dialogBinding.lvHasilKota.setOnItemClickListener { _, _, position, _ ->
            if (position in daftarKotaSaatIni.indices) {
                viewModel.pilihKotaManual(daftarKotaSaatIni[position])
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun updateJamDanCountdown() {
        val now = Date()
        val jamFormat = SimpleDateFormat("HH:mm:ss", Locale.US)
        binding.tvJamRealtime.text = jamFormat.format(now)

        val jadwal = jadwalAktif ?: return

        val daftarMentah = listOf(
            "Subuh" to jadwal.subuh,
            "Dzuhur" to jadwal.dzuhur,
            "Ashar" to jadwal.ashar,
            "Maghrib" to jadwal.maghrib,
            "Isya" to jadwal.isya
        )

        val calSekarang = Calendar.getInstance()
        var targetSholatNama = ""
        var targetCal: Calendar? = null
        var ketemuBerikutnya = false

        val daftarItem = mutableListOf<ItemWaktuSholat>()

        for ((nama, jamStr) in daftarMentah) {
            val parts = jamStr.split(":")
            if (parts.size < 2) continue
            val jam = parts[0].toIntOrNull() ?: 0
            val menit = parts[1].toIntOrNull() ?: 0

            val calSholat = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, jam)
                set(Calendar.MINUTE, menit)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            if (calSholat.after(calSekarang) && !ketemuBerikutnya) {
                ketemuBerikutnya = true
                targetSholatNama = nama
                targetCal = calSholat
                daftarItem.add(ItemWaktuSholat(nama, jamStr, StatusWaktuSholat.BERIKUTNYA))
            } else if (!ketemuBerikutnya) {
                daftarItem.add(ItemWaktuSholat(nama, jamStr, StatusWaktuSholat.TERLEWATI))
            } else {
                daftarItem.add(ItemWaktuSholat(nama, jamStr, StatusWaktuSholat.AKAN_DATANG))
            }
        }

        if (!ketemuBerikutnya) {
            val subuhParts = jadwal.subuh.split(":")
            if (subuhParts.size >= 2) {
                targetSholatNama = "Subuh (Besok)"
                targetCal = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, 1)
                    set(Calendar.HOUR_OF_DAY, subuhParts[0].toIntOrNull() ?: 4)
                    set(Calendar.MINUTE, subuhParts[1].toIntOrNull() ?: 30)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
            }
        }

        binding.tvNamaSholatBerikutnya.text = targetSholatNama
        targetCal?.let { target ->
            val selisihMillis = (target.timeInMillis - calSekarang.timeInMillis).coerceAtLeast(0L)
            val totalDetik = selisihMillis / 1000
            val jamSisa = totalDetik / 3600
            val menitSisa = (totalDetik % 3600) / 60
            val detikSisa = totalDetik % 60
            binding.tvCountdownSholat.text = String.format(
                Locale.US,
                "%02d jam %02d menit %02d detik lagi",
                jamSisa,
                menitSisa,
                detikSisa
            )
        }

        renderDaftar5Waktu(daftarItem)
    }

    private fun renderDaftar5Waktu(items: List<ItemWaktuSholat>) {
        val container = binding.containerJadwal5Waktu
        if (container.childCount != items.size) {
            container.removeAllViews()
            repeat(items.size) {
                val itemBinding = ItemJadwalHomeBinding.inflate(layoutInflater, container, false)
                container.addView(itemBinding.root)
            }
        }

        items.forEachIndexed { index, item ->
            val rowView = container.getChildAt(index)
            val itemBinding = ItemJadwalHomeBinding.bind(rowView)
            itemBinding.tvNamaSholat.text = item.nama
            itemBinding.tvJamSholat.text = item.jam

            when (item.status) {
                StatusWaktuSholat.TERLEWATI -> {
                    itemBinding.rowRoot.setBackgroundColor(Color.TRANSPARENT)
                    itemBinding.tvStatusSholat.text = "Terlewati"
                    itemBinding.tvStatusSholat.setTextColor(Color.parseColor("#94A3B8"))
                    itemBinding.tvNamaSholat.alpha = 0.6f
                    itemBinding.tvJamSholat.alpha = 0.6f
                }
                StatusWaktuSholat.BERIKUTNYA -> {
                    itemBinding.rowRoot.setBackgroundColor(Color.parseColor("#1A10B981"))
                    itemBinding.tvStatusSholat.text = "Berikutnya"
                    itemBinding.tvStatusSholat.setTextColor(Color.parseColor("#10B981"))
                    itemBinding.tvNamaSholat.alpha = 1.0f
                    itemBinding.tvJamSholat.alpha = 1.0f
                }
                StatusWaktuSholat.AKAN_DATANG -> {
                    itemBinding.rowRoot.setBackgroundColor(Color.TRANSPARENT)
                    itemBinding.tvStatusSholat.text = "Akan Datang"
                    itemBinding.tvStatusSholat.setTextColor(Color.parseColor("#64748B"))
                    itemBinding.tvNamaSholat.alpha = 1.0f
                    itemBinding.tvJamSholat.alpha = 1.0f
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        clockHandler.removeCallbacks(clockRunnable)
        _binding = null
    }
}