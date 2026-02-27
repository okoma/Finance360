package com.i2medier.financialpro.activity

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.i2medier.financialpro.R
import com.i2medier.financialpro.util.AppPref
import com.i2medier.financialpro.util.CountrySettingsManager

class SplashActivity : AppCompatActivity() {
    private val splashTimeout = 3000L
    private lateinit var versiontxt: TextView
    private var handler: Handler? = null
    private var navigationRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CountrySettingsManager.applySelectedLocale(applicationContext)
        window.setFlags(1024, 1024)
        setContentView(R.layout.activity_splash)

        versiontxt = findViewById(R.id.versiontxt)
        versiontxt.text = getVersion(applicationContext)

        scheduleNavigation()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        // When activity is brought to front (singleTask), navigate immediately
        // This handles the case when clicking app ad from within the app
        navigateToNextScreen()
    }

    private fun scheduleNavigation() {
        handler = Handler(Looper.getMainLooper())
        navigationRunnable = Runnable {
            navigateToNextScreen()
        }
        handler?.postDelayed(navigationRunnable!!, splashTimeout)
    }

    private fun navigateToNextScreen() {
        // Remove any pending navigation
        navigationRunnable?.let { handler?.removeCallbacks(it) }
        
        if (!isFinishing) {
            if (AppPref.IsTermsAccept(this)) {
                startActivity(Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                })
            } else {
                startActivity(Intent(this, Disclosure::class.java))
            }
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        navigationRunnable?.let { handler?.removeCallbacks(it) }
    }

    companion object {
        @JvmStatic
        fun getVersion(context: Context): String {
            var version = ""
            try {
                version = context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
            } catch (e: PackageManager.NameNotFoundException) {
                e.printStackTrace()
            }
            return "Version $version"
        }
    }
}
