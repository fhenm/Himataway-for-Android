package com.github.fhenm.himataway.fragment.profile

import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import de.greenrobot.event.EventBus
import com.github.fhenm.himataway.adapter.DataItemAdapter
import com.github.fhenm.himataway.adapter.RecyclerTweetAdapter
import com.github.fhenm.himataway.event.action.StatusActionEvent
import com.github.fhenm.himataway.event.model.StreamingDestroyStatusEvent
import com.github.fhenm.himataway.extensions.getTwitterRepo
import com.github.fhenm.himataway.fragment.common.ListBasedFragment
import com.github.fhenm.himataway.fragment.dialog.StatusMenuFragment
import com.github.fhenm.himataway.listener.StatusLongClickListener
import com.github.fhenm.himataway.model.Row
import com.github.fhenm.himataway.viewmodel.UserTimelineFragmentViewModel
import twitter4j.Status
import twitter4j.User

/**
 * ユーザーのタイムライン
 */
class UserTimelineFragment : ListBasedFragment<Row, Long, Status, Long, UserTimelineFragmentViewModel>() {
    override val id: Long
        get() = (arguments.getSerializable("user") as User).id

    override fun createViewModel(userId: Long): UserTimelineFragmentViewModel =
            ViewModelProviders
                    .of(this, UserTimelineFragmentViewModel.Factory(
                            this.getTwitterRepo(),
                            userId
                    ))
                    .get(UserTimelineFragmentViewModel::class.java)

    override fun createAdapter(): DataItemAdapter<Row> =
            RecyclerTweetAdapter(activity, ArrayList())

    override fun convertDataToViewItem(dataItem: Status): Row = Row.newStatus(dataItem)

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = super.onCreateView(inflater, container, savedInstanceState)

        adapter.onItemClickListener = { row ->
            StatusMenuFragment.newInstance(row)
                    .show(activity.getSupportFragmentManager(), "dialog")
        }

        adapter.onItemLongClickListener = { row ->
            StatusLongClickListener.handleRow(activity, row)
        }

        return v
    }

    override fun onResume() {
        super.onResume()
        EventBus.getDefault().register(this)
    }

    override fun onPause() {
        EventBus.getDefault().unregister(this)
        super.onPause()
    }

    fun onEventMainThread(event: StatusActionEvent) {
        adapter.notifyDataSetChanged()
    }

    fun onEventMainThread(event: StreamingDestroyStatusEvent) {
        adapter.remove(event.statusId)
    }
}
