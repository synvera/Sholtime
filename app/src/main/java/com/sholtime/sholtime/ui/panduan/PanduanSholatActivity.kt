package com.sholtime.sholtime.ui.panduan

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.sholtime.sholtime.databinding.ActivityPanduanSholatBinding
import com.sholtime.sholtime.databinding.ItemPanduanSholatBinding

class PanduanSholatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPanduanSholatBinding

    private data class MateriSholat(
        val judul: String,
        val penjelasan: String,
        val arab: String,
        val latin: String,
        val arti: String
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPanduanSholatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(top = bars.top, bottom = bars.bottom)
            insets
        }

        binding.btnBackPanduan.setOnClickListener {
            finish()
        }

        tampilkanDaftarMateri()
    }

    private fun tampilkanDaftarMateri() {
        val daftarMateri = listOf(
            MateriSholat(
                judul = "1. Berdiri Tegak & Niat",
                penjelasan = "Berdiri tegak menghadap kiblat dalam keadaan suci dari hadas dan najis, lalu menghadirkan niat sholat di dalam hati sesuai waktu sholat yang dikerjakan.",
                arab = "نَوَيْتُ أَنْ أُصَلِّيَ لِلَّهِ تَعَالَى",
                latin = "Nawaitu an ushalliya lillahi ta'ala",
                arti = "Artinya: Aku berniat melaksanakan sholat karena Allah Ta'ala."
            ),
            MateriSholat(
                judul = "2. Takbiratul Ihram",
                penjelasan = "Mengangkat kedua tangan sejajar bahu atau daun telinga sambil mengucapkan takbir pembuka sholat, lalu meletakkan tangan kanan di atas tangan kiri di dada.",
                arab = "اللَّهُ أَكْبَرُ",
                latin = "Allahu Akbar",
                arti = "Artinya: Allah Maha Besar."
            ),
            MateriSholat(
                judul = "3. Membaca Surah Al-Fatihah",
                penjelasan = "Membaca surah Al-Fatihah pada setiap rakaat dengan tartil, kemudian dilanjutkan dengan membaca surah pendek pada rakaat pertama dan kedua.",
                arab = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ • الْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ",
                latin = "Bismillahirrahmanirrahim. Alhamdulillahi rabbil 'alamin...",
                arti = "Artinya: Dengan nama Allah Yang Maha Pengasih lagi Maha Penyayang. Segala puji bagi Allah Tuhan semesta alam..."
            ),
            MateriSholat(
                judul = "4. Rukuk dengan Tuma'ninah",
                penjelasan = "Membungkukkan badan hingga punggung rata sejajar dengan kepala, kedua telapak tangan memegang lutut, dan membaca tasbih rukuk secara tenang.",
                arab = "سُبْحَانَ رَبِّيَ الْعَظِيمِ وَبِحَمْدِهِ",
                latin = "Subhana rabbiyal 'adzimi wa bihamdih (3x)",
                arti = "Artinya: Maha Suci Tuhanku Yang Maha Agung dan segala puji bagi-Nya."
            ),
            MateriSholat(
                judul = "5. I'tidal (Bangkit dari Rukuk)",
                penjelasan = "Bangkit berdiri tegak kembali sambil mengangkat kedua tangan, lalu membaca doa i'tidal dengan tenang.",
                arab = "سَمِعَ اللَّهُ لِمَنْ حَمِدَهُ • رَبَّنَا وَلَكَ الْحَمْدُ",
                latin = "Sami'allahu liman hamidah. Rabbana wa lakal hamdu.",
                arti = "Artinya: Allah Maha Mendengar orang yang memuji-Nya. Ya Tuhan kami, segala puji hanya bagi-Mu."
            ),
            MateriSholat(
                judul = "6. Sujud dengan Tuma'ninah",
                penjelasan = "Turun bersujud dengan menempelkan tujuh anggota sujud (dahi beserta hidung, dua telapak tangan, dua lutut, dan ujung jari kedua kaki) ke tempat sujud.",
                arab = "سُبْحَانَ رَبِّيَ الْأَعْلَى وَبِحَمْدِهِ",
                latin = "Subhana rabbiyal a'la wa bihamdih (3x)",
                arti = "Artinya: Maha Suci Tuhanku Yang Maha Tinggi dan segala puji bagi-Nya."
            ),
            MateriSholat(
                judul = "7. Duduk di Antara Dua Sujud",
                penjelasan = "Bangkit dari sujud pertama lalu duduk iftirasy (duduk di atas telapak kaki kiri dan menegakkan telapak kaki kanan) sambil membaca doa.",
                arab = "رَبِّ اغْفِرْ لِي وَارْحَمْنِي وَاجْبُرْنِي وَارْفَعْنِي وَارْزُقْنِي وَاهْدِنِي",
                latin = "Rabbighfirli warhamni wajburni warfa'ni warzuqni wahdini.",
                arti = "Artinya: Ya Tuhanku, ampunilah aku, rahmatilah aku, cukupkanlah aku, angkatlah derajatku, berilah aku rezeki, dan berilah aku petunjuk."
            ),
            MateriSholat(
                judul = "8. Tasyahud Akhir & Salam",
                penjelasan = "Pada rakaat terakhir, duduk tawarruk membaca bacaan tasyahud, sholawat Nabi, lalu menoleh ke kanan dan ke kiri sambil mengucapkan salam.",
                arab = "السَّلَامُ عَلَيْكُمْ وَرَحْمَةُ اللَّهِ",
                latin = "Assalamu'alaikum wa rahmatullah",
                arti = "Artinya: Semoga keselamatan dan rahmat Allah tercurah atas kalian."
            )
        )

        val container = binding.containerPanduan
        container.removeAllViews()

        for (materi in daftarMateri) {
            val itemBinding = ItemPanduanSholatBinding.inflate(layoutInflater, container, false)
            itemBinding.tvJudulPanduan.text = materi.judul
            itemBinding.tvPenjelasanPanduan.text = materi.penjelasan
            if (materi.arab.isBlank()) {
                itemBinding.tvArabPanduan.visibility = View.GONE
            } else {
                itemBinding.tvArabPanduan.text = materi.arab
            }
            itemBinding.tvLatinPanduan.text = materi.latin
            itemBinding.tvArtiPanduan.text = materi.arti
            container.addView(itemBinding.root)
        }
    }
}