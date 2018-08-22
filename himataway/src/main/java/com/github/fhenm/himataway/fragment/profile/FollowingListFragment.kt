package com.github.fhenm.himataway.fragment.profile

import android.arch.lifecycle.ViewModelProviders
import com.github.fhenm.himataway.adapter.DataItemAdapter
import com.github.fhenm.himataway.adapter.RecyclerUserAdapter
import com.github.fhenm.himataway.extensions.getTwitterRepo
import com.github.fhenm.himataway.fragment.common.ListBasedFragment
import com.github.fhenm.himataway.viewmodel.FollowingListFragmentViewModel
import twitter4j.User

/**
 * フォロー一覧
 */
class FollowingListFragment : ListBasedFragment<User, Long, User, Long, FollowingListFragmentViewModel>() {
    override val id: Long
        get() = (arguments.getSerializable("user") as User).id

    override fun createViewModel(userId: Long): FollowingListFragmentViewModel =
            ViewModelProviders
                    .of(this, FollowingListFragmentViewModel.Factory(
                            this.getTwitterRepo(),
                            userId
                    ))
                    .get(FollowingListFragmentViewModel::class.java)

    override fun createAdapter(): DataItemAdapter<User> =
            RecyclerUserAdapter(activity, ArrayList())

    override fun convertDataToViewItem(dataItem: User): User = dataItem
}
