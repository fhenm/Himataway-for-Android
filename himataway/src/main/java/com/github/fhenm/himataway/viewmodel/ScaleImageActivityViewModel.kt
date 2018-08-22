package com.github.fhenm.himataway.viewmodel

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import android.content.Intent
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import com.github.fhenm.himataway.repository.TwitterRepository
import twitter4j.Status
import java.util.regex.Pattern

class ScaleImageActivityViewModel(
        private val twitterRepo: TwitterRepository
        ) : ViewModel() {

    class Factory(
            private val twitterRepo: TwitterRepository
    ) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = ScaleImageActivityViewModel(twitterRepo) as T
    }

    private val _statusWithIndex = MutableLiveData<Pair<Status, Int>>()
    val statusWithIndex : LiveData<Pair<Status, Int>> = _statusWithIndex

    fun loadImage(intent: Intent) {
        // Intent から 1:StatusID, 2:Status, 3:Index を得る
        val tri : Triple<Long?, Status?, Int> = intent.let { i ->
            if (Intent.ACTION_VIEW == i.action) {
                // インテント経由での起動をサポート
                val url = i.data ?: return
                val statusId = getStatusIdFromUrl(url.toString()) ?: return
                Triple(statusId, null, 0)
            } else {
                val args = i.extras ?: return
                val index = args.getInt("index", 0)

                if (args.containsKey("status")) {
                    val status = args.getSerializable("status") as Status
                    Triple(null, status, index)
                } else {
                    val url = args.getString("url")
                    val statusId = getStatusIdFromUrl(url.toString()) ?: return
                    Triple(statusId, null, index)
                }
            }
        }

        if (tri.second != null) {
            _statusWithIndex.postValue(Pair(tri.second!!, tri.third))
        } else if (tri.first != null) {
            launch (UI) {
                val status = twitterRepo.loadStatus(tri.first!!)
                _statusWithIndex.postValue(Pair(status, tri.third))
            }
        }
    }

    private fun getStatusIdFromUrl(url: String) : Long? {
        val pattern = Pattern.compile("https?://twitter\\.com/\\w+/status/(\\d+)/photo/(\\d+)/?.*")
        val matcher = pattern.matcher(url)
        if (matcher.find()) {
            val statusId = java.lang.Long.valueOf(matcher.group(1))
            return statusId
        } else {
            return null
        }
    }
}