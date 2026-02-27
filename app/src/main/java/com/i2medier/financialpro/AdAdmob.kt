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
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.TextView
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.*

class AdAdmob(private val activity: Activity) {

    // AdMob IDs
    private val bannerAdID = "ca-app-pub-2171775430865158/3528890933"
    private val fullscreenAdID = "ca-app-pub-2171775430865158/2985099486"
    private val nativeAdID = "ca-app-pub-2171775430865158/8962910629"

    private var loadingDialog: AlertDialog? = null

    init {
        // Initialize AdMob once
        MobileAds.initialize(activity)
    }

    /**
     * Banner ads are disabled app-wide.
     * We instead attach native calculator ad.
     */
    fun BannerAd(adLayout: ViewGroup) {
        adLayout.removeAllViews()
        adLayout.visibility = View.GONE
        waitForExplanationCardThenAttach(activity)
    }

    /**
     * Material 3 native binding (Media + Full CTA)
     */
    private fun bindNativeAd(adView: NativeAdView, nativeAd: NativeAd) {

        val mediaView = adView.findViewById<MediaView>(R.id.ad_media)
        val headline = adView.findViewById<TextView>(R.id.ad_headline)
        val body = adView.findViewById<TextView>(R.id.ad_body)
        val cta = adView.findViewById<Button>(R.id.ad_call_to_action)

        mediaView?.let { adView.mediaView = it }

        headline.text = nativeAd.headline
        adView.headlineView = headline

        body?.let {
            if (nativeAd.body.isNullOrBlank()) {
                it.visibility = View.GONE
            } else {
                it.text = nativeAd.body
                it.visibility = View.VISIBLE
            }
            adView.bodyView = it
        }

        cta?.let {
            if (nativeAd.callToAction.isNullOrBlank()) {
                it.visibility = View.GONE
            } else {
                it.text = nativeAd.callToAction
                it.visibility = View.VISIBLE
            }
            adView.callToActionView = it
        }

        adView.setNativeAd(nativeAd)
    }

    /**
     * Inject after ExplanationCard
     */
    private fun attachCalculatorNativeAd(activity: Activity): Boolean {

        val root = activity.findViewById<ViewGroup>(android.R.id.content) ?: return false
        val explanationCard = findExplanationCard(root) ?: return false
        if (explanationCard.visibility != View.VISIBLE) return false
        val parent = explanationCard.parent as? ViewGroup ?: return false

        val existing = parent.findViewById<FrameLayout>(R.id.calculatorNativeAd300x250Container)
        if (existing != null) {
            if (existing.childCount == 0) {
                existing.visibility = View.VISIBLE
                loadCalculatorNativeAd(existing, activity)
            }
            return true
        }

        val container = FrameLayout(activity).apply {
            id = R.id.calculatorNativeAd300x250Container
            visibility = View.VISIBLE
            layoutParams = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 24
                bottomMargin = 24
            }
        }

        parent.addView(container, parent.indexOfChild(explanationCard) + 1)

