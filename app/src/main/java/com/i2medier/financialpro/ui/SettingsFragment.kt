package com.i2medier.financialpro.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.i2medier.financialpro.R
import com.i2medier.financialpro.activity.LegalInfoActivity
import com.i2medier.financialpro.util.AppConstant
import com.i2medier.financialpro.util.CountrySettingsManager

class SettingsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_settings, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val countryListContainer = view.findViewById<LinearLayout>(R.id.layoutCountryList)
        bindCountrySelector(countryListContainer)
        view.findViewById<TextView>(R.id.tvItemAbout)?.text = getString(R.string.about_us_title)
        view.findViewById<View>(R.id.itemShare).setOnClickListener { shareApp() }
        view.findViewById<View>(R.id.itemRate).setOnClickListener { rateApp() }
        view.findViewById<View>(R.id.itemPrivacy).setOnClickListener {
            startActivity(LegalInfoActivity.privacyIntent(requireContext()))
        }
        view.findViewById<View>(R.id.itemTerms).setOnClickListener {
            startActivity(LegalInfoActivity.termsIntent(requireContext()))
        }
        view.findViewById<View>(R.id.itemAbout).setOnClickListener {
            startActivity(LegalInfoActivity.aboutIntent(requireContext()))
        }
    }

    private fun bindCountrySelector(container: LinearLayout) {
        container.removeAllViews()
        val countries = CountrySettingsManager.getSupportedCountries()
        val current = CountrySettingsManager.getSelectedCountry(requireContext())
        val checkBoxes = mutableListOf<CheckBox>()

        countries.forEachIndexed { index, option ->
            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                val pad = (14 * resources.displayMetrics.density).toInt()
                setPadding(pad, (10 * resources.displayMetrics.density).toInt(), pad, (10 * resources.displayMetrics.density).toInt())
            }

            val title = TextView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                text = "${countryFlagEmoji(option.countryCode)}  ${option.countryName}"
                textSize = 15f
                setTextColor(resources.getColor(R.color.colorDark, requireContext().theme))
            }

            val check = CheckBox(requireContext()).apply {
                isChecked = option.countryCode == current.countryCode
                isClickable = false
            }
            checkBoxes.add(check)

            row.setOnClickListener {
                checkBoxes.forEachIndexed { i, box -> box.isChecked = i == index }
                CountrySettingsManager.setSelectedCountry(requireContext(), option)
                Toast.makeText(
                    requireContext(),
                    getString(R.string.country_changed_format, option.countryName, option.currencyCode),
                    Toast.LENGTH_SHORT
                ).show()
                activity?.recreate()
            }

            row.addView(title)
            row.addView(check)
            container.addView(row)
        }
    }

    private fun countryFlagEmoji(countryCode: String): String {
        val code = countryCode.uppercase()
        if (code.length != 2) return countryCode
        val first = Character.codePointAt(code, 0) - 0x41 + 0x1F1E6
        val second = Character.codePointAt(code, 1) - 0x41 + 0x1F1E6
        return String(Character.toChars(first)) + String(Character.toChars(second))
    }

    private fun shareApp() {
        try {
            val appLink = "https://play.google.com/store/apps/details?id=${requireContext().packageName}"
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name))
                putExtra(Intent.EXTRA_TEXT, getString(R.string.app_name) + "\n" + appLink)
            }
            startActivity(Intent.createChooser(intent, "Share via"))
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Unable to share app", Toast.LENGTH_SHORT).show()
        }
    }

    private fun rateApp() {
        val packageName = requireContext().packageName
        val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NO_HISTORY or
                    Intent.FLAG_ACTIVITY_NEW_DOCUMENT or
                    Intent.FLAG_ACTIVITY_MULTIPLE_TASK
            )
        }
        try {
            startActivity(marketIntent)
        } catch (e: ActivityNotFoundException) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")))
        }
    }
}
