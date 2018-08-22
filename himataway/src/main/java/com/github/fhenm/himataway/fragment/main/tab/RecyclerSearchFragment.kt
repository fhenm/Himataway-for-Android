package com.github.fhenm.himataway.fragment.main.tab

import android.arch.lifecycle.ViewModelProviders
import com.github.fhenm.himataway.adapter.DataItemAdapter
import com.github.fhenm.himataway.adapter.RecyclerTweetAdapter
import com.github.fhenm.himataway.extensions.applyTapEvents
import com.github.fhenm.himataway.extensions.getTwitterRepo
import com.github.fhenm.himataway.fragment.common.ListBasedFragment
import com.github.fhenm.himataway.model.Row
import com.github.fhenm.himataway.viewmodel.SearchFragmentViewModel
import twitter4j.Query
import twitter4j.Status

class RecyclerSearchFragment : ListBasedFragment<Row, String, Status, Query, SearchFragmentViewModel>() {

    override val id: String
        get() = arguments.getString("searchWord")

    override fun createAdapter(): DataItemAdapter<Row> =
            RecyclerTweetAdapter(activity, ArrayList()).applyTapEvents(activity)

    override fun convertDataToViewItem(dataItem: Status): Row = Row.newStatus(dataItem)

    override fun createViewModel(keyword: String): SearchFragmentViewModel =
            ViewModelProviders
                    .of(this, SearchFragmentViewModel.Factory(
                            this.getTwitterRepo(),
                            keyword
                    ))
                    .get(SearchFragmentViewModel::class.java)
}