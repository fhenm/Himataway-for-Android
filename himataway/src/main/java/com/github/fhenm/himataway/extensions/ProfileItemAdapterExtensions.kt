package com.github.fhenm.himataway.extensions

import android.support.v4.app.FragmentActivity
import com.github.fhenm.himataway.adapter.DataItemAdapter
import com.github.fhenm.himataway.fragment.dialog.StatusMenuFragment
import com.github.fhenm.himataway.listener.StatusLongClickListener
import com.github.fhenm.himataway.model.Row

fun DataItemAdapter<Row>.applyTapEvents(activity:FragmentActivity) : DataItemAdapter<Row> {
    this.onItemClickListener = { row ->
        StatusMenuFragment.newInstance(row)
                .show(activity.getSupportFragmentManager(), "dialog")
    }

    this.onItemLongClickListener = { row ->
        StatusLongClickListener.handleRow(activity, row)
    }

    return this
}