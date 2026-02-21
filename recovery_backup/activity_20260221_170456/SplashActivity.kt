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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CountrySettingsManager.applySelectedLocale(applicationContext)
        window.setFlags(1024, 1024)
        setContentView(R.layout.activity_splash)

        versiontxt = findViewById(R.id.versiontxt)
        versiontxt.text = getVersion(applicationContext)

        Handler(Looper.getMainLooper()).postDelayed({
            if (AppPref.IsTermsAccept(this)) {
                startActivity(Intent(this, MainActivity::class.java))
            } else {
                startActivity(Intent(this, Disclosure::class.java))
            }
            finish()
        }, splashTimeout)
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
