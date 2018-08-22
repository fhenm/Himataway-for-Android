package com.github.fhenm.himataway.viewmodel

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import com.github.fhenm.himataway.model.PagedResponseList
import com.github.fhenm.himataway.repository.TwitterRepository
import com.github.fhenm.himataway.settings.BasicSettings
import twitter4j.DirectMessage

class MyDirectMessageFragmentViewModel(
        private val twitterRepo: TwitterRepository
) : ListBasedFragmentViewModel<Unit, DirectMessage, Long>(Unit) {

    class Factory(
            private val twitterRepo: TwitterRepository
    ) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
                MyDirectMessageFragmentViewModel(twitterRepo) as T
    }

    suspend override fun loadListItemsAsync(dummy: Unit, cursor: Long?): PagedResponseList<DirectMessage, Long> {

        val actualCursor = cursor ?: -1L
        val res = twitterRepo.loadMyDirectMessages(actualCursor, BasicSettings.getPageCount());

        val nextCursor = res.lastOrNull { status ->
            actualCursor < 0L || actualCursor > status.id
        }?.id ?: -1L

        return PagedResponseList(res, true, nextCursor)
    }

}