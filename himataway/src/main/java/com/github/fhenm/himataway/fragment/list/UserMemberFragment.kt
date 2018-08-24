package com.github.fhenm.himataway.fragment.list

import android.arch.lifecycle.ViewModelProviders
import com.github.fhenm.himataway.adapter.DataItemAdapter
import com.github.fhenm.himataway.adapter.RecyclerUserAdapter
import com.github.fhenm.himataway.extensions.getTwitterRepo
import com.github.fhenm.himataway.fragment.common.ListBasedFragment
import com.github.fhenm.himataway.viewmodel.UserMemberFragmentViewModel
import twitter4j.User

class UserMemberFragment : ListBasedFragment<User, Long, User, Long, UserMemberFragmentViewModel>() {
    override val id: Long
        get() = arguments!!.getLong("listId")

    override fun createViewModel(listId: Long): UserMemberFragmentViewModel =
            ViewModelProviders
                    .of(this, UserMemberFragmentViewModel.Factory(
                            this.getTwitterRepo(),
                            listId
                    ))
                    .get(UserMemberFragmentViewModel::class.java)

    override fun createAdapter(): DataItemAdapter<User> =
            RecyclerUserAdapter(context!!, ArrayList())

    override fun convertDataToViewItem(dataItem: User): User = dataItem
}
