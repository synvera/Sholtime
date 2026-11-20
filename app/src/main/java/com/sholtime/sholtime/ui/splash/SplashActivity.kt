package com.sholtime.sholtime.ui.splash

import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.sholtime.sholtime.MainActivity
import com.sholtime.sholtime.data.local.SholtimePreferences
import com.sholtime.sholtime.databinding.ActivitySplashBinding

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        SholtimePreferences(this).applySavedTheme()
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        startSplashAnimation()
    }

    private fun startSplashAnimation() {
        binding.logoContainer.animate()
            .alpha(1f)
            .setDuration(900L)
            .withEndAction {
                Handler(Looper.getMainLooper()).postDelayed({
                    startExitBlurAndTransition()
                }, 700L)
            }
            .start()
    }

    private fun startExitBlurAndTransition() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val blurAnimator = ValueAnimator.ofFloat(1f, 35f)
            blurAnimator.duration = 600L
            blurAnimator.addUpdateListener { animator ->
                val radius = animator.animatedValue as Float
                val effect = RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP)
                binding.logoContainer.setRenderEffect(effect)
            }
            blurAnimator.start()
        }

        binding.logoContainer.animate()
            .alpha(0f)
            .scaleX(1.08f)
            .scaleY(1.08f)
            .setDuration(600L)
            .withEndAction {
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    overrideActivityTransition(
                        OVERRIDE_TRANSITION_OPEN,
                        android.R.anim.fade_in,
                        android.R.anim.fade_out
                    )
                } else {
                    @Suppress("DEPRECATION")
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                }
                finish()
            }
            .start()
    }
}