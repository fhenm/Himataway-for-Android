package com.github.fhenm.himataway.fragment.common

import com.github.fhenm.himataway.adapter.DataItemAdapter
import com.github.fhenm.himataway.adapter.RecyclerTweetAdapter
import com.github.fhenm.himataway.extensions.applyTapEvents
import com.github.fhenm.himataway.model.Row
import com.github.fhenm.himataway.viewmodel.ListBasedFragmentViewModel
import twitter4j.Status

abstract class TweetListBasedFragment<TViewModel: ListBasedFragmentViewModel<Unit, Status, Long>>
    : ListBasedFragment<Row, Unit, Status, Long, TViewModel>() {
    override val id: Unit
        get() = Unit

    override fun convertDataToViewItem(dataItem: Status): Row = Row.newStatus(dataItem)

    override fun createAdapter(): DataItemAdapter<Row> =
            RecyclerTweetAdapter(activity, ArrayList()).applyTapEvents(activity)
}