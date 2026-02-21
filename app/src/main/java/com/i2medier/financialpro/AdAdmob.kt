package com.i2medier.financialpro

import android.app.Activity
import androidx.appcompat.app.AlertDialog
import android.content.Context
import android.util.Log
import android.widget.ProgressBar
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.view.ViewGroup
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import com.google.android.gms.ads.AdLoader

class AdAdmob(activity: Activity) {
    private val bannerAdID = "ca-app-pub-2171775430865158/3528890933"
    private val fullscreenAdID = "ca-app-pub-2171775430865158/2985099486"
    private val nativeAdID = "ca-app-pub-3940256099942544/2247696110"
    private var loadingDialog: AlertDialog? = null

    init {
        MobileAds.initialize(activity) {}
    }

    fun BannerAd(adLayout: RelativeLayout, activity: Activity) {
        // Banner ads are intentionally removed app-wide.
        adLayout.removeAllViews()
        adLayout.visibility = View.GONE
        attachCalculatorNativeAd(activity)
    }

    fun NativeAd(adLayout: FrameLayout, activity: Activity) {
        adLayout.removeAllViews()
        adLayout.visibility = View.GONE

        val adLoader = AdLoader.Builder(activity, nativeAdID)
            .forNativeAd { nativeAd ->
                val adView = activity.layoutInflater.inflate(
                    R.layout.item_native_ad,
                    adLayout,
                    false
                ) as NativeAdView
                bindNativeAd(adView, nativeAd)
                adLayout.removeAllViews()
                adLayout.addView(adView)
                adLayout.visibility = View.VISIBLE
            }
            .withNativeAdOptions(
                NativeAdOptions.Builder().build()
            )
            .withAdListener(object : com.google.android.gms.ads.AdListener() {
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    adLayout.removeAllViews()
                    adLayout.visibility = View.GONE
                    Log.w("AdAdmob", "Native ad failed: ${loadAdError.message}")
                }
            })
            .build()

        adLoader.loadAd(AdRequest.Builder().build())
    }

    private fun bindNativeAd(adView: NativeAdView, nativeAd: NativeAd) {
        val iconView = adView.findViewById<ImageView?>(R.id.ad_icon)
        val headlineView = adView.findViewById<TextView?>(R.id.ad_headline)
        val bodyView = adView.findViewById<TextView?>(R.id.ad_body)
        val callToAction = adView.findViewById<Button?>(R.id.ad_call_to_action)

        if (headlineView == null) {
            Log.w("AdAdmob", "Native ad layout missing ad_headline; skip binding.")
            return
        }

        if (iconView != null) {
            val icon = nativeAd.icon
            if (icon?.drawable != null) {
                iconView.setImageDrawable(icon.drawable)
                iconView.visibility = View.VISIBLE
            } else {
                iconView.visibility = View.GONE
            }
            adView.iconView = iconView
        }

        headlineView.text = nativeAd.headline
        adView.headlineView = headlineView

        if (bodyView != null) {
            if (nativeAd.body.isNullOrBlank()) {
                bodyView.visibility = View.GONE
            } else {
                bodyView.text = nativeAd.body
                bodyView.visibility = View.VISIBLE
            }
            adView.bodyView = bodyView
        }

        if (callToAction != null) {
            if (nativeAd.callToAction.isNullOrBlank()) {
                callToAction.visibility = View.INVISIBLE
            } else {
                callToAction.text = nativeAd.callToAction
                callToAction.visibility = View.VISIBLE
            }
            adView.callToActionView = callToAction
        }

        adView.setNativeAd(nativeAd)
    }

    private fun attachCalculatorNativeAd(activity: Activity) {
        val root = activity.findViewById<ViewGroup>(android.R.id.content) ?: return
        val explanationCard = findExplanationCard(root) ?: return
        val parent = explanationCard.parent as? ViewGroup ?: return

        val existingMediumContainer = parent.findViewById<FrameLayout>(R.id.calculatorNativeAd300x250Container)
        val mediumAdContainer = existingMediumContainer ?: FrameLayout(activity).apply {
            id = R.id.calculatorNativeAd300x250Container
            visibility = View.GONE
            layoutParams = createAdLayoutParams(explanationCard)
        }

        if (existingMediumContainer == null) {
            val index = parent.indexOfChild(explanationCard)
            parent.addView(mediumAdContainer, (index + 1).coerceAtMost(parent.childCount))
        }

        var mediumAdRequested = false
        val refreshAdVisibility: () -> Unit = {
            val shouldShow = explanationCard.visibility == View.VISIBLE
            if (shouldShow) {
                if (!mediumAdRequested) {
                    mediumAdRequested = true
                    loadCalculatorNativeAd(
                        adLayout = mediumAdContainer,
                        activity = activity,
                        layoutRes = R.layout.item_native_calculator_ad_300x250
                    )
                }
            } else {
                mediumAdContainer.visibility = View.GONE
            }
        }

        explanationCard.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            refreshAdVisibility()
        }
        refreshAdVisibility()
    }

    private fun loadCalculatorNativeAd(adLayout: FrameLayout, activity: Activity, layoutRes: Int) {
        adLayout.removeAllViews()
        adLayout.visibility = View.GONE

        val adLoader = AdLoader.Builder(activity, nativeAdID)
            .forNativeAd { nativeAd ->
                val adView = activity.layoutInflater.inflate(
                    layoutRes,
                    adLayout,
                    false
                ) as NativeAdView
                bindNativeAd(adView, nativeAd)
                adLayout.removeAllViews()
                adLayout.addView(adView)
                adLayout.visibility = View.VISIBLE
            }
            .withNativeAdOptions(
                NativeAdOptions.Builder().build()
            )
            .withAdListener(object : com.google.android.gms.ads.AdListener() {
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    adLayout.removeAllViews()
                    adLayout.visibility = View.GONE
                    Log.w("AdAdmob", "Calculator native ad failed: ${loadAdError.message}")
                }
            })
            .build()
        adLoader.loadAd(AdRequest.Builder().build())
    }

    private fun createAdLayoutParams(anchorView: View): ViewGroup.LayoutParams {
        return ViewGroup.MarginLayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun findExplanationCard(root: View): View? {
        if (root.id != View.NO_ID) {
            val entry = runCatching {
                root.resources.getResourceEntryName(root.id)
            }.getOrNull()
            if (entry != null && entry.contains("ExplanationCard", ignoreCase = true)) {
                return root
            }
        }
        if (root is ViewGroup) {
            for (i in 0 until root.childCount) {
                val found = findExplanationCard(root.getChildAt(i))
                if (found != null) return found
            }
        }
        return null
    }

    fun FullscreenAd(activity: Activity) {
        adPopup(activity)

        InterstitialAd.load(
            activity,
            fullscreenAdID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    interstitialAd.show(activity)
                    loadingDialog?.dismiss()
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    loadingDialog?.dismiss()
                }
            }
        )
    }

    private fun adPopup(context: Context) {
        val progressBar = ProgressBar(context).apply { isIndeterminate = true }
        loadingDialog = AlertDialog.Builder(context)
            .setTitle("Loading Ad")
            .setView(progressBar)
            .setCancelable(true)
            .create()
        loadingDialog?.show()
    }
}
