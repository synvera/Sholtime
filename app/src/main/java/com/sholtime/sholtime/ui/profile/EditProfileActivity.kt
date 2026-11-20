package com.sholtime.sholtime.ui.profile

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.bumptech.glide.Glide
import com.sholtime.sholtime.R
import com.sholtime.sholtime.data.local.SholtimePreferences
import com.sholtime.sholtime.databinding.ActivityEditProfileBinding
import java.io.File
import java.io.FileOutputStream

class EditProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditProfileBinding
    private lateinit var prefs: SholtimePreferences
    private var selectedPhotoPath: String? = null

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val savedPath = salinGambarKeInternal(uri)
            if (savedPath != null) {
                selectedPhotoPath = savedPath
                Glide.with(this)
                    .load(File(savedPath))
                    .circleCrop()
                    .into(binding.ivEditFotoProfil)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(top = bars.top, bottom = bars.bottom)
            insets
        }

        prefs = SholtimePreferences(this)
        isiFormAwal()
        setupListeners()
    }

    private fun isiFormAwal() {
        binding.etNamaLengkap.setText(prefs.getProfileNama())
        binding.etUsername.setText(prefs.getProfileUsername())
        binding.etDomisili.setText(prefs.getProfileDomisili())
        binding.etBio.setText(prefs.getProfileBio())

        val existingPhoto = prefs.getProfilePhotoPath()
        if (!existingPhoto.isNullOrBlank() && File(existingPhoto).exists()) {
            selectedPhotoPath = existingPhoto
            Glide.with(this)
                .load(File(existingPhoto))
                .circleCrop()
                .into(binding.ivEditFotoProfil)
        } else {
            binding.ivEditFotoProfil.setImageResource(R.drawable.ic_nav_profile)
        }
    }

    private fun setupListeners() {
        binding.btnBackEditProfile.setOnClickListener {
            finish()
        }

        binding.btnGantiFoto.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        binding.btnSimpanProfil.setOnClickListener {
            val nama = binding.etNamaLengkap.text?.toString()?.trim().orEmpty()
            val username = binding.etUsername.text?.toString()?.trim().orEmpty()
            val domisili = binding.etDomisili.text?.toString()?.trim().orEmpty()
            val bio = binding.etBio.text?.toString()?.trim().orEmpty()

            if (nama.isBlank()) {
                Toast.makeText(this, "Nama lengkap tidak boleh kosong", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val finalUsername = if (username.startsWith("@")) username else "@$username"
            prefs.saveUserProfile(nama, finalUsername, domisili, bio, selectedPhotoPath)
            Toast.makeText(this, "Profil berhasil diperbarui", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun salinGambarKeInternal(uri: Uri): String? {
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val targetFile = File(filesDir, "user_profile_avatar.jpg")
            val outputStream = FileOutputStream(targetFile)
            inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.close()
            targetFile.absolutePath
        } catch (e: Exception) {
            null
        }
    }
}