package com.i2medier.financialpro.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.text.HtmlCompat
import com.i2medier.financialpro.R

class LegalInfoActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_PAGE = "extra_page"
        private const val PAGE_ABOUT = "about"
        private const val PAGE_PRIVACY = "privacy"
        private const val PAGE_TERMS = "terms"

        fun aboutIntent(context: Context): Intent =
            Intent(context, LegalInfoActivity::class.java).putExtra(EXTRA_PAGE, PAGE_ABOUT)

        fun privacyIntent(context: Context): Intent =
            Intent(context, LegalInfoActivity::class.java).putExtra(EXTRA_PAGE, PAGE_PRIVACY)

        fun termsIntent(context: Context): Intent =
            Intent(context, LegalInfoActivity::class.java).putExtra(EXTRA_PAGE, PAGE_TERMS)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_legal_info)

        val toolbar = findViewById<Toolbar>(R.id.toolBar)
        val titleView = findViewById<TextView>(R.id.tvPageTitle)
        val bodyView = findViewById<TextView>(R.id.tvPageBody)

        val page = intent.getStringExtra(EXTRA_PAGE)
        val titleRes = when (page) {
            PAGE_PRIVACY -> R.string.privacy_policy_title
            PAGE_TERMS -> R.string.terms_conditions_title
            else -> R.string.about_us_title
        }
        val bodyRes = when (page) {
            PAGE_PRIVACY -> R.string.privacy_policy_body_html
            PAGE_TERMS -> R.string.terms_conditions_body_html
            else -> R.string.about_us_body_html
        }

        toolbar.title = getString(titleRes)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        titleView.text = getString(titleRes)
        bodyView.text = HtmlCompat.fromHtml(getString(bodyRes), HtmlCompat.FROM_HTML_MODE_LEGACY)
        bodyView.movementMethod = LinkMovementMethod.getInstance()
    }
}
