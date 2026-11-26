package com.dev.rybez

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.webkit.WebViewAssetLoader
import com.yandex.mobile.ads.common.AdError
import com.yandex.mobile.ads.common.AdRequestConfiguration
import com.yandex.mobile.ads.common.AdRequestError
import com.yandex.mobile.ads.common.ImpressionData
import com.yandex.mobile.ads.common.MobileAds
import com.yandex.mobile.ads.rewarded.Reward
import com.yandex.mobile.ads.rewarded.RewardedAd
import com.yandex.mobile.ads.rewarded.RewardedAdEventListener
import com.yandex.mobile.ads.rewarded.RewardedAdLoadListener
import com.yandex.mobile.ads.rewarded.RewardedAdLoader

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private var rewardedAd: RewardedAd? = null
    private var rewardedAdLoader: RewardedAdLoader? = null
    private val adUnitId = "R-M-20508083-1"

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        hideSystemUI()

        MobileAds.initialize(this) {
            loadRewardedAd()
        }

        webView = WebView(this)
        setContentView(webView)

        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView,
                request: WebResourceRequest
            ): WebResourceResponse? {
                return assetLoader.shouldInterceptRequest(request.url)
            }
        }

        webView.addJavascriptInterface(AndroidBridge(), "AndroidBridge")
        webView.loadUrl("https://androidplatform.net")
    }

    private fun loadRewardedAd() {
        rewardedAdLoader = RewardedAdLoader(this).apply {
            setAdLoadListener(object : RewardedAdLoadListener {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                }
                override fun onAdFailedToLoad(error: AdRequestError) {
                    rewardedAd = null
                }
            })
        }
        rewardedAdLoader?.loadAd(AdRequestConfiguration.Builder(adUnitId).build())
    }

    inner class AndroidBridge {
        @JavascriptInterface
        fun showRewardedAd() {
            runOnUiThread {
                val ad = rewardedAd
                if (ad != null) {
                    var isRewarded = false
                    ad.setAdEventListener(object : RewardedAdEventListener {
                        override fun onAdShown() {}
                        override fun onAdFailedToShow(adError: AdError) {
                            rewardedAd = null
                            loadRewardedAd()
                            notifyJs(false)
                        }
                        override fun onAdDismissed() {
                            rewardedAd = null
                            loadRewardedAd()
                            notifyJs(isRewarded)
                        }
                        override fun onAdClicked() {}
                        override fun onAdImpression(impressionData: ImpressionData?) {}
                        override fun onRewarded(reward: Reward) {
                            isRewarded = true
                        }
                    })
                    ad.show(this@MainActivity)
                } else {
                    loadRewardedAd()
                    notifyJs(true)
                }
            }
        }
    }

    private fun notifyJs(isRewarded: Boolean) {
        webView.evaluateJavascript("window.onAndroidAdFinished($isRewarded);", null)
    }

    private fun hideSystemUI() {
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )
    }

    override fun onPause() {
        super.onPause()
        webView.onPause()
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
        hideSystemUI()
    }
}
