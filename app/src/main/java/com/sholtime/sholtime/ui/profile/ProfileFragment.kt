package com.sholtime.sholtime.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.sholtime.sholtime.R
import com.sholtime.sholtime.data.local.SholtimePreferences
import com.sholtime.sholtime.databinding.FragmentProfileBinding
import com.sholtime.sholtime.ui.settings.SettingsActivity
import java.io.File

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var prefs: SholtimePreferences

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        prefs = SholtimePreferences(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnEditProfil.setOnClickListener {
            startActivity(Intent(requireContext(), EditProfileActivity::class.java))
        }

        binding.btnBukaSettings.setOnClickListener {
            startActivity(Intent(requireContext(), SettingsActivity::class.java))
        }

        binding.cardMenuSettings.setOnClickListener {
            startActivity(Intent(requireContext(), SettingsActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        muatDataProfil()
    }

    private fun muatDataProfil() {
        binding.tvProfilNamaLengkap.text = prefs.getProfileNama()
        binding.tvProfilUsername.text = prefs.getProfileUsername()
        binding.tvProfilDomisili.text = prefs.getProfileDomisili()
        binding.tvProfilBio.text = prefs.getProfileBio()

        val photoPath = prefs.getProfilePhotoPath()
        if (!photoPath.isNullOrBlank() && File(photoPath).exists()) {
            Glide.with(this)
                .load(File(photoPath))
                .circleCrop()
                .into(binding.ivFotoProfil)
        } else {
            binding.ivFotoProfil.setImageResource(R.drawable.ic_nav_profile)
        }
    }

    private fun tampilkanInfoVersi() {
        try {
            val packageInfo = requireContext().packageManager.getPackageInfo(
                requireContext().packageName,
                0
            )
            val versionName = packageInfo.versionName ?: "1.0.0"
            binding.tvAppVersion.text = "Sholtime v$versionName"
        } catch (e: Exception) {
            binding.tvAppVersion.text = "Sholtime v1.0.0"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}