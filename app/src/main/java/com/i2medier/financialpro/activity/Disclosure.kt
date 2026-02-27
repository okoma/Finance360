package com.i2medier.financialpro.activity

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import com.i2medier.financialpro.AdAdmob
import com.i2medier.financialpro.R
import com.i2medier.financialpro.util.AppConstant
import com.i2medier.financialpro.util.AppPref

class Disclosure : AppCompatActivity(), View.OnClickListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_disclosure)

        val adAdmob = AdAdmob(this)
        adAdmob.BannerAd(findViewById<RelativeLayout>(R.id.banner))
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.agreeAndContinue -> {
                AppPref.setIsTermsAccept(this, true)
                goToMainScreen()
            }
            R.id.privacyPolicy -> AppConstant.openUrl(this, AppConstant.PRIVACY_POLICY_URL)
            R.id.termsOfService -> AppConstant.openUrl(this, AppConstant.TERMS_OF_SERVICE_URL)
            R.id.userAgreement -> agreeAndContinueDialog()
        }
    }

    private fun agreeAndContinueDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setMessage(AppConstant.DISCLOSURE_DIALOG_DESC)
        builder.setCancelable(false)
        builder.setPositiveButton("Ok") { _, _ -> }
        builder.show()
    }

    private fun goToMainScreen() {
        try {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
