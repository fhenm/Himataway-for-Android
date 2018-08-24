package com.github.fhenm.himataway.fragment.main.tab

import android.os.AsyncTask
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.github.fhenm.himataway.model.AccessTokenManager
import com.github.fhenm.himataway.model.Row
import com.github.fhenm.himataway.model.TabManager
import com.github.fhenm.himataway.model.TwitterManager
import com.github.fhenm.himataway.settings.BasicSettings
import twitter4j.Paging
import twitter4j.ResponseList
import twitter4j.Status

/**
 * タイムライン、すべての始まり
 */
class TimelineFragment : BaseFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = super.onCreateView(inflater!!, container, savedInstanceState)
        firstLoad()
        return view
    }

    /**
     * このタブを表す固有のID、ユーザーリストで正数を使うため負数を使う
     */
    override val tabId: Long
        get() = TabManager.TIMELINE_TAB_ID

    /**
     * このタブに表示するツイートの定義
     * @param row ストリーミングAPIから受け取った情報（ツイート）
     * @return trueは表示しない、falseは表示する
     */
    override fun isSkip(row: Row): Boolean {
        if (row.isStatus) {
            val retweet = row.status.retweetedStatus
            return retweet != null && retweet.user.id == AccessTokenManager.getUserId()
        } else {
            return true
        }
    }

    override fun taskExecute() {
        HomeTimelineTask().execute()
    }

    private inner class HomeTimelineTask : AsyncTask<Void, Void, ResponseList<Status>>() {
        override fun doInBackground(vararg params: Void): ResponseList<twitter4j.Status>? {
            try {
                val paging = Paging()
                if (mMaxId > 0 && !mReloading) {
                    paging.maxId = mMaxId - 1
                    paging.count = BasicSettings.getPageCount()
                }
                return TwitterManager.getTwitter().getHomeTimeline(paging)
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
