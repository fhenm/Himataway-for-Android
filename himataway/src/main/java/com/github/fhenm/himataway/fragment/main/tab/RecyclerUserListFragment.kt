package com.github.fhenm.himataway.fragment.main.tab

import android.arch.lifecycle.ViewModelProviders
import com.github.fhenm.himataway.adapter.DataItemAdapter
import com.github.fhenm.himataway.adapter.RecyclerTweetAdapter
import com.github.fhenm.himataway.extensions.applyTapEvents
import com.github.fhenm.himataway.extensions.getTwitterRepo
import com.github.fhenm.himataway.fragment.common.ListBasedFragment
import com.github.fhenm.himataway.model.Row
import com.github.fhenm.himataway.viewmodel.UserListFragmentViewModel
import twitter4j.Status

class RecyclerUserListFragment : ListBasedFragment<Row, Long, Status, Long, UserListFragmentViewModel>() {

    override val id: Long
        get() = arguments.getLong("userListId")

    override fun createAdapter(): DataItemAdapter<Row> =
            RecyclerTweetAdapter(activity, ArrayList()).applyTapEvents(activity)

    override fun convertDataToViewItem(dataItem: Status): Row = Row.newStatus(dataItem)

    override fun createViewModel(userListId: Long): UserListFragmentViewModel =
            ViewModelProviders
                    .of(this, UserListFragmentViewModel.Factory(
                            this.getTwitterRepo(),
                            userListId
                    ))
                    .get(UserListFragmentViewModel::class.java)
}