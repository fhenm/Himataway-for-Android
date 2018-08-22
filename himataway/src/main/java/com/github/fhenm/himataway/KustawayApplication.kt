package com.github.fhenm.himataway

import android.app.Application
import android.content.res.Configuration
import android.graphics.Typeface
import android.os.StrictMode

import com.microsoft.appcenter.AppCenter
import com.microsoft.appcenter.analytics.Analytics
import com.microsoft.appcenter.crashes.Crashes

import com.github.fhenm.himataway.model.Relationship
import com.github.fhenm.himataway.model.TwitterManager
import com.github.fhenm.himataway.model.UserIconManager
import com.github.fhenm.himataway.repository.TwitterRepository
import com.github.fhenm.himataway.settings.BasicSettings
import com.github.fhenm.himataway.settings.MuteSettings
import com.github.fhenm.himataway.util.ImageUtil
import com.github.fhenm.himataway.BuildConfig

class himatawayApplication : Application() {

    /*var  twitterRepo: TwitterRepository? = null
        private set*/

    fun twitterRepo() = TwitterRepository(TwitterManager.getTwitter(), TwitterManager.getUserStreamAdapter())

    override fun onCreate() {
        super.onCreate()
        application = this

        /*AppCenter.start(this, BuildConfig.AppCenterAppSecret,
                Analytics::class.java, Crashes::class.java)*/

        // Twitter4J の user stream の shutdown() で NetworkOnMainThreadException が発生してしまうことに対する暫定対応
        if (!BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder().permitAll().build())
        }

        /**
         * 画像のキャッシュや角丸の設定を行う
         */
        ImageUtil.init()

        /**
         * 設定ファイル読み込み
         */
        MuteSettings.init()

        BasicSettings.init()

        UserIconManager.warmUpUserIconMap()

        Relationship.init()

        fontello = Typeface.createFromAsset(assets, "fontello.ttf")

        /*twitterRepo = TwitterRepository(
                TwitterManager.getTwitter(),
                TwitterManager.getUserStreamAdapter())*/
        twitterRepo();
    }

    /**
     * 終了時
     *
     * @see android.app.Application.onTerminate
     */
    override fun onTerminate() {
        super.onTerminate()
    }

    /**
     * 空きメモリ逼迫時
     *
     * @see android.app.Application.onLowMemory
     */
    override fun onLowMemory() {
        super.onLowMemory()
    }

    /**
     * 実行時において変更できるデバイスの設定時 ( 画面のオリエンテーション、キーボードの使用状態、および言語など )
     * https://sites.google.com/a/techdoctranslator.com/jp/android/guide/resources/runtime-changes
     *
     * @see android.app.Application.onConfigurationChanged
     */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
    }

    companion object {

        @JvmStatic
        var application: himatawayApplication? = null
            private set

        @JvmStatic
        var fontello: Typeface? = null
            private set

    }
}
