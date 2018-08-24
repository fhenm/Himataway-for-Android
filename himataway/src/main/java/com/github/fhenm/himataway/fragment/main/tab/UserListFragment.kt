package com.github.fhenm.himataway.fragment.main.tab

import android.os.AsyncTask
import android.os.Bundle
import android.support.v4.util.LongSparseArray
import android.view.View

import com.github.fhenm.himataway.model.Row
import com.github.fhenm.himataway.model.TwitterManager
import com.github.fhenm.himataway.settings.BasicSettings
import twitter4j.Paging
import twitter4j.ResponseList
import twitter4j.Status

class UserListFragment : BaseFragment() {

    /**
     * このタブを表す固有のID
     */
    private var mUserListId = 0L

    private val mMembers = LongSparseArray<Boolean>()

    override val tabId: Long
        get() = mUserListId

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        if (mUserListId == 0L) {
            mUserListId = arguments!!.getLong("userListId")
        }
        super.onActivityCreated(savedInstanceState)
    }

    override fun isSkip(row: Row): Boolean {
        return mMembers.get(row.status.user.id) == null
    }

    override fun taskExecute() {
        UserListStatusesTask().execute()
    }

    private inner class UserListStatusesTask : AsyncTask<Void, Void, ResponseList<Status>>() {
        override fun doInBackground(vararg params: Void): ResponseList<twitter4j.Status>? {
            try {
                val twitter = TwitterManager.getTwitter()
                val paging = Paging()
                if (mMaxId > 0 && !mReloading) {
                    paging.maxId = mMaxId - 1
                    paging.count = BasicSettings.getPageCount()
                } else {
                    val members = twitter.getUserListMembers(mUserListId, 0)
                    for (user in members) {
                        mMembers.append(user.id, true)
                    }
                }
                return twitter.getUserListStatuses(mUserListId, paging)
            } catch (e: OutOfMemoryError) {
                return null
            } catch (e: Exception) {
                e.printStackTrace()
                return null
            }

        }

        override fun onPostExecute(statuses: ResponseList<twitter4j.Status>?) {
            mFooter.visibility = View.GONE
            if (statuses == null || statuses.size == 0) {
                mReloading = false
                mPullToRefreshLayout.isRefreshing = false
                setListViewVisible(true)
                return
            }
            if (mReloading) {
                clear()
                for (status in statuses) {
                    if (mMaxId <= 0L || mMaxId > status.id) {
                        mMaxId = status.id
                    }
                    mAdapter!!.add(Row.newStatus(status))
                }
                mReloading = false
            } else {
                for (status in statuses) {
                    if (mMaxId <= 0L || mMaxId > status.id) {
                        mMaxId = status.id
                    }

                    // 最初のツイートに登場ユーザーをStreaming APIからの取り込み対象にすることでAPI節約!!!
                    mMembers.append(status.user.id, true)

                    mAdapter!!.extensionAdd(Row.newStatus(status))
                }
                mAutoLoader = true
                setListViewVisible(true)
            }
            mAdapter.notifyDataSetChanged()
            mPullToRefreshLayout.isRefreshing = false
        }
    }
}
