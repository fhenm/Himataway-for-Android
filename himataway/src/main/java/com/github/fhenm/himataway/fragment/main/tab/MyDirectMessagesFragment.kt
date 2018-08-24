package com.github.fhenm.himataway.fragment.main.tab

import android.arch.lifecycle.ViewModelProviders
import com.github.fhenm.himataway.adapter.DataItemAdapter
import com.github.fhenm.himataway.adapter.RecyclerTweetAdapter
import com.github.fhenm.himataway.extensions.applyTapEvents
import com.github.fhenm.himataway.extensions.getTwitterRepo
import com.github.fhenm.himataway.fragment.common.ListBasedFragment
import com.github.fhenm.himataway.model.Row
import com.github.fhenm.himataway.viewmodel.MyDirectMessageFragmentViewModel
import twitter4j.DirectMessage

class MyDirectMessagesFragment : ListBasedFragment<Row, Unit, DirectMessage, Long, MyDirectMessageFragmentViewModel>() {

    override val id: Unit
        get() = Unit

    override fun createAdapter(): DataItemAdapter<Row> =
            RecyclerTweetAdapter(activity!!, ArrayList()).applyTapEvents(activity!!)

    override fun convertDataToViewItem(dataItem: DirectMessage): Row = Row.newDirectMessage(dataItem)

    override fun createViewModel(dummy: Unit): MyDirectMessageFragmentViewModel =
            ViewModelProviders
                    .of(this, MyDirectMessageFragmentViewModel.Factory(
                            this.getTwitterRepo()
                    ))
                    .get(MyDirectMessageFragmentViewModel::class.java)
}