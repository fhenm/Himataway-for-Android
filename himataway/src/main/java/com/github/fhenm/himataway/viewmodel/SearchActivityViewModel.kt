package com.github.fhenm.himataway.viewmodel

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import com.github.fhenm.himataway.repository.TwitterRepository

/**
 * Created by h_okuyama on 2018/01/21.
 */
class SearchActivityViewModel(
    private val twitterRepo: TwitterRepository
) : ViewModel() {
    private val TAG = "SearchActivityViewModel"

    class Factory(
            private val twitterRepo: TwitterRepository
    ) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = SearchActivityViewModel(twitterRepo) as T
    }
}