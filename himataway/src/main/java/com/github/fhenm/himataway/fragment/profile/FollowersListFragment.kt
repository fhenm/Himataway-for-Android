package com.github.fhenm.himataway.fragment.profile

import android.arch.lifecycle.ViewModelProviders
import com.github.fhenm.himataway.adapter.DataItemAdapter
import com.github.fhenm.himataway.adapter.RecyclerUserAdapter
import com.github.fhenm.himataway.extensions.getTwitterRepo
import com.github.fhenm.himataway.fragment.common.ListBasedFragment
import com.github.fhenm.himataway.viewmodel.FollowersListFragmentViewModel
import twitter4j.User

/**
 * フォロワー一覧
 */
class FollowersListFragment : ListBasedFragment<User, Long, User, Long, FollowersListFragmentViewModel>() {
    override val id: Long
        get() = (arguments!!.getSerializable("user") as User).id

    override fun createViewModel(userId: Long): FollowersListFragmentViewModel =
        ViewModelProviders
                .of(this, FollowersListFragmentViewModel.Factory(
                        this.getTwitterRepo(),
                        userId
                ))
                .get(FollowersListFragmentViewModel::class.java)

    override fun createAdapter(): DataItemAdapter<User> =
            RecyclerUserAdapter(activity!!, ArrayList())

    override fun convertDataToViewItem(dataItem: User): User = dataItem
}

