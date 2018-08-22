package com.github.fhenm.himataway.viewmodel

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import com.github.fhenm.himataway.model.PagedResponseList
import com.github.fhenm.himataway.repository.TwitterRepository
import com.github.fhenm.himataway.settings.BasicSettings
import twitter4j.Status

class UserListFragmentViewModel(
        private val twitterRepo: TwitterRepository,
        private val userListId: Long
) : ListBasedFragmentViewModel<Long, Status, Long>(userListId) {

    class Factory(
            private val twitterRepo: TwitterRepository,
            private val userListId: Long
    ) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
                UserListFragmentViewModel(twitterRepo, userListId) as T
    }

    suspend override fun loadListItemsAsync(userListId: Long, cursor: Long?): PagedResponseList<Status, Long> {
        val actualCursor = cursor ?: -1L
        val res = twitterRepo.loadUserListStatuses(userListId, actualCursor, BasicSettings.getPageCount());

        val nextCursor = res.lastOrNull { status ->
            actualCursor < 0L || actualCursor > status.id
        }?.id ?: -1L

        return PagedResponseList(res, true, nextCursor)
    }

}