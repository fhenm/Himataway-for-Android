package com.github.fhenm.himataway.fragment.profile

import android.arch.lifecycle.ViewModelProviders
import com.github.fhenm.himataway.adapter.DataItemAdapter
import com.github.fhenm.himataway.adapter.RecyclerUserListAdapter
import com.github.fhenm.himataway.extensions.getTwitterRepo
import com.github.fhenm.himataway.fragment.common.ListBasedFragment
import com.github.fhenm.himataway.viewmodel.UserListMembershipsFragmentViewModel
import twitter4j.User
import twitter4j.UserList

/**
 * ユーザーの持つリスト一覧
 */
class UserListMembershipsFragment : ListBasedFragment<UserList, Long, UserList, Long, UserListMembershipsFragmentViewModel>() {
    override val id: Long
        get() = (arguments.getSerializable("user") as User).id

    override fun createViewModel(userId: Long): UserListMembershipsFragmentViewModel =
            ViewModelProviders
                    .of(this, UserListMembershipsFragmentViewModel.Factory(
                            this.getTwitterRepo(),
                            userId
                    ))
                    .get(UserListMembershipsFragmentViewModel::class.java)

    override fun createAdapter(): DataItemAdapter<UserList> =
            RecyclerUserListAdapter(context, ArrayList())

    override fun convertDataToViewItem(dataItem: UserList): UserList = dataItem
}