        loadCalculatorNativeAd(container, activity)
        return true
    }

    private fun waitForExplanationCardThenAttach(activity: Activity) {
        if (attachCalculatorNativeAd(activity)) return
        val root = activity.findViewById<ViewGroup>(android.R.id.content) ?: return
        val observer = root.viewTreeObserver ?: return
        val listener = object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                if (attachCalculatorNativeAd(activity)) {
                    if (root.viewTreeObserver.isAlive) {
                        root.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    }
                }
            }
        }
        observer.addOnGlobalLayoutListener(listener)
    }

    /**
     * Loads shimmer → native → collapse if no fill
     */
    private fun loadCalculatorNativeAd(adLayout: FrameLayout, activity: Activity) {

        adLayout.removeAllViews()

        // ---- SHIMMER PLACEHOLDER ----
        val shimmer = activity.layoutInflater.inflate(
            R.layout.item_native_shimmer,
            adLayout,
            false
        )

        adLayout.addView(shimmer)

        val adLoader = AdLoader.Builder(activity, nativeAdID)
            .forNativeAd { nativeAd ->

                val adView = activity.layoutInflater.inflate(
                    R.layout.item_native_calculator_ad_300x250,
                    adLayout,
                    false
                ) as NativeAdView

                bindNativeAd(adView, nativeAd)

                adLayout.removeAllViews()
                adLayout.addView(adView)

                // Fade in animation
                adView.alpha = 0f
                adView.animate().alpha(1f).setDuration(250).start()
            }
            .withNativeAdOptions(NativeAdOptions.Builder().build())
            .withAdListener(object : AdListener() {

                override fun onAdFailedToLoad(error: LoadAdError) {
                    // Collapse completely on no-fill
                    adLayout.removeAllViews()
                    adLayout.visibility = View.GONE
                    Log.w("AdAdmob", "Native no-fill: ${error.message}")
                }
            })
            .build()

        adLoader.loadAd(AdRequest.Builder().build())
    }

    /**
     * Backward-compatible inline native ad API used by list screens.
     */
    fun NativeAd(adLayout: FrameLayout, activity: Activity = this.activity) {
        adLayout.removeAllViews()
        adLayout.visibility = View.VISIBLE

        val adLoader = AdLoader.Builder(activity, nativeAdID)
            .forNativeAd { nativeAd ->
                val adView = activity.layoutInflater.inflate(
                    R.layout.item_native_ad,
                    adLayout,
                    false
                ) as NativeAdView

                bindInlineNativeAd(adView, nativeAd)
                adLayout.removeAllViews()
                adLayout.addView(adView)
            }
            .withNativeAdOptions(NativeAdOptions.Builder().build())
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    adLayout.removeAllViews()
                    adLayout.visibility = View.GONE
                    Log.w("AdAdmob", "Inline native no-fill: ${error.message}")
                }
            })
            .build()

        adLoader.loadAd(AdRequest.Builder().build())
    }

    private fun bindInlineNativeAd(adView: NativeAdView, nativeAd: NativeAd) {
        val headline = adView.findViewById<TextView>(R.id.ad_headline)
        val body = adView.findViewById<TextView>(R.id.ad_body)
        val cta = adView.findViewById<Button>(R.id.ad_call_to_action)
        val icon = adView.findViewById<ImageView>(R.id.ad_icon)

        headline.text = nativeAd.headline
        adView.headlineView = headline

        if (nativeAd.body.isNullOrBlank()) {
            body.visibility = View.GONE
        } else {
            body.text = nativeAd.body
            body.visibility = View.VISIBLE
        }
        adView.bodyView = body

        if (nativeAd.callToAction.isNullOrBlank()) {
            cta.visibility = View.GONE
        } else {
            cta.text = nativeAd.callToAction
            cta.visibility = View.VISIBLE
        }
        adView.callToActionView = cta

        val iconDrawable = nativeAd.icon?.drawable
        if (iconDrawable != null) {
            icon.setImageDrawable(iconDrawable)
            icon.visibility = View.VISIBLE
            adView.iconView = icon
        } else {
            icon.visibility = View.GONE
        }

        adView.setNativeAd(nativeAd)
    }

    /**
     * Finds ExplanationCard recursively
     */
    private fun findExplanationCard(root: View): View? {

        if (root.id != View.NO_ID) {
            val name = runCatching {
                root.resources.getResourceEntryName(root.id)
            }.getOrNull()

            if (name?.contains("ExplanationCard", true) == true) return root
        }

        if (root is ViewGroup) {
            for (i in 0 until root.childCount) {
                val found = findExplanationCard(root.getChildAt(i))
                if (found != null) return found
            }
        }

        return null
    }

    /**
     * Interstitial
     */
    fun FullscreenAd(activity: Activity) {

        adPopup(activity)

        InterstitialAd.load(
            activity,
            fullscreenAdID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {

                override fun onAdLoaded(ad: InterstitialAd) {
                    ad.show(activity)
                    loadingDialog?.dismiss()
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    loadingDialog?.dismiss()
                }
            }
        )
    }

    private fun adPopup(context: Context) {

        val bar = ProgressBar(context)

        loadingDialog = AlertDialog.Builder(context)
            .setTitle("Loading Ad")
            .setView(bar)
            .setCancelable(true)
            .create()

        loadingDialog?.show()
    }
}
