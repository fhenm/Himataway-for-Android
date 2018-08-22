package com.github.fhenm.himataway.viewmodel

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import com.github.fhenm.himataway.model.PagedResponseList
import com.github.fhenm.himataway.repository.TwitterRepository
import twitter4j.User

/**
 * リストのユーザー一覧画面の ViewModel
 */
class UserMemberFragmentViewModel (
        private val twitterRepo: TwitterRepository,
        listId: Long
) : ListBasedFragmentViewModel<Long, User, Long>(listId) {

    class Factory(
            private val twitterRepo: TwitterRepository,
            private val userId: Long
    ) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
                UserMemberFragmentViewModel(twitterRepo, userId) as T
    }

    suspend override fun loadListItemsAsync(listId:Long, cursor: Long?): PagedResponseList<User, Long> {
        val res = twitterRepo.loadUserListMembers(listId, cursor ?: -1L);
        return PagedResponseList(res, res.hasNext(), res.nextCursor)
    }
}