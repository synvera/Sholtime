package com.sholtime.sholtime

import android.os.Bundle
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import com.sholtime.sholtime.databinding.ActivityMainBinding
import com.sholtime.sholtime.ui.alarm.AlarmFragment
import com.sholtime.sholtime.ui.home.HomeFragment
import com.sholtime.sholtime.ui.jadwal.JadwalFragment
import com.sholtime.sholtime.ui.profile.ProfileFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private lateinit var homeFragment: HomeFragment
    private lateinit var alarmFragment: AlarmFragment
    private lateinit var jadwalFragment: JadwalFragment
    private lateinit var profileFragment: ProfileFragment
    private var activeFragment: Fragment? = null
    private var currentTabIndex: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentTabIndex = savedInstanceState?.getInt("KEY_TAB_INDEX", 0) ?: 0

        applySystemBarInsets()
        setupFragments(savedInstanceState)
        setupFloatingNavigation()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("KEY_TAB_INDEX", currentTabIndex)
    }

    private fun applySystemBarInsets() {
        val baseBottomMargin = (16 * resources.displayMetrics.density).toInt()
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.fragmentContainer.updatePadding(top = systemBars.top)
            binding.floatingNavContainer.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = baseBottomMargin + systemBars.bottom
            }
            insets
        }
    }

    private fun setupFragments(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) {
            homeFragment = HomeFragment()
            alarmFragment = AlarmFragment()
            jadwalFragment = JadwalFragment()
            profileFragment = ProfileFragment()
            activeFragment = homeFragment

            supportFragmentManager.beginTransaction().apply {
                add(R.id.fragmentContainer, profileFragment, "PROFILE").hide(profileFragment)
                add(R.id.fragmentContainer, jadwalFragment, "JADWAL").hide(jadwalFragment)
                add(R.id.fragmentContainer, alarmFragment, "ALARM").hide(alarmFragment)
                add(R.id.fragmentContainer, homeFragment, "HOME")
            }.commit()
        } else {
            homeFragment = supportFragmentManager.findFragmentByTag("HOME") as? HomeFragment ?: HomeFragment()
            alarmFragment = supportFragmentManager.findFragmentByTag("ALARM") as? AlarmFragment ?: AlarmFragment()
            jadwalFragment = supportFragmentManager.findFragmentByTag("JADWAL") as? JadwalFragment ?: JadwalFragment()
            profileFragment = supportFragmentManager.findFragmentByTag("PROFILE") as? ProfileFragment ?: ProfileFragment()

            activeFragment = when (currentTabIndex) {
                1 -> alarmFragment
                2 -> jadwalFragment
                3 -> profileFragment
                else -> homeFragment
            }

            supportFragmentManager.beginTransaction().apply {
                listOf(homeFragment, alarmFragment, jadwalFragment, profileFragment).forEach { frag ->
                    if (frag == activeFragment) show(frag) else hide(frag)
                }
            }.commit()
        }
    }

    private fun setupFloatingNavigation() {
        val navItems = listOf(
            Triple(binding.navItemHome, binding.ivNavHome, binding.tvNavHome),
            Triple(binding.navItemAlarm, binding.ivNavAlarm, binding.tvNavAlarm),
            Triple(binding.navItemJadwal, binding.ivNavJadwal, binding.tvNavJadwal),
            Triple(binding.navItemProfile, binding.ivNavProfile, binding.tvNavProfile)
        )

        fun updatePillUI(indexAktif: Int, animate: Boolean) {
            if (animate) {
                val transition = AutoTransition().apply { duration = 200L }
                TransitionManager.beginDelayedTransition(binding.floatingNavContainer, transition)
            }

            val warnaAktif = ContextCompat.getColor(this, R.color.nav_item_active_fg)
            val warnaNonAktif = ContextCompat.getColor(this, R.color.nav_item_inactive_fg)

            navItems.forEachIndexed { index, (container, icon, label) ->
                if (index == indexAktif) {
                    container.setBackgroundResource(R.drawable.bg_nav_pill_active)
                    icon.setColorFilter(warnaAktif)
                    label.visibility = View.VISIBLE
                } else {
                    container.background = null
                    icon.setColorFilter(warnaNonAktif)
                    label.visibility = View.GONE
                }
            }
        }

        binding.navItemHome.setOnClickListener {
            currentTabIndex = 0
            updatePillUI(0, animate = true)
            switchFragment(homeFragment)
        }
        binding.navItemAlarm.setOnClickListener {
            currentTabIndex = 1
            updatePillUI(1, animate = true)
            switchFragment(alarmFragment)
        }
        binding.navItemJadwal.setOnClickListener {
            currentTabIndex = 2
            updatePillUI(2, animate = true)
            switchFragment(jadwalFragment)
        }
        binding.navItemProfile.setOnClickListener {
            currentTabIndex = 3
            updatePillUI(3, animate = true)
            switchFragment(profileFragment)
        }

        updatePillUI(currentTabIndex, animate = false)
    }

    private fun switchFragment(targetFragment: Fragment) {
        if (targetFragment == activeFragment) return
        val transaction = supportFragmentManager.beginTransaction()
        activeFragment?.let { transaction.hide(it) }
        transaction.show(targetFragment).commit()
        activeFragment = targetFragment
    }

    fun navigateToTab(menuItemId: Int) {
        when (menuItemId) {
            R.id.nav_home -> binding.navItemHome.performClick()
            R.id.nav_alarm -> binding.navItemAlarm.performClick()
            R.id.nav_jadwal -> binding.navItemJadwal.performClick()
            R.id.nav_profile -> binding.navItemProfile.performClick()
        }
    }
}