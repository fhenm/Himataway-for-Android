package com.github.fhenm.himataway

import android.content.Context
import android.content.Intent
import android.databinding.DataBindingUtil
import android.media.AudioManager
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.Window
import android.widget.ProgressBar
import android.widget.VideoView

import com.github.fhenm.himataway.databinding.ActivityVideoBinding
import com.github.fhenm.himataway.model.TwitterManager
import com.github.fhenm.himataway.util.MessageUtil
import com.github.fhenm.himataway.util.StatusUtil
import com.github.fhenm.himataway.R

import java.util.regex.Pattern

import twitter4j.Status

class VideoActivity : AppCompatActivity() {

    private lateinit var player: VideoView

    private lateinit var guruguru: ProgressBar

    internal var musicWasPlaying = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestWindowFeature(Window.FEATURE_NO_TITLE)

        val binding = DataBindingUtil.setContentView<ActivityVideoBinding>(this, R.layout.activity_video)
        player = binding.player
        guruguru = binding.guruguru
        binding.cover.setOnClickListener { v -> finish() }

        val intent = intent
        val args = intent.extras
        if (args == null) {
            MessageUtil.showToast("Missing Bundle in Intent")
            finish()
            return
        }

        val statusUrl = args.getString("statusUrl")
        if (statusUrl != null && !statusUrl.isEmpty()) {
            val pattern = Pattern.compile("https?://twitter\\.com/\\w+/status/(\\d+)/video/(\\d+)/?.*")
            val matcher = pattern.matcher(statusUrl)
            if (matcher.find()) {
                val statusId = java.lang.Long.valueOf(matcher.group(1))
                object : AsyncTask<Void, Void, Status>() {
                    override fun doInBackground(vararg params: Void): twitter4j.Status? {
                        try {
                            return TwitterManager.getTwitter().showStatus(statusId!!)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            return null
                        }

                    }

                    override fun onPostExecute(status: twitter4j.Status?) {
                        if (status != null) {
                            val videoUrl = StatusUtil.getVideoUrl(status)
                            if (videoUrl != null && !videoUrl.isEmpty()) {
                                setVideoURI(videoUrl)
                            }
                        }
                    }
                }.execute()
                return
            }
        }
        val videoUrl = args.getString("videoUrl")

        if (videoUrl == null) {
            MessageUtil.showToast("Missing videoUrl in Bundle")
            finish()
            return
        }

        setVideoURI(videoUrl)
    }

    private fun setVideoURI(videoUrl: String?) {
        musicWasPlaying = (getSystemService(Context.AUDIO_SERVICE) as AudioManager).isMusicActive

        guruguru.visibility = View.VISIBLE
        player.setOnTouchListener { view, motionEvent ->
            finish()
            false
        }
        player.setOnPreparedListener { guruguru.visibility = View.GONE }
        player.setOnCompletionListener {
            player.seekTo(0)
            player.start()
        }
        player.setVideoURI(Uri.parse(videoUrl))
        player.start()
    }

    override fun onDestroy() {
        if (musicWasPlaying) {
            val i = Intent("com.android.music.musicservicecommand")
            i.putExtra("command", "play")
            sendBroadcast(i)
        }
        super.onDestroy()
    }
}
