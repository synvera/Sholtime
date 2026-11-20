package com.sholtime.sholtime.ui.jadwal

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.sholtime.sholtime.data.model.JadwalSholatHarian
import com.sholtime.sholtime.databinding.FragmentJadwalBinding
import com.sholtime.sholtime.databinding.ItemJadwalDetailBinding
import com.sholtime.sholtime.ui.home.JadwalViewModel
import com.sholtime.sholtime.ui.panduan.PanduanSholatActivity
import java.util.Calendar
import java.util.Locale

class JadwalFragment : Fragment() {

    private var _binding: FragmentJadwalBinding? = null
    private val binding get() = _binding!!

    private val viewModel: JadwalViewModel by activityViewModels()
    private val timerHandler = Handler(Looper.getMainLooper())
    private var jadwalAktif: JadwalSholatHarian? = null

    private val timerRunnable = object : Runnable {
        override fun run() {
            if (_binding != null) {
                perbaruiTampilanWaktuDanIqomah()
                timerHandler.postDelayed(this, 1000L)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentJadwalBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.jadwalHariIni.observe(viewLifecycleOwner) { jadwal ->
            jadwalAktif = jadwal
            if (jadwal != null) {
                binding.tvKotaDanTanggalJadwal.text = "${jadwal.kota} • ${jadwal.tanggalTeks}"
                perbaruiTampilanWaktuDanIqomah()
            }
        }

        binding.btnBukaPanduanSholat.setOnClickListener {
            startActivity(Intent(requireContext(), PanduanSholatActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        timerHandler.post(timerRunnable)
    }

    override fun onPause() {
        super.onPause()
        timerHandler.removeCallbacks(timerRunnable)
    }

    private fun getJedaIqomahMenit(namaSholat: String): Int {
        return when (namaSholat.lowercase()) {
            "subuh" -> 15
            "maghrib" -> 5
            else -> 10
        }
    }

    private fun perbaruiTampilanWaktuDanIqomah() {
        val jadwal = jadwalAktif ?: return

        val daftarSholat = listOf(
            "Subuh" to jadwal.subuh,
            "Dzuhur" to jadwal.dzuhur,
            "Ashar" to jadwal.ashar,
            "Maghrib" to jadwal.maghrib,
            "Isya" to jadwal.isya
        )

        val calSekarang = Calendar.getInstance()
        val nowMillis = calSekarang.timeInMillis

        var sedangFaseIqomah = false
        var namaSholatIqomah = ""
        var sisaMillisIqomah = 0L
        var jedaMenitIqomah = 10

        var ketemuBerikutnya = false
        var namaSholatBerikutnya = ""
        var sisaMillisBerikutnya = 0L
        var jedaMenitBerikutnya = 10

        val statusList = mutableListOf<String>()

        for ((nama, jamStr) in daftarSholat) {
            val parts = jamStr.split(":")
            if (parts.size < 2) {
                statusList.add("Akan Datang")
                continue
            }
            val calAdzan = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, parts[0].toIntOrNull() ?: 0)
                set(Calendar.MINUTE, parts[1].toIntOrNull() ?: 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val adzanMillis = calAdzan.timeInMillis
            val jedaMenit = getJedaIqomahMenit(nama)
            val batasIqomahMillis = adzanMillis + (jedaMenit * 60_000L)

            if (nowMillis in adzanMillis until batasIqomahMillis) {
                sedangFaseIqomah = true
                namaSholatIqomah = nama
                sisaMillisIqomah = batasIqomahMillis - nowMillis
                jedaMenitIqomah = jedaMenit
                statusList.add("Waktu Iqomah")
            } else if (nowMillis < adzanMillis && !ketemuBerikutnya) {
                ketemuBerikutnya = true
                namaSholatBerikutnya = nama
                sisaMillisBerikutnya = adzanMillis - nowMillis
                jedaMenitBerikutnya = jedaMenit
                statusList.add("Berikutnya")
            } else if (nowMillis >= batasIqomahMillis) {
                statusList.add("Terlewati")
            } else {
                statusList.add("Akan Datang")
            }
        }

        if (sedangFaseIqomah) {
            binding.cardStatusUtamaJadwal.setCardBackgroundColor(Color.parseColor("#065F46"))
            binding.tvJudulStatusTimer.text = "MENUNGGU IQOMAH SHOLAT"
            binding.tvJudulStatusTimer.setTextColor(Color.parseColor("#A7F3D0"))
            binding.tvNamaStatusTimer.setTextColor(Color.WHITE)
            binding.tvHitungMundurUtama.setTextColor(Color.WHITE)
            binding.tvKeteranganIqomah.setTextColor(Color.parseColor("#D1FAE5"))
            binding.tvNamaStatusTimer.text = namaSholatIqomah.uppercase(Locale.getDefault())
            binding.tvHitungMundurUtama.text = formatDurasi(sisaMillisIqomah)
            binding.tvKeteranganIqomah.text = "Waktu adzan $namaSholatIqomah telah tiba. Perkiraan iqomah ($jedaMenitIqomah menit setelah adzan)."
        } else {
            val bgNormal = androidx.core.content.ContextCompat.getColor(requireContext(), com.sholtime.sholtime.R.color.hero_card_bg)
            val accentNormal = androidx.core.content.ContextCompat.getColor(requireContext(), com.sholtime.sholtime.R.color.hero_accent)
            val textPrimary = androidx.core.content.ContextCompat.getColor(requireContext(), com.sholtime.sholtime.R.color.hero_text_primary)
            val textSecondary = androidx.core.content.ContextCompat.getColor(requireContext(), com.sholtime.sholtime.R.color.hero_text_secondary)

            binding.cardStatusUtamaJadwal.setCardBackgroundColor(bgNormal)
            binding.tvJudulStatusTimer.text = "MENUJU WAKTU SHOLAT BERIKUTNYA"
            binding.tvJudulStatusTimer.setTextColor(accentNormal)
            binding.tvNamaStatusTimer.setTextColor(textPrimary)
            binding.tvHitungMundurUtama.setTextColor(textPrimary)
            binding.tvKeteranganIqomah.setTextColor(textSecondary)

            if (ketemuBerikutnya) {
                binding.tvNamaStatusTimer.text = namaSholatBerikutnya.uppercase(Locale.getDefault())
                binding.tvHitungMundurUtama.text = formatDurasi(sisaMillisBerikutnya)
                binding.tvKeteranganIqomah.text = "Ketentuan jeda iqomah $namaSholatBerikutnya: $jedaMenitBerikutnya menit setelah adzan"
            } else {
                val subuhParts = jadwal.subuh.split(":")
                val calBesok = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, 1)
                    set(Calendar.HOUR_OF_DAY, subuhParts.getOrNull(0)?.toIntOrNull() ?: 4)
                    set(Calendar.MINUTE, subuhParts.getOrNull(1)?.toIntOrNull() ?: 30)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val sisaSubuh = (calBesok.timeInMillis - nowMillis).coerceAtLeast(0L)
                binding.tvNamaStatusTimer.text = "SUBUH (BESOK)"
                binding.tvHitungMundurUtama.text = formatDurasi(sisaSubuh)
                binding.tvKeteranganIqomah.text = "Ketentuan jeda iqomah Subuh: 15 menit setelah adzan"
            }
        }

        renderDaftarDetail(daftarSholat, statusList)
    }

    private fun renderDaftarDetail(daftar: List<Pair<String, String>>, statusList: List<String>) {
        val container = binding.containerDetail5Waktu
        if (container.childCount != daftar.size) {
            container.removeAllViews()
            repeat(daftar.size) {
                val itemBinding = ItemJadwalDetailBinding.inflate(layoutInflater, container, false)
                container.addView(itemBinding.root)
            }
        }

        daftar.forEachIndexed { index, (nama, jam) ->
            val rowView = container.getChildAt(index)
            val itemBinding = ItemJadwalDetailBinding.bind(rowView)
            val jeda = getJedaIqomahMenit(nama)
            val status = statusList.getOrElse(index) { "Akan Datang" }

            itemBinding.tvDetailNamaSholat.text = nama
            itemBinding.tvDetailJamSholat.text = jam
            itemBinding.tvDetailInfoIqomah.text = "Jeda Iqomah: $jeda menit setelah adzan"
            itemBinding.tvDetailBadge.text = status

            when (status) {
                "Waktu Iqomah" -> {
                    itemBinding.tvDetailBadge.setTextColor(Color.parseColor("#059669"))
                    itemBinding.cardItemJadwal.strokeWidth = 3
                    itemBinding.cardItemJadwal.strokeColor = Color.parseColor("#10B981")
                }
                "Berikutnya" -> {
                    itemBinding.tvDetailBadge.setTextColor(Color.parseColor("#10B981"))
                    itemBinding.cardItemJadwal.strokeWidth = 2
                    itemBinding.cardItemJadwal.strokeColor = Color.parseColor("#10B981")
                }
                "Terlewati" -> {
                    itemBinding.tvDetailBadge.setTextColor(Color.parseColor("#94A3B8"))
                    itemBinding.cardItemJadwal.strokeWidth = 0
                }
                else -> {
                    itemBinding.tvDetailBadge.setTextColor(Color.parseColor("#64748B"))
                    itemBinding.cardItemJadwal.strokeWidth = 0
                }
            }
        }
    }

    private fun formatDurasi(millis: Long): String {
        val totalDetik = (millis / 1000L).coerceAtLeast(0L)
        val jam = totalDetik / 3600
        val menit = (totalDetik % 3600) / 60
        val detik = totalDetik % 60
        return String.format(Locale.US, "%02d:%02d:%02d", jam, menit, detik)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        timerHandler.removeCallbacks(timerRunnable)
        _binding = null
    }
}