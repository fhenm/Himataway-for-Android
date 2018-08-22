package com.github.fhenm.himataway.viewmodel

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import com.github.fhenm.himataway.model.PagedResponseList
import com.github.fhenm.himataway.repository.TwitterRepository
import com.github.fhenm.himataway.settings.BasicSettings
import twitter4j.Status

/**
 * ユーザータイムライン画面の ViewModel
 */
class UserTimelineFragmentViewModel (
        private val twitterRepo: TwitterRepository,
        userId: Long
) : ListBasedFragmentViewModel<Long, Status, Long>(userId) {

    class Factory(
            private val twitterRepo: TwitterRepository,
            private val userId : Long
    ) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
                UserTimelineFragmentViewModel(twitterRepo, userId) as T
    }

    suspend override fun loadListItemsAsync(userId:Long, cursor: Long?): PagedResponseList<Status, Long> {

        val actualCursor = cursor ?: -1L
        val res = twitterRepo.loadUserTimeline(userId, actualCursor, BasicSettings.getPageCount());

        val nextCursor = res.lastOrNull { status ->
            actualCursor < 0L || actualCursor > status.id
        }?.id ?: -1L

        return PagedResponseList(res, true, nextCursor)
    }
}